# AI智慧医院智能导诊系统

基于 **Spring Boot 4.1 + LangChain4j + Agent + MCP + RAG** 的AI智慧医院智能导诊就诊系统。

> 智能预问诊 · 智能分诊 · 医学知识库 RAG · Agent多轮自主取证 · MCP工具中心 · SSE真流式

---

## 一、项目简介

本项目实现了从 **预问诊 → 智能分诊 → 在线挂号 → 接诊写病历 → 运营复盘** 的完整就诊闭环，核心创新点包括：

1. **数据库驱动的ReAct分诊Agent**: 模型自主决定调用哪些工具、调几次、何时收尾，全过程落agent_run/agent_step，推理可解释可复盘
2. **MCP工具中心真实开关**: 工具池按status='启动'现查现建，停用后模型真的调不到它
3. **AI输出先由代码核验再落库**: 科室必须真实存在、医生必须有排班，危急征象拦截写死代码中
4. **统一AI出口与调用观测**: 四类调用收敛到HospitalAiService，每次写成败/Token/耗时落ai_usage_log
5. **AI配置全部数据库化**: 模型参数/Prompt模板存在库里，后台改完立即生效不需重启
6. **SSE真流式预问诊**: 逐token推送，业务校验在进入流式之前完成
7. **分诊推荐命中率闭环**: 推荐了多少次、多少转成挂号，看板可看可调
8. **排班日期一键顺延**: 保住原有先后间隔整体平移，历史挂号不受影响

---

## 二、技术栈

### 后端
- **Spring Boot 4.1** - Web框架
- **LangChain4j** - AI模型调用、工具调用、Embedding
- **MyBatis 3.0.5 + PageHelper** - ORM与分页
- **Hutool** - 工具库
- **Java JWT (jjwt 0.12.6)** - 认证
- **Spring Security Crypto** - BCrypt密码加密
- **Apache POI + PDFBox** - 文档解析
- **MySQL 8.0+** - 主数据库（业务数据）
- **Milvus 2.6+** - 向量数据库（RAG 语义检索）

### AI模型
- 走OpenAI兼容接口，需要两类API Key:
  - **对话模型** (如 GPT-4o-mini / DeepSeek / Qwen): 用于聊天、ReAct循环
  - **向量模型** (如 text-embedding-3-small / BGE-M3): 用于RAG向量化

---

## 三、运行环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 21+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
│       │   │   │   ├── VectorIndexRebuildService.java  # 向量索引自动重建后台任务
│       │   │   │   └── DocumentEmbedService.java       # 知识库文档异步向量化
│       │   │   ├── AuthService.java
│       │   │   ├── UserService.java
│       │   │   ├── HospitalAiService.java  # 统一AI出口
│       │   │   └── ...
│       │   ├── mapper/             # MyBatis Mapper
│       │   ├── entity/             # 实体类
│       │   ├── ai/                 # AI核心模块
│       │   │   ├── HospitalAiService.java    # 统一AI调用出口
│       │   │   ├── ModelFactory.java         # 模型构建工厂
│       │   │   ├── RagService.java           # RAG知识库服务（Milvus+内存降级）
│       │   │   ├── MilvusVectorStore.java    # Milvus 向量CRUD封装
│       │   │   ├── TriageAgentService.java   # ReAct分诊Agent
│       │   │   └── McpToolExecutor.java      # MCP工具执行器
│       │   ├── config/             # 配置类
│       │   │   ├── MilvusProperties.java     # Milvus 配置属性
│       │   │   └── MilvusConfig.java         # Milvus 客户端+集合管理
│       │   ├── security/           # JWT认证
│       │   ├── common/             # 通用工具
│       │   ├── vo/                 # 视图对象
│       │   │   ├── VectorIndexRebuildStatus.java   # 向量重建任务状态
│       │   │   └── DocumentEmbedStatus.java        # 文档异步嵌入状态
│       │   └── util/               # 工具类
│       └── resources/
│           └── application.yml
└── frontend/                       # 前端项目
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/                 # 路由
        ├── store/                  # Pinia状态管理
        ├── api/                    # API封装
        ├── utils/                  # 工具函数
        ├── assets/                 # 样式资源
        ├── layouts/                # 布局组件
        └── views/                  # 页面视图
            ├── login/              # 登录注册
            ├── admin/              # 管理员模块
            ├── doctor/             # 医生模块
            └── patient/            # 患者模块
