package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 症状标签实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomTag {

    private Long id;

    private String name;

    private String description;

    private String relatedDepartments;

    private Integer weight;

    /** 是/否 */
    private String isRedFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
