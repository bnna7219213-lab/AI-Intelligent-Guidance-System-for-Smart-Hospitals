package com.expert.service.impl;

import com.expert.mapper.RegistrationMapper;
import com.expert.mapper.SchedulingMapper;
import com.expert.mapper.AgentRunMapper;
import com.expert.service.OperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营看板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationServiceImpl implements OperationService {

    private final RegistrationMapper registrationMapper;
    private final SchedulingMapper schedulingMapper;
    private final AgentRunMapper agentRunMapper;

    @Override
    public Map<String, Object> getRegistrationStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        // 总挂号数
        long totalCount = registrationMapper.countByDateRange(startDate.toString(), endDate.toString());
        result.put("totalCount", totalCount);
        // 按科室统计
        List<Map<String, Object>> byDepartment = registrationMapper.countByDepartmentGroupByDateRange(
                startDate.toString(), endDate.toString());
        result.put("byDepartment", byDepartment != null ? byDepartment : new ArrayList<>());
        // 按医生统计
        List<Map<String, Object>> byDoctor = registrationMapper.countByDoctorGroupByDateRange(
                startDate.toString(), endDate.toString());
        result.put("byDoctor", byDoctor != null ? byDoctor : new ArrayList<>());
        log.info("挂号统计查询完成: {} ~ {}", startDate, endDate);
        return result;
    }

    @Override
    public List<Map<String, Object>> getSchedulingUsageRate() {
        // 按科室统计排班使用率: (total - remain) / total
        List<Map<String, Object>> result = schedulingMapper.selectUsageRateByDepartment();
        log.info("排班使用率查询完成");
        return result != null ? result : new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getTriageHitRate() {
        // 分诊推荐命中率: 对每个推荐科室，统计is_registered='是'的agent_run占比
        List<Map<String, Object>> result = agentRunMapper.selectTriageHitRate();
        log.info("分诊命中率查询完成");
        return result != null ? result : new ArrayList<>();
    }
}
