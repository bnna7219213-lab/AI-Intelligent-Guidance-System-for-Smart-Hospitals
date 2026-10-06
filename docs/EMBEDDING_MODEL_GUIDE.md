# Embedding 模型切换与 Milvus 维度同步指南

## 📌 概述

当从默认的 `text-embedding-3-small` 切换到 BGE 或其他 Embedding 模型时，**必须同步调整 Milvus 集合的向量维度**。原因如下：

1. **向量空间不兼容**：不同模型的输出向量语义不同，即使维度相同也不能混用
2. **集合结构固定**：Milvus 集合在创建时指定维度，之后不可修改
3. **必须重建数据**：切换模型后需删除旧向量，用新模型重新嵌入全部文档

本指南提供完整的操作步骤、代码示例和维度对照表。

---

## 📊 Embedding 模型维度对照表

| 模型名称 | 维度 | 提供商 | 最大输入 Token | 适用场景 |
|---------|------|--------|---------------|---------|
| text-embedding-3-small | **1536** | OpenAI | 8191 | 通用，性价比高 |
| text-embedding-3-large | **3072** | OpenAI | 8191 | 高精度检索 |
| bge-large-zh-v1.5 | **1024** | BAAI | 512 | 中文场景优先 |
| bge-base-zh-v1.5 | **768** | BAAI | 512 | 中文轻量级 |
| bge-small-zh-v1.5 | **384** | BAAI | 512 | 中文超轻量 |
| bge-m3 | **1024** | BAAI | 8192 | 多语言 + 长文本 |
| m3e-large | **1024** | MokaAI | 512 | 中文 + 英文 |
| conch-embedding-text | **1024** | SenseTime | 2048 | 多模态（商汤） |
| text-embedding-v2 | **1536** | DashScope | 2048 | 阿里云通义 |
| jina-embeddings-v3 | **1024** | JinaAI | 8192 | 多语言高精度 |

> **注意**：`bge-*-zh` 系列模型的最大输入为 512 token，如果知识库分块较大（如 500 字符中文），可能被截断。建议使用 `bge-m3` 或降低分块大小。

---

## ⚙️ 项目配置结构

### 当前架构（默认 text-embedding-3-small, dim=1536）

```
┌─────────────────────────────────────────────────────────┐
│ application.yml                                         │
│                                                         │
│ milvus:                                                 │
│   dimension: 1536          ← 必须与模型输出维度一致      │
│   collection-name: kb_chunk_vectors                     │
│                                                         │
│ ai-config (数据库表 ai_config):                          │
│   config_key: VECTOR_MODEL                              │
│   model_name: text-embedding-3-small                    │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ Milvus 集合: kb_chunk_vectors                           │
│                                                         │
│ 字段:                                                    │
│   id          (Int64, PK)                               │
│   groupId     (Int64)                                   │
│   documentId  (Int64)                                   │
│   chunkIndex  (Int64)                                   │
│   content     (VarChar)                                 │
│   embedding   (FloatVector, **dim=1536**)  ← 固定维度   │
└─────────────────────────────────────────────────────────┘
```

### 切换后架构（例如 bge-large-zh-v1.5, dim=1024）

```
┌─────────────────────────────────────────────────────────┐
│ application.yml                                         │
│                                                         │
│ milvus:                                                 │
│   dimension: 1024          ← 调整为 BGE 的维度          │
│   collection-name: kb_chunk_vectors_bge                 │
│                                                         │
│ ai-config (数据库表 ai_config):                          │
│   config_key: VECTOR_MODEL                              │
│   model_name: bge-large-zh-v1.5                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ Milvus 集合: kb_chunk_vectors_bge                       │
│                                                         │
│ 字段:                                                    │
│   id          (Int64, PK)                               │
│   groupId     (Int64)                                   │
│   documentId  (Int64)                                   │
│   chunkIndex  (Int64)                                   │
│   content     (VarChar)                                 │
│   embedding   (FloatVector, **dim=1024**)  ← 新维度     │
└─────────────────────────────────────────────────────────┘
```

