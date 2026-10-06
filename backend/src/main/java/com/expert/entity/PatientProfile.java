package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 患者档案实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfile {

    private Long id;

    private Long userId;

    private String name;

    private String gender;

    private Integer age;

    private String chronicHistory;

    private String allergyHistory;

    private String phone;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
