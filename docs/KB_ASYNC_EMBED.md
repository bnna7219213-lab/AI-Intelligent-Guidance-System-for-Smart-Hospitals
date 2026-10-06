# 知识库文档异步向量化

## 📌 概述

本模块实现了 **知识库文档上传后的异步向量化处理**，解决上传大文档时 HTTP 请求被 embedding 调用阻塞的痛点。

### 核心能力

- ✅ 上传文档后**立即返回**（不再等待向量化）
- ✅ 后台线程自动分块 + 嵌入 + 写入 Milvus
- ✅ 支持**多个文档并发**嵌入（每个文档独立线程）
- ✅ 实时进度查询（状态 / 分块数 / 耗时）
- ✅ 重复提交**幂等保护**（同一文档只处理一次）
- ✅ 优雅降级（失败不中断其他文档）
- ✅ 与 `VectorIndexRebuildService` 模式统一，易于维护

---

## 🏗️ 架构

```
┌──────────────────────────────────────────────────────────────┐
│ AdminController                                              │
│                                                              │
│ POST /kb/documents (autoEmbed=true)   ← 上传+自动触发          │
│ POST /kb/documents/{id}/embed-async   ← 手动触发异步           │
│ GET  /kb/documents/{id}/embed/status  ← 查询进度              │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│ KbServiceImpl.docUpload(file, groupId, autoEmbed)            │
│   1. 本地解析文件（快，<1s）                                  │
│   2. 写入 kb_document 表                                     │
│   3. 如果 autoEmbed=true: documentEmbedService.embedDocumentAsync(id)
└──────────────────────────┬───────────────────────────────────┘
                           │ (立即返回，不等待向量化)
                           ▼
┌──────────────────────────────────────────────────────────────┐
│ DocumentEmbedService (异步后台线程)                           │
│                                                              │
│   embedDocumentAsync(id)                                     │
│     └── 启动 daemon 线程 "kb-embed-{id}"                     │
│           ├── 等待事务提交（短重试 10×200ms）                  │
│           ├── deleteDocumentVectors(id)      ← 清理旧向量     │
│           ├── embedAndStore(id, groupId)     ← 分块+嵌入+写入 │
│           ├── 更新 chunkCount                                │
│           └── 更新任务状态 SUCCESS/FAILED                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔌 API 接口

### 1. 上传文档（自动异步向量化）

```
POST /api/admin/kb/documents
```

**参数**：
| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| file | MultipartFile | ✅ | - | 上传文件（txt/pdf/docx） |
| groupId | Long | ✅ | - | 知识分组 ID |
| autoEmbed | Boolean | ❌ | true | 是否上传后自动异步向量化 |

**响应**：
```json
{
  "code": 200,
  "message": "文档上传成功，向量化任务已在后台启动，ID: 42",
  "data": null
}
```

### 2. 手动触发异步向量化

```
POST /api/admin/kb/documents/{id}/embed-async
```

**响应**：
```json
{
  "code": 200,
  "message": "异步向量化任务已启动",
  "data": {
    "status": "PENDING",
    "documentId": 42,
    "documentTitle": "心血管内科指南.pdf",
    "chunkCount": 0,
    "startTime": "2024-09-24T10:30:00"
  }
}
```

### 3. 查询异步向量化进度

```
GET /api/admin/kb/documents/{id}/embed/status
```

**响应**（运行中）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "RUNNING",
    "documentId": 42,
    "documentTitle": "心血管内科指南.pdf",
    "chunkCount": 0,
    "startTime": "2024-09-24T10:30:00",
    "elapsedMs": 5200
  }
}
```

**响应**（完成）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "SUCCESS",
    "documentId": 42,
    "documentTitle": "心血管内科指南.pdf",
    "chunkCount": 18,
    "startTime": "2024-09-24T10:30:00",
    "endTime": "2024-09-24T10:30:12",
    "elapsedMs": 12400
  }
}
```

**响应**（失败）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "FAILED",
    "documentId": 42,
    "documentTitle": "心血管内科指南.pdf",
    "errorMessage": "embedding API 调用超时",
    "endTime": "2024-09-24T10:30:08",
    "elapsedMs": 8300
  }
}
```

