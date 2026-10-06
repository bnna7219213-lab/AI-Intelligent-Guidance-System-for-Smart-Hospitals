package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排班使用率值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingUsage {

    private Long departmentId;

    private String departmentName;

    private long totalSchedules;

    private long usedSchedules;

    private double usageRate;
}
