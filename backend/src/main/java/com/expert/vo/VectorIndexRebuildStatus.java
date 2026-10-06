package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 向量索引重建任务状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorIndexRebuildStatus {

    /** RUNNING / SUCCESS / FAILED / IDLE */
    private String status;

    /** 任务 ID */
    private String taskId;

    /** 目标维度 */
    private int targetDimension;

    /** 当前模型名称 */
    private String currentModelName;

    /** 目标模型名称 */
    private String targetModelName;

    /** 总文档数 */
    private int totalDocuments;

    /** 已处理文档数 */
    private int processedDocuments;

    /** 当前处理文档 ID */
    private Long currentDocumentId;

    /** 当前处理文档标题 */
    private String currentDocumentTitle;

    /** 成功分块数 */
    private int totalChunks;

    /** 已写入向量数 */
    private int writtenVectors;

    /** 错误消息 */
    private String errorMessage;

    /** 状态说明消息 */
    private String message;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 已耗时（毫秒） */
    private long elapsedMs;

    /** 是否需要重建集合（维度不匹配） */
    private boolean needRebuildCollection;

    /**
     * 计算进度百分比 (0-100)
     */
    public double getProgressPercent() {
        if (totalDocuments == 0) {
            return 0.0;
        }
        return Math.min(100.0, (processedDocuments * 100.0) / totalDocuments);
    }
}
