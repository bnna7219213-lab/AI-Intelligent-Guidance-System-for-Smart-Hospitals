package com.expert.ai;

import com.expert.entity.AgentRun;
import com.expert.entity.McpTool;
import com.expert.entity.Scheduling;
import com.expert.entity.SymptomTag;
import com.expert.mapper.AgentRunMapper;
import com.expert.service.DepartmentService;
import com.expert.service.DoctorService;
import com.expert.service.McpToolService;
import com.expert.service.PromptTemplateService;
import com.expert.service.SymptomTagService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 分诊Agent服务 - 核心创新：模型输出必须经过验证，不直接信任AI推荐
 * 验证内容包括：科室存在性、医生排班可约性、红旗症状强制急诊
 */
@Slf4j
@Service
public class TriageAgentService {

    private final HospitalAiService hospitalAiService;
    private final McpToolExecutor mcpToolExecutor;
    private final McpToolService mcpToolService;
    private final AgentRunMapper agentRunMapper;
    private final PromptTemplateService promptTemplateService;
    private final SymptomTagService symptomTagService;
    private final DepartmentService departmentService;
    private final DoctorService doctorService;

    public TriageAgentService(HospitalAiService hospitalAiService,
                              McpToolExecutor mcpToolExecutor,
                              McpToolService mcpToolService,
                              AgentRunMapper agentRunMapper,
                              PromptTemplateService promptTemplateService,
                              SymptomTagService symptomTagService,
                              DepartmentService departmentService,
                              DoctorService doctorService) {
        this.hospitalAiService = hospitalAiService;
        this.mcpToolExecutor = mcpToolExecutor;
        this.mcpToolService = mcpToolService;
        this.agentRunMapper = agentRunMapper;
        this.promptTemplateService = promptTemplateService;
        this.symptomTagService = symptomTagService;
        this.departmentService = departmentService;
        this.doctorService = doctorService;
    }

    /**
     * 分诊主流程
     *
     * @param patientId           患者ID
     * @param symptomDescription  症状描述
     * @return 分诊结果（已通过验证）
     */
    @Transactional(rollbackFor = Exception.class)
    public TriageResult triage(Long patientId, String symptomDescription) {
        // 1. 创建Agent运行记录
        AgentRun agentRun = AgentRun.builder()
                .patientId(patientId)
                .status("RUNNING")
                .build();
        agentRunMapper.insert(agentRun);
        Long runId = agentRun.getId();
        log.info("分诊Agent启动: runId={}, patientId={}", runId, patientId);

        try {
            // 2. 获取系统提示词模板
            String systemPrompt = promptTemplateService.getByKey("TRIAGE_AGENT");
            if (systemPrompt == null || systemPrompt.isBlank()) {
                systemPrompt = buildDefaultTriagePrompt();
            }

            // 3. 获取激活的工具列表
            List<McpTool> activeTools = mcpToolService.getActiveTools();

            // 4. 构建初始上下文
            List<ChatMessage> conversation = new ArrayList<>();
            conversation.add(SystemMessage.from(systemPrompt));
            conversation.add(UserMessage.from("患者症状描述: " + symptomDescription));

            // 5. 执行ReAct循环
            String finalAnswer = mcpToolExecutor.executeTools(runId, conversation, activeTools, step -> {
                log.debug("ReAct步骤输出: {}", step);
            });

            // 6. 验证输出（核心创新：不直接信任模型结果）
            TriageResult result = verifyAndBuildResult(runId, finalAnswer, symptomDescription);

            // 7. 更新AgentRun为成功状态
            AgentRun updateRun = AgentRun.builder()
                    .id(runId)
                    .status("SUCCESS")
                    .recommendedDepartmentId(result.getRecommendedDepartmentId())
                    .recommendedDoctorId(result.getRecommendedDoctorId())
                    .confidence(result.getConfidence())
                    .emergencyLevel(result.getEmergencyLevel())
                    .recommendationReason(result.getReason())
                    .totalSteps(conversation.size())
                    .build();
            agentRunMapper.updateResult(updateRun);

            log.info("分诊完成: runId={}, deptId={}, doctorId={}, emergency={}",
                    runId, result.getRecommendedDepartmentId(),
                    result.getRecommendedDoctorId(), result.getEmergencyLevel());

            return result;

        } catch (Exception e) {
            log.error("分诊Agent异常: runId={}", runId, e);

            // 更新AgentRun为失败状态
            AgentRun failRun = AgentRun.builder()
                    .id(runId)
                    .status("FAIL")
                    .errorMessage(e.getMessage())
                    .build();
            agentRunMapper.update(failRun);

            // 返回降级结果
            return TriageResult.builder()
                    .recommendedDepartmentId(null)
                    .recommendedDoctorId(null)
                    .confidence(BigDecimal.ZERO)
                    .emergencyLevel("UNKNOWN")
                    .reason("分诊服务暂时不可用，请前往人工导诊台")
                    .isEmergency(false)
                    .build();
        }
    }