---

## 🔄 切换步骤（完整流程）

### 方式一：通过环境变量切换（推荐生产环境）

**Step 1**：设置环境变量覆盖 Milvus 维度

```bash
# 以切换到 bge-large-zh-v1.5 为例
export MILVUS_DIMENSION=1024

# 可选：换集合名避免数据混乱
export MILVUS_COLLECTION=kb_chunk_vectors_bge
```

**Step 2**：修改 ai_config 表中的模型配置

```sql
UPDATE ai_config 
SET model_name = 'bge-large-zh-v1.5',
    api_url = 'http://your-bge-endpoint:8000/v1',
    api_key = 'your-api-key',
    extra_config = '{"dimension":1024,"provider":"BAAI"}',
    update_time = NOW()
WHERE config_key = 'VECTOR_MODEL';
```

**Step 3**：重启应用，自动创建新维度集合

```bash
# MilvusConfig 会检测到新维度，自动创建新集合
# 如果使用同名集合但维度不同，会报错并需手动删除
```

**Step 4**：重新向量化知识库

调用以下接口或脚本触发全量重建：

```bash
# 遍历所有文档，重新分块和嵌入
POST /api/admin/kb/documents/{documentId}/chunk
```

---

### 方式二：通过配置文件切换（开发环境）

修改 `application.yml`：

```yaml
milvus:
  enabled: true
  host: localhost
  port: 19530
  collection-name: kb_chunk_vectors
  dimension: 1024          # ← 修改为 BGE 的维度
  metric-type: COSINE
  index-type: AUTOINDEX
```

然后执行上述 Step 2 ~ Step 4。

---

### 方式三：使用 Demo 代码自动识别维度（推荐）

项目中已提供 `EmbeddingModelSwitchDemo` 类，可通过以下方式使用：

```java
// 1. 自动识别维度并检查是否一致
embeddingModelSwitchDemo.autoSyncDimensionFromConfig("bge-large-zh-v1.5");
// 输出: 识别维度: 1024, 当前 Milvus 维度: 1536
// 输出: ⚠️ 维度不一致，需要重建集合！

// 2. 查看完整切换流程
embeddingModelSwitchDemo.switchToBgeLarge();

// 3. 验证向量维度是否匹配
embeddingModelSwitchDemo.verifyVectorDimension("测试文本");
// 输出: ✅ 维度匹配 或 ❌ 维度不匹配！
```

---

## 🏗️ 维度不匹配时的处理方案

### 方案 A：重建集合（推荐）

不同模型的向量空间完全不同，最正确的做法是**删除旧集合，用新维度重建，并重新嵌入所有文档**。

```java
// 伪代码：重建集合的完整流程
public void rebuildCollection(int newDimension) {
    // 1. 释放并删除旧集合
    milvusClient.releaseCollection(oldCollection);
    milvusClient.dropCollection(oldCollection);
    
    // 2. 设置新维度
    milvusProperties.setDimension(newDimension);
    
    // 3. 创建新集合（MilvusConfig.ensureCollection 自动完成）
    milvusClient.createCollection(newCollectionParam);
    milvusClient.createIndex(indexParam);
    milvusClient.loadCollection(loadParam);
    
    // 4. 重新嵌入所有文档
    kbService.reEmbedAllDocuments();
}
```

**优点**：向量空间纯净，检索精度最优  
**缺点**：需要重新调用 embedding 模型，消耗 API 费用和时间

---

### 方案 B：降维投影（快速但损失精度）

如果不想重建，可以通过 PCA 或随机投影将高维向量压缩到目标维度。

