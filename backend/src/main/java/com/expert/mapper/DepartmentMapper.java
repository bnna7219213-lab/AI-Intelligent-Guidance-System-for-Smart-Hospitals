package com.expert.mapper;

import com.expert.entity.Department;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 科室表 Mapper
 */
@Mapper
public interface DepartmentMapper {

    @Select("SELECT id, code, name, description, parent_id, sort_order, status, create_time, update_time " +
            "FROM department ORDER BY sort_order ASC")
    List<Department> findAll();

    @Select("SELECT id, code, name, description, parent_id, sort_order, status, create_time, update_time " +
            "FROM department WHERE id = #{id}")
    Department findById(@Param("id") Long id);

    @Select("SELECT id, code, name, description, parent_id, sort_order, status, create_time, update_time " +
            "FROM department WHERE code = #{code} LIMIT 1")
    Department findByCode(@Param("code") String code);

    @Insert("INSERT INTO department (code, name, description, parent_id, sort_order, status, create_time, update_time) " +
            "VALUES (#{code}, #{name}, #{description}, #{parentId}, #{sortOrder}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Department department);

    @Update("UPDATE department SET code = #{code}, name = #{name}, description = #{description}, " +
            "parent_id = #{parentId}, sort_order = #{sortOrder}, status = #{status}, update_time = NOW() " +
            "WHERE id = #{id}")
    int update(Department department);

    @Delete("DELETE FROM department WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // ===== Service Layer Methods =====

    @Update("UPDATE department SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
