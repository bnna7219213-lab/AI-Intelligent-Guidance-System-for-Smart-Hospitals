package com.expert.service.impl;

import com.expert.ai.RagService;
import com.expert.config.MilvusConfig;
import com.expert.config.MilvusProperties;
import com.expert.entity.KbDocument;
import com.expert.mapper.KbDocumentMapper;
import com.expert.service.AiConfigService;
import com.expert.vo.VectorIndexRebuildStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 向量索引重建服务
 * 
 * 提供完整的"自动重建集合 + 全量重新嵌入"能力：
 * - checkDimensionAndRebuild(): 检查维度是否需要重建
 * - startRebuildAsync(): 异步启动全量重建任务
 * - getCurrentStatus(): 查询任务状态和进度
 */
@Slf4j
@Service
public class VectorIndexRebuildService {

    private final MilvusConfig milvusConfig;
    private final MilvusProperties milvusProperties;
    private final KbDocumentMapper kbDocumentMapper;
    private final AiConfigService aiConfigService;
    private final RagService ragService;

    /** 当前任务状态 */
    private final AtomicReference<VectorIndexRebuildStatus> currentStatus =
            new AtomicReference<>(null);

    public VectorIndexRebuildService(MilvusConfig milvusConfig,
                                     MilvusProperties milvusProperties,
                                     KbDocumentMapper kbDocumentMapper,
                                     AiConfigService aiConfigService,
                                     RagService ragService) {
        this.milvusConfig = milvusConfig;
        this.milvusProperties = milvusProperties;
        this.kbDocumentMapper = kbDocumentMapper;
        this.aiConfigService = aiConfigService;
        this.ragService = ragService;
    }

    // ======================== 维度检查 ========================

    /**
     * 检查当前模型配置的维度与 Milvus 配置是否一致
     * 不一致时自动触发重建
     *
     * @return 检查结果状态
     */
    public VectorIndexRebuildStatus checkDimensionAndRebuild() {
        String targetModelName = resolveTargetModelName();
        int targetDimension = resolveTargetDimension(targetModelName);
        int currentDimension = milvusProperties.getDimension();

        log.info("维度检查: 模型={}, 目标维度={}, 当前维度={}",
                targetModelName, targetDimension, currentDimension);

        VectorIndexRebuildStatus status = VectorIndexRebuildStatus.builder()
                .taskId(UUID.randomUUID().toString().substring(0, 8))
                .targetModelName(targetModelName)
                .targetDimension(targetDimension)
                .currentModelName(resolveCurrentModelName())
                .needRebuildCollection(targetDimension != currentDimension)
                .status("IDLE")
                .build();

        if (targetDimension == currentDimension) {
            log.info("✅ 维度一致，无需重建");
            status.setStatus("IDLE");
            status.setMessage("维度一致，无需重建");
            return status;
        }

        // 维度不一致，自动触发异步重建
        log.warn("⚠️ 维度不一致，触发自动重建: {} → {}", currentDimension, targetDimension);

        // 关键：先更新 Milvus 配置中的维度，保证 rebuildCollection() 使用新维度
        milvusProperties.setDimension(targetDimension);
        log.info("Milvus 维度配置已更新: {} → {}", currentDimension, targetDimension);

        status.setStatus("RUNNING");
        status.setStartTime(LocalDateTime.now());
        currentStatus.set(status);

        startRebuildAsync();
        return status;
    }

    // ======================== 异步重建 ========================

    /**
     * 异步启动全量重建任务
     * 包括：重建集合 + 清空旧向量 + 全量重新嵌入
     */
    public void startRebuildAsync() {
        Thread rebuildThread = new Thread(this::executeRebuild, "vector-rebuild-task");
        rebuildThread.setDaemon(true);
        rebuildThread.start();
        log.info("后台重建任务已启动");
    }

    /**
     * 同步执行重建（供异步线程调用）
     */
    private void executeRebuild() {
        long startMs = System.currentTimeMillis();

        // 复用已有的任务状态（如果存在），否则新建
        VectorIndexRebuildStatus status = currentStatus.get();
        if (status == null) {
            status = VectorIndexRebuildStatus.builder()
                    .taskId(UUID.randomUUID().toString().substring(0, 8))
                    .status("RUNNING")
                    .targetModelName(resolveTargetModelName())
                    .targetDimension(milvusProperties.getDimension())
                    .currentModelName(resolveCurrentModelName())
                    .needRebuildCollection(true)
                    .startTime(LocalDateTime.now())
                    .build();
        }
        status.setStatus("RUNNING");
        status.setStartTime(LocalDateTime.now());
        status.setErrorMessage(null);
        currentStatus.set(status);

        try {
            // Step 1: 重建集合（新维度）
            log.info("[Rebuild] Step 1: 重建 Milvus 集合...");
            boolean rebuilt = milvusConfig.rebuildCollection();
            if (!rebuilt) {
                throw new RuntimeException("Milvus 集合重建失败");
            }

            // Step 2: 清空 MySQL 中的旧分块
            log.info("[Rebuild] Step 2: 清空旧分块...");
            List<KbDocument> documents = kbDocumentMapper.findAllActive();
            int totalDocs = documents != null ? documents.size() : 0;
            status.setTotalDocuments(totalDocs);
            status.setProcessedDocuments(0);
            status.setTotalChunks(0);
            status.setWrittenVectors(0);

            log.info("[Rebuild] 待处理文档数: {}", totalDocs);
            if (totalDocs == 0) {
                status.setStatus("SUCCESS");
                status.setEndTime(LocalDateTime.now());
                status.setElapsedMs(System.currentTimeMillis() - startMs);
                log.info("[Rebuild] 无文档需要处理，任务完成");
                return;
            }

            // Step 3: 逐个文档重新分块和嵌入
            for (KbDocument doc : documents) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeException("任务被中断");
                }

                status.setCurrentDocumentId(doc.getId());
                status.setCurrentDocumentTitle(doc.getTitle());

                try {
                    reEmbedSingleDocument(doc, status);
                } catch (Exception e) {
                    log.error("[Rebuild] 文档处理失败: id={}, title={}, error={}",
                            doc.getId(), doc.getTitle(), e.getMessage());
                }

                status.setProcessedDocuments(status.getProcessedDocuments() + 1);
                status.setElapsedMs(System.currentTimeMillis() - startMs);
                log.info("[Rebuild] 进度: {}/{} ({}%), 已写入向量={}",
                        status.getProcessedDocuments(), totalDocs,
                        (int) Math.round(status.getProgressPercent()),
                        status.getWrittenVectors());
            }

