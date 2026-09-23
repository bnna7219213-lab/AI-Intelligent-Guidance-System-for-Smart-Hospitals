package com.expert.mapper;

import com.expert.entity.PromptTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 提示词模板表 Mapper
 */
@Mapper
public interface PromptTemplateMapper {

    @Select("SELECT id, template_key, template_name, template_content, description, create_time, update_time " +
            "FROM prompt_template WHERE template_key = #{templateKey} LIMIT 1")
    PromptTemplate findByTemplateKey(@Param("templateKey") String templateKey);

    @Select("SELECT id, template_key, template_name, template_content, description, create_time, update_time " +
            "FROM prompt_template ORDER BY id ASC")
    List<PromptTemplate> findAll();

    @Insert("INSERT INTO prompt_template (template_key, template_name, template_content, description, create_time, update_time) " +
            "VALUES (#{templateKey}, #{templateName}, #{templateContent}, #{description}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PromptTemplate template);

    @Update("UPDATE prompt_template SET template_name = #{templateName}, template_content = #{templateContent}, " +
            "description = #{description}, update_time = NOW() WHERE template_key = #{templateKey}")
    int update(PromptTemplate template);

    @Delete("DELETE FROM prompt_template WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
