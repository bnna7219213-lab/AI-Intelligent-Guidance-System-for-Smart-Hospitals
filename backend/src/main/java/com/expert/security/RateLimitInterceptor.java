package com.expert.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单的基于内存的限流拦截器
 * 防止暴力破解和恶意攻击
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 60; // 每分钟最多60次请求
    private static final long WINDOW_MS = 60 * 1000; // 1分钟窗口

    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String key = clientIp + ":" + request.getRequestURI();

        RequestCounter counter = counters.computeIfAbsent(key, k -> new RequestCounter());

        // 清理过期窗口
        long now = System.currentTimeMillis();
        if (now - counter.windowStart > WINDOW_MS) {
            counter.reset(now);
        }

        // 检查限流
        if (counter.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
            return false;
        }

        return true;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static class RequestCounter {
        volatile long windowStart;
        final AtomicInteger count;

        RequestCounter() {
            this.windowStart = System.currentTimeMillis();
            this.count = new AtomicInteger(0);
        }

        void reset(long now) {
            this.windowStart = now;
            this.count.set(0);
        }
    }
}
