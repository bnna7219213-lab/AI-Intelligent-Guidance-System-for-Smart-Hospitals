package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档异步嵌入任务状态
 *
 * 用于知识库上传后自动触发的后台向量化任务：
 * - 上传 → 立即返回 → 后台异步分块+嵌入
 * - 前端通过 status 接口轮询进度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEmbedStatus {

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    /** 文档 ID */
    private Long documentId;

    /** 文档标题 */
    private String documentTitle;

    /** 已嵌入分块数 */
    private int chunkCount;

    /** 错误消息 */
    private String errorMessage;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 已耗时（毫秒） */
    private long elapsedMs;
}