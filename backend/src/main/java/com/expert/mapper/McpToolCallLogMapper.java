package com.expert.mapper;

import com.expert.entity.McpToolCallLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * MCP工具调用日志表 Mapper
 */
@Mapper
public interface McpToolCallLogMapper {

    @Select("SELECT id, tool_id, tool_code, run_id, step_id, input_params, output_result, " +
            "success, error_message, duration_ms, create_time FROM mcp_tool_call_log " +
            "WHERE tool_id = #{toolId} ORDER BY create_time DESC")
    List<McpToolCallLog> findByToolId(@Param("toolId") Long toolId);

    @Select("SELECT id, tool_id, tool_code, run_id, step_id, input_params, output_result, " +
            "success, error_message, duration_ms, create_time FROM mcp_tool_call_log " +
            "WHERE run_id = #{runId} ORDER BY create_time ASC")
    List<McpToolCallLog> findByRunId(@Param("runId") Long runId);

    @Insert("INSERT INTO mcp_tool_call_log (tool_id, tool_code, run_id, step_id, input_params, " +
            "output_result, success, error_message, duration_ms, create_time) VALUES (#{toolId}, #{toolCode}, " +
            "#{runId}, #{stepId}, #{inputParams}, #{outputResult}, #{success}, #{errorMessage}, #{durationMs}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(McpToolCallLog log);
}
