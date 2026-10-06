package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Prompt模板实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {

    private Long id;

    private String templateKey;

    private String templateName;

    private String templateContent;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
