package com.expert.controller;

import com.expert.common.Result;
import com.expert.service.AuthService;
import com.expert.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器 - 登录与注册
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param params 包含 username 和 password
     * @return 登录信息（含token）
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.error("用户名和密码不能为空");
        }
        try {
            LoginVO loginVO = authService.login(username, password);
            return Result.success(loginVO);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     *
     * @param params 包含 username, password, realName, role
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String realName = params.get("realName");
        String role = params.get("role");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.error("用户名和密码不能为空");
        }
        if (realName == null || realName.isBlank()) {
            return Result.error("真实姓名不能为空");
        }
        if (role == null || role.isBlank()) {
            return Result.error("角色不能为空");
        }
        try {
            authService.register(username, password, realName, role);
            return Result.success("注册成功");
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
