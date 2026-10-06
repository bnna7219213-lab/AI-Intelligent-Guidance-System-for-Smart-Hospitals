package com.expert.mapper;

import com.expert.entity.Scheduling;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 排班表 Mapper
 */
@Mapper
public interface SchedulingMapper {

    @Select("SELECT id, doctor_id, department_id, schedule_date, period, total_count, remain_count, " +
            "version, create_time, update_time FROM scheduling " +
            "WHERE doctor_id = #{doctorId} AND schedule_date >= #{startDate} AND schedule_date <= #{endDate} " +
            "ORDER BY schedule_date ASC, period ASC")
    List<Scheduling> findByDoctorIdAndDateRange(@Param("doctorId") Long doctorId,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);

    @Select("SELECT id, doctor_id, department_id, schedule_date, period, total_count, remain_count, " +
            "version, create_time, update_time FROM scheduling " +
            "WHERE department_id = #{departmentId} AND schedule_date >= #{startDate} AND schedule_date <= #{endDate} " +
            "ORDER BY schedule_date ASC, period ASC")
    List<Scheduling> findByDepartmentIdAndDateRange(@Param("departmentId") Long departmentId,
                                                    @Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);

    @Select("SELECT id, doctor_id, department_id, schedule_date, period, total_count, remain_count, " +
            "version, create_time, update_time FROM scheduling " +
            "WHERE department_id = #{departmentId} AND schedule_date >= #{startDate} " +
            "ORDER BY schedule_date ASC, period ASC")
    List<Scheduling> findByDepartmentId(@Param("departmentId") Long departmentId,
                                         @Param("startDate") String startDate);

    @Select("SELECT id, doctor_id, department_id, schedule_date, period, total_count, remain_count, " +
            "version, create_time, update_time FROM scheduling WHERE id = #{id}")
    Scheduling findById(@Param("id") Long id);

    @Select("SELECT id, doctor_id, department_id, schedule_date, period, total_count, remain_count, " +
            "version, create_time, update_time FROM scheduling ORDER BY schedule_date ASC, period ASC")
    List<Scheduling> findAll();

    @Insert("INSERT INTO scheduling (doctor_id, department_id, schedule_date, period, total_count, " +
            "remain_count, version, create_time, update_time) " +
            "VALUES (#{doctorId}, #{departmentId}, #{scheduleDate}, #{period}, #{totalCount}, " +
            "#{remainCount}, #{version}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Scheduling scheduling);

    @Update("UPDATE scheduling SET doctor_id = #{doctorId}, department_id = #{departmentId}, " +
            "schedule_date = #{scheduleDate}, period = #{period}, total_count = #{totalCount}, " +
            "remain_count = #{remainCount}, version = #{version} + 1, update_time = NOW() " +
            "WHERE id = #{id} AND version = #{version}")
    int update(Scheduling scheduling);

    @Delete("DELETE FROM scheduling WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE scheduling SET remain_count = remain_count - 1, version = version + 1, " +
            "update_time = NOW() WHERE id = #{id} AND version = #{version} AND remain_count > 0")
    int decrementRemain(@Param("id") Long id, @Param("version") Integer version);

    // ===== Service Layer Methods =====

    @Select("SELECT MIN(schedule_date) FROM scheduling WHERE deleted = 0")
    String findMinScheduleDate();

    @Update("UPDATE scheduling SET schedule_date = DATE_ADD(schedule_date, INTERVAL #{delta} DAY), " +
            "remain_count = total_count, version = version + 1, update_time = NOW()")
    int shiftAllDates(@Param("delta") int delta);

    @Select("SELECT department_id, " +
            "SUM(total_count) as total_count, " +
            "SUM(remain_count) as remain_count, " +
            "ROUND((SUM(total_count) - SUM(remain_count)) / SUM(total_count) * 100, 2) as usage_rate " +
            "FROM scheduling WHERE deleted = 0 GROUP BY department_id")
    List<java.util.Map<String, Object>> selectUsageRateByDepartment();
}
