package com.expert.service;

import com.expert.entity.ConsultationMessage;
import com.expert.entity.ConsultationSession;

import java.util.List;

/**
 * 问诊会话服务接口
 */
public interface ConsultationService {

    /**
     * 创建问诊会话
     *
     * @param patientId 患者ID
     * @return 会话信息
     */
    ConsultationSession createSession(Long patientId);

    /**
     * 查询患者会话列表
     *
     * @param patientId 患者ID
     * @return 会话列表
     */
    List<ConsultationSession> listSessions(Long patientId);

    /**
     * 查询会话消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ConsultationMessage> getMessages(String sessionId);

    /**
     * 添加消息
     *
     * @param sessionId 会话ID
     * @param role      角色 (USER/ASSISTANT)
     * @param content   内容
     */
    void addMessage(String sessionId, String role, String content);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);
}
