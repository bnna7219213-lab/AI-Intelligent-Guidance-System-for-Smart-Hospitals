package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MCP工具调用日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolCallLog {

    private Long id;

    private Long toolId;

    private String toolCode;

    private Long runId;

    private Long stepId;

    private String inputParams;

    private String outputResult;

    private Integer success;

    private String errorMessage;

    private Integer durationMs;

    private LocalDateTime createTime;
}
