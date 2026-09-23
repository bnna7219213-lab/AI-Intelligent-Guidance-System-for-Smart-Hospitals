package com.expert.service;

import com.expert.entity.Registration;
import com.expert.vo.RegistrationVO;

import java.util.List;

/**
 * 挂号服务接口
 */
public interface RegistrationService {

    /**
     * 创建挂号（乐观锁减号源）
     *
     * @param patientId    患者ID
     * @param schedulingId 排班ID
     * @return 挂号记录
     */
    Registration createRegistration(Long patientId, Long schedulingId);

    /**
     * 查询患者挂号记录
     *
     * @param patientId 患者ID
     * @return 挂号VO列表
     */
    List<RegistrationVO> findByPatientId(Long patientId);

    /**
     * 更新挂号状态
     *
     * @param id     挂号ID
     * @param status 状态
     */
    void updateStatus(Long id, String status);

    /**
     * 查询医生挂号列表
     *
     * @param doctorId 医生ID
     * @return 挂号列表
     */
    List<Registration> findByDoctorId(Long doctorId);

    /**
     * 标记分诊已挂号
     *
     * @param triageId agent_run ID
     */
    void markTriageRegistered(Long triageId);
}
