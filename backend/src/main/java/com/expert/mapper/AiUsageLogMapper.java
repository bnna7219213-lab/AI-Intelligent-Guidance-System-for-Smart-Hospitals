package com.expert.mapper;

import com.expert.entity.AiUsageLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * AI使用日志表 Mapper
 */
@Mapper
public interface AiUsageLogMapper {

    @Select("SELECT id, usage_type, model_name, prompt_tokens, completion_tokens, total_tokens, " +
            "duration_ms, success, error_msg, create_time FROM ai_usage_log ORDER BY create_time DESC")
    List<AiUsageLog> findAll();

    @Insert("INSERT INTO ai_usage_log (usage_type, model_name, prompt_tokens, completion_tokens, total_tokens, " +
            "duration_ms, success, error_msg, create_time) VALUES (#{usageType}, #{modelName}, " +
            "#{promptTokens}, #{completionTokens}, #{totalTokens}, #{durationMs}, #{success}, #{errorMsg}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiUsageLog log);
}
