package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 医生实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    private Long id;

    private Long userId;

    private String name;

    private Long departmentId;

    private String title;

    private String specialty;

    private String introduction;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
