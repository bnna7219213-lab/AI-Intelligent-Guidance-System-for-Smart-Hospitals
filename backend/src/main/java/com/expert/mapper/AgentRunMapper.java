package com.expert.mapper;

import com.expert.entity.AgentRun;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * Agent运行记录表 Mapper
 */
@Mapper
public interface AgentRunMapper {

    @Select("SELECT id, patient_id, session_id, status, recommended_department_id, recommended_doctor_id, " +
            "confidence, emergency_level, recommendation_reason, is_registered, total_steps, total_tokens, " +
            "error_message, create_time, update_time FROM agent_run WHERE id = #{id}")
    AgentRun findById(@Param("id") Long id);

    @Select("SELECT id, patient_id, session_id, status, recommended_department_id, recommended_doctor_id, " +
            "confidence, emergency_level, recommendation_reason, is_registered, total_steps, total_tokens, " +
            "error_message, create_time, update_time FROM agent_run WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    List<AgentRun> findByPatientId(@Param("patientId") Long patientId);

    @Select("SELECT id, patient_id, session_id, status, recommended_department_id, recommended_doctor_id, " +
            "confidence, emergency_level, recommendation_reason, is_registered, total_steps, total_tokens, " +
            "error_message, create_time, update_time FROM agent_run WHERE session_id = #{sessionId} LIMIT 1")
    AgentRun findBySessionId(@Param("sessionId") String sessionId);

    @Insert("INSERT INTO agent_run (patient_id, session_id, status, recommended_department_id, recommended_doctor_id, " +
            "confidence, emergency_level, recommendation_reason, is_registered, total_steps, total_tokens, " +
            "error_message, create_time, update_time) VALUES (#{patientId}, #{sessionId}, #{status}, " +
            "#{recommendedDepartmentId}, #{recommendedDoctorId}, #{confidence}, #{emergencyLevel}, " +
            "#{recommendationReason}, #{isRegistered}, #{totalSteps}, #{totalTokens}, #{errorMessage}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentRun run);

    @Update("UPDATE agent_run SET status = #{status}, is_registered = #{isRegistered}, " +
            "update_time = NOW() WHERE id = #{id}")
    int update(AgentRun run);

    @Update("UPDATE agent_run SET status = #{status}, recommended_department_id = #{recommendedDepartmentId}, " +
            "recommended_doctor_id = #{recommendedDoctorId}, confidence = #{confidence}, " +
            "emergency_level = #{emergencyLevel}, recommendation_reason = #{recommendationReason}, " +
            "total_steps = #{totalSteps}, error_message = #{errorMessage}, update_time = NOW() WHERE id = #{id}")
    int updateResult(AgentRun run);

    @Update("UPDATE agent_run SET is_registered = '是', update_time = NOW() WHERE id = #{id}")
    int markRegistered(@Param("id") Long id);

    @Update("UPDATE agent_run SET is_registered = '是', registration_id = #{registrationId}, " +
            "update_time = NOW() WHERE id = #{id}")
    int markAsRegistered(@Param("id") Long id, @Param("registrationId") Long registrationId);

    @Select("SELECT id, patient_id, session_id, status, recommended_department_id, recommended_doctor_id, " +
            "confidence, emergency_level, recommendation_reason, is_registered, total_steps, total_tokens, " +
            "error_message, create_time, update_time FROM agent_run ORDER BY create_time DESC")
    List<AgentRun> findAll();

    @Select("SELECT COUNT(*) FROM agent_run")
    long countAll();

    // ===== Service Layer Methods =====

    /**
     * 分诊推荐命中率查询：按推荐科室统计is_registered='是'的比率
     */
    @Select("SELECT recommended_department_id, " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN is_registered = '是' THEN 1 ELSE 0 END) as hit_count, " +
            "ROUND(SUM(CASE WHEN is_registered = '是' THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) as hit_rate " +
            "FROM agent_run WHERE recommended_department_id IS NOT NULL " +
            "GROUP BY recommended_department_id")
    List<Map<String, Object>> selectTriageHitRate();
}