```java
// Demo 中的降维示例（简化版）
public float[] projectToTargetDimension(float[] source, int targetDim) {
    if (source.length > targetDim) {
        // 降维：取前 targetDim 维（PCA 简化版，精度损失较大）
        return Arrays.copyOf(source, targetDim);
    } else {
        // 升维：补零
        float[] result = new float[targetDim];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }
}
```

**优点**：无需重建集合  
**缺点**：精度损失明显，不同模型向量语义不兼容，不推荐

---

### 方案 C：使用新集合名并行运行（灰度切换）

```yaml
# 旧集合保持不动
milvus.collection-name: kb_chunk_vectors_bge  # 新集合
```

先在**新集合**中重建向量，验证效果后再切换，避免停机。

**优点**：可灰度验证、可回滚  
**缺点**：需要双份存储空间

---

## 🧪 维度验证与自检

### 启动时自动验证

在 `MilvusConfig.init()` 中添加维度检查逻辑：

```java
@PostConstruct
public void init() {
    // ...连接逻辑...
    
    // 验证维度配置是否与 embedding 模型一致
    AiConfig vectorConfig = aiConfigService.getVectorModelConfig();
    int modelDim = resolveDimensionFromModel(vectorConfig.getModelName());
    if (modelDim != properties.getDimension()) {
        log.warn("⚠️ 维度不匹配! 模型={}({}维), 配置={}维",
            vectorConfig.getModelName(), modelDim, properties.getDimension());
    }
}
```

### 运行时验证

```java
// Demo 方法: verifyVectorDimension
float[] vector = hospitalAiService.embed("测试文本");
int actualDim = vector.length;
int expectedDim = milvusProperties.getDimension();

if (actualDim == expectedDim) {
    log.info("✅ 维度匹配");
} else {
    log.error("❌ 模型输出{}维，配置{}维，检索将失败", actualDim, expectedDim);
}
```

---

## 🧬 BGE 模型本地部署示例

### 方式一：使用 FlagEmbedding 本地推理

```bash
# 安装依赖
pip install FlagEmbedding

# 启动本地推理服务 (兼容 OpenAI API)
python -m FlagEmbedding.reranker_server \
    --model_name bge-large-zh-v1.5 \
    --port 8000
```

### 方式二：使用 Transformers 手动推理

```python
from transformers import AutoTokenizer, AutoModel
import torch
import numpy as np

model_name = "BAAI/bge-large-zh-v1.5"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModel.from_pretrained(model_name)

def embed(text):
    encoded = tokenizer(text, padding=True, truncation=True, max_length=512)
    with torch.no_grad():
        output = model(**encoded)
    vector = output[0][0].numpy()  # 1024 维
    return vector

# 输出维度
print(embed("头痛发热").shape)  # (1024,)
```

### 方式三：使用 Sentence-Transformers

```python
from sentence_transformers import SentenceTransformer

model = SentenceTransformer('BAAI/bge-large-zh-v1.5')
vector = model.encode("头痛发热")
print(vector.shape)  # (1024,)
```

### 接入本项目（OpenAI 兼容接口）

BGE 模型可以通过 **Xinference**、**vLLM** 或 **TEI (Text Embedding Inference)** 暴露为 OpenAI 兼容接口：

```bash
# 使用 TEI 部署
docker run -d --gpus all \
  -p 8000:80 \
  -v ~/bge-model:/model \
  ghcr.io/huggingface/text-embeddings-inference:latest \
  --model-id BAAI/bge-large-zh-v1.5

# 然后修改 ai_config 中的 api_url 为
# http://localhost:8000/v1
```

---## 🔌 其他模型的接入方式

### 1. 阿里云通义 text-embedding-v2 (1536 维)

```yaml
# application.yml 中 milvus 配置不变（维度相同）
milvus:
  dimension: 1536
```

```sql
UPDATE ai_config SET
  model_name = 'text-embedding-v2',
  api_url = 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  api_key = 'sk-your-dashscope-key',
  update_time = NOW()
WHERE config_key = 'VECTOR_MODEL';
```

