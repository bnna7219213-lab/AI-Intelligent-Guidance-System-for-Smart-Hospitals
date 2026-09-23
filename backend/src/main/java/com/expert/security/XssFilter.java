package com.expert.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * XSS 过滤器
 * 过滤请求参数中的恶意脚本
 */
@Slf4j
@Component
public class XssFilter implements Filter {

    @Autowired
    private com.expert.config.SecurityProperties securityProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        
        // 检查是否需要过滤
        boolean shouldFilter = securityProperties.getXssFilterPaths().stream()
                .anyMatch(pattern -> pathMatches(pattern, path));
        
        if (shouldFilter) {
            chain.doFilter(new XssRequestWrapper(httpRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean pathMatches(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return pattern.equals(path);
    }

    /**
     * XSS 请求包装器
     */
    public static class XssRequestWrapper extends HttpServletRequestWrapper {

        private final com.expert.config.SecurityProperties securityProperties;

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
            this.securityProperties = null; // 简化处理，实际应通过构造函数传入
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] sanitizedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitizedValues[i] = sanitize(values[i]);
            }
            return sanitizedValues;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return sanitize(value);
        }

        private String sanitize(String value) {
            if (value == null) {
                return null;
            }
            
            // 简单的 XSS 过滤
            String sanitized = value;
            String[] patterns = {
                "<script>", "</script>", "javascript:", "onerror=", "onload=",
                "eval(", "expression(", "vbscript:", "onclick=", "onmouseover=",
                "<iframe", "</iframe>", "<object", "</object>", "<embed"
            };
            
            for (String pattern : patterns) {
                sanitized = sanitized.replaceAll("(?i)" + Pattern.quote(pattern), "");
            }
            
            return sanitized;
        }
    }
}
