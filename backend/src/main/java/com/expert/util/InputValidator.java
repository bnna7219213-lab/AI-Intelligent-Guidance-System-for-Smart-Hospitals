package com.expert.util;

import com.expert.common.BizException;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 输入验证工具
 * 防止恶意输入和注入攻击
 */
public class InputValidator {

    // 常见的 SQL 注入模式
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('|(\\-\\-)|(;)|(\\|)|(\\*)|(\\%27)|(\\%3B)|(\\%2D\\%2D))",
            Pattern.CASE_INSENSITIVE);

    // XSS 攻击模式
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(<script>|<\\/script>|javascript:|onerror=|onload=|eval\\(|expression\\()",
            Pattern.CASE_INSENSITIVE);

    // 路径遍历模式
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./)|(\\.\\.\\\\)|(%2e%2e%2f)|(%2e%2e\\\\)",
            Pattern.CASE_INSENSITIVE);

    // 手机号格式
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    // 身份证号格式
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    // 用户名格式（字母、数字、下划线，4-20位）
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");

    // 密码强度（至少8位，包含字母和数字）
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).{8,}$");

    /**
     * 验证字符串是否安全（防止 SQL 注入和 XSS）
     */
    public static void validateSafeString(String input, String fieldName) {
        if (input == null) {
            return;
        }
        
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            throw new BizException(fieldName + "包含非法字符");
        }
        
        if (XSS_PATTERN.matcher(input).find()) {
            throw new BizException(fieldName + "包含非法脚本");
        }
        
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            throw new BizException(fieldName + "包含非法路径");
        }
    }

    /**
     * 验证用户名格式
     */
    public static void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BizException("用户名不能为空");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BizException("用户名必须是4-20位的字母、数字或下划线");
        }
    }

    /**
     * 验证密码强度
     */
    public static void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BizException("密码不能为空");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BizException("密码至少8位，必须包含字母和数字");
        }
    }

    /**
     * 验证手机号格式
     */
    public static void validatePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return; // 手机号可为空
        }
        
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BizException("手机号格式不正确");
        }
    }

    /**
     * 验证身份证号格式
     */
    public static void validateIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return; // 身份证号可为空
        }
        
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            throw new BizException("身份证号格式不正确");
        }
    }

    /**
     * 验证ID是否为正数
     */
    public static void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BizException(fieldName + "必须为正数");
        }
    }

    /**
     * 验证字符串长度
     */
    public static void validateLength(String input, int minLength, int maxLength, String fieldName) {
        if (input == null) {
            return;
        }
        
        int length = input.length();
        if (length < minLength || length > maxLength) {
            throw new BizException(fieldName + "长度必须在" + minLength + "-" + maxLength + "之间");
        }
    }

    /**
     * 清理字符串（去除首尾空格，防止注入）
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        
        // 去除首尾空格
        String cleaned = input.trim();
        
        // 替换多个连续空格为单个空格
        cleaned = cleaned.replaceAll("\\s+", " ");
        
        return cleaned;
    }
}
