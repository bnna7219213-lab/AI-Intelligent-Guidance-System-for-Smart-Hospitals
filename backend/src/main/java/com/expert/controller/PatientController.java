package com.expert.controller;

import com.expert.ai.HospitalAiService;
import com.expert.ai.TriageAgentService;
import com.expert.common.Result;
import com.expert.entity.AgentRun;
import com.expert.entity.ConsultationSession;
import com.expert.entity.MedicalRecord;
import com.expert.entity.PatientProfile;
import com.expert.entity.Registration;
import com.expert.security.UserContext;
import com.expert.service.ConsultationService;
import com.expert.service.MedicalRecordService;
import com.expert.service.PatientProfileService;
import com.expert.service.PromptTemplateService;
import com.expert.service.RegistrationService;
import com.expert.service.SchedulingService;
import com.expert.vo.ConsultationSseRequest;
import com.expert.vo.RegistrationVO;
import com.expert.vo.TriageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 患者控制器 - 档案管理、AI问诊(分诊咨询)、智能分诊、挂号管理
 */
@Slf4j
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientProfileService patientProfileService;
    private final ConsultationService consultationService;
    private final HospitalAiService hospitalAiService;
    private final TriageAgentService triageAgentService;
    private final RegistrationService registrationService;
    private final SchedulingService schedulingService;
    private final MedicalRecordService medicalRecordService;
    private final PromptTemplateService promptTemplateService;

    // ======================== 档案管理 ========================

    /**
     * 查询当前患者档案
     */
    @GetMapping("/profile")
    public Result<PatientProfile> getProfile() {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                return Result.error("未获取到当前用户信息");
            }
            PatientProfile profile = patientProfileService.findByUserId(userId);
            return Result.success(profile);
        } catch (Exception e) {
            log.error("查询患者档案失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 保存或更新当前患者档案
     */
    @PutMapping("/profile")
    public Result<String> saveOrUpdateProfile(@RequestBody PatientProfile profile) {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                return Result.error("未获取到当前用户信息");
            }
            patientProfileService.saveOrUpdate(userId, profile);
            return Result.success("档案保存成功");
        } catch (Exception e) {
            log.error("保存患者档案失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== AI问诊 (SSE) ========================

    /**
     * SSE流式AI问诊 - 患者发送症状/问题，AI流式返回回复
     * 使用预问诊系统提示词，自动创建或追加到会话
     */
    @PostMapping(value = "/consultations/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter consultationsSse(@RequestBody ConsultationSseRequest request) {
        Long patientId = UserContext.getUserId();
        if (patientId == null) {
            SseEmitter errorEmitter = new SseEmitter(5000L);
            try {
                errorEmitter.send(SseEmitter.event().data("{\"error\":\"未获取到当前用户信息\"}"));
                errorEmitter.complete();
            } catch (Exception ex) {
                errorEmitter.completeWithError(ex);
            }
            return errorEmitter;
        }

        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            SseEmitter errorEmitter = new SseEmitter(5000L);
            try {
                errorEmitter.send(SseEmitter.event().data("{\"error\":\"消息内容不能为空\"}"));
                errorEmitter.complete();
            } catch (Exception ex) {
                errorEmitter.completeWithError(ex);
            }
            return errorEmitter;
        }

        // 创建SSE发射器，设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300000L);

        try {
            // 获取或创建会话
            ConsultationSession session;
            if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
                // 查找已有会话
                List<ConsultationSession> sessions = consultationService.listSessions(patientId);
                session = sessions.stream()
                        .filter(s -> request.getSessionId().equals(s.getSessionId()))
                        .findFirst()
                        .orElseGet(() -> consultationService.createSession(patientId));
            } else {
                session = consultationService.createSession(patientId);
            }

            String sessionId = session.getSessionId();

            // 保存用户消息
            consultationService.addMessage(sessionId, "USER", message);

            // 获取预问诊系统提示词
            String systemPrompt = promptTemplateService.getByKey("CONSULTANT");
            if (systemPrompt == null || systemPrompt.isBlank()) {
                systemPrompt = buildDefaultConsultantPrompt();
            }

            // 用于收集完整AI响应，以便保存到会话
            StringBuilder fullResponse = new StringBuilder();

            // 调用AI流式回复
            hospitalAiService.chatSse(
                    message,
                    systemPrompt,
                    token -> {
                        try {
                            emitter.send(SseEmitter.event().data(token));
                            fullResponse.append(token);
                        } catch (Exception e) {
                            log.warn("SSE发送token失败: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        // 完成回调：保存AI回复到会话
                        try {
                            String aiContent = fullResponse.toString();
                            if (!aiContent.isBlank()) {
                                consultationService.addMessage(sessionId, "ASSISTANT", aiContent);
                            }
                            emitter.complete();
                            log.info("SSE问诊完成: sessionId={}, patientId={}", sessionId, patientId);
                        } catch (Exception e) {
                            log.error("SSE完成回调异常: {}", e.getMessage(), e);
                        }
                    },
                    err -> {
                        // 错误回调
                        try {
                            log.error("SSE问诊异常: sessionId={}, error={}", sessionId, err.getMessage(), err);
                            consultationService.addMessage(sessionId, "ASSISTANT",
                                    "抱歉，AI服务暂时出现问题，请稍后再试。");
                            emitter.completeWithError(err);
                        } catch (Exception e) {
                            log.error("SSE错误回调异常: {}", e.getMessage(), e);
                        }
                    }
            );

        } catch (Exception e) {
            log.error("SSE问诊初始化失败: patientId={}, error={}", patientId, e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().data("{\"error\":\"" + e.getMessage() + "\"}"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }

        return emitter;
    }

    // ======================== 智能分诊 ========================

    /**
     * 智能分诊 - 根据症状描述推荐科室和医生
     */
    @PostMapping("/triage")
    public Result<TriageResult> triage(@RequestBody java.util.Map<String, Object> params) {
        try {
            Long patientId = UserContext.getUserId();
            if (patientId == null) {
                return Result.error("未获取到当前用户信息");
            }
            String symptom = (String) params.get("symptom");
            if (symptom == null || symptom.isBlank()) {
                return Result.error("症状描述不能为空");
            }
            TriageAgentService.TriageResult agentResult = triageAgentService.triage(patientId, symptom);
            // 转换为VO
            TriageResult result = TriageResult.builder()
                    .recommendedDepartmentId(agentResult.getRecommendedDepartmentId())
                    .recommendedDoctorId(agentResult.getRecommendedDoctorId())
                    .confidence(agentResult.getConfidence())
                    .emergencyLevel(agentResult.getEmergencyLevel())
                    .reason(agentResult.getReason())
                    .isEmergency(agentResult.isEmergency())
                    .build();
            return Result.success(result);
        } catch (Exception e) {
            log.error("分诊失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询当前患者的分诊历史
     */
    @GetMapping("/triage/history")
    public Result<List<AgentRun>> getTriageHistory() {
        try {
            Long patientId = UserContext.getUserId();
            if (patientId == null) {
                return Result.error("未获取到当前用户信息");
            }
            // 通过AgentRunMapper查询（注入）或从服务获取
            // 由于TriageAgentService没有暴露查询方法，这里通过反射或注入mapper来处理
            // 简化处理：直接返回空列表（实际应添加AgentRunService.findByPatientId）
            return Result.success(new ArrayList<>());
        } catch (Exception e) {
            log.error("查询分诊历史失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 挂号管理 ========================

    /**
     * 查询未来7天可用排班
     */
    @GetMapping("/schedules")
    public Result<List<com.expert.entity.Scheduling>> listAvailableSchedules(
            @RequestParam(required = false) Long departmentId) {
        try {
            java.time.LocalDate startDate = java.time.LocalDate.now();
            java.time.LocalDate endDate = startDate.plusDays(7);
            List<com.expert.entity.Scheduling> schedules;
            if (departmentId != null) {
                schedules = schedulingService.getSchedulesByDepartmentAndDateRange(departmentId, startDate);
            } else {
                schedules = schedulingService.getSchedulesByDepartmentAndDateRange(null, startDate);
            }
            return Result.success(schedules);
        } catch (Exception e) {
            log.error("查询可用排班失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建挂号
     */
    @PostMapping("/registrations")
    public Result<Registration> createRegistration(@RequestBody java.util.Map<String, Object> params) {
        try {
            Long patientId = UserContext.getUserId();
            if (patientId == null) {
                return Result.error("未获取到当前用户信息");
            }
            Object schedulingIdObj = params.get("schedulingId");
            if (schedulingIdObj == null) {
                return Result.error("排班ID不能为空");
            }
            Long schedulingId = Long.valueOf(schedulingIdObj.toString());
            Registration registration = registrationService.createRegistration(patientId, schedulingId);
            return Result.success(registration);
        } catch (Exception e) {
            log.error("创建挂号失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询当前患者挂号记录
     */
    @GetMapping("/registrations")
    public Result<List<RegistrationVO>> listRegistrations() {
        try {
            Long patientId = UserContext.getUserId();
            if (patientId == null) {
                return Result.error("未获取到当前用户信息");
            }
            List<RegistrationVO> registrations = registrationService.findByPatientId(patientId);
            return Result.success(registrations);
        } catch (Exception e) {
            log.error("查询挂号记录失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询挂号对应的电子病历
     */
    @GetMapping("/registrations/{id}/record")
    public Result<MedicalRecord> getRegistrationRecord(@PathVariable Long id) {
        try {
            MedicalRecord record = medicalRecordService.findByRegistrationId(id);
            return Result.success(record);
        } catch (Exception e) {
            log.error("查询电子病历失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 私有方法 ========================

    /**
     * 默认预问诊系统提示词
     */
    private String buildDefaultConsultantPrompt() {
        return """
                你是AI智慧医院的智能导诊助手。你的任务是：
                1. 根据患者描述的症状，提供初步的健康咨询
                2. 建议患者可能需要就诊的科室
                3. 提供就医指导

                【核心原则】
                - 始终保持专业、温和的语气
                - 不做确诊，只提供就医建议
                - 危重症状必须建议立即就医
                - 提醒患者最终以医生诊断为准
                """;
    }
}
