package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 问诊会话实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSession {

    private Long id;

    private String sessionId;

    private Long patientId;

    private String title;

    /** ACTIVE/ENDED */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
