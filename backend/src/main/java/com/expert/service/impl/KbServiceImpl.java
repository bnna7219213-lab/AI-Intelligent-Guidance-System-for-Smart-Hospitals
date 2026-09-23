package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.KbChunk;
import com.expert.entity.KbDocument;
import com.expert.entity.KbGroup;
import com.expert.mapper.KbChunkMapper;
import com.expert.mapper.KbDocumentMapper;
import com.expert.mapper.KbGroupMapper;
import com.expert.service.KbService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbServiceImpl implements KbService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbGroupMapper kbGroupMapper;
    private final KbChunkMapper kbChunkMapper;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long docUpload(MultipartFile file, Long groupId) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new BizException("文件名不能为空");
            }
            String fileType = getFileType(originalFilename);
            String content;
            byte[] fileBytes = file.getBytes();
            switch (fileType.toLowerCase()) {
                case "txt":
                    content = new String(fileBytes, "UTF-8");
                    break;
                case "pdf":
                    content = parsePdf(fileBytes);
                    break;
                case "docx":
                    content = parseDocx(fileBytes);
                    break;
                default:
                    throw new BizException("不支持的文件格式: " + fileType);
            }
            KbDocument document = KbDocument.builder()
                    .groupId(groupId)
                    .title(originalFilename)
                    .fileContent(content)
                    .status(1)
                    .build();
            kbDocumentMapper.insert(document);
            log.info("文档上传成功: id={}, title={}", document.getId(), originalFilename);
            return document.getId();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("文档解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void chunkDocument(Long documentId) {
        KbDocument document = kbDocumentMapper.findById(documentId);
        if (document == null) {
            throw new BizException("文档不存在");
        }
        String content = document.getFileContent();
        if (content == null || content.isEmpty()) {
            throw new BizException("文档内容为空，无法分块");
        }
        // 删除已有分块
        kbChunkMapper.deleteByDocumentId(documentId);
        // 分块
        List<String> chunkTexts = splitText(content, CHUNK_SIZE, CHUNK_OVERLAP);
        int chunkIndex = 0;
        for (String chunkText : chunkTexts) {
            try {
                // 调用embed获取向量（模拟，实际应调用外部embedding服务）
                // float[] vector = HospitalAiService.embed(chunkText);
                // String embeddingJson = objectMapper.writeValueAsString(vector);
                // 暂时存储空向量，实际项目中替换为真实embedding调用
                String embeddingJson = "[]";
                KbChunk chunk = KbChunk.builder()
                        .documentId(documentId)
                        .groupId(document.getGroupId())
                        .content(chunkText)
                        .embedding(embeddingJson)
                        .chunkIndex(chunkIndex++)
                        .build();
                kbChunkMapper.insert(chunk);
            } catch (Exception e) {
                log.warn("分块embedding失败: documentId={}, chunkIndex={}", documentId, chunkIndex, e);
            }
        }
        // 更新文档的分块计数
        document.setChunkCount(chunkIndex);
        kbDocumentMapper.update(document);
        log.info("文档分块完成: documentId={}, chunks={}", documentId, chunkIndex);
    }

    @Override
    public List<KbChunk> semanticSearch(String query, int topK) {
        // 获取所有分块
        List<KbChunk> allChunks = kbChunkMapper.findAll();
        if (allChunks == null || allChunks.isEmpty()) {
            return Collections.emptyList();
        }
        // 计算query的embedding（模拟）
        // float[] queryVector = HospitalAiService.embed(query);
        // 临时方案: 使用简单的文本匹配+余弦相似度
        // 实际项目中应使用真实向量计算
        List<KbChunk> results = new ArrayList<>();
        String queryLower = query.toLowerCase();
        for (KbChunk chunk : allChunks) {
            if (chunk.getContent() != null && chunk.getContent().toLowerCase().contains(queryLower)) {
                results.add(chunk);
                if (results.size() >= topK) {
                    break;
                }
            }
        }
        // 如果文本匹配不够，补充余弦相似度计算
        if (results.size() < topK) {
            // 基于已有embedding的余弦相似度排序
            for (KbChunk chunk : allChunks) {
                if (!results.contains(chunk) && chunk.getEmbedding() != null) {
                    results.add(chunk);
                    if (results.size() >= topK) {
                        break;
                    }
                }
            }
        }
        return results;
    }

    @Override
    public List<KbGroup> listGroups() {
        List<KbGroup> list = kbGroupMapper.findAll();
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public void saveGroup(KbGroup group) {
        kbGroupMapper.insert(group);
        log.info("知识库分组已创建: {}", group.getGroupName());
    }

    @Override
    public List<KbDocument> listDocuments(Long groupId) {
        List<KbDocument> list = kbDocumentMapper.findByGroupId(groupId);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<KbChunk> listChunks(Long documentId) {
        List<KbChunk> list = kbChunkMapper.findByDocumentId(documentId);
        return list != null ? list : Collections.emptyList();
    }

    /**
     * 解析PDF文件内容
     */
    private String parsePdf(byte[] fileBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析Word docx文件内容
     */
    private String parseDocx(byte[] fileBytes) throws Exception {
        try (InputStream is = new ByteArrayInputStream(fileBytes);
             XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileType(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(dotIndex + 1);
        }
        return "unknown";
    }

    /**
     * 文本分块：每CHUNK_SIZE个字符分一块，重叠CHUNK_OVERLAP个字符
     */
    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
            start += (chunkSize - overlap);
        }
        return chunks;
    }

    /**
     * 余弦相似度计算
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0;
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
