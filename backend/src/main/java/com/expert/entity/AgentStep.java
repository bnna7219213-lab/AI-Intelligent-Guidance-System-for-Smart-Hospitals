package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent执行步骤实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStep {

    private Long id;

    private Long runId;

    private Integer stepNumber;

    private String toolCode;

    private String toolName;

    private String inputParams;

    private String outputResult;

    private Integer success;

    private String errorMessage;

    private Integer durationMs;

    private LocalDateTime createTime;
}
