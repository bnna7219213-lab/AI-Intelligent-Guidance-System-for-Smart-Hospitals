package com.expert.ai;

import cn.hutool.json.JSONUtil;
import com.expert.entity.AgentStep;
import com.expert.entity.McpTool;
import com.expert.entity.McpToolCallLog;
import com.expert.entity.Scheduling;
import com.expert.mapper.AgentStepMapper;
import com.expert.mapper.McpToolCallLogMapper;
import com.expert.service.DepartmentService;
import com.expert.service.DoctorService;
import com.expert.service.McpToolService;
import com.expert.service.SchedulingService;
import com.expert.service.SymptomTagService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * MCP工具执行器 - ReAct循环核心
 * 模型每轮决定调用工具还是给出最终回答，最多10轮迭代
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolExecutor {

    private static final int MAX_ITERATIONS = 10;

    private final McpToolService mcpToolService;
    private final SymptomTagService symptomTagService;
    private final DepartmentService departmentService;
    private final DoctorService doctorService;
    private final SchedulingService schedulingService;
    private final AgentStepMapper agentStepMapper;
    private final McpToolCallLogMapper mcpToolCallLogMapper;
    private final RagService ragService;
    private final ModelFactory modelFactory;

    /**
     * 执行ReAct循环
     *
     * @param runId               Agent运行ID
     * @param conversationMessages 对话上下文（会被修改，添加中间结果）
     * @param toolSpecs           工具规格列表
     * @param stepCallback        每步执行后的回调
     * @return 最终AI回答文本
     */
    public String executeTools(Long runId, List<ChatMessage> conversationMessages,
                               List<McpTool> tools, Consumer<String> stepCallback) {
        // 将McpTool列表转为LangChain4j的ToolSpecification
        List<ToolSpecification> toolSpecifications = buildToolSpecifications(tools);

        ChatLanguageModel chatModel = modelFactory.getChatModel();
        int stepNumber = 0;
        boolean hasRedFlag = false;

        // 预检查红旗症状
        hasRedFlag = checkRedFlagSymptoms(conversationMessages);

        try {
            for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                stepNumber++;

                // 如果有红旗症状，注入紧急提示
                if (hasRedFlag && iteration == 0) {
                    conversationMessages.add(SystemMessage.from(
                            "【紧急警告】检测到红旗症状（危重标识），请优先推荐急诊科室！"));
                }

                // 调用模型
                Response<AiMessage> response = chatModel.generate(conversationMessages, toolSpecifications);
                AiMessage aiMessage = response.content();

                // 保存AI思考步骤
                if (aiMessage.text() != null && !aiMessage.text().isBlank()) {
                    conversationMessages.add(aiMessage);
                    if (stepCallback != null) {
                        stepCallback.accept(aiMessage.text());
                    }
                    saveAgentStep(runId, stepNumber, "THINKING", "", aiMessage.text(), true, null);
                }

                // 检查是否有工具调用请求
                if (aiMessage.hasToolExecutionRequests()) {
                    List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();

                    for (ToolExecutionRequest request : toolRequests) {
                        stepNumber++;
                        String toolCode = request.name();
                        String inputParams = request.arguments();

                        log.info("ReAct循环[{}] 工具调用: toolCode={}, params={}", iteration, toolCode, inputParams);

                        // 执行工具
                        long toolStart = System.currentTimeMillis();
                        String toolResult;
                        boolean toolSuccess;
                        String errorMsg = null;

                        try {
                            toolResult = executeLocalTool(toolCode, inputParams, tools);
                            toolSuccess = true;
                        } catch (Exception e) {
                            toolResult = "工具执行失败: " + e.getMessage();
                            toolSuccess = false;
                            errorMsg = e.getMessage();
                            log.error("工具执行失败: toolCode={}", toolCode, e);
                        }

                        long toolDuration = System.currentTimeMillis() - toolStart;

                        // 保存AgentStep
                        saveAgentStep(runId, stepNumber, "TOOL_CALL", inputParams,
                                toolResult, toolSuccess, errorMsg);

                        // 记录MCP工具调用日志
                        McpTool matchedTool = findToolByCode(tools, toolCode);
                        McpToolCallLog callLog = McpToolCallLog.builder()
                                .toolId(matchedTool != null ? matchedTool.getId() : null)
                                .toolCode(toolCode)
                                .runId(runId)
                                .stepId((long) stepNumber)
                                .inputParams(inputParams)
                                .outputResult(toolResult)
                                .success(toolSuccess ? 1 : 0)
                                .errorMessage(errorMsg)
                                .durationMs((int) toolDuration)
                                .build();
                        mcpToolCallLogMapper.insert(callLog);

                        // 将工具结果加入对话上下文
                        conversationMessages.add(ToolExecutionResultMessage.from(request, toolResult));

                        if (stepCallback != null) {
                            stepCallback.accept("[工具:" + toolCode + "] " + toolResult);
                        }
                    }
                } else {
                    // 模型没有工具调用请求 → 返回最终回答
                    log.info("ReAct循环结束，最终回答已生成，共{}步", stepNumber);
                    return aiMessage.text();
                }
            }

            // 超过最大轮数，强制结束
            log.warn("ReAct循环达到最大轮数({})，强制结束", MAX_ITERATIONS);
            return "分析已完成，请基于以上信息做出选择。";
        } catch (Exception e) {
            log.error("ReAct循环异常: runId={}", runId, e);
            saveAgentStep(runId, stepNumber, "ERROR", "", e.getMessage(), false, e.getMessage());
            throw e;
        }
    }

    /**
     * 执行本地工具 - 路由到对应服务
     */
    private String executeLocalTool(String toolCode, String inputParams, List<McpTool> tools) {
        Map<String, String> params = parseParams(inputParams);

        switch (toolCode) {
            case "query_symptom_tags": {
                String keyword = params.getOrDefault("input", params.getOrDefault("keyword", ""));
                var result = symptomTagService.searchByName(keyword);
                return JSONUtil.toJsonStr(result);
            }
            case "query_departments": {
                String deptCode = params.getOrDefault("code", "");
                if (deptCode != null && !deptCode.isBlank()) {
                    var dept = departmentService.listAll().stream()
                            .filter(d -> deptCode.equals(d.getCode()))
                            .findFirst().orElse(null);
                    return dept != null ? JSONUtil.toJsonStr(dept) : "[]";
                }
                var result = departmentService.listAll();
                return JSONUtil.toJsonStr(result);
            }
            case "query_doctor_schedule": {
                String doctorIdStr = params.getOrDefault("doctorId", params.getOrDefault("doctor_id", ""));
                String dateStr = params.getOrDefault("date", LocalDate.now().toString());
                if (doctorIdStr != null && !doctorIdStr.isBlank()) {
                    Long doctorId = Long.parseLong(doctorIdStr);
                    LocalDate date = LocalDate.parse(dateStr);
                    var result = schedulingService.findAvailable(doctorId, date);
                    return JSONUtil.toJsonStr(result);
                }
                return "[]";
            }
            case "query_knowledge_base": {
                String query = params.getOrDefault("query", params.getOrDefault("input", ""));
                String groupIdStr = params.getOrDefault("groupId", params.getOrDefault("group_id", ""));
                String topKStr = params.getOrDefault("topK", "5");
                Long groupId = groupIdStr != null && !groupIdStr.isBlank() ? Long.parseLong(groupIdStr) : null;
                int topK = Integer.parseInt(topKStr);
                var result = ragService.search(query, groupId, topK);
                return JSONUtil.toJsonStr(result);
            }
            default:
                // 尝试从McpTool描述中匹配
                log.warn("未知工具代码: {}, 尝试通用执行", toolCode);
                return "未知工具: " + toolCode;
        }
    }

    /**
     * 提前检测对话上下文中是否有红旗症状
     */
    private boolean checkRedFlagSymptoms(List<ChatMessage> messages) {
        try {
            var redFlagTags = symptomTagService.findRedFlagSymptoms();
            if (redFlagTags == null || redFlagTags.isEmpty()) {
                return false;
            }
            String conversationText = messages.stream()
                    .map(ChatMessage::toString)
                    .collect(Collectors.joining(" "));
            return redFlagTags.stream()
                    .any(tag -> tag.getName() != null && conversationText.contains(tag.getName()));
        } catch (Exception e) {
            log.warn("红旗症状检测失败", e);
            return false;
        }
    }

    /**
     * 将McpTool列表转为LangChain4j ToolSpecification列表
     */
    private List<ToolSpecification> buildToolSpecifications(List<McpTool> tools) {
        return tools.stream()
                .map(tool -> ToolSpecification.builder()
                        .name(tool.getToolCode())
                        .description(tool.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 根据code查找McpTool
     */
    private McpTool findToolByCode(List<McpTool> tools, String code) {
        return tools.stream()
                .filter(t -> code.equals(t.getToolCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 保存AgentStep记录
     */
    private void saveAgentStep(Long runId, int stepNumber, String stepType,
                               String input, String output, boolean success, String errorMsg) {
        try {
            AgentStep step = AgentStep.builder()
                    .runId(runId)
                    .stepNumber(stepNumber)
                    .toolCode(stepType)
                    .toolName(stepType)
                    .inputParams(input)
                    .outputResult(output != null ? output : "")
                    .success(success ? 1 : 0)
                    .errorMessage(errorMsg)
                    .durationMs(0)
                    .build();
            agentStepMapper.insert(step);
        } catch (Exception e) {
            log.error("保存AgentStep失败: runId={}, stepNumber={}", runId, stepNumber, e);
        }
    }

    /**
     * 解析工具输入参数
     */
    private Map<String, String> parseParams(String inputParams) {
        Map<String, String> result = new HashMap<>();
        if (inputParams == null || inputParams.isBlank()) {
            return result;
        }
        try {
            // 尝试JSON解析
            if (inputParams.trim().startsWith("{")) {
                Map<String, Object> map = JSONUtil.toBean(inputParams, Map.class);
                map.forEach((k, v) -> result.put(k, v != null ? v.toString() : ""));
            } else {
                // 纯文本，作为input参数
                result.put("input", inputParams);
            }
        } catch (Exception e) {
            result.put("input", inputParams);
        }
        return result;
    }
}
