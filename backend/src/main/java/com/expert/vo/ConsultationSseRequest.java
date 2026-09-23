package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问诊SSE请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSseRequest {

    /**
     * 患者输入的症状/问题文本
     */
    private String message;

    /**
     * 会话ID（可选，为空则自动创建新会话）
     */
    private String sessionId;
}
