package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI使用统计值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageStats {

    private long totalRequests;

    private long totalTokens;

    private long totalDurationMs;

    private double avgDurationMs;

    private long successCount;

    private long failCount;

    private double successRate;
}
