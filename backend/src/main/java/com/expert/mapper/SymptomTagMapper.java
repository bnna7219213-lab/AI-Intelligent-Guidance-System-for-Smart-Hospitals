package com.expert.mapper;

import com.expert.entity.SymptomTag;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 症状标签表 Mapper
 */
@Mapper
public interface SymptomTagMapper {

    @Select("SELECT id, name, description, related_departments, weight, is_red_flag, create_time, update_time " +
            "FROM symptom_tag ORDER BY id ASC")
    List<SymptomTag> findAll();

    @Select("SELECT id, name, description, related_departments, weight, is_red_flag, create_time, update_time " +
            "FROM symptom_tag WHERE name LIKE CONCAT('%', #{name}, '%') ORDER BY id ASC")
    List<SymptomTag> findByNameLike(@Param("name") String name);

    @Select("SELECT id, name, description, related_departments, weight, is_red_flag, create_time, update_time " +
            "FROM symptom_tag WHERE id = #{id}")
    SymptomTag findById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT id, name, description, related_departments, weight, is_red_flag, create_time, update_time FROM symptom_tag " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<SymptomTag> findByIds(@Param("ids") List<Long> ids);

    @Insert("INSERT INTO symptom_tag (name, description, related_departments, weight, is_red_flag, create_time, update_time) " +
            "VALUES (#{name}, #{description}, #{relatedDepartments}, #{weight}, #{isRedFlag}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SymptomTag tag);

    @Update("UPDATE symptom_tag SET name = #{name}, description = #{description}, " +
            "related_departments = #{relatedDepartments}, weight = #{weight}, is_red_flag = #{isRedFlag}, " +
            "update_time = NOW() WHERE id = #{id}")
    int update(SymptomTag tag);

    @Delete("DELETE FROM symptom_tag WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // ===== Service Layer Methods =====

    @Select("SELECT id, name, description, related_departments, weight, is_red_flag, create_time, update_time " +
            "FROM symptom_tag WHERE is_red_flag = #{isRedFlag}")
    List<SymptomTag> findByIsRedFlag(@Param("isRedFlag") String isRedFlag);
}
