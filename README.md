# 职业技能等级认定考试报名与考场编排系统

基于 Spring Boot 4 + 原生 HTML/JS + MySQL 8 的考试业务全流程系统，覆盖「考生注册报名 → 两级审核 → 缴费 → 考场座位编排 → 准考证 → 签到 → 成绩 → 证书发证与查验」，并提供管理员、考务人员、超级管理员三类后台。

## 技术栈

- 后端：Java 25、Spring Boot 4.1、Spring JDBC（JdbcTemplate）、BCrypt、JWT、JavaMail
- 前端：原生 HTML/CSS/JavaScript（无构建步骤）、Chart.js
- 数据库：MySQL 8（库名 `skill_exam`，29 张表，建表脚本 `src/main/resources/schema.sql`）

## 本地运行

1. 准备 MySQL 8，确认账号可建库（连接串含 `createDatabaseIfNotExist=true`，会自动创建 `skill_exam`）。
2. 复制配置模板并填入你自己的连接信息与邮箱授权码：
   ```bash
   cp src/main/resources/application.example.yml src/main/resources/application.yml
   ```
   或通过环境变量注入：`DB_PASSWORD`、`MAIL_PASSWORD`、`APP_JWT_SECRET`。
3. 启动：
   ```bash
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```
4. 浏览器访问 <http://localhost:8080/>

## 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 考生 | candidate | 123456 |
| 管理员 | admin | 123456 |
| 考务人员 | staff | 123456 |
| 超级管理员 | super | 123456 |

> 演示账号由 `data.sql` 初始化，正式部署请修改默认密码。

## 主要模块

- 考生端：注册（邮箱验证码）、登录、报名、缴费、准考证打印、自助签到、成绩/证书查询、站内消息、售后工单
- 管理端：考试计划/工种等级/报名分类、初审复审、缴费确认、成绩录入发布、证书发放、公告、用户与系统配置
- 考务端：编排参数配置、一键自动编排、考场座位可视化、手动调座、监考员、签到/缺考登记
- 超管端：操作日志、登录日志、系统健康、数据库健康
- 公共：首页考试计划（报名热度/倒计时）、通知公告、证书公开查验、AI 智能问答

## 安全说明

- 密码 BCrypt 哈希存储；登录失败 5 次锁定 30 分钟；账号不存在时执行哑 BCrypt 比对消除时序侧信道
- 邮箱验证码 5 分钟有效、60 秒发送间隔、每日上限，原子核销防双花
- 找回密码对未注册邮箱返回统一响应，防止账号枚举
- **请勿把含真实凭证的 `application.yml` 提交到仓库**（已在 `.gitignore` 忽略，仅提交 `application.example.yml` 模板）
