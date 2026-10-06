package com.expert.mapper;

import com.expert.entity.ConsultationMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 问诊消息表 Mapper
 */
@Mapper
public interface ConsultationMessageMapper {

    @Select("SELECT id, session_id, role, content, create_time " +
            "FROM consultation_message WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<ConsultationMessage> findBySessionId(@Param("sessionId") String sessionId);

    @Insert("INSERT INTO consultation_message (session_id, role, content, create_time) " +
            "VALUES (#{sessionId}, #{role}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsultationMessage message);

    @Delete("DELETE FROM consultation_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
