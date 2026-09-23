package com.expert.mapper;

import com.expert.entity.ConsultationSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 问诊会话表 Mapper
 */
@Mapper
public interface ConsultationSessionMapper {

    @Select("SELECT id, session_id, patient_id, title, status, create_time, update_time " +
            "FROM consultation_session WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    List<ConsultationSession> findByPatientId(@Param("patientId") Long patientId);

    @Select("SELECT id, session_id, patient_id, title, status, create_time, update_time " +
            "FROM consultation_session WHERE session_id = #{sessionId} LIMIT 1")
    ConsultationSession findBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT id, session_id, patient_id, title, status, create_time, update_time " +
            "FROM consultation_session WHERE id = #{id}")
    ConsultationSession findById(@Param("id") Long id);

    @Insert("INSERT INTO consultation_session (session_id, patient_id, title, status, create_time, update_time) " +
            "VALUES (#{sessionId}, #{patientId}, #{title}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsultationSession session);

    @Update("UPDATE consultation_session SET patient_id = #{patientId}, title = #{title}, " +
            "status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(ConsultationSession session);

    @Delete("DELETE FROM consultation_session WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
