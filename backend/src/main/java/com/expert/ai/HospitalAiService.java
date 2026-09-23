package com.expert.ai;

import com.expert.entity.AiUsageLog;
import com.expert.service.AiUsageService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 医院AI统一出口 - 所有模型调用通过此类
 * 负责模型调用、用量统计、错误翻译
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalAiService {

    private final ModelFactory modelFactory;
    private final AiUsageService aiUsageService;

    /**
     * 同步聊天
     *
     * @param userMessage  用户消息
     * @param systemPrompt 系统提示词
     * @return AI回复文本
     */
    public String chatSync(String userMessage, String systemPrompt) {
        long startTime = System.currentTimeMillis();
        String modelName = "unknown";
        try {
            modelName = modelFactory.getChatModel().toString();
            List<ChatMessage> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(SystemMessage.from(systemPrompt));
            }
            messages.add(UserMessage.from(userMessage));

            Response<AiMessage> response = modelFactory.getChatModel().generate(messages);
            String result = response.content().text();

            long duration = System.currentTimeMillis() - startTime;
            int totalTokens = response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : 0;
            int promptTokens = response.tokenUsage() != null ? response.tokenUsage().inputTokenCount() : 0;
            int completionTokens = response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : 0;

            logUsage("CHAT", modelName, duration,
                    promptTokens, completionTokens, totalTokens, true, null);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = translateError(e);
            log.error("chatSync失败: {}", errorMsg, e);
            logUsage("CHAT", modelName, duration, 0, 0, 0, false, errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * 流式聊天 (SSE)
     *
     * @param userMessage 用户消息
     * @param systemPrompt 系统提示词
     * @param onToken 每个文本块到达时的回调
     * @param onComplete 完成时的回调
     * @param onError 错误回调
     */
    public void chatSse(String userMessage, String systemPrompt,
                        Consumer<String> onToken, Runnable onComplete,
                        Consumer<Throwable> onError) {
        long startTime = System.currentTimeMillis();
        String modelName = "unknown";

        try {
            StreamingChatLanguageModel streamingModel = modelFactory.getStreamingChatModel();
            modelName = streamingModel.toString();

            List<ChatMessage> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(SystemMessage.from(systemPrompt));
            }
            messages.add(UserMessage.from(userMessage));

            StringBuilder fullResponse = new StringBuilder();

            streamingModel.generate(messages, new StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    if (onToken != null) {
                        onToken.accept(token);
                    }
                    fullResponse.append(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    long duration = System.currentTimeMillis() - startTime;
                    int totalTokens = response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : 0;
                    int promptTokens = response.tokenUsage() != null ? response.tokenUsage().inputTokenCount() : 0;
                    int completionTokens = response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : 0;

                    logUsage("SSE_CHAT", modelName, duration,
                            promptTokens, completionTokens, totalTokens, true, null);

                    log.info("SSE_CHAT完成: duration={}ms, tokens={}", duration, totalTokens);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }

                @Override
                public void onError(Throwable error) {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorMsg = translateError(error);
                    log.error("SSE_CHAT失败: {}", errorMsg, error);
                    logUsage("SSE_CHAT", modelName, duration, 0, 0, 0, false, errorMsg);
                    if (onError != null) {
                        onError.accept(new RuntimeException(errorMsg, error));
                    }
                }
            });
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = translateError(e);
            log.error("SSE_CHAT初始化失败: {}", errorMsg, e);
            logUsage("SSE_CHAT", modelName, duration, 0, 0, 0, false, errorMsg);
            if (onError != null) {
                onError.accept(new RuntimeException(errorMsg, e));
            }
        }
    }

    /**
     * 向量嵌入
     *
     * @param text 待嵌入文本
     * @return 浮点向量数组
     */
    public float[] embed(String text) {
        long startTime = System.currentTimeMillis();
        String modelName = "unknown";

        try {
            EmbeddingModel embeddingModel = modelFactory.getEmbeddingModel();
            modelName = embeddingModel.toString();

            Response<dev.langchain4j.model.embedding.Embedding> response = embeddingModel.embed(text);
            float[] vector = response.content().vector();

            long duration = System.currentTimeMillis() - startTime;
            int totalTokens = response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : 0;

            logUsage("EMBEDDING", modelName, duration, totalTokens, 0, totalTokens, true, null);

            return vector;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = translateError(e);
            log.error("embed失败: {}", errorMsg, e);
            logUsage("EMBEDDING", modelName, duration, 0, 0, 0, false, errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * 记录AI用量日志
     */
    public void logUsage(String usageType, String modelName, long durationMs, AiUsageLog usage) {
        try {
            aiUsageService.log(
                    usageType,
                    modelName,
                    usage.getPromptTokens() != null ? usage.getPromptTokens() : 0,
                    usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0,
                    usage.getTotalTokens() != null ? usage.getTotalTokens() : 0,
                    durationMs,
                    usage.getSuccess() != null && usage.getSuccess() == 1,
                    usage.getErrorMsg()
            );
        } catch (Exception e) {
            log.error("记录用量日志失败: usageType={}, modelName={}", usageType, modelName, e);
        }
    }

    /**
     * 内部用量日志记录（使用分参数）
     */
    private void logUsage(String usageType, String modelName, long durationMs,
                          int promptTokens, int completionTokens, int totalTokens,
                          boolean success, String errorMsg) {
        try {
            aiUsageService.log(usageType, modelName, promptTokens, completionTokens,
                    totalTokens, durationMs, success, errorMsg);
        } catch (Exception e) {
            log.error("记录用量日志失败: usageType={}, modelName={}", usageType, modelName, e);
        }
    }

    /**
     * 错误翻译 - 将常见API异常转为中文提示
     */
    String translateError(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return "网络连接超时，请检查API地址配置";
        }
        if (t instanceof ConnectException) {
            return "无法连接到AI服务，请检查网络或API地址";
        }

        String message = t.getMessage() != null ? t.getMessage().toLowerCase() : "";

        if (message.contains("401") || message.contains("unauthorized")) {
            return "API密钥无效或已过期，请检查配置";
        }
        if (message.contains("403") || message.contains("forbidden")) {
            return "API访问被拒绝，请检查账户状态";
        }
        if (message.contains("429") || message.contains("rate limit") || message.contains("too many requests")) {
            return "请求过于频繁，请稍后重试";
        }
        if (message.contains("500") || message.contains("502") || message.contains("503")
                || message.contains("internal server error") || message.contains("bad gateway")
                || message.contains("service unavailable")) {
            return "AI服务暂时不可用，请稍后重试";
        }

        return "AI服务调用失败: " + t.getMessage();
    }
}
