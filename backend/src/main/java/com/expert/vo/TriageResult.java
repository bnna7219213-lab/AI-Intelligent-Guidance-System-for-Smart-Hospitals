package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 分诊结果值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageResult {

    private Long recommendedDepartmentId;

    private Long recommendedDoctorId;

    private BigDecimal confidence;

    private String emergencyLevel;

    private String reason;

    private boolean isEmergency;
}
