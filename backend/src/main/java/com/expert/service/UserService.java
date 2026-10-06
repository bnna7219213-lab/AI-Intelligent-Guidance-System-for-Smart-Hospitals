package com.expert.service;

import com.expert.entity.SysUser;
import com.expert.util.PageResult;

/**
 * 用户管理服务接口
 */
public interface UserService {

    /**
     * 分页查询用户列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param keyword  关键字
     * @return 分页结果
     */
    PageResult<SysUser> listAll(int pageNum, int pageSize, String keyword);

    /**
     * 启用/禁用用户
     *
     * @param id     用户ID
     * @param status 状态
     */
    void toggleStatus(Long id, Integer status);

    /**
     * 重置密码为默认密码
     *
     * @param id 用户ID
     */
    void resetPassword(Long id);

    /**
     * 创建医生账号
     *
     * @param username     用户名
     * @param password     密码
     * @param realName     真实姓名
     * @param departmentId 科室ID
     * @param title        职称
     */
    void createDoctor(String username, String password, String realName, Long departmentId, String title);
}
