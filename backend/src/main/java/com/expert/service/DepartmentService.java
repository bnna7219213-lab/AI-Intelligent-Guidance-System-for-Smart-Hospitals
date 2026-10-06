package com.expert.service;

import com.expert.entity.Department;

import java.util.List;

/**
 * 科室服务接口
 */
public interface DepartmentService {

    /**
     * 查询所有科室
     *
     * @return 科室列表
     */
    List<Department> listAll();

    /**
     * 新增科室
     *
     * @param department 科室信息
     */
    void save(Department department);

    /**
     * 更新科室
     *
     * @param department 科室信息
     */
    void update(Department department);

    /**
     * 启用/禁用科室
     *
     * @param id     科室ID
     * @param status 状态
     */
    void toggleStatus(Long id, Integer status);

    /**
     * 根据ID查询科室
     *
     * @param id 科室ID
     * @return 科室信息
     */
    Department getById(Long id);
}
