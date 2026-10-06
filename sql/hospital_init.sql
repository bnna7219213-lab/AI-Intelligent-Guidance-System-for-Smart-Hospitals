-- ============================================
-- AI智慧医院智能导诊系统 数据库初始化脚本
-- MySQL 8.0+
-- ============================================

DROP DATABASE IF EXISTS `ai_hospital`;
CREATE DATABASE `ai_hospital` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_hospital`;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '账号',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: ADMIN/DOCTOR/PATIENT',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `doctor_id` BIGINT DEFAULT NULL COMMENT '关联医生ID(医生角色)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 患者档案表
-- ============================================
CREATE TABLE `patient_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
    `age` INT DEFAULT NULL COMMENT '年龄',
    `chronic_history` TEXT DEFAULT NULL COMMENT '慢病史',
    `allergy_history` TEXT DEFAULT NULL COMMENT '过敏史',
    `phone` VARCHAR(20) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者档案表';

-- ============================================
-- 科室表
-- ============================================
CREATE TABLE `department` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '科室名称',
    `code` VARCHAR(30) NOT NULL COMMENT '科室编码',
    `description` TEXT DEFAULT NULL COMMENT '科室简介',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0停用 1启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

-- ============================================
-- 医生表
-- ============================================
CREATE TABLE `doctor` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '医生姓名',
    `department_id` BIGINT NOT NULL COMMENT '所属科室ID',
    `title` VARCHAR(30) DEFAULT NULL COMMENT '职称',
    `specialty` TEXT DEFAULT NULL COMMENT '擅长',
    `introduction` TEXT DEFAULT NULL COMMENT '医生简介',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生表';

