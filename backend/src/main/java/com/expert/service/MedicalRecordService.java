package com.expert.service;

import com.expert.entity.MedicalRecord;

import java.util.List;

/**
 * 电子病历服务接口
 */
public interface MedicalRecordService {

    /**
     * 根据挂号ID查询病历
     *
     * @param registrationId 挂号ID
     * @return 病历信息
     */
    MedicalRecord findByRegistrationId(Long registrationId);

    /**
     * 保存或更新病历（草稿）
     *
     * @param record 病历信息
     */
    void saveOrUpdate(MedicalRecord record);

    /**
     * 提交病历（锁定编辑）
     *
     * @param record 病历信息
     */
    void submit(MedicalRecord record);

    /**
     * 查询患者病历列表
     *
     * @param patientId 患者ID
     * @return 病历列表
     */
    List<MedicalRecord> findByPatientId(Long patientId);

    /**
     * 查询医生下的病历列表
     *
     * @param doctorId 医生ID
     * @return 病历列表
     */
    List<MedicalRecord> findByDoctorId(Long doctorId);
}