    /**
     * 验证并构建分诊结果 - 模型输出必须通过数据库真实性验证
     */
    private TriageResult verifyAndBuildResult(Long runId, String finalAnswer, String symptomDescription) {
        TriageResult.TriageResultBuilder resultBuilder = TriageResult.builder();

        // 1. 红旗症状检查 - 直接覆盖为急诊
        boolean redFlagFound = checkRedFlagInAnswer(finalAnswer, symptomDescription);
        if (redFlagFound) {
            log.warn("检测到红旗症状，强制转急诊: runId={}", runId);

            // 查找急诊科室
            var emergencyDept = departmentService.listAll().stream()
                    .filter(d -> "EMERGENCY".equals(d.getCode()) || "急诊".equals(d.getName()))
                    .findFirst().orElse(null);

            resultBuilder
                    .recommendedDepartmentId(emergencyDept != null ? emergencyDept.getId() : null)
                    .recommendedDoctorId(null)
                    .confidence(new BigDecimal("0.95"))
                    .emergencyLevel("RED")
                    .reason("检测到红旗症状，建议立即前往急诊科就诊")
                    .isEmergency(true);

            return resultBuilder.build();
        }

        // 2. 从模型回答中提取推荐的科室和医生
        Long suggestedDeptId = extractDepartmentIdFromAnswer(finalAnswer);
        Long suggestedDoctorId = extractDoctorIdFromAnswer(finalAnswer);

        // 3. 验证科室存在性
        Long verifiedDeptId = verifyDepartment(suggestedDeptId);

        // 4. 验证医生排班可用性
        Long verifiedDoctorId = verifyDoctorSchedule(suggestedDoctorId);

        // 5. 计算置信度
        BigDecimal confidence = calculateConfidence(verifiedDeptId, verifiedDoctorId);

        // 6. 确定紧急级别
        String emergencyLevel = determineEmergencyLevel(finalAnswer);

        resultBuilder
                .recommendedDepartmentId(verifiedDeptId)
                .recommendedDoctorId(verifiedDoctorId)
                .confidence(confidence)
                .emergencyLevel(emergencyLevel)
                .reason(finalAnswer)
                .isEmergency(false);

        return resultBuilder.build();
    }

    /**
     * 红旗症状检测
     */
    private boolean checkRedFlagInAnswer(String answer, String symptomDescription) {
        try {
            List<SymptomTag> redFlagTags = symptomTagService.findRedFlagSymptoms();
            if (redFlagTags == null || redFlagTags.isEmpty()) {
                return false;
            }
            String combinedText = (answer != null ? answer : "") + " " + symptomDescription;
            return redFlagTags.stream()
                    .anyMatch(tag -> tag.getName() != null && combinedText.contains(tag.getName()));
        } catch (Exception e) {
            log.warn("红旗症状检测异常", e);
            return false;
        }
    }

