package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 挂号记录返回值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationVO {

    private Long id;

    private String registrationNo;

    private Long patientId;

    private String patientName;

    private Long doctorId;

    private String doctorName;

    private Long departmentId;

    private String departmentName;

    private LocalDate visitDate;

    private String period;

    private String status;

    private BigDecimal fee;

    private LocalDateTime createTime;
}
