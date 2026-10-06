package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.Scheduling;
import com.expert.mapper.SchedulingMapper;
import com.expert.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排班服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingServiceImpl implements SchedulingService {

    private final SchedulingMapper schedulingMapper;

    @Override
    public void createSchedule(Long doctorId, Long departmentId, LocalDate date, String period, Integer totalCount, BigDecimal fee) {
        Scheduling scheduling = Scheduling.builder()
                .doctorId(doctorId)
                .departmentId(departmentId)
                .scheduleDate(date)
                .period(period)
                .totalCount(totalCount)
                .remainCount(totalCount)
                .fee(fee)
                .version(0)
                .deleted(0)
                .build();
        schedulingMapper.insert(scheduling);
        log.info("排班已创建: doctorId={}, date={}, period={}", doctorId, date, period);
    }

    @Override
    public List<Scheduling> listByDepartment(Long departmentId, LocalDate startDate, LocalDate endDate) {
        List<Scheduling> list = schedulingMapper.findByDepartmentIdAndDateRange(
                departmentId, startDate.toString(), endDate.toString());
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public void shiftAllDates() {
        // 找到最早的排班日期
        LocalDate minDate = schedulingMapper.findMinScheduleDate();
        if (minDate == null) {
            log.info("没有排班记录，跳过日期顺延");
            return;
        }
        LocalDate today = LocalDate.now();
        // 计算需要平移的天数: delta = today - minDate
        int delta = (int) java.time.temporal.ChronoUnit.DAYS.between(minDate, today);
        if (delta == 0) {
            log.info("排班日期已是最新，无需顺延");
            return;
        }
        // 批量更新所有排班日期 = 原日期 + delta，同时重置 remainCount = totalCount
        int affected = schedulingMapper.shiftAllDates(delta);
        log.info("排班日期一键顺延完成: delta={}, affectedRows={}", delta, affected);
    }

    @Override
    public List<Scheduling> getSchedulesByDepartmentAndDateRange(Long departmentId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6); // 未来7天
        List<Scheduling> list = schedulingMapper.findByDepartmentIdAndDateRange(
                departmentId, startDate.toString(), endDate.toString());
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<Scheduling> findAvailable(Long doctorId, LocalDate date) {
        List<Scheduling> list = schedulingMapper.findByDoctorIdAndDateRange(
                doctorId, date.toString(), date.toString());
        if (list == null) {
            return Collections.emptyList();
        }
        // 仅返回有剩余号源的排班
        return list.stream()
                .filter(s -> s.getRemainCount() != null && s.getRemainCount() > 0)
                .collect(Collectors.toList());
    }
}
