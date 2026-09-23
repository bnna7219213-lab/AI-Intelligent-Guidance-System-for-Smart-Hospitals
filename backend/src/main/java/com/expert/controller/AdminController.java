package com.expert.controller;

import com.expert.common.Result;
import com.expert.entity.*;
import com.expert.mapper.AgentRunMapper;
import com.expert.mapper.AgentStepMapper;
import com.expert.mapper.RegistrationMapper;
import com.expert.mapper.SchedulingMapper;
import com.expert.service.*;
import com.expert.util.PageResult;
import com.expert.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器 - 用户管理、科室管理、排班管理、AI配置、知识库、MCP工具、症状标签、运营看板、可观测性
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final SchedulingService schedulingService;
    private final AiConfigService aiConfigService;
    private final PromptTemplateService promptTemplateService;
    private final KbService kbService;
    private final McpToolService mcpToolService;
    private final SymptomTagService symptomTagService;
    private final OperationService operationService;
    private final AiUsageService aiUsageService;
    private final AgentRunMapper agentRunMapper;
    private final AgentStepMapper agentStepMapper;
    private final RegistrationMapper registrationMapper;
    private final SchedulingMapper schedulingMapper;

    // ======================== 用户管理 ========================

    /**
     * 分页查询所有用户
     */
    @GetMapping("/users")
    public Result<PageResult<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            PageResult<SysUser> page = userService.listAll(pageNum, pageSize, keyword);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询用户列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/users/{id}/status")
    public Result<String> toggleUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            userService.toggleStatus(id, status);
            return Result.success("操作成功");
        } catch (Exception e) {
            log.error("修改用户状态失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建医生账号
     */
    @PostMapping("/users/doctor")
    public Result<String> createDoctor(@RequestBody Map<String, Object> params) {
        try {
            String username = (String) params.get("username");
            String password = (String) params.get("password");
            String realName = (String) params.get("realName");
            Long departmentId = params.get("departmentId") != null
                    ? Long.valueOf(params.get("departmentId").toString()) : null;
            String title = (String) params.get("title");
            userService.createDoctor(username, password, realName, departmentId, title);
            return Result.success("医生账号创建成功");
        } catch (Exception e) {
            log.error("创建医生账号失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/users/{id}/reset-pwd")
    public Result<String> resetPassword(@PathVariable Long id) {
        try {
            userService.resetPassword(id);
            return Result.success("密码已重置");
        } catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 科室管理 ========================

    /**
     * 查询所有科室
     */
    @GetMapping("/departments")
    public Result<List<Department>> listDepartments() {
        try {
            List<Department> departments = departmentService.listAll();
            return Result.success(departments);
        } catch (Exception e) {
            log.error("查询科室列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增科室
     */
    @PostMapping("/departments")
    public Result<String> saveDepartment(@RequestBody Department department) {
        try {
            departmentService.save(department);
            return Result.success("科室创建成功");
        } catch (Exception e) {
            log.error("创建科室失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新科室
     */
    @PutMapping("/departments/{id}")
    public Result<String> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        try {
            department.setId(id);
            departmentService.update(department);
            return Result.success("科室更新成功");
        } catch (Exception e) {
            log.error("更新科室失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用科室
     */
    @PutMapping("/departments/{id}/status")
    public Result<String> toggleDepartmentStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            departmentService.toggleStatus(id, status);
            return Result.success("操作成功");
        } catch (Exception e) {
            log.error("修改科室状态失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 排班管理 ========================

    /**
     * 创建排班
     */
    @PostMapping("/schedules")
    public Result<String> createSchedule(@RequestBody Map<String, Object> params) {
        try {
            Long doctorId = Long.valueOf(params.get("doctorId").toString());
            Long departmentId = Long.valueOf(params.get("departmentId").toString());
            LocalDate date = LocalDate.parse(params.get("date").toString());
            String period = (String) params.get("period");
            Integer totalCount = Integer.valueOf(params.get("totalCount").toString());
            BigDecimal fee = params.get("fee") != null
                    ? new BigDecimal(params.get("fee").toString()) : BigDecimal.ZERO;
            schedulingService.createSchedule(doctorId, departmentId, date, period, totalCount, fee);
            return Result.success("排班创建成功");
        } catch (Exception e) {
            log.error("创建排班失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询所有排班
     */
    @GetMapping("/schedules")
    public Result<List<Scheduling>> listSchedules(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now().plusDays(30);
            List<Scheduling> schedules;
            if (departmentId != null) {
                schedules = schedulingService.listByDepartment(departmentId, start, end);
            } else {
                schedules = schedulingMapper.findAll();
            }
            return Result.success(schedules);
        } catch (Exception e) {
            log.error("查询排班失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 排班日期一键顺延
     */
    @PostMapping("/schedules/shift-dates")
    public Result<String> shiftScheduleDates() {
        try {
            schedulingService.shiftAllDates();
            return Result.success("排班日期已顺延");
        } catch (Exception e) {
            log.error("排班日期顺延失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== AI配置 ========================

    /**
     * 查询所有AI配置
     */
    @GetMapping("/ai-configs")
    public Result<List<AiConfig>> listAiConfigs() {
        try {
            List<AiConfig> configs = aiConfigService.listAll();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("查询AI配置失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 保存或更新AI配置
     */
    @PutMapping("/ai-configs")
    public Result<String> saveOrUpdateAiConfig(@RequestBody Map<String, Object> params) {
        try {
            String configKey = (String) params.get("configKey");
            String apiUrl = (String) params.get("apiUrl");
            String apiKey = (String) params.get("apiKey");
            String modelName = (String) params.get("modelName");
            String extraConfig = (String) params.get("extraConfig");
            aiConfigService.saveOrUpdate(configKey, apiUrl, apiKey, modelName, extraConfig);
            return Result.success("配置保存成功");
        } catch (Exception e) {
            log.error("保存AI配置失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 测试API连接
     */
    @PostMapping("/ai-configs/test")
    public Result<Boolean> testAiConfig(@RequestBody Map<String, Object> params) {
        try {
            String apiUrl = (String) params.get("apiUrl");
            String apiKey = (String) params.get("apiKey");
            String modelName = (String) params.get("modelName");
            boolean connected = aiConfigService.testConnection(apiUrl, apiKey, modelName);
            return Result.success(connected);
        } catch (Exception e) {
            log.error("测试AI连接失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 提示词模板 ========================

    /**
     * 查询所有提示词模板
     */
    @GetMapping("/prompts")
    public Result<List<PromptTemplate>> listPrompts() {
        try {
            List<PromptTemplate> templates = promptTemplateService.listAll();
            return Result.success(templates);
        } catch (Exception e) {
            log.error("查询提示词模板失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新提示词模板
     */
    @PutMapping("/prompts")
    public Result<String> updatePrompt(@RequestBody PromptTemplate template) {
        try {
            promptTemplateService.update(template);
            return Result.success("提示词模板更新成功");
        } catch (Exception e) {
            log.error("更新提示词模板失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 知识库 ========================

    /**
     * 查询知识库分组
     */
    @GetMapping("/kb/groups")
    public Result<List<KbGroup>> listKbGroups() {
        try {
            List<KbGroup> groups = kbService.listGroups();
            return Result.success(groups);
        } catch (Exception e) {
            log.error("查询知识库分组失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增知识库分组
     */
    @PostMapping("/kb/groups")
    public Result<String> saveKbGroup(@RequestBody KbGroup group) {
        try {
            kbService.saveGroup(group);
            return Result.success("分组创建成功");
        } catch (Exception e) {
            log.error("创建知识库分组失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 上传知识库文档
     */
    @PostMapping("/kb/documents")
    public Result<String> uploadKbDocument(@RequestParam("file") MultipartFile file,
                                           @RequestParam Long groupId) {
        try {
            Long docId = kbService.docUpload(file, groupId);
            return Result.success("文档上传成功，ID: " + docId);
        } catch (Exception e) {
            log.error("上传知识库文档失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 文档分块并向量化
     */
    @PostMapping("/kb/documents/{id}/embed")
    public Result<String> embedKbDocument(@PathVariable Long id) {
        try {
            kbService.chunkDocument(id);
            return Result.success("文档向量化完成");
        } catch (Exception e) {
            log.error("文档向量化失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 语义搜索
     */
    @PostMapping("/kb/search")
    public Result<List<KbChunk>> searchKb(@RequestBody Map<String, Object> params) {
        try {
            String query = (String) params.get("query");
            int topK = params.get("topK") != null
                    ? Integer.valueOf(params.get("topK").toString()) : 5;
            List<KbChunk> chunks = kbService.semanticSearch(query, topK);
            return Result.success(chunks);
        } catch (Exception e) {
            log.error("语义搜索失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== MCP工具 ========================

    /**
     * 查询所有MCP工具
     */
    @GetMapping("/mcp-tools")
    public Result<List<McpTool>> listMcpTools() {
        try {
            List<McpTool> tools = mcpToolService.listAll();
            return Result.success(tools);
        } catch (Exception e) {
            log.error("查询MCP工具失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增MCP工具
     */
    @PostMapping("/mcp-tools")
    public Result<String> saveMcpTool(@RequestBody McpTool tool) {
        try {
            mcpToolService.save(tool);
            return Result.success("工具创建成功");
        } catch (Exception e) {
            log.error("创建MCP工具失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/停用MCP工具
     */
    @PutMapping("/mcp-tools/{id}/toggle")
    public Result<String> toggleMcpTool(@PathVariable Long id, @RequestParam String status) {
        try {
            mcpToolService.toggleStatus(id, status);
            return Result.success("操作成功");
        } catch (Exception e) {
            log.error("修改MCP工具状态失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 症状标签 ========================

    /**
     * 查询所有症状标签
     */
    @GetMapping("/symptom-tags")
    public Result<List<SymptomTag>> listSymptomTags() {
        try {
            List<SymptomTag> tags = symptomTagService.listAll();
            return Result.success(tags);
        } catch (Exception e) {
            log.error("查询症状标签失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增症状标签
     */
    @PostMapping("/symptom-tags")
    public Result<String> saveSymptomTag(@RequestBody SymptomTag tag) {
        try {
            symptomTagService.save(tag);
            return Result.success("症状标签创建成功");
        } catch (Exception e) {
            log.error("创建症状标签失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除症状标签
     */
    @DeleteMapping("/symptom-tags/{id}")
    public Result<String> deleteSymptomTag(@PathVariable Long id) {
        try {
            symptomTagService.deleteById(id);
            return Result.success("症状标签删除成功");
        } catch (Exception e) {
            log.error("删除症状标签失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 运营看板 ========================

    /**
     * 获取挂号统计数据
     */
    @GetMapping("/operations/registrations")
    public Result<OperationStats> getRegistrationStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            Map<String, Object> stats = operationService.getRegistrationStats(start, end);
            OperationStats vo = OperationStats.builder()
                    .totalRegistrations(toLong(stats.get("totalRegistrations")))
                    .completedRegistrations(toLong(stats.get("completedRegistrations")))
                    .cancelledRegistrations(toLong(stats.get("cancelledRegistrations")))
                    .totalRevenue(toBigDecimal(stats.get("totalRevenue")))
                    .build();
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取挂号统计失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取排班使用率
     */
    @GetMapping("/operations/scheduling-usage")
    public Result<List<SchedulingUsage>> getSchedulingUsageRate() {
        try {
            List<Map<String, Object>> rawList = operationService.getSchedulingUsageRate();
            List<SchedulingUsage> result = new ArrayList<>();
            for (Map<String, Object> map : rawList) {
                result.add(SchedulingUsage.builder()
                        .departmentId(toLong(map.get("departmentId")))
                        .departmentName((String) map.get("departmentName"))
                        .totalSchedules(toLong(map.get("totalSchedules")))
                        .usedSchedules(toLong(map.get("usedSchedules")))
                        .usageRate(toDouble(map.get("usageRate")))
                        .build());
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取排班使用率失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取分诊命中率
     */
    @GetMapping("/operations/triage-hit-rate")
    public Result<List<TriageHitRate>> getTriageHitRate() {
        try {
            List<Map<String, Object>> rawList = operationService.getTriageHitRate();
            List<TriageHitRate> result = new ArrayList<>();
            for (Map<String, Object> map : rawList) {
                result.add(TriageHitRate.builder()
                        .departmentId(toLong(map.get("departmentId")))
                        .departmentName((String) map.get("departmentName"))
                        .totalTriage(toLong(map.get("totalTriage")))
                        .hitCount(toLong(map.get("hitCount")))
                        .hitRate(toDouble(map.get("hitRate")))
                        .build());
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取分诊命中率失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 可观测性 ========================

    /**
     * 获取AI使用统计
     */
    @GetMapping("/observability/ai-usage")
    public Result<AiUsageStats> getAiUsageStats() {
        try {
            List<Map<String, Object>> statistics = aiUsageService.getStatistics();
            long totalRequests = 0;
            long totalTokens = 0;
            long totalDurationMs = 0;
            long successCount = 0;
            long failCount = 0;
            for (Map<String, Object> stat : statistics) {
                long count = toLong(stat.get("requestCount"));
                totalRequests += count;
                totalTokens += toLong(stat.get("totalTokens"));
                totalDurationMs += toLong(stat.get("totalDurationMs"));
                successCount += toLong(stat.get("successCount"));
                failCount += toLong(stat.get("failCount"));
            }
            double avgDurationMs = totalRequests > 0 ? (double) totalDurationMs / totalRequests : 0;
            double successRate = totalRequests > 0 ? (double) successCount / totalRequests : 0;
            AiUsageStats stats = AiUsageStats.builder()
                    .totalRequests(totalRequests)
                    .totalTokens(totalTokens)
                    .totalDurationMs(totalDurationMs)
                    .avgDurationMs(avgDurationMs)
                    .successCount(successCount)
                    .failCount(failCount)
                    .successRate(successRate)
                    .build();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取AI使用统计失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询Agent运行记录
     */
    @GetMapping("/observability/agent-runs")
    public Result<PageResult<AgentRun>> listAgentRuns(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            List<AgentRun> allRuns = agentRunMapper.findAll();
            long total = allRuns.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, allRuns.size());
            List<AgentRun> records = fromIndex < allRuns.size()
                    ? allRuns.subList(fromIndex, toIndex)
                    : new ArrayList<>();
            PageResult<AgentRun> result = PageResult.of(records, total, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询Agent运行记录失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询Agent运行详情（含步骤）
     */
    @GetMapping("/observability/agent-runs/{id}")
    public Result<AgentRunWithSteps> getAgentRunDetail(@PathVariable Long id) {
        try {
            AgentRun run = agentRunMapper.findById(id);
            if (run == null) {
                return Result.error("Agent运行记录不存在");
            }
            List<AgentStep> steps = agentStepMapper.findByRunId(id);
            AgentRunWithSteps detail = AgentRunWithSteps.builder()
                    .run(run)
                    .steps(steps)
                    .build();
            return Result.success(detail);
        } catch (Exception e) {
            log.error("查询Agent运行详情失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ======================== 辅助方法 ========================

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }
}
