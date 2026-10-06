package com.expert.mapper;

import com.expert.entity.KbGroup;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 知识库分组表 Mapper
 */
@Mapper
public interface KbGroupMapper {

    @Select("SELECT id, group_name, description, status, create_time, update_time FROM kb_group ORDER BY id ASC")
    List<KbGroup> findAll();

    @Select("SELECT id, group_name, description, status, create_time, update_time FROM kb_group WHERE id = #{id}")
    KbGroup findById(@Param("id") Long id);

    @Insert("INSERT INTO kb_group (group_name, description, status, create_time, update_time) " +
            "VALUES (#{groupName}, #{description}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KbGroup group);

    @Update("UPDATE kb_group SET group_name = #{groupName}, description = #{description}, " +
            "status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(KbGroup group);

    @Delete("DELETE FROM kb_group WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
