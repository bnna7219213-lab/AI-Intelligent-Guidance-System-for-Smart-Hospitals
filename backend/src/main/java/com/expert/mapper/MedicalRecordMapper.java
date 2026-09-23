package com.expert.mapper;

import com.expert.entity.MedicalRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 电子病历表 Mapper
 */
@Mapper
public interface MedicalRecordMapper {

    @Select("SELECT id, registration_id, patient_id, doctor_id, department_id, chief_complaint, " +
            "present_illness, past_history, allergy_history, physical_exam, diagnosis, treatment_plan, " +
            "doctor_notes, status, create_time, update_time " +
            "FROM medical_record WHERE registration_id = #{registrationId} LIMIT 1")
    MedicalRecord findByRegistrationId(@Param("registrationId") Long registrationId);

    @Select("SELECT id, registration_id, patient_id, doctor_id, department_id, chief_complaint, " +
            "present_illness, past_history, allergy_history, physical_exam, diagnosis, treatment_plan, " +
            "doctor_notes, status, create_time, update_time " +
            "FROM medical_record WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    List<MedicalRecord> findByPatientId(@Param("patientId") Long patientId);

    @Select("SELECT id, registration_id, patient_id, doctor_id, department_id, chief_complaint, " +
            "present_illness, past_history, allergy_history, physical_exam, diagnosis, treatment_plan, " +
            "doctor_notes, status, create_time, update_time " +
            "FROM medical_record WHERE doctor_id = #{doctorId} ORDER BY create_time DESC")
    List<MedicalRecord> findByDoctorId(@Param("doctorId") Long doctorId);

    @Select("SELECT id, registration_id, patient_id, doctor_id, department_id, chief_complaint, " +
            "present_illness, past_history, allergy_history, physical_exam, diagnosis, treatment_plan, " +
            "doctor_notes, status, create_time, update_time FROM medical_record WHERE id = #{id}")
    MedicalRecord findById(@Param("id") Long id);

    @Insert("INSERT INTO medical_record (registration_id, patient_id, doctor_id, department_id, chief_complaint, " +
            "present_illness, past_history, allergy_history, physical_exam, diagnosis, treatment_plan, " +
            "doctor_notes, status, create_time, update_time) VALUES (#{registrationId}, #{patientId}, " +
            "#{doctorId}, #{departmentId}, #{chiefComplaint}, #{presentIllness}, #{pastHistory}, #{allergyHistory}, " +
            "#{physicalExam}, #{diagnosis}, #{treatmentPlan}, #{doctorNotes}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MedicalRecord record);

    @Update("UPDATE medical_record SET chief_complaint = #{chiefComplaint}, present_illness = #{presentIllness}, " +
            "past_history = #{pastHistory}, allergy_history = #{allergyHistory}, physical_exam = #{physicalExam}, " +
            "diagnosis = #{diagnosis}, treatment_plan = #{treatmentPlan}, doctor_notes = #{doctorNotes}, " +
            "status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(MedicalRecord record);
}
