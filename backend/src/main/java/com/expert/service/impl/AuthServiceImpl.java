package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.PatientProfile;
import com.expert.entity.SysUser;
import com.expert.mapper.PatientProfileMapper;
import com.expert.mapper.SysUserMapper;
import com.expert.service.AuthService;
import com.expert.util.JwtUtil;
import com.expert.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(String username, String password) {
        SysUser user = sysUserMapper.findByUsername(username);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException("密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("用户登录成功: {}", username);
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .realName(user.getRealName())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password, String realName, String role) {
        // 校验用户名唯一
        SysUser existing = sysUserMapper.findByUsername(username);
        if (existing != null) {
            throw new BizException("用户名已存在");
        }
        // 创建系统用户
        SysUser user = SysUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .realName(realName)
                .role(role)
                .status(1)
                .deleted(0)
                .build();
        sysUserMapper.insert(user);
        log.info("用户注册成功: {}, role={}", username, role);
        // 如果是患者角色，同步创建患者档案
        if ("PATIENT".equals(role)) {
            PatientProfile profile = PatientProfile.builder()
                    .userId(user.getId())
                    .name(realName)
                    .deleted(0)
                    .build();
            patientProfileMapper.insert(profile);
            log.info("患者档案已创建: userId={}", user.getId());
        }
    }
}
