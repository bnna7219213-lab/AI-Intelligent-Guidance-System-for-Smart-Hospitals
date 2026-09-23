package com.expert.service.impl;

import com.expert.entity.McpTool;
import com.expert.entity.McpToolCallLog;
import com.expert.mapper.McpToolCallLogMapper;
import com.expert.mapper.McpToolMapper;
import com.expert.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * MCP工具服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolServiceImpl implements McpToolService {

    private final McpToolMapper mcpToolMapper;
    private final McpToolCallLogMapper mcpToolCallLogMapper;

    @Override
    public List<McpTool> listAll() {
        List<McpTool> list = mcpToolMapper.findAll();
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public void save(McpTool tool) {
        mcpToolMapper.insert(tool);
        log.info("MCP工具已新增: code={}", tool.getToolCode());
    }

    @Override
    public void toggleStatus(Long id, String status) {
        McpTool existing = mcpToolMapper.findById(id);
        if (existing == null) {
            throw new com.expert.common.BizException("工具不存在");
        }
        mcpToolMapper.updateStatus(id, status);
        log.info("工具状态已变更: id={}, status={}", id, status);
    }

    @Override
    public List<McpTool> getActiveTools() {
        // CORE Innovation: 返回status='启动'的工具
        List<McpTool> list = mcpToolMapper.findByStatus("启动");
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public void logCall(Long toolId, String toolCode, Long runId, Long stepId,
                        String inputParams, String outputResult, boolean success,
                        String errorMsg, int durationMs) {
        McpToolCallLog callLog = McpToolCallLog.builder()
                .toolId(toolId)
                .toolCode(toolCode)
                .runId(runId)
                .stepId(stepId)
                .inputParams(inputParams)
                .outputResult(outputResult)
                .success(success ? 1 : 0)
                .errorMessage(errorMsg)
                .durationMs(durationMs)
                .build();
        mcpToolCallLogMapper.insert(callLog);
        log.debug("MCP工具调用日志已记录: toolCode={}, success={}, duration={}ms",
                toolCode, success, durationMs);
    }
}
