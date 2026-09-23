package com.expert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 安全配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 是否需要HTTPS
     */
    private boolean requireHttps = false;

    /**
     * 允许的IP白名单（空表示允许所有）
     */
    private List<String> allowedIps = Arrays.asList();

    /**
     * 是否启用限流
     */
    private boolean rateLimitEnabled = true;

    /**
     * 每分钟最大请求数
     */
    private int maxRequestsPerMinute = 60;

    /**
     * 敏感词过滤（防止XSS）
     */
    private List<String> xssPatterns = Arrays.asList(
            "<script>", "</script>", "javascript:", "onerror=", "onload=",
            "eval(", "expression(", "vbscript:", "onclick=", "onmouseover="
    );

    /**
     * 需要过滤的路径
     */
    private List<String> xssFilterPaths = Arrays.asList(
            "/patient/**", "/doctor/**", "/admin/**"
    );
}
