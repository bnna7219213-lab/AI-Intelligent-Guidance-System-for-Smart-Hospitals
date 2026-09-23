package com.expert.service;

import com.expert.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录信息
     */
    LoginVO login(String username, String password);

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param realName 真实姓名
     * @param role     角色 (DOCTOR/PATIENT)
     */
    void register(String username, String password, String realName, String role);
}
