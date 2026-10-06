package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 电子病历实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {

    private Long id;

    private Long registrationId;

    private Long patientId;

    private Long doctorId;

    private Long departmentId;

    private String chiefComplaint;

    private String presentIllness;

    private String pastHistory;

    private String allergyHistory;

    private String physicalExam;

    private String diagnosis;

    private String treatmentPlan;

    private String doctorNotes;

    /** DRAFT/SUBMITTED */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
