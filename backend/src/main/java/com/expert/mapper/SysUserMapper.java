package com.expert.mapper;

import com.expert.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 系统用户表 Mapper
 */
@Mapper
public interface SysUserMapper {

    @Select("SELECT id, username, password, real_name, phone, email, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, real_name, phone, email, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user WHERE id = #{id}")
    SysUser findById(@Param("id") Long id);

    @Insert("INSERT INTO sys_user (username, password, real_name, phone, email, role, status, create_time, update_time) " +
            "VALUES (#{username}, #{password}, #{realName}, #{phone}, #{email}, #{role}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("UPDATE sys_user SET real_name = #{realName}, phone = #{phone}, email = #{email}, " +
            "role = #{role}, status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(SysUser user);

    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // ===== Service Layer Methods =====

    @Select("SELECT id, username, password, real_name, phone, email, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR real_name LIKE CONCAT('%', #{keyword}, "%') " +
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    List<SysUser> findByKeyword(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR real_name LIKE CONCAT('%', #{keyword}, '%')")
    long countByKeyword(@Param("keyword") String keyword);

    @Select("SELECT id, username, password, real_name, phone, email, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    List<SysUser> findAllPaged(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM sys_user")
    long countAll();

    @Update("UPDATE sys_user SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_user SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE sys_user SET doctor_id = #{doctorId}, update_time = NOW() WHERE id = #{id}")
    int updateDoctorId(@Param("id") Long id, @Param("doctorId") Long doctorId);
}