```

---

## 七、核心功能

### 管理员模块
- **用户管理**: 用户CRUD、状态开关、重置密码、创建医生账号
- **科室管理**: 科室CRUD、状态管理
- **医生管理**: 查看医生列表、所属科室、职称
- **排班管理**: 创建排班、日期筛选、**排班日期一键顺延**
- **AI模型配置**: 聊天/向量模型配置、连通性测试、**改完即生效**
- **Prompt模板**: 预问诊与分诊Agent的Prompt在线编辑
- **医学知识库**: 文档上传(TXT/PDF/DOCX)、自动切片、向量化、语义检索
- **MCP工具中心**: 工具注册、真开关控制、调用日志
- **症状标签库**: 症状名称、关联科室、权重、危急征象标记
- **运营看板**: 挂号量、号源使用率、分诊推荐命中率
- **观测面板**: Agent执行时间线、AI调用统计与明细

### 医生模块
- **门诊接诊**: 查看挂号患者、开始/完成接诊、状态流转
- **病历书写**: 七段式结构化录入，草稿/提交两种状态

### 患者模块
- **个人档案**: 基本信息、慢病史、过敏史
- **智能预问诊**: SSE流式多轮对话收集症状
- **智能分诊**: ReAct多步取证，推荐科室/医生/置信度/危急等级
- **预约挂号**: 按推荐一键挂号，乐观锁防超卖
- **我的就诊**: 挂号状态流转、电子病历查看

---

## 八、API接口文档

Base URL: `http://localhost:8080/api`

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /auth/login | 登录，返回JWT |
| POST | /auth/register | 注册 |

### 管理员接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | /admin/users?pageNum=1&pageSize=10&keyword= | 用户列表 |
| PUT  | /admin/users/{id}/status | 用户状态开关 |
| POST | /admin/users/doctor | 创建医生 |
| PUT  | /admin/users/{id}/reset-pwd | 重置密码 |
| GET  | /admin/departments | 科室列表 |
| POST | /admin/departments | 添加科室 |
| GET  | /admin/doctors | 医生列表 |
| POST | /admin/schedules | 创建排班 |
| POST | /admin/schedules/shift-dates | **排班日期顺延** |
| GET  | /admin/ai-configs | AI配置列表 |
| PUT  | /admin/ai-configs | 保存AI配置 |
| POST | /admin/ai-configs/test | 测试连通性 |
| GET  | /admin/kb/groups | 知识库分组 |
| POST | /admin/kb/documents | 上传文档（autoEmbed=true 默认异步向量化） |
| POST | /admin/kb/documents/{id}/embed | 同步向量化 |
| POST | /admin/kb/documents/{id}/embed-async | **异步向量化（立即返回）** |
| GET  | /admin/kb/documents/{id}/embed/status | **查询异步向量化进度** |
| POST | /admin/kb/search | 语义检索 |
| GET  | /admin/mcp-tools | MCP工具列表 |
| PUT  | /admin/mcp-tools/{id}/toggle | 工具开关 |
| GET  | /admin/operations/triage-hit-rate | **分诊命中率** |
| GET  | /admin/observability/agent-runs?pageNum=1 | Agent执行记录 |
| GET  | /admin/observability/ai-usage | AI调用统计 |
| POST | /admin/kb/rebuild | **触发向量索引自动重建** |
| GET  | /admin/kb/rebuild/status | **查询重建任务进度** |

### 医生接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | /doctor/patients | 我的患者 |
| PUT  | /doctor/registrations/{id}/start | 开始接诊 |
| PUT  | /doctor/registrations/{id}/complete | 完成就诊 |
| POST | /doctor/medical-records | 保存病历 |

### 患者接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | /patient/profile | 查看档案 |
| PUT  | /patient/profile | 保存档案 |
| POST | /patient/consultations/sse | **SSE流式预问诊** |
| POST | /patient/triage | **发起分诊** |
| GET  | /patient/triage/history | 分诊历史 |
| GET  | /patient/schedules | 查询排班 |
| POST | /patient/registrations | 挂号 |
| GET  | /patient/registrations | 挂号记录 |
| GET  | /patient/registrations/{id}/record | 查看病历 |

---

## 九、常见问题

1. **启动报错 "无法连接MySQL"**: 检查application.yml中的数据库地址、端口、账号密码
2. **AI调用失败 "API密钥无效"**: 进入后台AI模型配置页，检查和更新API Key
3. **向量检索无结果**: 需要先上传知识库文档并完成向量化(embed)
4. **分诊结果异常**: 确保症状标签库和科室数据完整，MCP工具处于"启动"状态

---

## 十、开发说明

### 后端开发
- 所有AI调用通过 `HospitalAiService` 进行，便于统一观测与错误翻译
- 数据库配置变更后调用 `ModelFactory.clearCache()` 使新配置生效
- 工具中心修改状态后，下次Agent执行时从DB现查，立即生效

### 前端开发
- 所有API调用使用 `@/utils/request` 中封装的axios实例
- 流式使用 `@/utils/sse.js` 中的 `streamChat` 函数
- 样式变量在 `src/assets/main.css` 的CSS自定义属性中定义

---

## License

MIT