---

## 📊 状态说明

| 状态 | 含义 |
|------|------|
| PENDING | 任务已提交，等待线程启动 |
| RUNNING | 正在分块和嵌入 |
| SUCCESS | 向量化完成 |
| FAILED | 向量化失败（查看 errorMessage） |

> **首次查询时的默认行为**：如果某文档从未提交过异步任务，系统会返回当前分块数：
> - 分块数 > 0 → 返回 SUCCESS（已存在向量数据）
> - 分块数 = 0 → 返回 PENDING（尚未向量化）

---

## 🚀 使用流程

### 场景一：上传并自动向量化（推荐）

```bash
curl -X POST http://localhost:8080/api/admin/kb/documents \
  -H "Authorization: Bearer <token>" \
  -F "file=@心血管指南.pdf" \
  -F "groupId=1" \
  -F "autoEmbed=true"
```

**返回**：立即得到文档 ID（例如 42），不等向量化完成。

```bash
# 几秒后查询进度
curl http://localhost:8080/api/admin/kb/documents/42/embed/status \
  -H "Authorization: Bearer <token>"
```

### 场景二：先上传，稍后手动触发

```bash
# 1. 上传（不自动向量化）
curl -X POST http://localhost:8080/api/admin/kb/documents \
  -H "Authorization: Bearer <token>" \
  -F "file=@指南.pdf" \
  -F "groupId=1" \
  -F "autoEmbed=false"

# 2. 后续手动触发异步
curl -X POST http://localhost:8080/api/admin/kb/documents/42/embed-async \
  -H "Authorization: Bearer <token>"
```

### 场景三：保留同步模式（大文档手动控制）

```bash
# 如果需要同步等待结果（例如测试）
curl -X POST http://localhost:8080/api/admin/kb/documents/42/embed \
  -H "Authorization: Bearer <token>"
```

---

## 🧵 并发模型

```
┌─────────────────────────────────────────────────┐
│ ConcurrentHashMap<Long, AtomicReference<Status>> │  ← 状态缓存
│ ConcurrentHashMap<Long, AtomicBoolean>           │  ← 运行锁（防重复提交）
└─────────────────────────────────────────────────┘

Thread: "kb-embed-42"     ← 文档 42 的后台线程（daemon）
Thread: "kb-embed-43"     ← 文档 43 的后台线程（daemon）
```

- **每个文档独立线程**，互不阻塞
- **幂等**：同一文档重复提交会被忽略
- **线程类型**：daemon（随 JVM 退出自动终止）

---

## ⚠️ 注意事项

1. **竞态处理**：`docUpload` 是 `@Transactional`，后台线程启动时可能文档还没提交。服务内置了 10×200ms 的短重试机制等待事务提交。
2. **失败不影响其他文档**：单文档失败只标记 FAILED，不阻塞其他文档的异步任务。
3. **AI API 配额**：多个文档同时嵌入会消耗 embedding API 配额，生产环境建议通过队列限流（Redis + 信号量）。
4. **任务状态存内存**：应用重启后历史任务状态丢失。如需持久化，可将状态写入 `kb_document.embed_status` 字段。

---

## 📁 相关文件

| 文件 | 用途 |
|------|------|
| `service/impl/DocumentEmbedService.java` | **核心服务**（异步嵌入执行器） |
| `vo/DocumentEmbedStatus.java` | 任务状态 DTO |
| `service/KbService.java` | 接口（新增 `docUpload(file, groupId, autoEmbed)`） |
| `service/impl/KbServiceImpl.java` | 实现（上传后自动触发异步） |
| `controller/AdminController.java` | 新增 2 个接口 + 上传接口参数 |

---

**文档版本**: v1.0  
**更新日期**: 2024年9月