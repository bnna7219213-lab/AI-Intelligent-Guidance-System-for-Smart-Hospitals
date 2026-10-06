package com.expert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Milvus 向量数据库配置属性
 * 由 MilvusConfig 通过 @EnableConfigurationProperties 注册
 */
@Data
@Component
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    /**
     * 是否启用 Milvus
     * 设置为 false 时自动回退到内存余弦检索
     */
    private boolean enabled = true;

    /**
     * Milvus 服务地址 (host:port)
     * 示例: localhost:19530
     */
    private String host = "localhost";

    /**
     * Milvus 服务端口
     */
    private int port = 19530;

    /**
     * 数据库名称
     */
    private String dbName = "default";

    /**
     * 集合名称
     */
    private String collectionName = "kb_chunk_vectors";

    /**
     * 向量维度（应与嵌入模型输出一致）
     */
    private int dimension = 1536;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectTimeoutMs = 5000;

    /**
     * 索引类型: IVF_FLAT / HNSW / AUTOINDEX
     */
    private String indexType = "AUTOINDEX";

    /**
     * 度量类型: IP / L2 / COSINE
     */
    private String metricType = "COSINE";
}
