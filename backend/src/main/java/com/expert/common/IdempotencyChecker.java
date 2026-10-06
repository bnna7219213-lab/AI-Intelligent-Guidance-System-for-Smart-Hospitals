package com.expert.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的幂等性检查器
 * 防止重复提交导致的重复数据
 */
@Slf4j
@Component
public class IdempotencyChecker {

    private final Map<String, Long> processedRequests = new ConcurrentHashMap<>();
    private static final long EXPIRY_MS = 5 * 60 * 1000; // 5分钟过期

    /**
     * 检查请求是否已处理（幂等性检查）
     *
     * @param key 请求唯一标识（可以是请求ID、用户ID+操作类型等）
     * @return true 如果请求是新的，false 如果请求已处理
     */
    public boolean isNewRequest(String key) {
        long now = System.currentTimeMillis();
        
        // 清理过期记录
        processedRequests.entrySet().removeIf(entry -> now - entry.getValue() > EXPIRY_MS);
        
        // 检查是否已存在
        Long existingTimestamp = processedRequests.putIfAbsent(key, now);
        
        if (existingTimestamp != null) {
            log.warn("Duplicate request detected: {}", key);
            return false;
        }
        
        return true;
    }

    /**
     * 标记请求为已处理
     */
    public void markAsProcessed(String key) {
        processedRequests.put(key, System.currentTimeMillis());
    }

    /**
     * 清除指定请求记录
     */
    public void clear(String key) {
        processedRequests.remove(key);
    }
}
