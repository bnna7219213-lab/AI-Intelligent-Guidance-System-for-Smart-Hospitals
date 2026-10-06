package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI配置实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfig {

    private Long id;

    private String configKey;

    private String configName;

    private String apiUrl;

    private String apiKey;

    private String modelName;

    private String extraConfig;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
