package com.expert;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI智慧医院智能导诊系统 启动类
 *
 * 缓存策略由 RedisCacheConfig 全权管理，Spring Boot Redis 自动装配通过
 * @ConditionalOnMissingBean 自动退让（我们已自定义 RedisConnectionFactory 和 CacheManager）：
 * - Redis 可用 → RedisCacheManager（跨进程共享，TTL 10 分钟）
 * - Redis 不可用 → 本地内存缓存 ConcurrentMapCacheManager（进程内降级）
 * - 缓存完全禁用 → 空缓存 SimpleCacheManager（直查数据库）
 */
@SpringBootApplication
@MapperScan("com.expert.mapper")
public class AiHospitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHospitalApplication.class, args);
    }
}