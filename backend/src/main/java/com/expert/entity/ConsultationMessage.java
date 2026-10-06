package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 问诊消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMessage {

    private Long id;

    private String sessionId;

    /** USER/ASSISTANT */
    private String role;

    private String content;

    private LocalDateTime createTime;
}
