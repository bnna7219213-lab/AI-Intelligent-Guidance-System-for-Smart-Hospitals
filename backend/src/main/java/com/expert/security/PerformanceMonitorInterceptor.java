package com.expert.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 性能监控拦截器
 * 记录请求处理时间，识别慢请求
 */
@Slf4j
@Component
public class PerformanceMonitorInterceptor implements HandlerInterceptor {

    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000; // 1秒
    private static final String START_TIME_ATTRIBUTE = "performanceStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime == null) {
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        String uri = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();

        // 记录慢请求
        if (duration > SLOW_REQUEST_THRESHOLD_MS) {
            log.warn("Slow request detected: {} {} took {}ms, status={}", 
                    method, uri, duration, status);
        } else {
            log.debug("Request completed: {} {} took {}ms, status={}", 
                    method, uri, duration, status);
        }
    }
}