### 2. SenseTime 商量 conch-embedding-text (1024 维)

```yaml
milvus:
  dimension: 1024
```

```sql
UPDATE ai_config SET
  model_name = 'conch-embedding-text',
  api_url = 'https://your-conch-endpoint/v1',
  api_key = 'your-conch-api-key',
  update_time = NOW()
WHERE config_key = 'VECTOR_MODEL';
```

### 3. 多语言 bge-m3 (1024 维，支持 8192 token)

```yaml
milvus:
  dimension: 1024
```

```sql
UPDATE ai_config SET
  model_name = 'bge-m3',
  api_url = 'http://localhost:8000/v1',
  update_time = NOW()
WHERE config_key = 'VECTOR_MODEL';
```

> 切换到 bge-m3 后，可适当增大 RagService 中的 `DEFAULT_CHUNK_SIZE`（如 800~1000），因为 bge-m3 支持更长输入。

---

## 🔧 RagService 分块参数调整建议

不同模型的最大输入 Token 不同，需要同步调整分块大小：

| 模型 | 最大输入 | 建议分块大小(中文字符) | 建议重叠 |
|------|---------|---------------------|---------|
| text-embedding-3-small | 8191 | 500 | 50 |
| text-embedding-3-large | 8191 | 500 | 50 |
| bge-large-zh-v1.5 | 512 | **300~400** | 30~50 |
| bge-base-zh-v1.5 | 512 | **300~400** | 30~50 |
| bge-m3 | 8192 | 800~1000 | 80~100 |
| m3e-large | 512 | **300~400** | 30~50 |
| conch-embedding-text | 2048 | 500~600 | 50 |

修改方式：

```java
// RagService.java
private static final int DEFAULT_CHUNK_SIZE = 400;  // 改为 bge-large 推荐值
private static final int DEFAULT_OVERLAP = 40;
```

---

## ✅ 切换检查清单

### 切换前
- [ ] 确认新模型的**输出维度**
- [ ] 确认新模型的**最大输入 Token**
- [ ] 备份知识库原始文档（防止重新嵌入时失败）
- [ ] 确认 embedding API 服务可用

### 切换中
- [ ] 更新 `milvus.dimension` 配置
- [ ] 更新 `ai_config` 表中的模型配置
- [ ] 重启应用（Milvus 集合自动重建）
- [ ] 触发全量重新嵌入

### 切换后验证
- [ ] 启动日志中检查维度匹配（无 `⚠️ 维度不匹配` 警告）
- [ ] 调用一次搜索接口，确认返回结果
- [ ] 抽样检查搜索结果的相关性
- [ ] 对比新旧模型的检索命中率（TriageHitRate）
- [ ] 确认向量数量与文档分块数量一致

---

## 📝 常见问题

### Q1: 为什么切换模型后检索结果变差？
**原因**：新模型的向量语义分布不同，需要重新嵌入所有文档。即使旧向量还在 Milvus 中，用新模型生成的查询向量也无法与旧向量正确匹配。
**解决**：必须执行全量重新嵌入。

### Q2: 可以直接改维度不重建集合吗？
**不能**。Milvus 集合创建时维度固定，无法原地修改。必须删除并重建集合。

### Q3: 切换后 Milvus 中还有旧数据怎么办？
**处理**：删除集合时旧数据一并删除。如需保留，使用方案 C（新集合名并行运行）。

### Q4: BGE 模型支持 512 token，我的分块 500 字符会不会被截断？
**可能**。中文 1 字符约 1 token，500 字符约 500 token，接近上限。建议把分块调整为 300~400 字符。

### Q5: 有没有不需要重建集合的模型切换方式？
**没有完美方案**。维度不同必须重建。维度相同（如 1536）切换模型虽不用重建集合，但旧向量与新模型查询向量语义不兼容，检索精度会大幅下降，仍建议重新嵌入。

