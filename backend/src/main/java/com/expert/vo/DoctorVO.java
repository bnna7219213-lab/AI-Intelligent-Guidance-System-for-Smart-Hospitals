package com.expert.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 医生列表返回值对象（含科室名称）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorVO {

    private Long id;

    private Long userId;

    private String name;

    private Long departmentId;

    private String departmentName;

    private String title;

    private String specialty;

    private String introduction;

    private BigDecimal fee;
}
