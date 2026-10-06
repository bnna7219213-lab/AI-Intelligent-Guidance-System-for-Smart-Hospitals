# 向量索引自动重建后台任务

## 📌 概述

本模块实现了 **"自动重建集合 + 全量重新嵌入"** 的完整后台任务系统。当管理员切换 Embedding 模型后（维度变化），系统可以自动检测差异并触发异步重建，无需人工干预。

### 核心能力

- ✅ 自动检测模型维度与 Milvus 集合维度是否匹配
- ✅ 优雅降级（单文档失败不中断全量任务）

---

## 🏗️ 架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                      AdminController                                 │
│  POST /admin/kb/rebuild           → 触发重建                         │
│  GET  /admin/kb/rebuild/status    → 查询进度                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   VectorIndexRebuildService                          │
│                                                                     │
│  checkDimensionAndRebuild()     ← 检查维度+触发重建                   │
│  startRebuildAsync()            ← 启动后台线程                        │
│  executeRebuild()               ← 异步执行主流程                      │
│  getCurrentStatus()             ← 查询当前状态                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                ┌──────────────┼──────────────────┐
                ▼              ▼                  ▼
    ┌─────────────────┐ ┌────────────┐ ┌────────────────┐
    │  MilvusConfig   │ │ RagService │ │ KbDocumentMapper│
    │  重建集合        │ │ 重新嵌入   │ │ 查询文档列表    │
    └─────────────────┘ └────────────┘ └────────────────┘
```

---

## 🔌 API 接口

### 1. 触发重建

```
POST /api/admin/kb/rebuild
```

**响应**：
```json
{
  "code": 200,
  "message": "重建任务已启动",
  "data": {
    "taskId": "a1b2c3d4",
    "status": "RUNNING",
    "targetDimension": 1024,
    "currentDimension": 1536,
    "needRebuildCollection": true,
    "startTime": "2024-09-24T10:30:00"
  }
}
```

**说明**：如果已有任务在运行，返回错误 `"重建任务已在运行中"`。

---

### 2. 查询任务状态

```
GET /api/admin/kb/rebuild/status
```

**响应**（运行中）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "a1b2c3d4",
    "status": "RUNNING",
    "targetDimension": 1024,
    "totalDocuments": 50,
    "processedDocuments": 23,
    "totalChunks": 1200,
    "writtenVectors": 560,
    "currentDocumentId": 42,
    "currentDocumentTitle": "心血管内科指南.pdf",
    "elapsedMs": 34500,
    "progressPercent": 46.0
  }
}
```

**响应**（空闲）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "IDLE",
    "targetDimension": 1024,
    "needRebuildCollection": false
  }
}
```

---

## 🚀 使用流程

### 场景：从 text-embedding-3-small 切换到 bge-large-zh-v1.5

**Step 1**：更新 AI 模型配置

```bash
curl -X PUT http://localhost:8080/api/admin/ai-configs \
  -H "Content-Type: application/json" \
  -d '{
    "configKey": "VECTOR_MODEL",
    "apiUrl": "http://localhost:8000/v1",
    "apiKey": "",
    "modelName": "bge-large-zh-v1.5",
    "extraConfig": "{\"dimension\":1024,\"provider\":\"BAAI\"}"
  }'
```

**Step 2**：设置 Milvus 维度（通过环境变量或配置文件）

```bash
export MILVUS_DIMENSION=1024
# 或修改 application.yml 中 milvus.dimension = 1024
```

**Step 3**：触发重建任务

```bash
curl -X POST http://localhost:8080/api/admin/kb/rebuild \
  -H "Authorization: Bearer <token>"
```

**Step 4**：轮询查询进度

```bash
curl http://localhost:8080/api/admin/kb/rebuild/status \
  -H "Authorization: Bearer <token>"
```

**Step 5**：任务完成后验证检索效果

```bash
curl -X POST http://localhost:8080/api/admin/kb/search \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"query":"头痛发热", "topK":5}'
```

---

## 🔄 自动触发机制

当前系统支持两种触发方式：

### 方式一：手动触发（API）

管理员切换模型后，手动调用 `POST /admin/kb/rebuild`。

### 方式二：启动时自动检查

可以在 `AiHospitalApplication` 中添加启动检查：

```java
@Component
public class StartupVectorCheck implements ApplicationRunner {

    private final VectorIndexRebuildService rebuildService;

    @Override
    public void run(ApplicationArguments args) {
        VectorIndexRebuildStatus status = rebuildService.checkDimensionAndRebuild();
        if (status.isNeedRebuildCollection()) {
            log.info("检测到维度不匹配，自动触发重建");
        }
    }
}
```

### 方式三：定时检查（可选）

```java
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
public void scheduledCheck() {
    rebuildService.checkDimensionAndRebuild();
}
```

---

## 📊 任务状态说明

| 状态 | 含义 |
|------|------|
| IDLE | 空闲，无任务运行 |
| RUNNING | 任务正在执行 |
| SUCCESS | 任务完成成功 |
| FAILED | 任务失败（查看 errorMessage） |

---

## ⚠️ 注意事项

1. **重建期间搜索可能不准确**：任务执行过程中，部分文档已被重新嵌入，部分还是旧向量。建议在低峰期执行。
2. **需保证 Milvus 可用**：如果 Milvus 连接失败，任务会在 Step 1 就失败。
3. **API 消耗**：全量重新嵌入会调用 embedding 模型，消耗 API 配额。
4. **文档量大的话耗时较长**：每个文档需分块+嵌入+写入，10K 文档约需 5~30 分钟。

---

## 📁 相关文件

| 文件 | 用途 |
|------|------|
| `service/impl/VectorIndexRebuildService.java` | **核心服务**（本任务） |
| `vo/VectorIndexRebuildStatus.java` | 任务状态 DTO |
| `config/MilvusConfig.java` | 新增 `rebuildCollection()` 和 `rebuildWithNewDimension()` |
| `ai/RagService.java` | 新增 `deleteDocumentVectors()` 和 `countChunks()` |
| `mapper/KbDocumentMapper.java` | 新增 `findAllActive()` |
| `mapper/KbChunkMapper.java` | 新增 `countByDocumentId()` |
| `controller/AdminController.java` | 新增 2 个管理接口 |

---

## 🧪 测试建议

### 单元测试

```java
@Test
void testDimensionCheck() {
    // 设置 milvusProperties.dimension = 1536
    // 设置 aiConfig.modelName = "bge-large-zh-v1.5"
    // 预期: needRebuildCollection = true
    VectorIndexRebuildStatus status = service.checkDimensionAndRebuild();
    assertTrue(status.isNeedRebuildCollection());
}

@Test
void testRebuildFlow() {
    // 验证: 重建集合 → 清空旧向量 → 重新嵌入
    service.startRebuildAsync();
    // 等待完成后
    VectorIndexRebuildStatus status = service.getCurrentStatus();
    assertEquals("SUCCESS", status.getStatus());
}
```

---

**文档版本**: v1.0  
**更新日期**: 2024年9月