package com.expert.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.expert.entity.KbChunk;
import com.expert.entity.KbDocument;
import com.expert.mapper.KbChunkMapper;
import com.expert.mapper.KbDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG检索服务 - 按库收窄的内存余弦topK检索
 * 核心创新：对知识库分块进行纯Java余弦相似度计算，不依赖外部向量数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final HospitalAiService hospitalAiService;

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 50;

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
     * 分块并向量化存储到KB_CHUNK表
     *
     * @param documentId 文档ID
     * @param groupId    分组ID
     */
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
        for (String chunkText : chunks) {
            try {
                float[] vector = hospitalAiService.embed(chunkText);
                String embeddingJson = JSONUtil.toJsonStr(vector);

                KbChunk chunk = KbChunk.builder()
                        .documentId(documentId)
                        .groupId(groupId)
                        .content(chunkText)
                        .embedding(embeddingJson)
                        .chunkIndex(index++)
                        .build();
                kbChunkMapper.insert(chunk);
            } catch (Exception e) {
                log.error("块嵌入失败: documentId={}, chunkIndex={}", documentId, index, e);
            }
        }

        log.info("文档向量化完成: documentId={}, storedChunks={}", documentId, index);
    }

    /**
     * 语义搜索 - 按库收窄的内存余弦topK检索
     *
     * @param query   查询文本
     * @param groupId 知识分组ID（收窄范围）
     * @param topK    返回条数
     * @return 匹配的KbChunk列表，按相似度降序
     */
    public List<KbChunk> search(String query, Long groupId, int topK) {
        // 1. 嵌入查询文本
        float[] queryVector = hospitalAiService.embed(query);

        // 2. 按groupId加载知识库所有块
        List<KbChunk> allChunks;
        if (groupId != null) {
            allChunks = kbChunkMapper.findByGroupId(groupId);
        } else {
            allChunks = kbChunkMapper.findAll();
        }

        if (allChunks == null || allChunks.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 计算每条chunk的余弦相似度，排序后返回topK
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
     *
     * @param a 向量a
     * @param b 向量b
     * @return 余弦相似度 [-1, 1]
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
     * 解析JSON数组字符串为float[]
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
            log.warn("解析embedding失败: {}", e.getMessage());
            return new float[0];
        }
    }
}
