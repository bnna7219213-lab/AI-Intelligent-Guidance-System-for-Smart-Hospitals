package com.expert.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.HasCollectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Milvus 客户端管理
 * 
 * 与普通 @Bean 不同，本类在启动时尝试连接：
 * - 连接成功：正常初始化集合，Milvus 检索可用
 * - 连接失败：记录警告并置为不可用，系统自动降级到内存检索
 */
@Slf4j
@Configuration
public class MilvusConfig {

    private final MilvusProperties properties;
    private final AtomicReference<MilvusServiceClient> clientRef = new AtomicReference<>();
    private final AtomicBoolean available = new AtomicBoolean(false);

    public MilvusConfig(MilvusProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("Milvus 已禁用 (milvus.enabled=false)，使用内存检索模式");
            return;
        }

        try {
            log.info("正在连接 Milvus: {}:{}", properties.getHost(), properties.getPort());

            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(properties.getHost())
                    .withPort(properties.getPort())
                    .withDatabaseName(properties.getDbName())
                    .withConnectTimeout(properties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                    .build();

            MilvusServiceClient client = new MilvusServiceClient(connectParam);
            clientRef.set(client);

            // 确保集合存在
            ensureCollection(client);

            available.set(true);
            log.info("Milvus 连接成功，集合: {}", properties.getCollectionName());

        } catch (Exception e) {
            log.warn("Milvus 连接失败，将使用内存检索降级模式: {}", e.getMessage());
            available.set(false);
        }
    }

    /**
     * 获取 Milvus 客户端
     * @return 客户端（可能为 null）
     */
    public MilvusServiceClient getClient() {
        return clientRef.get();
    }

    /**
     * Milvus 是否可用
     */
    public boolean isAvailable() {
        return available.get() && clientRef.get() != null;
    }

    /**
     * 确保目标集合存在，不存在则创建
     */
    private void ensureCollection(MilvusServiceClient client) {
        R<HasCollectionResponse> hasResp = client.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(properties.getCollectionName())
                        .build()
        );

        if (hasResp.getStatus() == R.Status.Success && hasResp.getData() != null
                && hasResp.getData().hasCollection()) {
            log.info("集合已存在: {}", properties.getCollectionName());
            loadCollection(client);
            return;
        }

        log.info("创建集合: {}", properties.getCollectionName());

        // 字段定义
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(io.milvus.param.DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();

        FieldType groupIdField = FieldType.newBuilder()
                .withName("groupId")
                .withDataType(io.milvus.param.DataType.Int64)
                .build();

        FieldType documentIdField = FieldType.newBuilder()
                .withName("documentId")
                .withDataType(io.milvus.param.DataType.Int64)
                .build();

        FieldType chunkIndexField = FieldType.newBuilder()
                .withName("chunkIndex")
                .withDataType(io.milvus.param.DataType.Int64)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(io.milvus.param.DataType.VarChar)
                .withMaxLength(65535)
                .build();

        FieldType embeddingField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(io.milvus.param.DataType.FloatVector)
                .withDimension(properties.getDimension())
                .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(properties.getCollectionName())
                .withDescription("医院知识库向量集合")
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(groupIdField)
                .addFieldType(documentIdField)
                .addFieldType(chunkIndexField)
                .addFieldType(contentField)
                .addFieldType(embeddingField)
                .build();

        R<R.RpcStatus> createResp = client.createCollection(createParam);
        if (createResp.getStatus() != R.Status.Success) {
            log.warn("创建集合失败: {}", createResp.getMessage());
            return;
        }

        // 创建索引
        MetricType metric = resolveMetricType(properties.getMetricType());
        IndexType indexType = resolveIndexType(properties.getIndexType());

        R<R.RpcStatus> indexResp = client.createIndex(
                CreateIndexParam.newBuilder()
                        .withCollectionName(properties.getCollectionName())
                        .withFieldName("embedding")
                        .withIndexType(indexType)
                        .withMetricType(metric)
                        .withExtraParam("{\"nlist\":1024}")
                        .build()
        );
        if (indexResp.getStatus() != R.Status.Success) {
            log.warn("创建索引失败: {}", indexResp.getMessage());
            return;
        }

        loadCollection(client);
        log.info("集合创建完成: {}", properties.getCollectionName());
    }

    /**
     * 加载集合到内存
     */
    private void loadCollection(MilvusServiceClient client) {
        client.loadCollection(
                LoadCollectionParam.newBuilder()
                        .withCollectionName(properties.getCollectionName())
                        .build()
        );
    }

    private MetricType resolveMetricType(String metricType) {
        switch (metricType.toUpperCase()) {
            case "IP":
                return MetricType.IP;
            case "L2":
                return MetricType.L2;
            case "COSINE":
            default:
                return MetricType.COSINE;
        }
    }

    private IndexType resolveIndexType(String indexType) {
        switch (indexType.toUpperCase()) {
            case "IVF_FLAT":
                return IndexType.IVF_FLAT;
            case "HNSW":
                return IndexType.HNSW;
            case "AUTOINDEX":
            default:
                return IndexType.AUTOINDEX;
        }
    }

    /**
     * 重建集合（删除旧集合，按当前配置重新创建）
     * 用于切换 Embedding 模型后维度变化的情况
     */
    public boolean rebuildCollection() {
        MilvusServiceClient client = clientRef.get();
        if (client == null) {
            log.error("Milvus 客户端不可用，无法重建集合");
            return false;
        }

        try {
            String collectionName = properties.getCollectionName();

            // 1. 释放集合
            client.releaseCollection(
                    ReleaseCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            log.info("集合已释放: {}", collectionName);

            // 2. 删除旧集合
            var dropResp = client.dropCollection(
                    io.milvus.param.collection.DropCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (dropResp.getStatus() != R.Status.Success) {
                log.warn("删除集合失败: {}", dropResp.getMessage());
            }

            // 3. 重新创建（维度使用当前配置）
            ensureCollection(client);
            log.info("集合重建完成: {}, 维度={}", collectionName, properties.getDimension());
            return true;

        } catch (Exception e) {
            log.error("重建集合异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 设置新的向量维度并重建集合
     * 返回是否成功
     */
    public boolean rebuildWithNewDimension(int newDimension) {
        int oldDimension = properties.getDimension();
        if (oldDimension == newDimension) {
            log.info("维度未变化，无需重建: {}", newDimension);
            return true;
        }

        log.info("开始重建集合: 旧维度={}, 新维度={}", oldDimension, newDimension);

        // 更新维度配置
        properties.setDimension(newDimension);

        boolean success = rebuildCollection();
        if (success) {
            log.info("集合重建成功，新维度={}", newDimension);
        } else {
            log.error("集合重建失败，回滚维度配置");
            properties.setDimension(oldDimension);
        }

        return success;
    }

    @PreDestroy
    public void destroy() {
        MilvusServiceClient client = clientRef.get();
        if (client != null) {
            try {
                client.releaseCollection(
                        ReleaseCollectionParam.newBuilder()
                                .withCollectionName(properties.getCollectionName())
                                .build()
                );
                client.close();
                log.info("Milvus 连接已关闭");
            } catch (Exception e) {
                log.warn("关闭 Milvus 连接异常: {}", e.getMessage());
            }
        }
    }
}
