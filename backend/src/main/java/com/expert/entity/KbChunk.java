package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档分块实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbChunk {

    private Long id;

    private Long documentId;

    private Long groupId;

    private String content;

    /** JSON array */
    private String embedding;

    private Integer chunkIndex;

    private LocalDateTime createTime;
}