---## 📁 相关文件

| 文件 | 用途 |
|------|------|
| `backend/.../config/MilvusProperties.java` | Milvus 配置属性（含 `dimension`） |
| `backend/.../config/MilvusConfig.java` | Milvus 客户端 + 集合管理 |
| `backend/.../ai/MilvusVectorStore.java` | 向量 CRUD 操作 |
| `backend/.../ai/RagService.java` | 双模式 RAG 检索 |
| `backend/.../demo/EmbeddingModelSwitchDemo.java` | **模型切换 Demo**（本指南配套） |
| `docs/MILVUS_INTEGRATION.md` | Milvus 集成指南 |

---

## 🚀 快速切换示例（以 bge-large-zh-v1.5 为例）

### 完整命令序列

```bash
# 1. 部署 BGE 模型服务（TEI）
docker run -d --gpus all \
  -p 8000:80 \
  -v ~/bge-large:/model \
  ghcr.io/huggingface/text-embeddings-inference:latest \
  --model-id BAAI/bge-large-zh-v1.5

# 2. 设置环境变量
export MILVUS_DIMENSION=1024
export MILVUS_COLLECTION=kb_chunk_vectors

# 3. 更新数据库配置
mysql -u root -p ai_hospital -e "
UPDATE ai_config SET 
  model_name='bge-large-zh-v1.5',
  api_url='http://localhost:8000/v1',
  api_key='', 
  extra_config='{\"dimension\":1024,\"provider\":\"BAAI\"}',
  update_time=NOW()
WHERE config_key='VECTOR_MODEL';"

# 4. 重启应用
mvn spring-boot:run

# 5. 触发全量重新嵌入（通过管理接口或脚本）
curl -X POST http://localhost:8080/api/admin/kb/reembed-all
```

### 验证

```bash
# 查看启动日志，应看到：
# "Milvus 连接成功，集合: kb_chunk_vectors (dim=1024)"
# "✅ 维度匹配，检索功能正常"

# 调用搜索接口测试
curl -X POST http://localhost:8080/api/patient/triage \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"symptom":"头痛发热三天"}'
```

---

## 🔒 安全注意事项

1. **API Key 保护**：embedding 模型的 API Key 不要硬编码在代码中，使用环境变量
2. **内网部署**：BGE 等本地模型建议部署在内网，避免公网暴露
3. **数据一致性**：切换期间建议只读，完成重新嵌入后再开放写操作
4. **备份**：切换前务必备份 `kb_document` 表（原始文档内容）

---

## 📈 性能对比参考

| 模型 | 维度 | 单条嵌入耗时 | 10K 文档索引耗时 | 检索延迟 |
|------|------|------------|----------------|---------|
| text-embedding-3-small | 1536 | ~30ms | ~5min | ~1ms |
| text-embedding-3-large | 3072 | ~60ms | ~10min | ~2ms |
| bge-large-zh-v1.5 | 1024 | ~20ms (GPU) | ~3min | <1ms |
| bge-base-zh-v1.5 | 768 | ~15ms (GPU) | ~2.5min | <1ms |
| bge-m3 | 1024 | ~25ms (GPU) | ~4min | <1ms |

> 以上为估算值，实际性能取决于硬件、网络和并发。

---

## 📋 参考链接

- [BGE 官方模型卡 (BAAI)](https://huggingface.co/BAAI/bge-large-zh-v1.5)
- [OpenAI Embedding 模型文档](https://platform.openai.com/docs/guides/embeddings)
- [Milvus 官方文档](https://milvus.io/docs)
- [Text Embedding Inference (TEI)](https://github.com/huggingface/text-embeddings-inference)
- [FlagEmbedding 项目](https://github.com/FlagOpen/FlagEmbedding)

---

**文档版本**: v1.0  
**更新日期**: 2024年9月  
**适用版本**: Milvus SDK 2.6.25+ / Spring Boot 4.x