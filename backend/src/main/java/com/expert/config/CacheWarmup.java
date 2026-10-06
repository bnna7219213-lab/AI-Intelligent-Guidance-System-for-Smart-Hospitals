package com.expert.config;

import com.expert.service.DepartmentService;
import com.expert.service.SymptomTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 缓存预热（异步）
 *
 * 应用启动后，用后台线程主动把高频热点数据写入缓存：
 * - 科室列表（前端导航、挂号必查）
 * - 症状标签全部 + 红旗症状（分诊 Agent 核心依据）
 *
 * 后台异步执行，不阻塞 Spring Boot 启动流程。
 * 若 Redis/缓存不可用或数据为空，静默跳过，不影响主流程。
 */
@Slf4j
@Component
public class CacheWarmup implements ApplicationRunner {

    private final DepartmentService departmentService;
    private final SymptomTagService symptomTagService;

    public CacheWarmup(DepartmentService departmentService,
                       SymptomTagService symptomTagService) {
        this.departmentService = departmentService;
        this.symptomTagService = symptomTagService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread warmupThread = new Thread(this::doWarmup, "cache-warmup");
        warmupThread.setDaemon(true);
        warmupThread.start();
    }

    private void doWarmup() {
        log.info("[CacheWarmup] 后台预热线程启动");

        // 预热科室列表
        try {
            int deptCount = departmentService.listAll().size();
            log.info("[CacheWarmup] 科室缓存预热完成: {} 个科室", deptCount);
        } catch (Exception e) {
            log.warn("[CacheWarmup] 科室预热失败（不影响启动）: {}", e.getMessage());
        }

        // 预热症状标签（全部 + 红旗）
        try {
            int tagCount = symptomTagService.listAll().size();
            int redFlagCount = symptomTagService.findRedFlagSymptoms().size();
            log.info("[CacheWarmup] 症状标签缓存预热完成: {} 个标签, {} 个红旗",
                    tagCount, redFlagCount);
        } catch (Exception e) {
            log.warn("[CacheWarmup] 症状标签预热失败（不影响启动）: {}", e.getMessage());
        }

        log.info("[CacheWarmup] 缓存预热流程结束");
    }
}