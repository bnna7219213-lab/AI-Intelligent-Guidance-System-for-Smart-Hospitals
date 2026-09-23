package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分诊命中率值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageHitRate {

    private Long departmentId;

    private String departmentName;

    private long totalTriage;

    private long hitCount;

    private double hitRate;
}
