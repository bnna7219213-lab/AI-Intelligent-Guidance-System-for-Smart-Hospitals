package com.expert.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUser {

    private Long id;

    private String username;

    private String password;

    private String realName;

    /** ADMIN/DOCTOR/PATIENT */
    private String role;

    private Integer status;

    private Long doctorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
