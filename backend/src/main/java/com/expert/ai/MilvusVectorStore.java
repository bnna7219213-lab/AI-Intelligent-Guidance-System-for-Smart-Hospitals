package com.expert.ai;

import com.expert.config.MilvusConfig;
import com.expert.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.InsertParam.Field;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus 向量存储服务
 * 提供向量插入、删除和相似度搜索
 * 使用 Milvus Java SDK v2.6.x
 * 
 * 客户端通过 MilvusConfig 获取；Milvus 不可用时 RagService 会自动降级
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusVectorStore {

    private final MilvusConfig milvusConfig;
    private final MilvusProperties properties;

    private static final String ID_FIELD = "id";
    private static final String GROUP_ID_FIELD = "groupId";
    private static final String DOCUMENT_ID_FIELD = "documentId";
    private static final String CHUNK_INDEX_FIELD = "chunkIndex";
    private static final String CONTENT_FIELD = "content";
    private static final String EMBEDDING_FIELD = "embedding";

    /**
     * 获取 Milvus 客户端，不可用时返回 null
     */
    private MilvusServiceClient getClient() {
        return milvusConfig.getClient();
    }

    /**
     * 当前是否可用
     */
    public boolean isAvailable() {
        return milvusConfig.isAvailable();
    }

    /**
     * 插入单条向量
     */
    public boolean upsert(Long id, Long groupId, Long documentId, int chunkIndex,
                          String content, List<Float> vector) {
        MilvusServiceClient client = getClient();
        if (client == null) {
            log.warn("Milvus 不可用，跳过插入: id={}", id);
            return false;
        }
        try {
            List<Field> fields = Arrays.asList(
                    Field.builder().name(ID_FIELD).values(Collections.singletonList(id)).build(),
                    Field.builder().name(GROUP_ID_FIELD).values(Collections.singletonList(groupId)).build(),
                    Field.builder().name(DOCUMENT_ID_FIELD).values(Collections.singletonList(documentId)).build(),
                    Field.builder().name(CHUNK_INDEX_FIELD).values(Collections.singletonList((long) chunkIndex)).build(),
                    Field.builder().name(CONTENT_FIELD).values(Collections.singletonList(content)).build(),
                    Field.builder().name(EMBEDDING_FIELD).values(Collections.singletonList(vector)).build()
            );

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(properties.getCollectionName())
                    .withFields(fields)
                    .build();

            R<io.milvus.grpc.MutationResult> response = client.insert(insertParam);
            if (!isSuccess(response)) {
                log.warn("Milvus 插入失败: {}", response.getMessage());
                return false;
            }

            flush();
            return true;
        } catch (Exception e) {
            log.error("Milvus 插入异常: id={}, error={}", id, e.getMessage());
            return false;
        }
    }

    /**
     * 批量插入向量
     */
    public boolean batchUpsert(List<VectorEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return true;
        }
        MilvusServiceClient client = getClient();
        if (client == null) {
            log.warn("Milvus 不可用，跳过批量插入: count={}", entities.size());
            return false;
        }

        try {
            List<Long> ids = entities.stream().map(VectorEntity::getId).collect(Collectors.toList());
            List<Long> groupIds = entities.stream().map(VectorEntity::getGroupId).collect(Collectors.toList());
            List<Long> documentIds = entities.stream().map(VectorEntity::getDocumentId).collect(Collectors.toList());
            List<Long> chunkIndices = entities.stream()
                    .map(e -> (long) e.getChunkIndex()).collect(Collectors.toList());
            List<String> contents = entities.stream().map(VectorEntity::getContent).collect(Collectors.toList());
            List<List<Float>> vectors = entities.stream().map(VectorEntity::getVector).collect(Collectors.toList());

            List<Field> fields = Arrays.asList(
                    Field.builder().name(ID_FIELD).values(ids).build(),
                    Field.builder().name(GROUP_ID_FIELD).values(groupIds).build(),
                    Field.builder().name(DOCUMENT_ID_FIELD).values(documentIds).build(),
                    Field.builder().name(CHUNK_INDEX_FIELD).values(chunkIndices).build(),
                    Field.builder().name(CONTENT_FIELD).values(contents).build(),
                    Field.builder().name(EMBEDDING_FIELD).values(vectors).build()
            );

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(properties.getCollectionName())
                    .withFields(fields)
                    .build();

            R<io.milvus.grpc.MutationResult> response = client.insert(insertParam);
            if (!isSuccess(response)) {
                log.warn("Milvus 批量插入失败: {}", response.getMessage());
                return false;
            }

            flush();
            log.info("Milvus 批量插入成功: count={}", entities.size());
            return true;
        } catch (Exception e) {
            log.error("Milvus 批量插入异常: count={}, error={}", entities.size(), e.getMessage());
            return false;
        }
    }

    /**
     * 向量相似度搜索
     *
     * @param queryVector 查询向量
     * @param groupId     分组ID（可选，null 表示全部）
     * @param topK        返回条数
     * @return 搜索结果列表（按相似度降序）
     */
    public List<VectorSearchResult> search(float[] queryVector, Long groupId, int topK) {
        MilvusServiceClient client = getClient();
        if (client == null) {
            log.warn("Milvus 不可用，跳过搜索");
            return Collections.emptyList();
        }
        try {
            List<Float> queryList = new ArrayList<>();
            for (float v : queryVector) {
                queryList.add(v);
            }

            SearchParam.Builder searchBuilder = SearchParam.newBuilder()
                    .withCollectionName(properties.getCollectionName())
                    .withMetricType(resolveMetric())
                    .withTopK(topK)
                    .withVectors(Collections.singletonList(queryList))
                    .withOutFields(Arrays.asList(GROUP_ID_FIELD, DOCUMENT_ID_FIELD,
                            CHUNK_INDEX_FIELD, CONTENT_FIELD));

            // 如果指定了 groupId，添加过滤条件
            if (groupId != null) {
                searchBuilder.withExpr(GROUP_ID_FIELD + " == " + groupId);
            }

            R<SearchResults> response = client.search(searchBuilder.build());
            if (!isSuccess(response)) {
                log.warn("Milvus 搜索失败: {}", response.getMessage());
                return Collections.emptyList();
            }

            SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
            List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);

            List<VectorSearchResult> results = new ArrayList<>();
            // SDK 2.6.x 中 RowRecord 已移到 QueryResultsWrapper 内
            List<QueryResultsWrapper.RowRecord> records = wrapper.getRowRecords(0);

            for (int i = 0; i < idScores.size(); i++) {
                SearchResultsWrapper.IDScore idScore = idScores.get(i);
                long id = idScore.getLongID();
                float score = idScore.getScore();

                Map<String, Object> fields = new HashMap<>();
                if (records != null && i < records.size()) {
                    fields = records.get(i).getFieldValues();
                }

                VectorSearchResult result = VectorSearchResult.builder()
                        .id(id)
                        .score(score)
                        .groupId(getLongField(fields, GROUP_ID_FIELD))
                        .documentId(getLongField(fields, DOCUMENT_ID_FIELD))
                        .chunkIndex(getIntField(fields, CHUNK_INDEX_FIELD))
                        .content(getStringField(fields, CONTENT_FIELD))
                        .build();
                results.add(result);
            }

            return results;
        } catch (Exception e) {
            log.error("Milvus 搜索异常: error={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 按文档ID删除向量
     */
    public boolean deleteByDocumentId(Long documentId) {
        MilvusServiceClient client = getClient();
        if (client == null) {
            log.warn("Milvus 不可用，跳过删除: documentId={}", documentId);
            return false;
        }
        try {
            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(properties.getCollectionName())
                    .withExpr(DOCUMENT_ID_FIELD + " == " + documentId)
                    .build();

            R<io.milvus.grpc.MutationResult> response = client.delete(deleteParam);
            if (!isSuccess(response)) {
                log.warn("Milvus 删除失败: {}", response.getMessage());
                return false;
            }

            flush();
            return true;
        } catch (Exception e) {
            log.error("Milvus 删除异常: documentId={}, error={}", documentId, e.getMessage());
            return false;
        }
    }

    /**
     * 按分组ID删除向量
     */
    public boolean deleteByGroupId(Long groupId) {
        MilvusServiceClient client = getClient();
        if (client == null) {
            log.warn("Milvus 不可用，跳过删除: groupId={}", groupId);
            return false;
        }
        try {
            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(properties.getCollectionName())
                    .withExpr(GROUP_ID_FIELD + " == " + groupId)
                    .build();

            R<io.milvus.grpc.MutationResult> response = client.delete(deleteParam);
            if (!isSuccess(response)) {
                log.warn("Milvus 删除失败: {}", response.getMessage());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Milvus 删除异常: groupId={}, error={}", groupId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取集合中的向量总数
     */
    public long count() {
        MilvusServiceClient client = getClient();
        if (client == null) {
            return 0;
        }
        try {
            var stats = client.getCollectionStatistics(
                    io.milvus.param.collection.GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(properties.getCollectionName())
                            .build()
            );
            if (isSuccess(stats) && stats.getData() != null) {
                // 2.6.x 返回 KeyValuePair 列表，需遍历找 row_count
                for (io.milvus.grpc.KeyValuePair kv : stats.getData().getStatsList()) {
                    if ("row_count".equals(kv.getKey())) {
                        return Long.parseLong(kv.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取向量统计失败: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * SDK 2.6.x 中 R.getStatus() 返回 Integer，用 R.Status.valueOf(int) 转枚举比较
     */
    private boolean isSuccess(R<?> r) {
        if (r == null || r.getStatus() == null) {
            return false;
        }
        try {
            return R.Status.valueOf(r.getStatus()) == R.Status.Success;
        } catch (Exception e) {
            return false;
        }
    }

    // ======================== 辅助方法 ========================

    private void flush() {
        MilvusServiceClient client = getClient();
        if (client == null) return;
        try {
            client.flush(FlushParam.newBuilder()
                    .addCollectionName(properties.getCollectionName())
                    .build());
        } catch (Exception e) {
            log.debug("Flush 异常: {}", e.getMessage());
        }
    }

    private MetricType resolveMetric() {
        switch (properties.getMetricType().toUpperCase()) {
            case "IP":
                return MetricType.IP;
            case "L2":
                return MetricType.L2;
            default:
                return MetricType.COSINE;
        }
    }

    private Long getLongField(Map<String, Object> fields, String name) {
        Object value = fields.get(name);
        if (value == null) return null;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntField(Map<String, Object> fields, String name) {
        Object value = fields.get(name);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getStringField(Map<String, Object> fields, String name) {
        Object value = fields.get(name);
        return value != null ? value.toString() : null;
    }

    // ======================== 内部数据类 ========================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VectorEntity {
        private Long id;
        private Long groupId;
        private Long documentId;
        private int chunkIndex;
        private String content;
        private List<Float> vector;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VectorSearchResult {
        private Long id;
        private float score;
        private Long groupId;
        private Long documentId;
        private Integer chunkIndex;
        private String content;
    }
}
