package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.PatientProfile;
import com.expert.mapper.PatientProfileMapper;
import com.expert.service.PatientProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 患者档案服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientProfileServiceImpl implements PatientProfileService {

    private final PatientProfileMapper patientProfileMapper;

    @Override
    public PatientProfile findByUserId(Long userId) {
        return patientProfileMapper.findByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(Long userId, PatientProfile profile) {
        PatientProfile existing = patientProfileMapper.findByUserId(userId);
        if (existing != null) {
            // 更新已有档案
            profile.setId(existing.getId());
            profile.setUserId(userId);
            patientProfileMapper.update(profile);
            log.info("患者档案已更新: userId={}", userId);
        } else {
            // 新建档案
            profile.setUserId(userId);
            profile.setDeleted(0);
            patientProfileMapper.insert(profile);
            log.info("患者档案已创建: userId={}", userId);
        }
    }

    @Override
    public PatientProfile findById(Long id) {
        PatientProfile profile = patientProfileMapper.findById(id);
        if (profile == null) {
            throw new BizException("患者档案不存在");
        }
        return profile;
    }
}
