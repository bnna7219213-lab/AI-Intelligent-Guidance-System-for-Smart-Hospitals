package com.expert.mapper;

import com.expert.entity.AiConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * AI配置表 Mapper
 */
@Mapper
public interface AiConfigMapper {

    @Select("SELECT id, config_key, config_name, api_url, api_key, model_name, extra_config, create_time, update_time " +
            "FROM ai_config WHERE config_key = #{configKey} LIMIT 1")
    AiConfig findByConfigKey(@Param("configKey") String configKey);

    @Select("SELECT id, config_key, config_name, api_url, api_key, model_name, extra_config, create_time, update_time " +
            "FROM ai_config WHERE config_key = #{configKey} LIMIT 1")
    AiConfig findByKey(@Param("configKey") String configKey);

    @Select("SELECT id, config_key, config_name, api_url, api_key, model_name, extra_config, create_time, update_time " +
            "FROM ai_config ORDER BY id ASC")
    List<AiConfig> findAll();

    @Insert("INSERT INTO ai_config (config_key, config_name, api_url, api_key, model_name, extra_config, create_time, update_time) " +
            "VALUES (#{configKey}, #{configName}, #{apiUrl}, #{apiKey}, #{modelName}, #{extraConfig}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiConfig config);

    @Update("UPDATE ai_config SET api_url = #{apiUrl}, api_key = #{apiKey}, " +
            "model_name = #{modelName}, extra_config = #{extraConfig}, " +
            "update_time = NOW() WHERE config_key = #{configKey}")
    int updateByKey(AiConfig config);

    @Update("UPDATE ai_config SET config_name = #{configName}, api_url = #{apiUrl}, api_key = #{apiKey}, " +
            "model_name = #{modelName}, extra_config = #{extraConfig}, " +
            "update_time = NOW() WHERE config_key = #{configKey}")
    int update(AiConfig config);
}
