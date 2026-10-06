package com.expert.mapper;

import com.expert.entity.Doctor;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 医生表 Mapper
 */
@Mapper
public interface DoctorMapper {

    @Select("SELECT id, user_id, department_id, title, specialty, introduction, fee, create_time, update_time " +
            "FROM doctor ORDER BY id ASC")
    List<Doctor> findAll();

    @Select("SELECT id, user_id, department_id, title, specialty, introduction, fee, create_time, update_time " +
            "FROM doctor WHERE id = #{id}")
    Doctor findById(@Param("id") Long id);

    @Select("SELECT id, user_id, department_id, title, specialty, introduction, fee, create_time, update_time " +
            "FROM doctor WHERE user_id = #{userId} LIMIT 1")
    Doctor findByUserId(@Param("userId") Long userId);

    @Select("SELECT id, user_id, department_id, title, specialty, introduction, fee, create_time, update_time " +
            "FROM doctor WHERE department_id = #{departmentId} ORDER BY id ASC")
    List<Doctor> findListByDepartmentId(@Param("departmentId") Long departmentId);

    @Insert("INSERT INTO doctor (user_id, department_id, title, specialty, introduction, fee, create_time, update_time) " +
            "VALUES (#{userId}, #{departmentId}, #{title}, #{specialty}, #{introduction}, #{fee}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Doctor doctor);

    @Update("UPDATE doctor SET user_id = #{userId}, department_id = #{departmentId}, title = #{title}, " +
            "specialty = #{specialty}, introduction = #{introduction}, fee = #{fee}, update_time = NOW() " +
            "WHERE id = #{id}")
    int update(Doctor doctor);

    @Delete("DELETE FROM doctor WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // ===== Service Layer Methods =====

    @Select("SELECT id, user_id, department_id, title, specialty, introduction, fee, create_time, update_time " +
            "FROM doctor WHERE department_id = #{departmentId} ORDER BY id ASC LIMIT #{offset}, #{pageSize}")
    List<Doctor> findByDepartmentIdPaged(@Param("departmentId") Long departmentId,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM doctor WHERE department_id = #{departmentId}")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Select("SELECT id, user_id, department_id, title, specialty, introduction, fee, create_time, update_time " +
            "FROM doctor ORDER BY id ASC LIMIT #{offset}, #{pageSize}")
    List<Doctor> findAllPaged(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM doctor")
    long countAll();
}
