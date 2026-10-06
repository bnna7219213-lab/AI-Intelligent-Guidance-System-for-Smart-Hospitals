package com.expert.demo;

import com.expert.ai.HospitalAiService;
import com.expert.ai.MilvusVectorStore;
import com.expert.config.MilvusProperties;
import com.expert.entity.AiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Embedding 模型切换与维度同步 Demo
 * 
 * 演示：
 * 1. 如何从 OpenAI text-embedding-3-small 切换到 BGE / 其他模型
 * 2. 如何同步调整 Milvus 集合的向量维度
 * 3. 切换后如何重建向量数据（因不同模型向量空间不兼容）
 */
@Slf4j
@Component
public class EmbeddingModelSwitchDemo {

    private final HospitalAiService hospitalAiService;
    private final MilvusVectorStore milvusVectorStore;
    private final MilvusProperties milvusProperties;

    public EmbeddingModelSwitchDemo(HospitalAiService hospitalAiService,
                                    MilvusVectorStore milvusVectorStore,
                                    MilvusProperties milvusProperties) {
        this.hospitalAiService = hospitalAiService;
        this.milvusVectorStore = milvusVectorStore;
        this.milvusProperties = milvusProperties;
    }

    /**
     * 各主流 Embedding 模型的维度对照表
     */
    public enum EmbeddingModel {
        OPENAI_TEXT_EMBEDDING_3_SMALL("text-embedding-3-small", 1536, "OpenAI"),
        OPENAI_TEXT_EMBEDDING_3_LARGE("text-embedding-3-large", 3072, "OpenAI"),
        BGE_LARGE_ZH_V15("bge-large-zh-v1.5", 1024, "BAAI"),
        BGE_BASE_ZH_V15("bge-base-zh-v1.5", 768, "BAAI"),
        BGE_SMALL_ZH_V15("bge-small-zh-v1.5", 384, "BAAI"),
        BGE_M3("bge-m3", 1024, "BAAI"),
        M3E_LARGE("m3e-large", 1024, "MokaAI"),
        CONCH_EMBEDDING_TEXT("conch-embedding-text", 1024, "SenseTime"),
        DASHSCOPE_TEXT_EMBEDDING_V2("text-embedding-v2", 1536, "DashScope"),
        JINA_EMBEDDINGS_V3("jina-embeddings-v3", 1024, "JinaAI");

        private final String modelName;
        private final int dimension;
        private final String provider;

        EmbeddingModel(String modelName, int dimension, String provider) {
            this.modelName = modelName;
            this.dimension = dimension;
            this.provider = provider;
        }

        public String getModelName() {
            return modelName;
        }

        public int getDimension() {
            return dimension;
        }

        public String getProvider() {
            return provider;
        }

        /**
         * 根据模型名查找对应的维度
         */
        public static int resolveDimension(String modelName) {
            if (modelName == null || modelName.isBlank()) {
                throw new IllegalArgumentException("模型名称不能为空");
            }
            for (EmbeddingModel model : values()) {
                if (model.modelName.equalsIgnoreCase(modelName.trim())) {
                    return model.dimension;
                }
            }
            throw new IllegalArgumentException(
                    "未知的 Embedding 模型: " + modelName + "，请手动指定维度或参考文档");
        }
    }

    /**
     * Demo 1：查看当前使用的 Embedding 模型及维度
     */
    public void inspectCurrentModel() {
        log.info("=== 当前 Milvus 向量维度配置: {} ===", milvusProperties.getDimension());
        log.info("=== Milvus 集合: {} ===", milvusProperties.getCollectionName());

        // 实际项目中可以从 AiConfigService 读取当前模型配置
        // 这里用默认值演示
        log.info("=== 假设当前模型: text-embedding-3-small (dim=1536) ===");

        long vectorCount = milvusVectorStore.count();
        log.info("=== Milvus 现有向量数: {} ===", vectorCount);
    }

