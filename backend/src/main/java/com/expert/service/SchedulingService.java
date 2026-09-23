package com.expert.service;

import com.expert.entity.Scheduling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 排班服务接口
 */
public interface SchedulingService {

    /**
     * 创建排班
     *
     * @param doctorId     医生ID
     * @param departmentId 科室ID
     * @param date         排班日期
     * @param period       时段 (MORNING/AFTERNOON)
     * @param totalCount   总号数
     * @param fee          挂号费
     */
    void createSchedule(Long doctorId, Long departmentId, LocalDate date, String period, Integer totalCount, BigDecimal fee);

    /**
     * 查询科室排班
     *
     * @param departmentId 科室ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 排班列表
     */
    List<Scheduling> listByDepartment(Long departmentId, LocalDate startDate, LocalDate endDate);

    /**
     * 排班日期一键顺延：将所有排班日期平移至以今天为基准
     * 计算 delta = today - min(schedule_date)，然后将所有日期 + delta，
     * 同时重置 remainCount = totalCount。
     */
    void shiftAllDates();

    /**
     * 患者查看未来7天排班
     *
     * @param departmentId 科室ID
     * @param startDate    起始日期
     * @return 排班列表
     */
    List<Scheduling> getSchedulesByDepartmentAndDateRange(Long departmentId, LocalDate startDate);

    /**
     * 查询医生指定日期是否有可用排班
     *
     * @param doctorId 医生ID
     * @param date     日期
     * @return 可用排班列表（remainCount > 0）
     */
    List<Scheduling> findAvailable(Long doctorId, LocalDate date);
}
