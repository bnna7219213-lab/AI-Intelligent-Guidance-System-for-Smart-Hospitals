package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.AiConfig;
import com.expert.mapper.AiConfigMapper;
import com.expert.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private final AiConfigMapper aiConfigMapper;

    private static final String CHAT_MODEL_KEY = "chat_model";
    private static final String VECTOR_MODEL_KEY = "vector_model";

    // 简单内存缓存
    private final Map<String, AiConfig> cache = new ConcurrentHashMap<>();

    @Override
    public AiConfig getChatModelConfig() {
        return cache.computeIfAbsent(CHAT_MODEL_KEY, k -> {
            AiConfig config = aiConfigMapper.findByConfigKey(k);
            if (config == null) {
                throw new BizException("聊天模型配置不存在");
            }
            return config;
        });
    }

    @Override
    public AiConfig getVectorModelConfig() {
        return cache.computeIfAbsent(VECTOR_MODEL_KEY, k -> {
            AiConfig config = aiConfigMapper.findByConfigKey(k);
            if (config == null) {
                throw new BizException("向量模型配置不存在");
            }
            return config;
        });
    }

    @Override
    public void saveOrUpdate(String configKey, String apiUrl, String apiKey, String modelName, String extraConfig) {
        AiConfig existing = aiConfigMapper.findByConfigKey(configKey);
        if (existing != null) {
            existing.setApiUrl(apiUrl);
            existing.setApiKey(apiKey);
            existing.setModelName(modelName);
            existing.setExtraConfig(extraConfig);
            aiConfigMapper.updateByKey(existing);
        } else {
            AiConfig config = AiConfig.builder()
                    .configKey(configKey)
                    .configName(configKey)
                    .apiUrl(apiUrl)
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .extraConfig(extraConfig)
                    .build();
            aiConfigMapper.insert(config);
        }
        // 清除缓存
        cache.remove(configKey);
        log.info("AI配置已保存: key={}", configKey);
    }

    @Override
    public boolean testConnection(String apiUrl, String apiKey, String modelName) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            // 发送最小请求体
            String body = String.format("{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":1}", modelName);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = conn.getResponseCode();
            // 200 表示成功；401 表示密钥无效但服务可达；都算可达
            boolean reachable = (responseCode == 200 || responseCode == 401 || responseCode == 400);
            log.info("API连接测试: url={}, responseCode={}, reachable={}", apiUrl, responseCode, reachable);
            // 读取响应体避免连接泄漏
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (Exception e) {
                // 忽略读取错误
            }
            conn.disconnect();
            return reachable;
        } catch (Exception e) {
            log.warn("API连接测试失败: url={}, error={}", apiUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public List<AiConfig> listAll() {
        List<AiConfig> list = aiConfigMapper.findAll();
        return list != null ? list : Collections.emptyList();
    }
}
