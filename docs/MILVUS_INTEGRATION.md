# Milvus 向量数据库集成指南

## 📖 概述

本项目已将 RAG 检索从**纯内存余弦计算**升级为 **Milvus 向量数据库** 优先检索。

### 架构对比

```
改进前 (内存检索):
┌──────────┐     ┌──────────────────────┐     ┌──────────────┐
│  查询     │ ──▶ │  遍历所有 kb_chunk    │ ──▶ │  内存余弦计算  │
└──────────┘     │  (O(n) 全量扫描)      │     └──────────────┘
                 └──────────────────────┘

改进后 (Milvus 检索):
┌──────────┐     ┌──────────────────┐     ┌─────────────────┐
│  查询     │ ──▶ │  Milvus ANN 检索  │ ──▶ │  O(log n) 命中  │
└──────────┘     │  (近似最近邻搜索)   │     └─────────────────┘
                 └──────────────────┘
        │
        └──▶ 失败时自动降级到内存余弦检索
```

## 🚀 部署 Milvus

### 方案一：Docker 部署（推荐）

```bash
# 拉取 Milvus 镜像
docker pull milvusdb/milvus:v2.6.4

# 创建数据目录
mkdir -p ~/milvus/data

# 启动 Milvus Standalone
docker run -d \
  --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  -v ~/milvus/data:/var/lib/milvus \
  milvusdb/milvus:v2.6.4
```

### 方案二：Milvus Lite（嵌入式，开发环境）

```bash
# Milvus Lite 无需独立服务，随应用一起运行
# 适合本地开发和测试
```

### 方案三：Zilliz Cloud（云端托管）

