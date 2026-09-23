package com.expert.controller;

import com.expert.common.Result;
import com.expert.entity.MedicalRecord;
import com.expert.entity.Registration;
import com.expert.security.UserContext;
import com.expert.service.MedicalRecordService;
import com.expert.service.RegistrationService;
import com.expert.util.PageResult;
import com.expert.vo.RegistrationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 医生控制器 - 患者管理、病历书写
 */
@Slf4j
@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final RegistrationService registrationService;
    private final MedicalRecordService medicalRecordService;

    /**
     * 查询医生当日挂号患者列表
     */
    @GetMapping("/patients")
    public Result<PageResult<RegistrationVO>> listPatients(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            Long doctorId = UserContext.getUserId();
            if (doctorId == null) {
                return Result.error("未获取到当前医生信息");
            }
            List<Registration> registrations = registrationService.findByDoctorId(doctorId);
            // Convert to RegistrationVO (simplified - just wrap)
            long total = registrations.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, registrations.size());
            List<RegistrationVO> records = fromIndex < registrations.size()
                    ? registrations.subList(fromIndex, toIndex).stream()
                            .map(reg -> RegistrationVO.builder()
                                    .id(reg.getId())
                                    .registrationNo(reg.getRegistrationNo())
                                    .patientId(reg.getPatientId())
                                    .doctorId(reg.getDoctorId())
                                    .visitDate(reg.getVisitDate())
                                    .period(reg.getPeriod())
                                    .status(reg.getStatus())
                                    .fee(reg.getFee())
                                    .createTime(reg.getCreateTime())
                                    .build())
                            .toList()
                    : Collections.emptyList();
            PageResult<RegistrationVO> page = PageResult.of(records, total, pageNum, pageSize);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询医生患者列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 开始就诊（更新挂号状态为IN_PROGRESS）
     */
    @PutMapping("/registrations/{id}/start")
    public Result<String> startConsultation(@PathVariable Long id) {
        try {
            registrationService.updateStatus(id, "IN_PROGRESS");
            return Result.success("就诊已开始");
        } catch (Exception e) {
            log.error("开始就诊失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 完成就诊（更新挂号状态为COMPLETED）
     */
    @PutMapping("/registrations/{id}/complete")
    public Result<String> completeConsultation(@PathVariable Long id) {
        try {
            registrationService.updateStatus(id, "COMPLETED");
            return Result.success("就诊已完成");
        } catch (Exception e) {
            log.error("完成就诊失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询电子病历（按挂号ID）
     */
    @GetMapping("/medical-records/{registrationId}")
    public Result<MedicalRecord> getMedicalRecord(@PathVariable Long registrationId) {
        try {
            MedicalRecord record = medicalRecordService.findByRegistrationId(registrationId);
            return Result.success(record);
        } catch (Exception e) {
            log.error("查询病历失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 保存或更新电子病历
     */
    @PostMapping("/medical-records")
    public Result<String> saveOrUpdateMedicalRecord(@RequestBody MedicalRecord record) {
        try {
            Long doctorId = UserContext.getUserId();
            if (doctorId == null) {
                return Result.error("未获取到当前医生信息");
            }
            record.setDoctorId(doctorId);
            medicalRecordService.saveOrUpdate(record);
            return Result.success("病历保存成功");
        } catch (Exception e) {
            log.error("保存病历失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
