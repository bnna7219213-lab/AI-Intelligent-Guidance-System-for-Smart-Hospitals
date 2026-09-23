package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.Department;
import com.expert.entity.Doctor;
import com.expert.mapper.DepartmentMapper;
import com.expert.mapper.DoctorMapper;
import com.expert.service.DoctorService;
import com.expert.util.PageResult;
import com.expert.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 医生服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    public PageResult<DoctorVO> listAll(Long departmentId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Doctor> doctors;
        long total;
        if (departmentId != null) {
            doctors = doctorMapper.findByDepartmentIdPaged(departmentId, offset, pageSize);
            total = doctorMapper.countByDepartmentId(departmentId);
        } else {
            doctors = doctorMapper.findAllPaged(offset, pageSize);
            total = doctorMapper.countAll();
        }
        if (doctors == null) {
            doctors = Collections.emptyList();
        }
        // 批量获取科室信息用于join科室名称
        List<Long> deptIds = doctors.stream()
                .map(Doctor::getDepartmentId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Department> deptMap = deptIds.isEmpty() ? Collections.emptyMap() :
                departmentMapper.findAll().stream()
                        .collect(Collectors.toMap(Department::getId, Function.identity()));
        // 组装VO
        final Map<Long, Department> finalDeptMap = deptMap;
        List<DoctorVO> voList = doctors.stream().map(d -> {
            DoctorVO vo = DoctorVO.builder()
                    .id(d.getId())
                    .userId(d.getUserId())
                    .name(d.getName())
                    .departmentId(d.getDepartmentId())
                    .title(d.getTitle())
                    .specialty(d.getSpecialty())
                    .introduction(d.getIntroduction())
                    .fee(d.getFee())
                    .build();
            Department dept = finalDeptMap.get(d.getDepartmentId());
            if (dept != null) {
                vo.setDepartmentName(dept.getName());
            }
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(voList, total, pageNum, pageSize);
    }

    @Override
    public Doctor getById(Long id) {
        Doctor doctor = doctorMapper.findById(id);
        if (doctor == null) {
            throw new BizException("医生不存在");
        }
        return doctor;
    }

    @Override
    public void update(Doctor doctor) {
        Doctor existing = doctorMapper.findById(doctor.getId());
        if (existing == null) {
            throw new BizException("医生不存在");
        }
        doctorMapper.update(doctor);
        log.info("医生信息已更新: id={}", doctor.getId());
    }
}
