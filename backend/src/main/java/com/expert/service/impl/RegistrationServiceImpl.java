package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.AgentRun;
import com.expert.entity.Registration;
import com.expert.entity.Scheduling;
import com.expert.mapper.AgentRunMapper;
import com.expert.mapper.RegistrationMapper;
import com.expert.mapper.SchedulingMapper;
import com.expert.service.RegistrationService;
import com.expert.vo.RegistrationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 挂号服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationMapper registrationMapper;
    private final SchedulingMapper schedulingMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final AgentRunMapper agentRunMapper;
    private final com.expert.common.IdempotencyChecker idempotencyChecker;

    private static final DateTimeFormatter REG_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Registration createRegistration(Long patientId, Long schedulingId) {
        // 幂等性检查：防止重复挂号
        String idempotencyKey = "REG:" + patientId + ":" + schedulingId;
        if (!idempotencyChecker.isNewRequest(idempotencyKey)) {
            throw new BizException("请勿重复提交挂号请求");
        }

        // 查询排班信息
        Scheduling scheduling = schedulingMapper.findById(schedulingId);
        if (scheduling == null) {
            throw new BizException("排班不存在");
        }
        if (scheduling.getRemainCount() <= 0) {
            throw new BizException("号源已满");
        }
        if (scheduling.getScheduleDate().isBefore(LocalDate.now())) {
            throw new BizException("排班已过期，不能挂号");
        }
        
        // 检查患者是否已挂该排班（业务幂等性）
        List<Registration> existingRegs = registrationMapper.findByPatientId(patientId);
        boolean alreadyRegistered = existingRegs.stream()
                .anyMatch(reg -> reg.getSchedulingId().equals(schedulingId) 
                        && !"CANCELLED".equals(reg.getStatus()));
        if (alreadyRegistered) {
            throw new BizException("您已挂该排班，请勿重复挂号");
        }

        // 乐观锁减号源
        int rows = schedulingMapper.decrementRemain(schedulingId, scheduling.getVersion());
        if (rows == 0) {
            throw new BizException("号源已被抢光，请刷新重试");
        }
        // 生成挂号编号
        String registrationNo = generateRegistrationNo();
        // 快照就诊日期
        LocalDate visitDate = scheduling.getScheduleDate();
        // 创建挂号记录
        Registration registration = Registration.builder()
                .registrationNo(registrationNo)
                .patientId(patientId)
                .doctorId(scheduling.getDoctorId())
                .departmentId(scheduling.getDepartmentId())
                .schedulingId(schedulingId)
                .visitDate(visitDate)
                .period(scheduling.getPeriod())
                .status("REGISTERED")
                .fee(scheduling.getFee())
                .deleted(0)
                .build();
        registrationMapper.insert(registration);
        
        // 脱敏日志：不记录敏感信息
        log.info("挂号成功: registrationNo={}, patientId=[REDACTED], schedulingId={}", 
                registrationNo, schedulingId);
        
        return registration;
    }

    @Override
    public List<RegistrationVO> findByPatientId(Long patientId) {
        List<Registration> list = registrationMapper.findByPatientId(patientId);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(reg -> RegistrationVO.builder()
                .id(reg.getId())
                .registrationNo(reg.getRegistrationNo())
                .patientId(reg.getPatientId())
                .doctorId(reg.getDoctorId())
                .departmentId(reg.getDepartmentId())
                .visitDate(reg.getVisitDate())
                .period(reg.getPeriod())
                .status(reg.getStatus())
                .fee(reg.getFee())
                .createTime(reg.getCreateTime())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public void updateStatus(Long id, String status) {
        Registration reg = registrationMapper.findById(id);
        if (reg == null) {
            throw new BizException("挂号记录不存在");
        }
        registrationMapper.updateStatus(id, status);
        log.info("挂号状态已更新: id={}, status={}", id, status);
    }

    @Override
    public List<Registration> findByDoctorId(Long doctorId) {
        List<Registration> list = registrationMapper.findByDoctorId(doctorId);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public void markTriageRegistered(Long triageId) {
        AgentRun agentRun = agentRunMapper.findById(triageId);
        if (agentRun == null) {
            throw new BizException("分诊记录不存在");
        }
        agentRunMapper.markRegistered(triageId);
        log.info("分诊已标记为已挂号: triageId={}", triageId);
    }

    /**
     * 生成挂号编号: REG + yyyyMMddHHmmss + 6位随机数
     * 使用 ThreadLocalRandom 提高并发性能
     */
    private String generateRegistrationNo() {
        String timestamp = LocalDateTime.now().format(REG_NO_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(900000) + 100000;
        return "REG" + timestamp + random;
    }
}
