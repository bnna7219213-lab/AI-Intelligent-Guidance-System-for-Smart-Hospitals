package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.ConsultationMessage;
import com.expert.entity.ConsultationSession;
import com.expert.mapper.ConsultationMessageMapper;
import com.expert.mapper.ConsultationSessionMapper;
import com.expert.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 问诊会话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationSessionMapper consultationSessionMapper;
    private final ConsultationMessageMapper consultationMessageMapper;

    @Override
    public ConsultationSession createSession(Long patientId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        ConsultationSession session = ConsultationSession.builder()
                .sessionId(sessionId)
                .patientId(patientId)
                .status("ACTIVE")
                .build();
        consultationSessionMapper.insert(session);
        log.info("问诊会话已创建: sessionId={}, patientId={}", sessionId, patientId);
        return session;
    }

    @Override
    public List<ConsultationSession> listSessions(Long patientId) {
        List<ConsultationSession> list = consultationSessionMapper.findByPatientId(patientId);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<ConsultationMessage> getMessages(String sessionId) {
        List<ConsultationMessage> list = consultationMessageMapper.findBySessionId(sessionId);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public void addMessage(String sessionId, String role, String content) {
        ConsultationSession session = consultationSessionMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new BizException("会话不存在");
        }
        ConsultationMessage message = ConsultationMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .build();
        consultationMessageMapper.insert(message);
        log.debug("消息已添加: sessionId={}, role={}", sessionId, role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        ConsultationSession session = consultationSessionMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new BizException("会话不存在");
        }
        // 先删除消息
        consultationMessageMapper.deleteBySessionId(sessionId);
        // 再删除会话
        consultationSessionMapper.deleteById(session.getId());
        log.info("问诊会话已删除: sessionId={}", sessionId);
    }
}
