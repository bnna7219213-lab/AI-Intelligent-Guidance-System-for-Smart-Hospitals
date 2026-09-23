package com.expert.service.impl;

import com.expert.entity.AiUsageLog;
import com.expert.mapper.AiUsageLogMapper;
import com.expert.service.AiUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI使用统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageServiceImpl implements AiUsageService {

    private final AiUsageLogMapper aiUsageLogMapper;

    @Override
    public void log(String usageType, String modelName, int promptTokens, int completionTokens,
                    int totalTokens, long durationMs, boolean success, String errorMsg) {
        AiUsageLog usageLog = AiUsageLog.builder()
                .usageType(usageType)
                .modelName(modelName)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .durationMs((int) durationMs)
                .success(success ? 1 : 0)
                .errorMsg(errorMsg)
                .build();
        aiUsageLogMapper.insert(usageLog);
        log.debug("AI使用日志已记录: type={}, model={}, tokens={}, duration={}ms",
                usageType, modelName, totalTokens, durationMs);
    }

    @Override
    public List<Map<String, Object>> getStatistics() {
        List<AiUsageLog> allLogs = aiUsageLogMapper.findAll();
        if (allLogs == null || allLogs.isEmpty()) {
            return new ArrayList<>();
        }
        // 按usage_type分组聚合
        Map<String, List<AiUsageLog>> groups = new HashMap<>();
        for (AiUsageLog log : allLogs) {
            groups.computeIfAbsent(log.getUsageType(), k -> new ArrayList<>()).add(log);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<AiUsageLog>> entry : groups.entrySet()) {
            String usageType = entry.getKey();
            List<AiUsageLog> logs = entry.getValue();
            long totalTokens = 0;
            long totalDuration = 0;
            int successCount = 0;
            for (AiUsageLog logItem : logs) {
                totalTokens += (logItem.getTotalTokens() != null ? logItem.getTotalTokens() : 0);
                totalDuration += (logItem.getDurationMs() != null ? logItem.getDurationMs() : 0);
                if (logItem.getSuccess() != null && logItem.getSuccess() == 1) {
                    successCount++;
                }
            }
            Map<String, Object> stat = new HashMap<>();
            stat.put("usageType", usageType);
            stat.put("count", logs.size());
            stat.put("avgDuration", logs.isEmpty() ? 0 : (double) totalDuration / logs.size());
            stat.put("totalTokens", totalTokens);
            stat.put("successRate", logs.isEmpty() ? 0 : (double) successCount / logs.size());
            result.add(stat);
        }
        log.info("AI使用统计查询完成: 共{}种使用类型", result.size());
        return result;
    }
}
