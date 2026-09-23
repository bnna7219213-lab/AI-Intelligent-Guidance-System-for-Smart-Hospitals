package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 挂号实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registration {

    private Long id;

    private String registrationNo;

    private Long patientId;

    private Long doctorId;

    private Long departmentId;

    private Long schedulingId;

    private LocalDate visitDate;

    private String period;

    private Long triageId;

    /** REGISTERED/IN_PROGRESS/COMPLETED/CANCELLED */
    private String status;

    private BigDecimal fee;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