1. 访问 [Zilliz Cloud](https://cloud.zilliz.com)
2. 创建免费集群
3. 获取连接信息和 API Key

## ⚙️ 项目配置

### application.yml

```yaml
milvus:
  enabled: ${MILVUS_ENABLED:true}          # 是否启用 Milvus
  host: ${MILVUS_HOST:localhost}            # 服务地址
  port: ${MILVUS_PORT:19530}                # 服务端口
  db-name: default                          # 数据库名
  collection-name: kb_chunk_vectors         # 集合名称
  dimension: 1536                           # 向量维度
  metric-type: COSINE                       # 度量方式
  index-type: AUTOINDEX                     # 索引类型
  connect-timeout-ms: 5000                  # 连接超时
```

### 环境变量

```bash
export MILVUS_ENABLED=true
export MILVUS_HOST=localhost
export MILVUS_PORT=19530
```

## 🏗️ 代码架构

### 文件结构

```
com.expert.config/
├── MilvusProperties.java      # Milvus 配置属性
└── MilvusConfig.java          # 客户端初始化 + 集合管理 + 集合重建

com.expert.ai/
├── MilvusVectorStore.java     # 向量 CRUD 封装
└── RagService.java            # RAG 检索（Milvus + 降级）
    private String dbName = "default";
    private String collectionName = "kb_chunk_vectors";
    private int dimension = 1536;
    private long connectTimeoutMs = 5000;
    private String metricType = "COSINE";
    private String indexType = "AUTOINDEX";
}
```

#### MilvusConfig
```java
// 初始化 MilvusServiceClient 和集合
// 与 @Bean 不同，启动时尝试连接，失败时自动降级到内存检索
@Configuration
public class MilvusConfig {

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("Milvus 已禁用，使用内存检索");
            return;
        }
        try {
            ConnectParam param = ConnectParam.newBuilder()
                .withHost(properties.getHost())
                .withPort(properties.getPort())
                .withDatabaseName(properties.getDbName())
                .withConnectTimeout(properties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
            MilvusServiceClient client = new MilvusServiceClient(param);
            clientRef.set(client);
            ensureCollection(client);      // 确保集合存在
            available.set(true);
        } catch (Exception e) {
            available.set(false);          // 降级到内存检索
        }
    }

    public boolean rebuildCollection() { ... }        // 释放+删除+重建
    public boolean rebuildWithNewDimension(int dim) { ... }
}
```

#### MilvusVectorStore
```java
// 向量操作封装
@Component
public class MilvusVectorStore {
    public boolean upsert(Long id, Long groupId, Long documentId, int chunkIndex, String content, List<Float> vector);
    public boolean batchUpsert(List<VectorEntity> entities);   // 批量写入
    public List<VectorSearchResult> search(float[] queryVector, Long groupId, int topK);
    public boolean deleteByDocumentId(Long documentId);
    public boolean deleteByGroupId(Long groupId);
    public long count();
}
```

## 📊 Milvus 集合设计

### 字段定义

| 字段名      | 类型       | 说明           |
|-------------|------------|----------------|
| id          | Int64 (PK) | 主键，对应 kb_chunk.id |
| groupId     | Int64      | 知识分组 ID    |
| documentId  | Int64      | 文档 ID        |
| chunkIndex  | Int64      | 块索引         |
| content     | VarChar    | 文本内容 (max 65535) |
| embedding   | FloatVector| 向量 (dim=1536)|

### 索引配置

- **索引类型**: AUTOINDEX (Milvus 自动选择最优索引)
- **度量方式**: COSINE (余弦相似度)
- **搜索性能**: 支持大规模数据集 (百万级向量) 毫秒级响应

## 🔄 降级机制

当 Milvus 不可用时，RAG 检索自动降级：

```java
// RagService 中的核心逻辑
public List<KbChunk> search(String query, Long groupId, int topK) {
    float[] queryVector = hospitalAiService.embed(query);
    
    if (isMilvusAvailable()) {
        var results = milvusStore.search(queryVector, groupId, topK);
        if (!results.isEmpty()) {
            return results;
        }
    }
    
    // 降级：内存余弦检索
    return memorySearch(queryVector, groupId, topK);
}
```

### 降级触发条件

1. `milvus.enabled=false`
2. Milvus 服务不可用
3. Milvus 检索结果为空

## 🧪 测试

### 连接测试

```bash
# 检查 Milvus 是否可用
docker exec milvus-standalone curl localhost:19530/healthz
```

### 集成测试

```java
@SpringBootTest
class RagServiceIntegrationTest {
    
    @Autowired
    private RagService ragService;
    
    @Test
    void testMilvusSearch() {
        String query = "头痛发热";
        var results = ragService.search(query, null, 5);
        assertFalse(results.isEmpty());
    }
    
    @Test
    void testMilvusAvailable() {
        var store = ragService.getMilvusStore();
        assertTrue(store.isPresent());
    }
}
```

## 📈 性能对比

| 指标         | 内存检索       | Milvus 检索    | 提升幅度    |
|--------------|----------------|----------------|-------------|
| 10K 向量     | ~50ms          | ~1ms           | 50x         |
| 100K 向量    | ~500ms         | ~2ms           | 250x        |
| 1M 向量      | ~5000ms        | ~5ms           | 1000x       |
| 内存占用     | O(n) 全量载入  | 索引+磁盘      | 大幅降低    |

## 🔒 安全建议

1. **网络隔离**: Milvus 端口不要暴露到公网
2. **认证配置**: 生产环境启用 Milvus 认证
3. **数据备份**: 定期备份 Milvus 数据目录
4. **资源限制**: 配置容器内存和 CPU 限制

## 📋 常见问题

### Q: Milvus 连接失败怎么办？
A: 检查以下内容：
- Milvus 服务是否启动 (`docker ps`)
- 端口是否开放 (19530)
- 配置的 host:port 是否正确

### Q: 向量维度不匹配？
A: 确认嵌入模型的输出维度与 `milvus.dimension` 配置一致。

### Q: 如何切换到内存模式？
A: 设置 `milvus.enabled=false`，系统自动降级到内存检索。

### Q: 切换 Embedding 模型后维度不一致怎么办？
A: 调用 `POST /api/admin/kb/rebuild`，系统会自动：
1. 更新 Milvus 维度配置
2. 释放 + 删除 + 按新维度重建集合
3. 遍历所有文档重新分块和嵌入

详见 `docs/VECTOR_REBUILD_TASK.md`。

### Q: 数据量增长后性能下降？
A: Milvus 支持水平扩展，可以：
- 增加分片数 (shardsNum)
- 使用分布式部署
- 使用 Zilliz Cloud 托管

---

**集成版本**: 2.6.25  
**更新时间**: 2024年9月
