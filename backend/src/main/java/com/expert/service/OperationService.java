package com.expert.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 运营看板服务接口
 */
public interface OperationService {

    /**
     * 获取挂号统计数据
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计数据
     */
    Map<String, Object> getRegistrationStats(LocalDate startDate, LocalDate endDate);

    /**
     * 获取排班使用率（按科室）
     *
     * @return 各科室使用率
     */
    List<Map<String, Object>> getSchedulingUsageRate();

    /**
     * 获取分诊推荐命中率
     *
     * @return 各科室分诊命中率
     */
    List<Map<String, Object>> getTriageHitRate();
}