            // Step 4: 完成
            status.setStatus("SUCCESS");
            status.setEndTime(LocalDateTime.now());
            status.setElapsedMs(System.currentTimeMillis() - startMs);
            status.setCurrentDocumentId(null);
            status.setCurrentDocumentTitle(null);
            log.info("[Rebuild] ✅ 全量重建完成! 总耗时={}ms, 向量总数={}",
                    status.getElapsedMs(), status.getWrittenVectors());

        } catch (Exception e) {
            log.error("[Rebuild] ❌ 任务失败: {}", e.getMessage(), e);
            status.setStatus("FAILED");
            status.setErrorMessage(e.getMessage());
            status.setEndTime(LocalDateTime.now());
            status.setElapsedMs(System.currentTimeMillis() - startMs);
        }
    }

    /**
     * 重新嵌入单个文档：删除旧向量 + 委托 RagService 重新分块和嵌入
     */
    private void reEmbedSingleDocument(KbDocument doc, VectorIndexRebuildStatus status) {
        Long documentId = doc.getId();
        String content = doc.getFileContent();

        if (content == null || content.isBlank()) {
            log.warn("[Rebuild] 文档内容为空，跳过: id={}", documentId);
            return;
        }

        // 1. 删除旧向量（MySQL + Milvus）
        ragService.deleteDocumentVectors(documentId);
        log.info("[Rebuild] 已删除旧向量: id={}", documentId);

        // 2. 调用 RagService 重新分块 + 嵌入 + 批量写入 Milvus
        ragService.embedAndStore(documentId, doc.getGroupId());
        log.info("[Rebuild] 重新嵌入完成: id={}", documentId);

        // 3. 更新文档分块计数
        Long chunkCount = ragService.countChunks(documentId);
        if (chunkCount != null) {
            doc.setChunkCount(chunkCount.intValue());
            kbDocumentMapper.update(doc);
            status.setTotalChunks(status.getTotalChunks() + chunkCount.intValue());
            status.setWrittenVectors(status.getWrittenVectors() + chunkCount.intValue());
        }
    }

    // ======================== 查询状态 ========================

    /**
     * 获取当前任务状态
     */
    public VectorIndexRebuildStatus getCurrentStatus() {
        VectorIndexRebuildStatus status = currentStatus.get();
        if (status == null) {
            return VectorIndexRebuildStatus.builder()
                    .status("IDLE")
                    .targetDimension(milvusProperties.getDimension())
                    .currentModelName(resolveCurrentModelName())
                    .targetModelName(resolveTargetModelName())
                    .needRebuildCollection(false)
                    .build();
        }
        return status;
    }

    /**
     * 当前是否有任务在运行
     */
    public boolean isRunning() {
        VectorIndexRebuildStatus status = currentStatus.get();
        return status != null && "RUNNING".equals(status.getStatus());
    }

    // ======================== 辅助方法 ========================

    /**
     * 解析目标模型名称（从 ai_config 表）
     */
    private String resolveTargetModelName() {
        try {
            var config = aiConfigService.getVectorModelConfig();
            return config != null && config.getModelName() != null
                    ? config.getModelName() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 解析当前模型名称
     */
    private String resolveCurrentModelName() {
        return "previous";
    }

    /**
     * 根据模型名称解析维度
     */
    private int resolveTargetDimension(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return milvusProperties.getDimension();
        }
        switch (modelName.toLowerCase()) {
            case "text-embedding-3-small":
            case "text-embedding-v2":
                return 1536;
            case "text-embedding-3-large":
                return 3072;
            case "bge-large-zh-v1.5":
            case "bge-m3":
            case "m3e-large":
            case "conch-embedding-text":
            case "jina-embeddings-v3":
                return 1024;
            case "bge-base-zh-v1.5":
                return 768;
            case "bge-small-zh-v1.5":
                return 384;
            default:
                log.warn("未知模型维度: {}, 使用当前配置 {}", modelName, milvusProperties.getDimension());
                return milvusProperties.getDimension();
        }
    }
}