    /**
     * Demo 2：从 OpenAI text-embedding-3-small 切换到 BGE-large-zh-v1.5
     * 
     * 切换步骤：
     * 1. 修改 Milvus 集合的向量维度（需重建集合，因维度不同）
     * 2. 修改 application.yml 中 milvus.dimension 配置
     * 3. 重新向量化所有文档
     * 4. 验证检索效果
     */
    public void switchToBgeLarge() {
        EmbeddingModel targetModel = EmbeddingModel.BGE_LARGE_ZH_V15;
        log.info("=== 切换到 {} (维度: {}) ===", targetModel.getModelName(), targetModel.getDimension());

        // 步骤 1: 获取当前维度
        int currentDim = milvusProperties.getDimension();
        int targetDim = targetModel.getDimension();

        if (currentDim == targetDim) {
            log.info("维度相同，无需重建集合，只需切换模型配置");
            return;
        }

        log.info("维度不一致: 当前={}, 目标={}", currentDim, targetDim);
        log.info("⚠️ 警告: 不同模型生成的向量空间不兼容，必须重建集合和重新向量化！");
        log.info("执行流程: 删除旧集合 → 设置新维度 → 重新创建集合 → 重新嵌入所有文档");
    }

    /**
     * Demo 3：展示 Milvus 集合重建的完整逻辑
     * 实际项目中应通过配置重启或管理接口触发
     */
    public void rebuildCollectionForNewDimension(int newDimension) {
        log.info("=== 开始重建 Milvus 集合，新维度: {} ===", newDimension);

        // 实际代码中会调用 Milvus 客户端 API 删除并重建集合
        // 这里只演示逻辑流程
        log.info("Step 1: 删除旧集合 {} ...", milvusProperties.getCollectionName());
        log.info("Step 2: 设置 milvus.dimension={} ...", newDimension);
        log.info("Step 3: 创建新集合（维度={}）...", newDimension);
        log.info("Step 4: 创建向量索引 (AUTOINDEX / COSINE) ...");
        log.info("Step 5: 加载集合到内存 ...");
        log.info("Step 6: 重新对知识库所有文档进行分块和嵌入 ...");
        log.info("Step 7: 验证检索质量 ...");
        log.info("=== 集合重建完成 ===");
    }

    /**
     * Demo 4：验证切换后向量的维度正确性
     * 用于自检，确保 Milvus 中的向量与模型输出维度一致
     */
    public void verifyVectorDimension(String sampleText) {
        log.info("=== 验证向量维度 ===");

        // 实际调用 embedding 模型
        float[] vector = hospitalAiService.embed(sampleText);
        int actualDim = vector.length;
        int expectedDim = milvusProperties.getDimension();

        log.info("模型输出向量维度: {}", actualDim);
        log.info("Milvus 配置维度: {}", expectedDim);

        if (actualDim == expectedDim) {
            log.info("✅ 维度匹配，检索功能正常");
        } else {
            log.error("❌ 维度不匹配！模型输出 {} 维，Milvus 配置 {} 维", actualDim, expectedDim);
            log.error("请调整 milvus.dimension 配置，或使用维度转换工具（见文档）");
        }
    }

    /**
     * Demo 5：维度不匹配时的向量投影转换（降维/升维）
     * 如果不想重建集合，可以使用 PCA 降维或随机投影降维
     * 注意：投影会损失精度，不推荐用于生产环境
     */
    public float[] projectToTargetDimension(float[] sourceVector, int targetDimension) {
        int sourceDim = sourceVector.length;
        if (sourceDim == targetDimension) {
            return sourceVector;
        }

        log.info("维度转换: {} → {} (使用随机投影)", sourceDim, targetDimension);

        if (sourceDim > targetDimension) {
            // 降维：PCA 简化版 - 取前 N 维
            float[] result = new float[targetDimension];
            System.arraycopy(sourceVector, 0, result, 0, targetDimension);
            return result;
        } else {
            // 升维：末尾补零
            float[] result = new float[targetDimension];
            System.arraycopy(sourceVector, 0, result, 0, sourceDim);
            return result;
        }
    }

