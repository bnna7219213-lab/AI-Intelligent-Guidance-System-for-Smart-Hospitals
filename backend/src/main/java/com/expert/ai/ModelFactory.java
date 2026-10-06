package com.expert.ai;

import com.expert.entity.AiConfig;
import com.expert.service.AiConfigService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型工厂 - 基于数据库配置构建LangChain4j模型实例
 * 支持配置热更新：当管理员修改配置后，清除缓存下次重建
 */
@Slf4j
@Component
public class ModelFactory {

    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final AiConfigService aiConfigService;

    private final Map<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingChatModelCache = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    public ModelFactory(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    /**
     * 获取聊天模型（非流式）
     */
    public ChatLanguageModel getChatModel() {
        AiConfig config = aiConfigService.getChatModelConfig();
        String cacheKey = buildCacheKey(config);
        return chatModelCache.computeIfAbsent(cacheKey, k -> {
            log.info("构建ChatLanguageModel: modelName={}, apiUrl={}", config.getModelName(), config.getApiUrl());
            return OpenAiChatModel.builder()
                    .baseUrl(config.getApiUrl())
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .temperature(DEFAULT_TEMPERATURE)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        });
    }

    /**
     * 获取流式聊天模型
     */
    public StreamingChatLanguageModel getStreamingChatModel() {
        AiConfig config = aiConfigService.getChatModelConfig();
        String cacheKey = "stream_" + buildCacheKey(config);
        return streamingChatModelCache.computeIfAbsent(cacheKey, k -> {
            log.info("构建StreamingChatLanguageModel: modelName={}, apiUrl={}", config.getModelName(), config.getApiUrl());
            return OpenAiStreamingChatModel.builder()
                    .baseUrl(config.getApiUrl())
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .temperature(DEFAULT_TEMPERATURE)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        });
    }

    /**
     * 获取向量嵌入模型
     */
    public EmbeddingModel getEmbeddingModel() {
        AiConfig config = aiConfigService.getVectorModelConfig();
        String cacheKey = buildCacheKey(config);
        return embeddingModelCache.computeIfAbsent(cacheKey, k -> {
            log.info("构建EmbeddingModel: modelName={}, apiUrl={}", config.getModelName(), config.getApiUrl());
            return OpenAiEmbeddingModel.builder()
                    .baseUrl(config.getApiUrl())
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .build();
        });
    }

    /**
     * 清除所有模型缓存 - 管理员更新配置时调用
     */
    public void clearCache() {
        chatModelCache.clear();
        streamingChatModelCache.clear();
        embeddingModelCache.clear();
        log.info("ModelFactory缓存已清除");
    }

    private String buildCacheKey(AiConfig config) {
        return config.getConfigKey() + ":" + config.getApiUrl() + ":" + config.getModelName();
    }
}