    /**
     * 验证科室是否存在且启用
     */
    private Long verifyDepartment(Long suggestedDeptId) {
        if (suggestedDeptId == null) {
            return null;
        }
        try {
            var dept = departmentService.getById(suggestedDeptId);
            if (dept != null && dept.getStatus() != null && dept.getStatus() == 1) {
                return dept.getId();
            }
        } catch (Exception e) {
            log.warn("科室验证失败: deptId={}", suggestedDeptId);
        }
        return null;
    }

    /**
     * 验证医生是否有当天可用排班
     */
    private Long verifyDoctorSchedule(Long suggestedDoctorId) {
        if (suggestedDoctorId == null) {
            return null;
        }
        try {
            var doctor = doctorService.getById(suggestedDoctorId);
            if (doctor == null) {
                return null;
            }
            // 检查当天是否有可用排班
            List<Scheduling> availableSchedules = schedulingService.findAvailable(
                    suggestedDoctorId, LocalDate.now());
            if (availableSchedules != null && !availableSchedules.isEmpty()) {
                return suggestedDoctorId;
            }
            log.info("医生无可用排班: doctorId={}", suggestedDoctorId);
        } catch (Exception e) {
            log.warn("医生排班验证失败: doctorId={}", suggestedDoctorId);
        }
        return null;
    }

    /**
     * 计算置信度
     */
    private BigDecimal calculateConfidence(Long deptId, Long doctorId) {
        BigDecimal score = BigDecimal.ZERO;
        if (deptId != null) {
            score = score.add(new BigDecimal("0.5"));
        }
        if (doctorId != null) {
            score = score.add(new BigDecimal("0.4"));
        }
        score = score.add(new BigDecimal("0.1")); // 基础分
        return score.min(BigDecimal.ONE);
    }

    /**
     * 确定紧急级别
     */
    private String determineEmergencyLevel(String answer) {
        if (answer == null) {
            return "UNKNOWN";
        }
        String lower = answer.toLowerCase();
        if (lower.contains("紧急") || lower.contains("危重") || lower.contains("急诊")) {
            return "RED";
        }
        if (lower.contains("优先") || lower.contains("尽快")) {
            return "YELLOW";
        }
        return "GREEN";
    }

    /**
     * 从AI回答中提取科室ID（简单匹配）
     */
    private Long extractDepartmentIdFromAnswer(String answer) {
        if (answer == null) {
            return null;
        }
        // 尝试匹配 "科室ID: xxx" 或 "departmentId: xxx" 格式
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?:科室|Department)[IDid编号]*[：:]?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(answer);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (Exception e) {
            log.debug("提取科室ID失败");
        }
        return null;
    }

    /**
     * 从AI回答中提取医生ID（简单匹配）
     */
    private Long extractDoctorIdFromAnswer(String answer) {
        if (answer == null) {
            return null;
        }
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?:医生|Doctor)[IDid编号]*[：:]?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(answer);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (Exception e) {
            log.debug("提取医生ID失败");
        }
        return null;
    }

    /**
     * 默认分诊提示词
     */
    private String buildDefaultTriagePrompt() {
        return """
                你是AI智慧医院的智能导诊助手。你的任务是：
                1. 根据患者描述的症状，使用工具查询症状标签、科室信息、医生排班等数据
                2. 基于真实数据做出科室推荐和医生推荐
                3. 如果检测到危险症状（红旗症状），立即建议急诊科

                【核心原则】
                - 只推荐数据库中存在的科室和医生
                - 推荐医生时必须确认其有可用排班
                - 危重症状必须推荐急诊科

                【输出格式】
                科室ID: [推荐科室的ID]
                医生ID: [推荐医生的ID]
                推荐理由: [简明的推荐理由]
                紧急级别: [RED/YELLOW/GREEN]
                """;
    }

    /**
     * 分诊结果DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TriageResult {
        private Long recommendedDepartmentId;
        private Long recommendedDoctorId;
        private BigDecimal confidence;
        private String emergencyLevel;
        private String reason;
        private boolean isEmergency;
    }
}
