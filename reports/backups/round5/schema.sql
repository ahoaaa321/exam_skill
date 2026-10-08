-- ============================================================
-- 职业技能等级认定考试报名与考场编排系统 - 数据库脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS skill_exam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE skill_exam;

-- ---------- 用户与权限 ----------
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT 'BCrypt密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    id_card VARCHAR(18) NOT NULL UNIQUE COMMENT '身份证号',
    phone VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    email VARCHAR(100) NULL COMMENT '电子邮箱',
    recovery_token VARCHAR(64) NULL COMMENT '账号恢复令牌（注册时生成，仅显示一次）',
    gender TINYINT NULL COMMENT '性别 0女1男',
    work_unit VARCHAR(100) NULL COMMENT '工作单位',
    age INT NULL COMMENT '年龄',
    occupation VARCHAR(100) NULL COMMENT '职业',
    income_range VARCHAR(50) NULL COMMENT '收入范围',
    region VARCHAR(100) NULL COMMENT '所在地区',
    avatar_url VARCHAR(255) NULL,
    role TINYINT NOT NULL DEFAULT 0 COMMENT '0考生 1管理员 2考务 3超管',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    must_change_pwd TINYINT NOT NULL DEFAULT 0,
    login_fail_count INT NOT NULL DEFAULT 0,
    lock_until DATETIME NULL,
    last_login_time DATETIME NULL,
    last_login_ip VARCHAR(50) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    username VARCHAR(50) NOT NULL,
    login_type TINYINT NOT NULL DEFAULT 1 COMMENT '1登录 2登出',
    login_result TINYINT NOT NULL DEFAULT 1 COMMENT '0失败 1成功',
    client_ip VARCHAR(50) NULL,
    fail_reason VARCHAR(200) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 基础数据 ----------
