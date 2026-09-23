package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI使用日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLog {

    private Long id;

    /** CHAT/SSE_CHAT/REACT/EMBEDDING */
    private String usageType;

    private String modelName;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer durationMs;

    private Integer success;

    private String errorMsg;

    private LocalDateTime createTime;
}
