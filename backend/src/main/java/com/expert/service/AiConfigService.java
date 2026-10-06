package com.expert.service;

import com.expert.entity.AiConfig;

import java.util.List;

/**
 * AI配置服务接口
 */
public interface AiConfigService {

    /**
     * 获取聊天模型配置
     *
     * @return AI配置
     */
    AiConfig getChatModelConfig();

    /**
     * 获取向量模型配置
     *
     * @return AI配置
     */
    AiConfig getVectorModelConfig();

    /**
     * 保存或更新配置
     *
     * @param configKey   配置键
     * @param apiUrl      API地址
     * @param apiKey      API密钥
     * @param modelName   模型名称
     * @param extraConfig 扩展配置
     */
    void saveOrUpdate(String configKey, String apiUrl, String apiKey, String modelName, String extraConfig);

    /**
     * 测试API连接是否可用
     *
     * @param apiUrl    API地址
     * @param apiKey    API密钥
     * @param modelName 模型名称
     * @return true if connection successful
     */
    boolean testConnection(String apiUrl, String apiKey, String modelName);

    /**
     * 查询所有配置
     *
     * @return 配置列表
     */
    List<AiConfig> listAll();
}