CREATE TABLE IF NOT EXISTS trade (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trade_name VARCHAR(100) NOT NULL,
    trade_code VARCHAR(50) NOT NULL UNIQUE,
    trade_category VARCHAR(50) NULL,
    description TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skill_level (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trade_id BIGINT NOT NULL,
    level_name VARCHAR(50) NOT NULL,
    level_code VARCHAR(20) NOT NULL,
    level_rank INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS registration_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 考试报名 ----------
CREATE TABLE IF NOT EXISTS exam_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_name VARCHAR(200) NOT NULL,
    plan_code VARCHAR(50) NOT NULL UNIQUE,
    trade_id BIGINT NOT NULL,
    level_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    register_start_time DATETIME NOT NULL,
    register_end_time DATETIME NOT NULL,
    exam_time DATETIME NOT NULL,
    exam_end_time DATETIME NULL,
    exam_location VARCHAR(200) NULL,
    max_candidates INT NOT NULL DEFAULT 0 COMMENT '0不限',
    fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    condition_desc TEXT NULL,
    remark TEXT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布 2已暂停 3已关闭',
    current_count INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    work_years INT NULL,
    education VARCHAR(50) NULL,
    emergency_contact VARCHAR(50) NULL,
    emergency_phone VARCHAR(11) NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待提交 1待审核 2审核通过 3审核退回 4已缴费 5已确认 6已取消',
    reject_reason TEXT NULL,
    first_audit_by BIGINT NULL,
    first_audit_at DATETIME NULL,
    second_audit_by BIGINT NULL,
    second_audit_at DATETIME NULL,
    submitted_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_plan (user_id, plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS registration_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    auditor_id BIGINT NOT NULL,
    audit_level TINYINT NOT NULL COMMENT '1初审 2复审',
    audit_result TINYINT NOT NULL COMMENT '1通过 2退回',
    reason TEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    pay_method VARCHAR(50) NULL COMMENT 'wechat/alipay/bank',
    trade_no VARCHAR(100) NULL,
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已退款',
    paid_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 考场编排 ----------
CREATE TABLE IF NOT EXISTS exam_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_code VARCHAR(50) NOT NULL UNIQUE,
    building VARCHAR(100) NOT NULL,
    classroom VARCHAR(50) NOT NULL,
    seat_count INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS arrangement_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL UNIQUE,
    default_seat_count INT NOT NULL DEFAULT 30,
    seat_gap INT NOT NULL DEFAULT 1 COMMENT '相邻座位间隔',
    shuffle_unit TINYINT NOT NULL DEFAULT 1 COMMENT '同单位打散',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS room_arrangement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL UNIQUE,
    room_id BIGINT NOT NULL,
    seat_no INT NOT NULL,
    ticket_no VARCHAR(80) UNIQUE,
    adjusted TINYINT NOT NULL DEFAULT 0,
    adjust_reason VARCHAR(200) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_seat (room_id, seat_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admission_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL UNIQUE,
    ticket_no VARCHAR(80) NOT NULL UNIQUE,
    generated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    printed_count INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS arrangement_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    total_candidates INT NOT NULL DEFAULT 0,
    arranged_count INT NOT NULL DEFAULT 0,
    room_used INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0进行中 1完成 2失败',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    operator_id BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 成绩与证书 ----------
CREATE TABLE IF NOT EXISTS score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL UNIQUE,
    theory_score DECIMAL(5,2) NULL,
    practice_score DECIMAL(5,2) NULL,
    comprehensive_score DECIMAL(5,2) NULL,
    theory_weight DECIMAL(3,2) NOT NULL DEFAULT 0.40,
    practice_weight DECIMAL(3,2) NOT NULL DEFAULT 0.60,
    result TINYINT NULL COMMENT '1合格 2不合格',
    published TINYINT NOT NULL DEFAULT 0,
    published_at DATETIME NULL,
    created_by BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS score_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    score_id BIGINT NOT NULL,
    old_value VARCHAR(100) NULL,
    new_value VARCHAR(100) NULL,
    reason VARCHAR(300) NOT NULL,
    operator_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS certificate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL UNIQUE,
    certificate_no VARCHAR(80) NOT NULL UNIQUE,
    trade_name VARCHAR(100) NOT NULL,
    level_name VARCHAR(50) NOT NULL,
    issued_at DATE NULL,
    issuer VARCHAR(200) NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待发放 1已发放',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 系统支撑 ----------
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    is_top TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0下线 1已发布',
    created_by BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NULL,
    config_name VARCHAR(100) NOT NULL,
    remark VARCHAR(200) NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NULL,
    operator_name VARCHAR(50) NULL,
    operation_type VARCHAR(50) NOT NULL,
    target VARCHAR(100) NULL,
    detail TEXT NULL,
    before_value TEXT NULL COMMENT '操作前快照',
    after_value TEXT NULL COMMENT '操作后快照',
    client_ip VARCHAR(50) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_email_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    to_email VARCHAR(100) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    send_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待发送 1已发送 2失败',
    send_time DATETIME NULL,
    error_msg VARCHAR(200) NULL,
    user_id BIGINT NULL,
    verify_code VARCHAR(10) NULL,
    expire_at DATETIME NULL,
    used TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invigilator (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(11) NULL,
    invigilate_count INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_signin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    arrangement_id BIGINT NOT NULL,
    signin_time DATETIME NULL,
    signin_type TINYINT NOT NULL DEFAULT 0 COMMENT '0未签到 1扫码 2人脸',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0未签到 1已签到 2缺考',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 索引
CREATE INDEX IF NOT EXISTS idx_reg_user ON registration(user_id);
CREATE INDEX IF NOT EXISTS idx_reg_plan ON registration(plan_id);
CREATE INDEX IF NOT EXISTS idx_reg_status ON registration(status);
CREATE INDEX IF NOT EXISTS idx_arr_room ON room_arrangement(room_id);
CREATE INDEX IF NOT EXISTS idx_msg_user ON message_notification(user_id);
-- 热点查询补索引（报名材料/审核记录/支付流水按报名ID、工单按用户、验证码按邮箱+时间）
CREATE INDEX IF NOT EXISTS idx_material_reg ON registration_material(registration_id);
CREATE INDEX IF NOT EXISTS idx_audit_reg ON audit_record(registration_id);
CREATE INDEX IF NOT EXISTS idx_payment_reg ON payment_record(registration_id);
CREATE INDEX IF NOT EXISTS idx_ticket_user ON after_sales_ticket(user_id);
CREATE INDEX IF NOT EXISTS idx_email_toemail_created ON sys_email_record(to_email, created_at);
-- 一个考场编排对应至多一条签到记录（数据库层兜底并发幂等，插入统一使用 INSERT IGNORE）
CREATE UNIQUE INDEX IF NOT EXISTS uk_signin_arrangement ON exam_signin(arrangement_id);

-- ---------- 增量迁移（兼容已有库） ----------
ALTER TABLE sys_user ADD COLUMN age INT NULL COMMENT '年龄';
ALTER TABLE sys_user ADD COLUMN occupation VARCHAR(100) NULL COMMENT '职业';
ALTER TABLE sys_user ADD COLUMN income_range VARCHAR(50) NULL COMMENT '收入范围';
ALTER TABLE sys_user ADD COLUMN region VARCHAR(100) NULL COMMENT '所在地区';

-- 操作日志增加操作前/后快照
ALTER TABLE operation_log ADD COLUMN before_value TEXT NULL COMMENT '操作前快照';
ALTER TABLE operation_log ADD COLUMN after_value TEXT NULL COMMENT '操作后快照';

-- ---------- AI 问答记录 ----------
CREATE TABLE IF NOT EXISTS ai_qa_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(50) NULL COMMENT '问题分类',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 售后服务工单 ----------
CREATE TABLE IF NOT EXISTS after_sales_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '提交用户',
    registration_id BIGINT NULL COMMENT '关联报名记录',
    title VARCHAR(200) NOT NULL COMMENT '工单标题',
    content TEXT NOT NULL COMMENT '问题描述',
    type VARCHAR(50) NULL COMMENT '类型：refund退款/consult咨询/complaint投诉/other其他',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已解决 3已关闭',
    reply TEXT NULL COMMENT '回复内容',
    replied_by BIGINT NULL COMMENT '回复人',
    replied_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
