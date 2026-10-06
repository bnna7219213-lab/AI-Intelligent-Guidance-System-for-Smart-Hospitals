package com.expert.mapper;

import com.expert.entity.Registration;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 挂号表 Mapper
 */
@Mapper
public interface RegistrationMapper {

    @Select("SELECT id, registration_no, patient_id, doctor_id, scheduling_id, department_id, " +
            "visit_date, period, status, fee, triage_id, create_time, update_time " +
            "FROM registration WHERE id = #{id}")
    Registration findById(@Param("id") Long id);

    @Select("SELECT id, registration_no, patient_id, doctor_id, scheduling_id, department_id, " +
            "visit_date, period, status, fee, triage_id, create_time, update_time " +
            "FROM registration WHERE registration_no = #{registrationNo} LIMIT 1")
    Registration findByRegistrationNo(@Param("registrationNo") String registrationNo);

    @Select("SELECT id, registration_no, patient_id, doctor_id, scheduling_id, department_id, " +
            "visit_date, period, status, fee, triage_id, create_time, update_time " +
            "FROM registration WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    List<Registration> findByPatientId(@Param("patientId") Long patientId);

    @Select("SELECT id, registration_no, patient_id, doctor_id, scheduling_id, department_id, " +
            "visit_date, period, status, fee, triage_id, create_time, update_time " +
            "FROM registration WHERE doctor_id = #{doctorId} ORDER BY create_time DESC")
    List<Registration> findByDoctorId(@Param("doctorId") Long doctorId);

    @Insert("INSERT INTO registration (registration_no, patient_id, doctor_id, scheduling_id, department_id, " +
            "visit_date, period, status, fee, triage_id, create_time, update_time) " +
            "VALUES (#{registrationNo}, #{patientId}, #{doctorId}, #{schedulingId}, #{departmentId}, " +
            "#{visitDate}, #{period}, #{status}, #{fee}, #{triageId}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Registration registration);

    @Update("UPDATE registration SET registration_no = #{registrationNo}, patient_id = #{patientId}, " +
            "doctor_id = #{doctorId}, scheduling_id = #{schedulingId}, department_id = #{departmentId}, " +
            "status = #{status}, fee = #{fee}, update_time = NOW() WHERE id = #{id}")
    int update(Registration registration);

    @Update("UPDATE registration SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // ===== Service Layer Methods =====

    @Select("SELECT COUNT(*) FROM registration WHERE create_time >= #{startDate} AND create_time < #{endDate}")
    long countByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT department_id, COUNT(*) as count FROM registration " +
            "WHERE create_time >= #{startDate} AND create_time < #{endDate} " +
            "GROUP BY department_id ORDER BY count DESC")
    List<Map<String, Object>> countByDepartmentGroupByDateRange(@Param("startDate") String startDate,
                                                                 @Param("endDate") String endDate);

    @Select("SELECT doctor_id, COUNT(*) as count FROM registration " +
            "WHERE create_time >= #{startDate} AND create_time < #{endDate} " +
            "GROUP BY doctor_id ORDER BY count DESC")
    List<Map<String, Object>> countByDoctorGroupByDateRange(@Param("startDate") String startDate,
                                                             @Param("endDate") String endDate);
}
