package com.expert.mapper;

import com.expert.entity.AgentStep;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Agent运行步骤表 Mapper
 */
@Mapper
public interface AgentStepMapper {

    @Select("SELECT id, run_id, step_number, tool_code, tool_name, input_params, output_result, " +
            "success, error_message, duration_ms, create_time FROM agent_step " +
            "WHERE run_id = #{runId} ORDER BY step_number ASC")
    List<AgentStep> findByRunId(@Param("runId") Long runId);

    @Insert("INSERT INTO agent_step (run_id, step_number, tool_code, tool_name, input_params, " +
            "output_result, success, error_message, duration_ms, create_time) VALUES (#{runId}, " +
            "#{stepNumber}, #{toolCode}, #{toolName}, #{inputParams}, #{outputResult}, #{success}, " +
            "#{errorMessage}, #{durationMs}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentStep step);
}
