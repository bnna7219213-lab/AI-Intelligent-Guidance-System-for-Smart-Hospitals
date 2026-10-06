package com.expert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 缓存配置属性
 *
 * 通过环境变量或配置文件控制：
 * - redis.enabled=false 时完全禁用缓存（直接查库）
 * - redis.enabled=true 且连接成功时使用 Redis
 * - redis.enabled=true 但连接失败时自动降级到本地内存缓存
 */
@Component
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {

    /** 是否启用缓存 */
    private boolean enabled = true;

    /** Redis 主机 */
    private String host = "localhost";

    /** Redis 端口 */
    private int port = 6379;

    /** 密码（可空） */
    private String password = "";

    /** 数据库索引 */
    private int database = 0;

    /** 连接超时（毫秒） */
    private int connectTimeoutMs = 3000;

    /** 默认缓存过期时间（秒） */
    private long defaultTtlSeconds = 600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public void setDefaultTtlSeconds(long defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }
}