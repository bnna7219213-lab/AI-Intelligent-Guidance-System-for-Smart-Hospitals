package com.expert.common;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 带消息的业务异常，默认错误码 500
     *
     * @param message 错误消息
     */
    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 带错误码和消息的业务异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 带消息和原因的业务异常
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    /**
     * 带错误码、消息和原因的业务异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原因
     */
    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
