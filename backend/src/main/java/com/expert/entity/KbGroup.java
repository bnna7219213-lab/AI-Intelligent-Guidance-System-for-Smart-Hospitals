package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库分组实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbGroup {

    private Long id;

    private String groupName;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
