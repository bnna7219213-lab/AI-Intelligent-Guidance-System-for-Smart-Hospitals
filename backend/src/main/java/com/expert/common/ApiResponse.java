package com.expert.common;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一 API 响应包装器
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String traceId;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("操作成功");
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getMessage());
        return response;
    }

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        SUCCESS(200, "操作成功"),
        BAD_REQUEST(400, "请求参数错误"),
        UNAUTHORIZED(401, "未登录或登录已过期"),
        FORBIDDEN(403, "没有权限执行此操作"),
        NOT_FOUND(404, "请求的资源不存在"),
        METHOD_NOT_ALLOWED(405, "请求方法不允许"),
        CONFLICT(409, "数据冲突"),
        TOO_MANY_REQUESTS(429, "请求过于频繁"),
        INTERNAL_ERROR(500, "系统内部错误"),
        SERVICE_UNAVAILABLE(503, "服务暂时不可用");

        private final int code;
        private final String message;

        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
