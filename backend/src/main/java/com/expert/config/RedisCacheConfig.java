package com.expert.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 缓存配置（Spring Boot 4.x / Spring Data Redis 4.x / Jackson 3）
 *
 * 与 MilvusConfig 采用相同策略：
 * - redis.enabled=false  → 完全禁用缓存（CacheManager 直查库，空缓存）
 * - redis.enabled=true   → 尝试连接 Redis
 *     - 连接成功 → 使用 RedisCacheManager（JSON 序列化 + TTL）
 *     - 连接失败 → 降级到本地内存缓存 ConcurrentMapCacheManager（进程内缓存）
 *
 * 三种模式下 Spring Cache 注解（@Cacheable/@CacheEvict/@CachePut）无需改动，
 * 通过替换 CacheManager Bean 实现透明切换。
 *
 * 注意：
 * - Spring Boot 4.x 使用 Jackson 3（包名 tools.jackson.*），
 *   GenericJackson2JsonRedisSerializer 已被移除，改用 GenericJacksonJsonRedisSerializer。
 * - JavaTime 序列化支持由 jackson-databind 内置的 JavaTimeInitializer 自动注册，无需手动 addModule。
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private final RedisProperties properties;

    /** Redis 连接是否成功（降级判定用） */
    private final AtomicBoolean redisAvailable = new AtomicBoolean(false);

    public RedisCacheConfig(RedisProperties properties) {
        this.properties = properties;
    }

    /**
     * 缓存管理器：
     * 优先 Redis，连接失败自动降级到内存缓存
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        if (!properties.isEnabled()) {
            log.info("[Cache] Redis 缓存已禁用 (redis.enabled=false)，使用空缓存，全部直查数据库");
            return emptyCacheManager();
        }

        // 探测 Redis 连接（快速失败，不阻塞启动）
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            try {
                connection.ping();
            } finally {
                connection.close();
            }
            redisAvailable.set(true);
            log.info("[Cache] Redis 连接成功: {}:{}/{}，启用 RedisCacheManager, TTL={}s",
                    properties.getHost(), properties.getPort(),
                    properties.getDatabase(), properties.getDefaultTtlSeconds());
            return redisCacheManager(redisConnectionFactory);
        } catch (Exception e) {
            redisAvailable.set(false);
            log.warn("[Cache] Redis 连接失败，降级到本地内存缓存: {}", e.getMessage());
            return localCacheManager();
        }
    }

    /**
     * Redis 连接工厂：显式配置，避免依赖自动装配
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getHost());
        config.setPort(properties.getPort());
        config.setDatabase(properties.getDatabase());
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            config.setPassword(properties.getPassword());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        // 命令超时：避免启动时长时间阻塞
        factory.setTimeout(properties.getConnectTimeoutMs());
        factory.setShareNativeConnection(true);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Redis CacheManager：JSON 序列化 + TTL
     */
    private CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        GenericJacksonJsonRedisSerializer valueSerializer = GenericJacksonJsonRedisSerializer
                .builder()
                .enableDefaultTyping(typeValidator())
                .build();

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(properties.getDefaultTtlSeconds()))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .transactionAware()
                .build();
    }

    /**
     * 多态类型校验器：限制允许反序列化的类型，避免任意类加载攻击。
     * 仅放行本项目实体（com.expert.*）与 JDK 集合/时间类型（java.*）。
     */
    private PolymorphicTypeValidator typeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.expert.")
                .allowIfSubType("java.")
                .build();
    }

    /**
     * 本地内存缓存：应用重启后清空，仅进程内共享
     *
     * 注意：aiConfig 不在此列表内 —— AiConfig 含 apiKey，禁止序列化到 Redis 外部存储，
     * 因此 AI 配置保留进程内 ConcurrentHashMap + DB 双保险（见 AiConfigServiceImpl）。
     */
    private CacheManager localCacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.setCacheNames(List.of(
                "departments",
                "schedules",
                "symptomTags",
                "kbSearch"
        ));
        return manager;
    }

    /**
     * 空缓存：所有方法直查数据库
     */
    private CacheManager emptyCacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(new ArrayList<>());
        return manager;
    }

    /**
     * Redis 当前是否可用（供监控/调试用）
     */
    public boolean isRedisAvailable() {
        return redisAvailable.get();
    }
}