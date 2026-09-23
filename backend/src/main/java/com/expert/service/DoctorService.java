package com.expert.service;

import com.expert.entity.Doctor;
import com.expert.util.PageResult;
import com.expert.vo.DoctorVO;

/**
 * 医生服务接口
 */
public interface DoctorService {

    /**
     * 分页查询医生列表
     *
     * @param departmentId 科室ID（可选）
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @return 分页结果
     */
    PageResult<DoctorVO> listAll(Long departmentId, int pageNum, int pageSize);

    /**
     * 根据ID查询医生
     *
     * @param id 医生ID
     * @return 医生信息
     */
    Doctor getById(Long id);

    /**
     * 更新医生信息
     *
     * @param doctor 医生信息
     */
    void update(Doctor doctor);
}
