package com.expert.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param ex BizException
     * @return Result
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid）
     *
     * @param ex MethodArgumentNotValidException
     * @return Result
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation exception: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理参数绑定异常（表单）
     *
     * @param ex BindException
     * @return Result
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Bind exception: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理请求体解析异常
     *
     * @param ex HttpMessageNotReadableException
     * @return Result
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage());
        return Result.error(400, "请求参数格式错误");
    }

    /**
     * 处理 IllegalArgument 异常
     *
     * @param ex IllegalArgumentException
     * @return Result
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return Result.error(400, ex.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     *
     * @param ex Exception
     * @return Result
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("Unexpected exception: ", ex);
        return Result.error(500, "系统内部错误，请联系管理员");
    }
}
