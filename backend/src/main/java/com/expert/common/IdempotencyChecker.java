package com.expert.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等性检查器（Redis 分布式版，自动降级）
 *
 * 双后端策略：
 * - Redis 可用 → 使用 StringRedisTemplate + SETNX + TTL，支持多实例部署共享状态
 * - Redis 不可用 → 降级到进程内 ConcurrentHashMap（应用重启后丢失，但业务不阻断）
 *
 * 用于防止挂号、提交等操作的重复提交。
 */
@Slf4j
@Component
public class IdempotencyChecker {

    private static final long EXPIRY_MS = 5 * 60 * 1000; // 5分钟过期
    private static final Duration EXPIRY = Duration.ofMillis(EXPIRY_MS);

    /** Redis 降级时的内存后端 */
    private final Map<String, Long> processedRequests = new ConcurrentHashMap<>();

    /** Redis 连接状态（由 RedisCacheConfig 决定） */
    private boolean redisAvailable = true;

    private final StringRedisTemplate redisTemplate;

    public IdempotencyChecker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 探测 Redis 是否可用（不可用时降级，避免启动即报错）
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
        } catch (Exception e) {
            log.warn("[Idempotency] Redis 不可用，降级到本地内存模式: {}", e.getMessage());
            redisAvailable = false;
        }
    }

    /**
     * 检查请求是否已处理（幂等性检查）
     *
     * @param key 请求唯一标识（用户ID + 操作类型等）
     * @return true 如果请求是新的，false 如果请求已处理
     */
    public boolean isNewRequest(String key) {
        if (redisAvailable) {
            return isNewRequestRedis(key);
        }
        return isNewRequestLocal(key);
    }

    /**
     * 标记请求为已处理
     */
    public void markAsProcessed(String key) {
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, "1", EXPIRY);
                return;
            } catch (Exception e) {
                log.warn("[Idempotency] Redis 写入失败，降级到本地: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        processedRequests.put(key, System.currentTimeMillis());
    }

    /**
     * 清除指定请求记录
     */
    public void clear(String key) {
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("[Idempotency] Redis 删除失败: {}", e.getMessage());
            }
            return;
        }
        processedRequests.remove(key);
    }

    /**
     * Redis 后端：SET NX + TTL，原子操作保证并发下只有一个线程成功
     */
    private boolean isNewRequestRedis(String key) {
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", EXPIRY);
            if (Boolean.TRUE.equals(success)) {
                return true;
            }
            log.warn("Duplicate request detected: {}", key);
            return false;
        } catch (Exception e) {
            log.warn("[Idempotency] Redis 读取失败，降级到本地: {}", e.getMessage());
            redisAvailable = false;
            return isNewRequestLocal(key);
        }
    }

    /**
     * 本地后端：ConcurrentHashMap + TTL 惰性清理
     */
    private boolean isNewRequestLocal(String key) {
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
}