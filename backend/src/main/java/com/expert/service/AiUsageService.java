package com.expert.service;

import java.util.List;
import java.util.Map;

/**
 * AI使用统计服务接口
 */
public interface AiUsageService {

    /**
     * 记录AI调用日志
     *
     * @param usageType        使用类型 (CHAT/SSE_CHAT/REACT/EMBEDDING)
     * @param modelName        模型名称
     * @param promptTokens     prompt token数
     * @param completionTokens completion token数
     * @param totalTokens      总token数
     * @param durationMs       耗时毫秒
     * @param success          是否成功
     * @param errorMsg         错误信息
     */
    void log(String usageType, String modelName, int promptTokens, int completionTokens,
             int totalTokens, long durationMs, boolean success, String errorMsg);

    /**
     * 获取使用统计（按usage_type分组聚合）
     *
     * @return 统计列表
     */
    List<Map<String, Object>> getStatistics();
}
