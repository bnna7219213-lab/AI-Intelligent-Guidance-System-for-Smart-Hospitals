package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent运行记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRun {

    private Long id;

    private Long patientId;

    private String sessionId;

    /** RUNNING/SUCCESS/FAIL */
    private String status;

    private Long recommendedDepartmentId;

    private Long recommendedDoctorId;

    private BigDecimal confidence;

    private String emergencyLevel;

    private String recommendationReason;

    /** 是/否 */
    private String isRegistered;

    private Integer totalSteps;

    private Integer totalTokens;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
