package com.expert.controller;

import com.expert.common.Result;
import com.expert.service.AuthService;
import com.expert.util.InputValidator;
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
        
        // 输入验证
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.error("用户名和密码不能为空");
        }
        
        try {
            // 验证输入安全性
            InputValidator.validateSafeString(username, "用户名");
            InputValidator.validateLength(username, 4, 20, "用户名");
            InputValidator.validateLength(password, 8, 50, "密码");
            
            LoginVO loginVO = authService.login(username.trim(), password);
            return Result.success(loginVO);
        } catch (Exception e) {
            log.warn("登录失败: {}", e.getMessage());
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
        
        // 输入验证
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.error("用户名和密码不能为空");
        }
        if (realName == null || realName.isBlank()) {
            return Result.error("真实姓名不能为空");
        }
        if (role == null || role.isBlank()) {
            return Result.error("角色不能为空");
        }
        
        // 验证角色合法性
        if (!isValidRole(role)) {
            return Result.error("非法的角色类型");
        }
        
        try {
            // 验证输入安全性
            InputValidator.validateUsername(username);
            InputValidator.validatePassword(password);
            InputValidator.validateSafeString(realName, "真实姓名");
            InputValidator.validateLength(realName, 2, 50, "真实姓名");
            
            authService.register(username.trim(), password, realName.trim(), role.toUpperCase());
            return Result.success("注册成功");
        } catch (Exception e) {
            log.warn("注册失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证角色是否合法
     */
    private boolean isValidRole(String role) {
        return "ADMIN".equalsIgnoreCase(role) 
                || "DOCTOR".equalsIgnoreCase(role) 
                || "PATIENT".equalsIgnoreCase(role);
    }
}
