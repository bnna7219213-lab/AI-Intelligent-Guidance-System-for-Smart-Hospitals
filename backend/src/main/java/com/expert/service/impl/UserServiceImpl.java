package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.Doctor;
import com.expert.entity.SysUser;
import com.expert.mapper.DoctorMapper;
import com.expert.mapper.SysUserMapper;
import com.expert.service.UserService;
import com.expert.util.JwtUtil;
import com.expert.util.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 用户管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final DoctorMapper doctorMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public PageResult<SysUser> listAll(int pageNum, int pageSize, String keyword) {
        int offset = (pageNum - 1) * pageSize;
        List<SysUser> records;
        long total;
        if (StringUtils.hasText(keyword)) {
            records = sysUserMapper.findByKeyword(keyword, offset, pageSize);
            total = sysUserMapper.countByKeyword(keyword);
        } else {
            records = sysUserMapper.findAllPaged(offset, pageSize);
            total = sysUserMapper.countAll();
        }
        if (records == null) {
            records = Collections.emptyList();
        }
        return PageResult.of(records, total, pageNum, pageSize);
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        SysUser user = sysUserMapper.findById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        sysUserMapper.updateStatus(id, status);
        log.info("用户状态已变更: id={}, status={}", id, status);
    }

    @Override
    public void resetPassword(Long id) {
        SysUser user = sysUserMapper.findById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        String encodedPwd = passwordEncoder.encode(DEFAULT_PASSWORD);
        sysUserMapper.updatePassword(id, encodedPwd);
        log.info("用户密码已重置: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDoctor(String username, String password, String realName, Long departmentId, String title) {
        // 校验用户名唯一
        SysUser existing = sysUserMapper.findByUsername(username);
        if (existing != null) {
            throw new BizException("用户名已存在");
        }
        // 创建系统用户 (role=DOCTOR)
        SysUser user = SysUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .realName(realName)
                .role("DOCTOR")
                .status(1)
                .deleted(0)
                .build();
        sysUserMapper.insert(user);
        // 创建医生记录
        Doctor doctor = Doctor.builder()
                .userId(user.getId())
                .name(realName)
                .departmentId(departmentId)
                .title(title)
                .deleted(0)
                .build();
        doctorMapper.insert(doctor);
        // 回填doctorId到sys_user
        sysUserMapper.updateDoctorId(user.getId(), doctor.getId());
        log.info("医生账号已创建: username={}, doctorId={}", username, doctor.getId());
    }
}
