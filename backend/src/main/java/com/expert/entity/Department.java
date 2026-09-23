package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 科室实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    private Long id;

    private String name;

    private String code;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
