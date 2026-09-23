package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.Department;
import com.expert.mapper.DepartmentMapper;
import com.expert.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 科室服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Override
    public List<Department> listAll() {
        return departmentMapper.findAll();
    }

    @Override
    public void save(Department department) {
        // 校验编码唯一性
        Department existing = departmentMapper.findByCode(department.getCode());
        if (existing != null) {
            throw new BizException("科室编码已存在");
        }
        departmentMapper.insert(department);
        log.info("科室已新增: {}", department.getName());
    }

    @Override
    public void update(Department department) {
        Department existing = departmentMapper.findById(department.getId());
        if (existing == null) {
            throw new BizException("科室不存在");
        }
        departmentMapper.update(department);
        log.info("科室已更新: id={}", department.getId());
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        Department existing = departmentMapper.findById(id);
        if (existing == null) {
            throw new BizException("科室不存在");
        }
        departmentMapper.updateStatus(id, status);
        log.info("科室状态已变更: id={}, status={}", id, status);
    }

    @Override
    public Department getById(Long id) {
        Department department = departmentMapper.findById(id);
        if (department == null) {
            throw new BizException("科室不存在");
        }
        return department;
    }
}
