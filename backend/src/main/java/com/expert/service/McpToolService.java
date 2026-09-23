package com.expert.service;

import com.expert.entity.McpTool;

import java.util.List;

/**
 * MCP工具服务接口
 */
public interface McpToolService {

    /**
     * 查询所有工具
     *
     * @return 工具列表
     */
    List<McpTool> listAll();

    /**
     * 新增工具
     *
     * @param tool 工具信息
     */
    void save(McpTool tool);

    /**
     * 启用/停用工具
     *
     * @param id     工具ID
     * @param status 状态 (启动/停用)
     */
    void toggleStatus(Long id, String status);

    /**
     * 获取所有启动状态的工具
     *
     * @return 活跃工具列表
     */
    List<McpTool> getActiveTools();

    /**
     * 记录工具调用日志
     *
     * @param toolId       工具ID
     * @param toolCode     工具编码
     * @param runId        Agent运行ID
     * @param stepId       步骤ID
     * @param inputParams  输入参数
     * @param outputResult 输出结果
     * @param success      是否成功
     * @param errorMsg     错误信息
     * @param durationMs   耗时毫秒
     */
    void logCall(Long toolId, String toolCode, Long runId, Long stepId,
                 String inputParams, String outputResult, boolean success,
                 String errorMsg, int durationMs);
}
