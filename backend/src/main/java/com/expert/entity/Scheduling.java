package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scheduling {

    private Long id;

    private Long doctorId;

    private Long departmentId;

    private LocalDate scheduleDate;

    /** MORNING/AFTERNOON */
    private String period;

    private Integer totalCount;

    private Integer remainCount;

    private BigDecimal fee;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