    /**
     * Demo 6：展示完整的环境变量配置方式（生产环境推荐）
     * 通过环境变量覆盖 Milvus 维度配置，无需修改代码
     */
    public String getRecommendedEnvConfig(EmbeddingModel model) {
        return String.join("\n",
                "# 设置 Milvus 连接信息",
                "export MILVUS_HOST=your-milvus-host",
                "export MILVUS_PORT=19530",
                "",
                "# 设置 Embedding 模型及对应维度",
                "# 模型: " + model.getModelName() + " (" + model.getProvider() + ")",
                "export MILVUS_DIMENSION=" + model.getDimension(),
                "",
                "# OpenAI 兼容 API 配置",
                "export OPENAI_API_KEY=your-api-key",
                "export OPENAI_BASE_URL=https://api.your-provider.com/v1",
                "export OPENAI_EMBEDDING_MODEL=" + model.getModelName()
        );
    }

    /**
     * Demo 7：展示一个完整的 AiConfig 实体示例（用于数据库中存储模型配置）
     */
    public AiConfig buildAiConfigForModel(EmbeddingModel model, String apiUrl, String apiKey) {
        return AiConfig.builder()
                .configKey("EMBEDDING_MODEL")
                .configName("Embedding 模型配置")
                .apiUrl(apiUrl)
                .apiKey(apiKey)
                .modelName(model.getModelName())
                .extraConfig("{\"dimension\":" + model.getDimension() + ",\"provider\":\"" + model.getProvider() + "\"}")
                .build();
    }

    /**
     * Demo 8：批量嵌入并写入 Milvus 的示例
     * 展示如何在新模型下重新向量化文档
     */
    public void reEmbedAndStore(List<String> chunkTexts, Long documentId, Long groupId) {
        log.info("=== 使用新模型重新向量化: chunks={}, documentId={} ===", chunkTexts.size(), documentId);

        List<MilvusVectorStore.VectorEntity> entities = new java.util.ArrayList<>();

        int index = 0;
        for (String chunkText : chunkTexts) {
            try {
                float[] vector = hospitalAiService.embed(chunkText);
                List<Float> vectorList = new java.util.ArrayList<>();
                for (float v : vector) {
                    vectorList.add(v);
                }

                MilvusVectorStore.VectorEntity entity = MilvusVectorStore.VectorEntity.builder()
                        .id(generateChunkId(documentId, index))
                        .groupId(groupId)
                        .documentId(documentId)
                        .chunkIndex(index)
                        .content(chunkText)
                        .vector(vectorList)
                        .build();
                entities.add(entity);
                index++;
            } catch (Exception e) {
                log.error("嵌入失败: chunkIndex={}, error={}", index, e.getMessage());
            }
        }

        boolean success = milvusVectorStore.batchUpsert(entities);
        log.info("批量写入完成: success={}, count={}", success, entities.size());
    }

    /**
     * Demo 9：模拟从 AiConfigService 读取配置并自动同步维度
     */
    public void autoSyncDimensionFromConfig(String configuredModelName) {
        try {
            int dimension = EmbeddingModel.resolveDimension(configuredModelName);
            int currentDimension = milvusProperties.getDimension();

            log.info("模型配置: {}", configuredModelName);
            log.info("识别维度: {}", dimension);
            log.info("当前 Milvus 维度: {}", currentDimension);

            if (dimension != currentDimension) {
                log.warn("⚠️ 维度不一致，需要重建集合！");
                log.warn("建议执行: 更新 milvus.dimension={} 后重启应用", dimension);
            } else {
                log.info("✅ 维度已同步，无需调整");
            }
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            log.error("请参考 docs/EMBEDDING_MODEL_GUIDE.md 中的维度对照表");
        }
    }

    private Long generateChunkId(Long documentId, int index) {
        return documentId * 1000000L + index;
    }
}
