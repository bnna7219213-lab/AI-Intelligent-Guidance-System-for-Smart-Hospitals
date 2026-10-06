package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MCP工具实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {

    private Long id;

    private String toolCode;

    private String toolName;

    private String description;

    private String inputSchema;

    /** 启动/停用 */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
