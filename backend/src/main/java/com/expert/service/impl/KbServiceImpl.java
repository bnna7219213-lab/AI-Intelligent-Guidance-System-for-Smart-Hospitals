package com.expert.service.impl;

import com.expert.ai.RagService;
import com.expert.common.BizException;
import com.expert.entity.KbChunk;
import com.expert.entity.KbDocument;
import com.expert.entity.KbGroup;
import com.expert.mapper.KbChunkMapper;
import com.expert.mapper.KbDocumentMapper;
import com.expert.mapper.KbGroupMapper;
import com.expert.service.KbService;
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
import java.util.Collections;
import java.util.List;

/**
 * 知识库服务实现
 *
 * 向量化存储委托给 RagService，实现 Milvus + 内存双模式
 * 文档上传后可自动触发异步向量化（DocumentEmbedService）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbServiceImpl implements KbService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbGroupMapper kbGroupMapper;
    private final KbChunkMapper kbChunkMapper;
    private final RagService ragService;
    private final DocumentEmbedService documentEmbedService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long docUpload(MultipartFile file, Long groupId) {
        return docUpload(file, groupId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long docUpload(MultipartFile file, Long groupId, boolean autoEmbed) {
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

            // 可选：上传后自动触发异步向量化
            if (autoEmbed) {
                documentEmbedService.embedDocumentAsync(document.getId());
                log.info("已触发异步向量化任务: id={}", document.getId());
            }

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

        // 先删除旧分块（MySQL 和 Milvus）
        kbChunkMapper.deleteByDocumentId(documentId);

        // 委托 RagService 执行真实向量化（调用 embedding 模型 + 写入 Milvus）
        ragService.embedAndStore(documentId, document.getGroupId());

        // 更新分块计数
        List<KbChunk> chunks = kbChunkMapper.findByDocumentId(documentId);
        int chunkCount = chunks != null ? chunks.size() : 0;
        document.setChunkCount(chunkCount);
        kbDocumentMapper.update(document);

        log.info("文档分块+向量化完成: documentId={}, chunks={}", documentId, chunkCount);
    }

    @Override
    public List<KbChunk> semanticSearch(String query, int topK) {
        // 委托 RagService 执行语义检索（Milvus 优先，内存降级）
        return ragService.search(query, null, topK);
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
     * 解析 PDF 文件内容
     */
    private String parsePdf(byte[] fileBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析 Word docx 文件内容
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
}