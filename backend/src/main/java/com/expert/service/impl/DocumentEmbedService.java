package com.expert.service.impl;

import com.expert.ai.RagService;
import com.expert.entity.KbDocument;
import com.expert.mapper.KbChunkMapper;
import com.expert.mapper.KbDocumentMapper;
import com.expert.vo.DocumentEmbedStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 知识库文档异步嵌入服务
 *
 * 解决上传大文档时 HTTP 请求被 embedding 调用阻塞的问题：
 * - docUpload() 本地解析完成后立即返回，不等待向量化
 * - embedDocumentAsync() 在后台线程中执行分块+嵌入
 * - 前端通过 getEmbedStatus() 轮询任务进度
 *
 * 与 VectorIndexRebuildService 采用相同模式，但粒度更细：
 * - 一个文档一个任务
 * - 可并发多个文档同时嵌入
 */
@Slf4j
@Service
public class DocumentEmbedService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final RagService ragService;

    /** 任务状态缓存：documentId → 状态 */
    private final ConcurrentHashMap<Long, AtomicReference<DocumentEmbedStatus>> taskMap =
            new ConcurrentHashMap<>();

    /** 运行中的文档 ID 集合 */
    private final ConcurrentHashMap<Long, AtomicBoolean> runningMap = new ConcurrentHashMap<>();

    public DocumentEmbedService(KbDocumentMapper kbDocumentMapper,
                                KbChunkMapper kbChunkMapper,
                                RagService ragService) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.ragService = ragService;
    }

    // ======================== 提交异步任务 ========================

    /**
     * 异步启动文档向量化任务
     * 立即返回，不等待任务完成
     *
     * @param documentId 文档 ID
     */
    public void embedDocumentAsync(Long documentId) {
        KbDocument document = kbDocumentMapper.findById(documentId);
        if (document == null) {
            log.warn("[EmbedAsync] 文档不存在，无法提交任务: id={}", documentId);
            return;
        }

        // 已在运行中，忽略重复提交
        AtomicBoolean running = runningMap.computeIfAbsent(documentId, k -> new AtomicBoolean(false));
        if (!running.compareAndSet(false, true)) {
            log.info("[EmbedAsync] 文档已在处理中，忽略重复提交: id={}", documentId);
            return;
        }

        // 初始化任务状态
        DocumentEmbedStatus status = DocumentEmbedStatus.builder()
                .status("PENDING")
                .documentId(documentId)
                .documentTitle(document.getTitle())
                .chunkCount(0)
                .startTime(LocalDateTime.now())
                .build();
        taskMap.put(documentId, new AtomicReference<>(status));

        // 启动后台线程
        Thread worker = new Thread(() -> executeEmbed(documentId), "kb-embed-" + documentId);
        worker.setDaemon(true);
        worker.start();
        log.info("[EmbedAsync] 后台向量化任务已启动: id={}, title={}", documentId, document.getTitle());
    }

    // ======================== 查询任务状态 ========================

    /**
     * 查询文档的异步嵌入任务状态
     *
     * @param documentId 文档 ID
     * @return 任务状态（如果从未提交过则返回 null）
     */
    public DocumentEmbedStatus getEmbedStatus(Long documentId) {
        AtomicReference<DocumentEmbedStatus> ref = taskMap.get(documentId);
        if (ref == null) {
            // 没有历史任务：检查当前分块数，作为已完成的默认状态
            Long count = kbChunkMapper.countByDocumentId(documentId);
            int chunkCount = count != null ? count.intValue() : 0;
            return DocumentEmbedStatus.builder()
                    .status(chunkCount > 0 ? "SUCCESS" : "PENDING")
                    .documentId(documentId)
                    .chunkCount(chunkCount)
                    .build();
        }
        return ref.get();
    }

    // ======================== 执行嵌入 ========================

    /**
     * 后台执行：删除旧向量 → 重新分块嵌入 → 更新状态
     */
    private void executeEmbed(Long documentId) {
        long startMs = System.currentTimeMillis();
        AtomicReference<DocumentEmbedStatus> statusRef = taskMap.get(documentId);

        try {
            // 状态：RUNNING
            updateStatus(statusRef, s -> {
                s.setStatus("RUNNING");
                s.setStartTime(LocalDateTime.now());
            });

            KbDocument document = null;
            // 事务提交后才会可见，加短重试防止竞态
            for (int attempt = 0; attempt < 10; attempt++) {
                document = kbDocumentMapper.findById(documentId);
                if (document != null) break;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("任务被中断", ie);
                }
            }
            if (document == null) {
                throw new RuntimeException("文档不存在: " + documentId);
            }

            // 1. 删除旧向量（MySQL + Milvus）
            ragService.deleteDocumentVectors(documentId);

            // 2. 委托 RagService 重新分块 + 嵌入 + 批量写入 Milvus
            ragService.embedAndStore(documentId, document.getGroupId());

            // 3. 更新文档分块计数
            Long chunkCount = kbChunkMapper.countByDocumentId(documentId);
            int count = chunkCount != null ? chunkCount.intValue() : 0;
            document.setChunkCount(count);
            kbDocumentMapper.update(document);

            // 4. 更新任务状态：SUCCESS
            int finalCount = count;
            updateStatus(statusRef, s -> {
                s.setStatus("SUCCESS");
                s.setChunkCount(finalCount);
                s.setEndTime(LocalDateTime.now());
                s.setElapsedMs(System.currentTimeMillis() - startMs);
            });

            log.info("[EmbedAsync] ✅ 向量化完成: id={}, chunks={}, 耗时={}ms",
                    documentId, count, System.currentTimeMillis() - startMs);

        } catch (Exception e) {
            log.error("[EmbedAsync] ❌ 向量化失败: id={}, error={}", documentId, e.getMessage(), e);
            updateStatus(statusRef, s -> {
                s.setStatus("FAILED");
                s.setErrorMessage(e.getMessage());
                s.setEndTime(LocalDateTime.now());
                s.setElapsedMs(System.currentTimeMillis() - startMs);
            });
        } finally {
            AtomicBoolean running = runningMap.get(documentId);
            if (running != null) {
                running.set(false);
            }
        }
    }

    /**
     * 更新状态引用中的当前对象
     */
    private void updateStatus(AtomicReference<DocumentEmbedStatus> ref,
                              java.util.function.Consumer<DocumentEmbedStatus> updater) {
        if (ref == null) return;
        DocumentEmbedStatus current = ref.get();
        updater.accept(current);
    }
}