-- ============================================
-- 排班表
-- ============================================
CREATE TABLE `scheduling` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `department_id` BIGINT NOT NULL COMMENT '科室ID',
    `schedule_date` DATE NOT NULL COMMENT '排班日期',
    `period` VARCHAR(20) NOT NULL COMMENT '时段: MORNING/AFTERNOON',
    `total_count` INT NOT NULL DEFAULT 30 COMMENT '号源总数',
    `remain_count` INT NOT NULL DEFAULT 30 COMMENT '剩余号源',
    `fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '挂号费',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_doctor_date` (`doctor_id`, `schedule_date`),
    KEY `idx_dept_date` (`department_id`, `schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班表';

-- ============================================
-- 挂号表
-- ============================================
CREATE TABLE `registration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `registration_no` VARCHAR(30) NOT NULL COMMENT '挂号单号',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `department_id` BIGINT NOT NULL COMMENT '科室ID',
    `scheduling_id` BIGINT NOT NULL COMMENT '排班ID',
    `visit_date` DATE NOT NULL COMMENT '就诊日期快照',
    `period` VARCHAR(20) NOT NULL COMMENT '时段',
    `triage_id` BIGINT DEFAULT NULL COMMENT '关联分诊ID',
    `status` VARCHAR(20) NOT NULL COMMENT '状态: REGISTERED/IN_PROGRESS/COMPLETED/CANCELLED',
    `fee` DECIMAL(10,2) NOT NULL COMMENT '挂号费',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_registration_no` (`registration_no`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_doctor_id` (`doctor_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号表';

-- ============================================
-- A.I模型配置表
-- ============================================
CREATE TABLE `ai_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(50) NOT NULL COMMENT '配置键: CHAT_MODEL/VECTOR_MODEL',
    `config_name` VARCHAR(50) NOT NULL COMMENT '显示名称',
    `api_url` VARCHAR(255) NOT NULL COMMENT 'API地址',
    `api_key` VARCHAR(255) NOT NULL COMMENT 'API密钥',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `extra_config` TEXT DEFAULT NULL COMMENT '额外配置(JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- ============================================
-- Prompt模板表
-- ============================================
CREATE TABLE `prompt_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `template_key` VARCHAR(50) NOT NULL COMMENT '模板标识',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_content` TEXT NOT NULL COMMENT '模板内容',
    `description` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_key` (`template_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt模板表';

-- ============================================
-- 症状标签表
-- ============================================
CREATE TABLE `symptom_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '症状名称',
    `description` TEXT DEFAULT NULL COMMENT '症状描述',
    `related_departments` VARCHAR(255) DEFAULT NULL COMMENT '关联科室编码(逗号分隔)',
    `weight` INT DEFAULT 0 COMMENT '权重',
    `is_red_flag` VARCHAR(2) DEFAULT '否' COMMENT '是否危急征象: 是/否',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='症状标签表';

-- ============================================
-- 分诊Agent运行记录表
-- ============================================
CREATE TABLE `agent_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `session_id` VARCHAR(100) NOT NULL COMMENT '会话ID',
    `status` VARCHAR(20) NOT NULL COMMENT '状态: RUNNING/SUCCESS/FAIL',
    `recommended_department_id` BIGINT DEFAULT NULL COMMENT '推荐科室ID',
    `recommended_doctor_id` BIGINT DEFAULT NULL COMMENT '推荐医生ID',
    `confidence` DECIMAL(5,2) DEFAULT NULL COMMENT '置信度',
    `emergency_level` VARCHAR(20) DEFAULT NULL COMMENT '危急等级',
    `recommendation_reason` TEXT DEFAULT NULL COMMENT '推荐理由',
    `is_registered` VARCHAR(2) DEFAULT '否' COMMENT '是否已挂号: 是/否',
    `total_steps` INT DEFAULT 0,
    `total_tokens` INT DEFAULT 0,
    `error_message` TEXT DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分诊Agent运行记录表';

-- ============================================
-- 分诊Agent步骤表
-- ============================================
CREATE TABLE `agent_step` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `run_id` BIGINT NOT NULL COMMENT '运行ID',
    `step_number` INT NOT NULL COMMENT '步骤序号',
    `tool_code` VARCHAR(50) DEFAULT NULL COMMENT '调用的工具编码',
    `tool_name` VARCHAR(50) DEFAULT NULL COMMENT '工具名称',
    `input_params` TEXT DEFAULT NULL COMMENT '输入参数',
    `output_result` TEXT DEFAULT NULL COMMENT '输出结果',
    `success` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    `error_message` TEXT DEFAULT NULL,
    `duration_ms` INT DEFAULT 0 COMMENT '耗时(ms)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_run_id` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分诊Agent步骤表';

-- ============================================
-- MCP工具注册表
-- ============================================
CREATE TABLE `mcp_tool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tool_code` VARCHAR(50) NOT NULL COMMENT '工具编码',
    `tool_name` VARCHAR(100) NOT NULL COMMENT '工具名称',
    `description` TEXT NOT NULL COMMENT '工具说明',
    `input_schema` TEXT DEFAULT NULL COMMENT '入参定义(JSON Schema)',
    `status` VARCHAR(10) NOT NULL DEFAULT '启动' COMMENT '状态: 启动/停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tool_code` (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP工具注册表';

-- ============================================
-- MCP工具调用日志表
-- ============================================
CREATE TABLE `mcp_tool_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tool_id` BIGINT NOT NULL COMMENT '工具ID',
    `tool_code` VARCHAR(50) NOT NULL,
    `run_id` BIGINT DEFAULT NULL COMMENT '关联运行记录',
    `step_id` BIGINT DEFAULT NULL COMMENT '关联步骤',
    `input_params` TEXT DEFAULT NULL,
    `output_result` TEXT DEFAULT NULL,
    `success` TINYINT NOT NULL DEFAULT 1,
    `error_message` TEXT DEFAULT NULL,
    `duration_ms` INT DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tool_id` (`tool_id`),
    KEY `idx_run_id` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP工具调用日志表';

-- ============================================
-- AI使用观测表 (ai_usage_log)
-- ============================================
CREATE TABLE `ai_usage_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `usage_type` VARCHAR(30) NOT NULL COMMENT '用途: CHAT/SSE_CHAT/REACT/EMBEDDING',
    `model_name` VARCHAR(100) DEFAULT NULL,
    `prompt_tokens` INT DEFAULT 0 COMMENT '输入Token数',
    `completion_tokens` INT DEFAULT 0 COMMENT '输出Token数',
    `total_tokens` INT DEFAULT 0 COMMENT '总Token数',
    `duration_ms` INT DEFAULT 0 COMMENT '耗时(ms)',
    `success` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    `error_msg` TEXT DEFAULT NULL COMMENT '错误信息(中文翻译)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_usage_type` (`usage_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI使用观测表';

-- ============================================
-- 医学知识库分组表
-- ============================================
CREATE TABLE `kb_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_name` VARCHAR(100) NOT NULL COMMENT '分组名称',
    `description` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分组表';

-- ============================================
-- 医学知识库文档表
-- ============================================
CREATE TABLE `kb_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `file_content` MEDIUMTEXT DEFAULT NULL COMMENT '文档内容',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- ============================================
-- 知识库切片表 (向量)
-- ============================================
CREATE TABLE `kb_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `document_id` BIGINT NOT NULL,
    `group_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL COMMENT '切片内容',
    `embedding` JSON COMMENT '向量数据(JSON数组)',
    `chunk_index` INT NOT NULL COMMENT '切片序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库切片表';

-- ============================================
-- 预问诊会话表
-- ============================================
CREATE TABLE `consultation_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(100) NOT NULL COMMENT '会话ID',
    `patient_id` BIGINT NOT NULL,
    `title` VARCHAR(200) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ENDED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预问诊会话表';

-- ============================================
-- 预问诊消息表
-- ============================================
CREATE TABLE `consultation_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(100) NOT NULL,
    `role` VARCHAR(20) NOT NULL COMMENT 'USER/ASSISTANT',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预问诊消息表';

-- ============================================
-- 病历记录表
-- ============================================
CREATE TABLE `medical_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `registration_id` BIGINT NOT NULL COMMENT '挂号ID',
    `patient_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NOT NULL,
    `department_id` BIGINT NOT NULL,
    `chief_complaint` TEXT COMMENT '主诉',
    `present_illness` TEXT COMMENT '现病史',
    `past_history` TEXT COMMENT '既往史',
    `allergy_history` TEXT COMMENT '过敏史',
    `physical_exam` TEXT COMMENT '体格检查',
    `diagnosis` TEXT COMMENT '诊断',
    `treatment_plan` TEXT COMMENT '治疗方案',
    `doctor_notes` TEXT COMMENT '医生备注',
    `status` VARCHAR(20) NOT NULL COMMENT 'DRAFT/SUBMITTED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_registration_id` (`registration_id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_doctor_id` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病历记录表';

-- ============================================
-- 初始化数据
-- ============================================

-- 管理员账号: admin / admin123 (BCrypt加密后)
INSERT INTO `sys_user` (id, username, password, real_name, role, status) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', 'ADMIN', 1);

-- 默认聊天模型配置(用户自行修改)
INSERT INTO `ai_config` (id, config_key, config_name, api_url, api_key, model_name) VALUES
(1, 'CHAT_MODEL', '聊天模型', 'https://api.openai.com/v1', 'sk-xxxxx', 'gpt-4o-mini'),
(2, 'VECTOR_MODEL', '向量模型', 'https://api.openai.com/v1', 'sk-xxxxx', 'text-embedding-3-small');

-- 默认Prompt模板
INSERT INTO `prompt_template` (id, template_key, template_name, template_content, description) VALUES
(1, 'PRE_CONSULT', '预问诊Prompt',
'你是一位专业的AI预问诊助手，负责与患者进行多轮对话，收集患者的症状信息。请用温和的语气与患者交流，逐步引导患者描述症状，包括发病时间、症状特点、伴随症状等。不要直接给出诊断建议。',
'预问诊系统提示词'),
(2, 'TRIAGE_AGENT', '分诊Agent Prompt',
'你是一位专业的医疗分诊AI助手。你可以使用提供的工具来分析患者的症状，推荐合适的科室和医生。请根据工具返回的结果进行推理，给出准确的科室推荐。输出格式为JSON: {"department_code":"","doctor_id":0,"confidence":0.0,"emergency_level":"","reason":""}',
'分诊Agent系统提示词');

-- 默认MCP工具
INSERT INTO `mcp_tool` (id, tool_code, tool_name, description, status) VALUES
(1, 'query_symptom_tags', '症状标签查询', '根据症状关键词查询匹配的症状标签、关联科室、权重和危急征象标记', '启动'),
(2, 'query_departments', '科室信息查询', '查询科室列表、科室详细信息和简介', '启动'),
(3, 'query_doctor_schedule', '医生排班查询', '查询指定科室/日期下的医生排班信息', '启动'),
(4, 'query_knowledge_base', '知识库检索', '对医学知识库进行向量检索，获取相关医学知识', '启动');

-- 默认症状标签
INSERT INTO `symptom_tag` (id, name, description, related_departments, weight, is_red_flag) VALUES
(1, '胸痛', '胸部疼痛或压迫感', 'CARDIOLOGY,EMERGENCY', 10, '是'),
(2, '头痛', '头部疼痛', 'NEUROLOGY,GENERAL', 5, '否'),
(3, '发热', '体温升高', 'INFECTIOUS,GENERAL', 6, '否'),
(4, '腹痛', '腹部疼痛', 'GASTRO,SURGERY', 7, '否'),
(5, '呼吸困难', '呼吸急促或困难', 'RESPIRATORY,EMERGENCY', 10, '是'),
(6, '咳嗽', '呼吸道症状', 'RESPIRATORY,GENERAL', 4, '否'),
(7, '恶心呕吐', '消化系统症状', 'GASTRO,GENERAL', 5, '否'),
(8, '头晕', '眩晕、头昏', 'NEUROLOGY,CARDIOLOGY', 6, '否'),
(9, '心悸', '心脏跳动异常', 'CARDIOLOGY', 7, '否'),
(10, '意识障碍', '意识模糊或丧失', 'NEUROLOGY,EMERGENCY', 10, '是');

-- 默认科室
INSERT INTO `department` (id, name, code, description, status) VALUES
(1, '急诊科', 'EMERGENCY', '处理危急重症，24小时开放', 1),
(2, '心内科', 'CARDIOLOGY', '心血管疾病诊治', 1),
(3, '神经内科', 'NEUROLOGY', '神经系统疾病诊治', 1),
(4, '呼吸内科', 'RESPIRATORY', '呼吸系统疾病诊治', 1),
(5, '消化内科', 'GASTRO', '消化系统疾病诊治', 1),
(6, '普外科', 'SURGERY', '普通外科手术', 1),
(7, '全科门诊', 'GENERAL', '常见病多发病诊治', 1),
(8, '感染科', 'INFECTIOUS', '感染性疾病诊治', 1);

-- 知识库分组
INSERT INTO `kb_group` (id, group_name, description) VALUES
(1, '常见疾病', '常见疾病诊疗指南'),
(2, '用药指导', '常用药品使用指导'),
(3, '检验检查', '检验检查项目说明');
