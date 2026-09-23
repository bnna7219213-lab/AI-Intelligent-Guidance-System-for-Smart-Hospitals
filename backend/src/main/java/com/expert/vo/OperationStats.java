package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 运营统计值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationStats {

    private long totalRegistrations;

    private long completedRegistrations;

    private long cancelledRegistrations;

    private BigDecimal totalRevenue;
}
