package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbDocument {

    private Long id;

    private Long groupId;

    private String title;

    private String fileName;

    /** MEDIUMTEXT */
    private String fileContent;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
