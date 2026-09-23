package com.expert.service;

import com.expert.entity.PatientProfile;

/**
 * 患者档案服务接口
 */
public interface PatientProfileService {

    /**
     * 根据用户ID查询患者档案
     *
     * @param userId 用户ID
     * @return 患者档案
     */
    PatientProfile findByUserId(Long userId);

    /**
     * 保存或更新患者档案
     *
     * @param userId  用户ID
     * @param profile 档案信息
     */
    void saveOrUpdate(Long userId, PatientProfile profile);

    /**
     * 根据ID查询患者档案
     *
     * @param id 档案ID
     * @return 患者档案
     */
    PatientProfile findById(Long id);
}
