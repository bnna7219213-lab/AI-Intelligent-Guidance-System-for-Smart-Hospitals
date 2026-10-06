package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.MedicalRecord;
import com.expert.mapper.MedicalRecordMapper;
import com.expert.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 电子病历服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;

    @Override
    public MedicalRecord findByRegistrationId(Long registrationId) {
        return medicalRecordMapper.findByRegistrationId(registrationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(MedicalRecord record) {
        if (record.getId() != null) {
            MedicalRecord existing = medicalRecordMapper.findById(record.getId());
            if (existing == null) {
                throw new BizException("病历不存在");
            }
            if ("SUBMITTED".equals(existing.getStatus())) {
                throw new BizException("病历已提交，不能修改");
            }
            medicalRecordMapper.update(record);
            log.info("病历已更新(草稿): id={}", record.getId());
        } else {
            record.setStatus("DRAFT");
            medicalRecordMapper.insert(record);
            log.info("病历已创建(草稿): id={}", record.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(MedicalRecord record) {
        if (record.getId() != null) {
            MedicalRecord existing = medicalRecordMapper.findById(record.getId());
            if (existing == null) {
                throw new BizException("病历不存在");
            }
            if ("SUBMITTED".equals(existing.getStatus())) {
                throw new BizException("病历已提交，不能重复提交");
            }
            record.setStatus("SUBMITTED");
            medicalRecordMapper.update(record);
        } else {
            record.setStatus("SUBMITTED");
            medicalRecordMapper.insert(record);
        }
        log.info("病历已提交(锁定): registrationId={}", record.getRegistrationId());
    }

    @Override
    public List<MedicalRecord> findByPatientId(Long patientId) {
        List<MedicalRecord> list = medicalRecordMapper.findByPatientId(patientId);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<MedicalRecord> findByDoctorId(Long doctorId) {
        List<MedicalRecord> list = medicalRecordMapper.findByDoctorId(doctorId);
        return list != null ? list : Collections.emptyList();
    }
}
