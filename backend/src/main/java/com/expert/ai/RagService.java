package com.expert.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.expert.config.MilvusProperties;
import com.expert.entity.KbChunk;
import com.expert.entity.KbDocument;
import com.expert.mapper.KbChunkMapper;
import com.expert.mapper.KbDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG检索服务 - 双模式向量检索
 * 
 * 优先使用 Milvus 向量数据库进行高效检索；
 * 当 Milvus 不可用或未启用时，自动降级到内存余弦相似度计算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final HospitalAiService hospitalAiService;
    private final MilvusProperties milvusProperties;
    private final MilvusVectorStore milvusVectorStore;

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 50;

    /**
     * 删除文档的所有向量（MySQL + Milvus）
     * 在重新嵌入前调用，避免残留旧向量
     */
    @CacheEvict(cacheNames = "kbSearch", allEntries = true)
    public void deleteDocumentVectors(Long documentId) {
        // 删除 MySQL 中的旧分块
        kbChunkMapper.deleteByDocumentId(documentId);
        // 删除 Milvus 中的旧向量
        if (isMilvusAvailable()) {
            milvusVectorStore.deleteByDocumentId(documentId);
        }
        log.info("已删除文档向量: documentId={}", documentId);
    }

    /**
     * 检查 Milvus 是否可用
     */
    private boolean isMilvusAvailable() {
        return milvusProperties.isEnabled() && milvusVectorStore.isAvailable();
    }

    /**
     * 文本分块 - 按段落优先，超长段落切分为重叠块
     *
     * @param text      原始文本
     * @param chunkSize 每块最大字符数
     * @param overlap   重叠字符数
     * @return 分块列表
     */
    public List<String> splitText(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();

        // 按段落分割（双换行或换行）
        String[] paragraphs = text.split("\\n{2,}");
        if (paragraphs.length == 1) {
            paragraphs = text.split("\\n");
        }

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= chunkSize) {
                chunks.add(trimmed);
            } else {
                // 超长段落切分为重叠块
                int start = 0;
                while (start < trimmed.length()) {
                    int end = Math.min(start + chunkSize, trimmed.length());
                    chunks.add(trimmed.substring(start, end));
                    start += (chunkSize - overlap);
                }
            }
        }

        return chunks;
    }

    /**
     * 统计文档的分块数量
     */
    public Long countChunks(Long documentId) {
        try {
            return kbChunkMapper.countByDocumentId(documentId);
        } catch (Exception e) {
            log.warn("统计分块失败: documentId={}, error={}", documentId, e.getMessage());
            return 0L;
        }
    }

    /**
     * 分块并向量化存储到 KB_CHUNK 表 + Milvus
     *
     * 重新向量化后主动失效 kbSearch 缓存，确保检索结果反映最新知识库。
     *
     * @param documentId 文档ID
     * @param groupId    分组ID
     */
    @CacheEvict(cacheNames = "kbSearch", allEntries = true)
    public void embedAndStore(Long documentId, Long groupId) {
        KbDocument document = kbDocumentMapper.findById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在: " + documentId);
        }
        String content = document.getFileContent();
        if (content == null || content.isBlank()) {
            log.warn("文档内容为空，跳过向量化: documentId={}", documentId);
            return;
        }

        // 1. 分块
        List<String> chunks = splitText(content, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
        log.info("文档分块完成: documentId={}, chunkCount={}", documentId, chunks.size());

        // 2. 对每块嵌入并存储
        int index = 0;
        List<MilvusVectorStore.VectorEntity> milvusEntities = new ArrayList<>();

        for (String chunkText : chunks) {
            try {
                float[] vector = hospitalAiService.embed(chunkText);
                String embeddingJson = JSONUtil.toJsonStr(vector);

                KbChunk chunk = KbChunk.builder()
                        .documentId(documentId)
                        .groupId(groupId)
                        .content(chunkText)
                        .embedding(embeddingJson)
                        .chunkIndex(index)
                        .build();
                kbChunkMapper.insert(chunk);

                // 同步写入 Milvus
                if (isMilvusAvailable()) {
                    MilvusVectorStore.VectorEntity entity = MilvusVectorStore.VectorEntity.builder()
                            .id(chunk.getId())
                            .groupId(groupId)
                            .documentId(documentId)
                            .chunkIndex(index)
                            .content(chunkText)
                            .vector(toFloatList(vector))
                            .build();
                    milvusEntities.add(entity);
                }

                index++;
            } catch (Exception e) {
                log.error("块嵌入失败: documentId={}, chunkIndex={}", documentId, index, e);
            }
        }

        // 3. 批量写入 Milvus
        if (!milvusEntities.isEmpty()) {
            boolean success = milvusVectorStore.batchUpsert(milvusEntities);
            log.info("Milvus 向量写入完成: documentId={}, count={}, success={}",
                    documentId, milvusEntities.size(), success);
        }

        log.info("文档向量化完成: documentId={}, storedChunks={}", documentId, index);
    }

    /**
     * 语义搜索 - 优先使用 Milvus，降级到内存余弦
     *
     * 缓存策略：embed + Milvus 检索是昂贵操作，对相同 query 走 kbSearch 缓存。
     * 文档重新向量化（embedAndStore / deleteDocumentVectors / 重建集合）时主动失效。
     *
     * @param query   查询文本
     * @param groupId 知识分组ID（收窄范围）
     * @param topK    返回条数
     * @return 匹配的KbChunk列表，按相似度降序
     */
    @Cacheable(cacheNames = "kbSearch",
            key = "'q:' + (#query == null ? '' : #query) + ':g' + (#groupId == null ? 'all' : #groupId) + ':k' + #topK")
    public List<KbChunk> search(String query, Long groupId, int topK) {
        // 1. 嵌入查询文本
        float[] queryVector = hospitalAiService.embed(query);

        // 2. 优先使用 Milvus 检索
        if (isMilvusAvailable()) {
            List<MilvusVectorStore.VectorSearchResult> milvusResults =
                    milvusVectorStore.search(queryVector, groupId, topK);

            if (!milvusResults.isEmpty()) {
                log.info("Milvus 检索命中: query={}, hits={}", query, milvusResults.size());
                return milvusResults.stream()
                        .map(this::convertToKbChunk)
                        .collect(Collectors.toList());
            }

            log.info("Milvus 检索为空，回退到内存检索: query={}", query);
        }

        // 3. 降级：内存余弦检索
        return memorySearch(queryVector, groupId, topK);
    }

    /**
     * 将 Milvus 搜索结果转换为 KbChunk
     */
    private KbChunk convertToKbChunk(MilvusVectorStore.VectorSearchResult result) {
        return KbChunk.builder()
                .id(result.getId())
                .groupId(result.getGroupId())
                .documentId(result.getDocumentId())
                .content(result.getContent())
                .chunkIndex(result.getChunkIndex())
                .build();
    }

    /**
     * 内存余弦检索（降级方案）
     */
    private List<KbChunk> memorySearch(float[] queryVector, Long groupId, int topK) {
        List<KbChunk> allChunks;
        if (groupId != null) {
            allChunks = kbChunkMapper.findByGroupId(groupId);
        } else {
            allChunks = kbChunkMapper.findAll();
        }

        if (allChunks == null || allChunks.isEmpty()) {
            return new ArrayList<>();
        }

        return allChunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isBlank())
                .map(chunk -> {
                    float[] chunkVector = parseEmbedding(chunk.getEmbedding());
                    double similarity = cosineSimilarity(queryVector, chunkVector);
                    return new AbstractMap.SimpleEntry<>(chunk, similarity);
                })
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 余弦相似度计算
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    /**
     * 解析 JSON 数组字符串为 float[]
     */
    private float[] parseEmbedding(String embeddingJson) {
        try {
            JSONArray jsonArray = JSONUtil.parseArray(embeddingJson);
            float[] result = new float[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                result[i] = jsonArray.getFloat(i);
            }
            return result;
        } catch (Exception e) {
            log.warn("解析 embedding 失败: {}", e.getMessage());
            return new float[0];
        }
    }

    /**
     * float[] 转 List<Float>
     */
    private List<Float> toFloatList(float[] vector) {
        List<Float> result = new ArrayList<>(vector.length);
        for (float v : vector) {
            result.add(v);
        }
        return result;
    }
}
