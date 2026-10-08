-- ============================================================
-- 大批量演示/测试数据（可重复执行，先清理旧批量数据再生成）
-- 生成量：300名考生 + 900条报名 + 完整业务链路数据约 4500 行
-- 执行方式：
--   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 < bulk_test_data.sql
-- 所有批量考生密码均为 123456，账号 stu001 ~ stu300
-- ============================================================
SET NAMES utf8mb4;
USE skill_exam;

-- ---------- 清理上一轮批量数据（保证可重复执行） ----------
DELETE FROM message_notification WHERE user_id BETWEEN 101 AND 400;
DELETE FROM sys_login_log      WHERE user_id BETWEEN 101 AND 400;
DELETE FROM ai_qa_log          WHERE user_id BETWEEN 101 AND 400;
DELETE FROM after_sales_ticket WHERE user_id BETWEEN 101 AND 400;
DELETE FROM sys_email_record   WHERE to_email REGEXP '^stu[0-9]+@qq.com';
DELETE FROM audit_record       WHERE registration_id BETWEEN 2000 AND 2899;
DELETE FROM payment_record     WHERE registration_id BETWEEN 2000 AND 2899;
DELETE FROM exam_signin        WHERE arrangement_id BETWEEN 3000 AND 3999;
DELETE FROM admission_ticket   WHERE registration_id BETWEEN 2000 AND 2899;
DELETE FROM room_arrangement   WHERE registration_id BETWEEN 2000 AND 2899;
DELETE FROM score_change_log   WHERE score_id BETWEEN 4001 AND 4899;
DELETE FROM score              WHERE registration_id BETWEEN 2000 AND 2899;
DELETE FROM certificate        WHERE registration_id BETWEEN 2000 AND 2899;
DELETE FROM registration       WHERE id BETWEEN 2000 AND 2899;
DELETE FROM sys_user           WHERE id BETWEEN 101 AND 400;
DELETE FROM operation_log      WHERE operator_name IN ('系统管理员','考务人员','超级管理员') AND id > 100;

-- ---------- 补充考场（编号10-21，专供批量编排使用） ----------
INSERT IGNORE INTO exam_room (id, room_code, building, classroom, seat_count, status) VALUES
(10, 'E-401', '教学楼E', '401', 50, 1), (11, 'E-402', '教学楼E', '402', 50, 1),
(12, 'E-403', '教学楼E', '403', 50, 1), (13, 'E-404', '教学楼E', '404', 50, 1),
(14, 'E-405', '教学楼E', '405', 50, 1), (15, 'E-406', '教学楼E', '406', 50, 1),
(16, 'F-401', '教学楼F', '401', 50, 1), (17, 'F-402', '教学楼F', '402', 50, 1),
(18, 'F-403', '教学楼F', '403', 50, 1), (19, 'F-404', '教学楼F', '404', 50, 1),
(20, 'F-405', '教学楼F', '405', 50, 1), (21, 'F-406', '教学楼F', '406', 50, 1);

-- ---------- 补充监考员 ----------
INSERT IGNORE INTO invigilator (id, name, phone, invigilate_count) VALUES
(4,  '王建国', '13920000001', 4), (5,  '刘淑华', '13920000002', 3),
(6,  '陈志强', '13920000003', 5), (7,  '林晓燕', '13920000004', 2),
(8,  '赵国庆', '13920000005', 4), (9,  '孙美玲', '13920000006', 3),
(10, '周文斌', '13920000007', 6), (11, '吴桂芳', '13920000008', 2),
(12, '郑海洋', '13920000009', 3), (13, '冯雪梅', '13920000010', 4),
(14, '许志远', '13920000011', 2), (15, '何静怡', '13920000012', 5);

-- ---------- 补充公告 ----------
INSERT IGNORE INTO announcement (id, title, content, is_top, status, created_by) VALUES
(8,  '关于2026年国庆节期间客服安排的通知', '国庆节假期期间（10月1日-10月8日）线上报名系统正常运行，人工客服暂停服务，10月9日起恢复。如有紧急问题请发送邮件至服务邮箱。', 0, 1, 3),
(9,  '新增12个职业技能等级认定工种的公告', '经主管部门批准，本季度新增美容师、茶艺师、育婴员、养老护理员、公共营养师等12个工种的等级认定服务，欢迎广大从业者报名。', 0, 1, 3),
(10, '考试成绩复核申请流程说明', '考生对成绩有异议的，可在成绩公布后5个工作日内，通过个人中心"售后服务"提交成绩复核申请，复核结果将在10个工作日内反馈。', 1, 1, 3);

-- ---------- 编排配置示例 ----------
INSERT IGNORE INTO arrangement_config (plan_id, default_seat_count, seat_gap, shuffle_unit) VALUES
(1, 30, 1, 1), (3, 30, 1, 1), (5, 30, 1, 1);

-- ============================================================
-- 主造数过程
-- ============================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS bulk_seed $$
CREATE PROCEDURE bulk_seed()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE k INT DEFAULT 1;
    DECLARE j INT DEFAULT 1;
    DECLARE uid INT;
    DECLARE pid INT;
    DECLARE rid INT;
    DECLARE st INT;
    DECLARE m INT;
    DECLARE sm INT;
    DECLARE v_num INT;
    DECLARE v_sub DATETIME;
    DECLARE v_fee DECIMAL(10,2);
    DECLARE v_plan_name VARCHAR(200);
    DECLARE v_reason VARCHAR(200);
    DECLARE v_theory DECIMAL(5,2);
    DECLARE v_practice DECIMAL(5,2);
    DECLARE v_comp DECIMAL(5,2);
    DECLARE v_result TINYINT;
    DECLARE v_room INT;
    DECLARE v_seat INT;
    DECLARE v_aid INT;
    DECLARE v_ticket VARCHAR(80);

    SET v_room = 10;
    SET v_seat = 1;
    SET v_aid  = 3000;

    -- ============ 1. 300名考生用户 ============
    WHILE i <= 300 DO
        SET uid = 100 + i;
        INSERT IGNORE INTO sys_user
            (id, username, password, real_name, id_card, phone, email, recovery_token, gender,
             work_unit, age, occupation, income_range, region, role, status, created_at)
        VALUES
            (uid, CONCAT('stu', LPAD(i, 3, '0')),
             '$2a$10$0RaD6iOffh1U.oeSyPBuQOT5tC.DajvlXCzJGaSx42c18K7Wisjoe',
             CONCAT(ELT(i % 20 + 1, '王','李','张','刘','陈','杨','赵','黄','周','吴','徐','孙','胡','朱','高','林','何','郭','马','罗'),
                    ELT((i * 7) % 30 + 1, '伟','芳','娜','敏','静','磊','军','洋','勇','艳','杰','娟','涛','明','超','秀英','霞','平','刚','桂英','文博','思远','雨欣','子豪','梦琪','志强','建国','丽华','海燕','小红')),
             CONCAT('11010119', LPAD(70 + (i % 30), 2, '0'), LPAD(i % 12 + 1, 2, '0'), LPAD(i % 28 + 1, 2, '0'), LPAD(i, 4, '0')),
             CONCAT('139', LPAD(10000000 + i * 13, 8, '0')),
             CONCAT('stu', LPAD(i, 3, '0'), '@qq.com'),
             CONCAT('RCVR', LPAD(i, 4, '0'), MD5(i * 31 + 7)),
             i % 2,
             CONCAT(ELT(i % 8 + 1, '华信科技有限公司','蓝天制造有限公司','东方建设工程公司','恒达服务有限公司','中南人力资源公司','绿源环保科技公司','宏远物流有限公司','市第一人民医院')),
             2026 - (1970 + i % 30),
             ELT(i % 10 + 1, '电工','焊工','钳工','行政文员','维修技师','销售顾问','教师','护士','在校学生','个体经营者'),
             ELT(i % 4 + 1, '5万以下','5-10万','10-20万','20万以上'),
             ELT(i % 10 + 1, '北京市朝阳区','上海市浦东新区','广州市天河区','深圳市南山区','杭州市西湖区','成都市武侯区','武汉市洪山区','西安市雁塔区','南京市鼓楼区','重庆市渝中区'),
             0, 1, DATE_SUB(NOW(), INTERVAL (i * 5) % 60 DAY));
        SET i = i + 1;
    END WHILE;

    -- ============ 2. 每人3条报名记录 = 900条，覆盖全部状态 ============
    SET i = 1;
    WHILE i <= 300 DO
        SET uid = 100 + i;
        SET k = 1;
        WHILE k <= 3 DO
            SET pid = ((i - 1) + (k - 1) * 9) % 24 + 1;   -- 3个偏移互不相同，同一用户不重复报同一计划
            SET rid = 2000 + (i - 1) * 3 + k;

            SELECT plan_name, fee INTO v_plan_name, v_fee FROM exam_plan WHERE id = pid;

            -- 状态分配：钳工初级(计划3,考试2026-09-10已结束)走完整生命周期=5已确认；
            -- 其余未来考试按 0待提交/1待审核/2审核通过/3审核退回/4已缴费 分布，少量6已取消
            IF pid = 3 THEN
                SET st = 5;
            ELSE
                SET m = (i + k) % 6;
                SET st = IF(m = 5, 4, m);
                IF m = 0 AND (i + k) % 4 = 0 THEN SET st = 6; END IF;
            END IF;

            IF st = 5 THEN
                SET v_sub = DATE_ADD('2026-08-12 09:00:00', INTERVAL (i + k) % 20 DAY);
            ELSEIF st = 0 OR st = 6 THEN
                SET v_sub = NULL;
            ELSE
                SET v_sub = DATE_SUB(NOW(), INTERVAL (i * 3 + k) % 72 HOUR);
            END IF;

            SET v_reason = ELT((i + k) % 5 + 1,
                '身份证照片模糊，无法核验身份信息',
                '工作年限证明材料不足，请补充上传',
                '学历证书不清晰，请重新扫描上传',
                '报考条件不符，工作年限未达到要求',
                '报名信息与材料信息不一致，请核对后重新提交');

            -- 报名主记录
            INSERT IGNORE INTO registration
                (id, user_id, plan_id, work_years, education, emergency_contact, emergency_phone,
                 status, reject_reason, first_audit_by, first_audit_at, second_audit_by, second_audit_at,
                 submitted_at, created_at)
            VALUES
                (rid, uid, pid, 1 + (i + k) % 8,
                 ELT((i + k) % 5 + 1, '初中','高中','大专','本科','硕士'),
                 ELT((i + k) % 10 + 1, '王强','李娜','张勇','刘敏','陈杰','杨芳','赵磊','黄霞','周涛','吴丽'),
                 CONCAT('136', LPAD(80000000 + rid, 8, '0')),
                 st,
                 IF(st = 3, v_reason, NULL),
                 IF(st IN (2,4,5), 1, NULL), IF(st IN (2,4,5), DATE_ADD(v_sub, INTERVAL 20 HOUR), NULL),
                 IF(st IN (4,5), 2, NULL),  IF(st IN (4,5), DATE_ADD(v_sub, INTERVAL 44 HOUR), NULL),
                 v_sub, IF(v_sub IS NULL, DATE_SUB(NOW(), INTERVAL (i * k) % 48 HOUR), v_sub));

            -- 审核记录
            IF st = 2 THEN
                INSERT INTO audit_record (registration_id, auditor_id, audit_level, audit_result, reason, created_at)
                VALUES (rid, 1, 1, 1, '材料齐全，符合报考条件，审核通过。', DATE_ADD(v_sub, INTERVAL 20 HOUR));
            ELSEIF st = 3 THEN
                INSERT INTO audit_record (registration_id, auditor_id, audit_level, audit_result, reason, created_at)
                VALUES (rid, 1, 1, 2, v_reason, DATE_ADD(v_sub, INTERVAL 20 HOUR));
            ELSEIF st IN (4, 5) THEN
                INSERT INTO audit_record (registration_id, auditor_id, audit_level, audit_result, reason, created_at)
                VALUES (rid, 1, 1, 1, '材料齐全，符合报考条件，初审通过。', DATE_ADD(v_sub, INTERVAL 20 HOUR)),
                       (rid, 2, 2, 1, '复审通过，同意报考。', DATE_ADD(v_sub, INTERVAL 44 HOUR));
            END IF;

            -- 缴费记录（已缴费/已确认）
            IF st IN (4, 5) THEN
                INSERT IGNORE INTO payment_record
                    (registration_id, amount, pay_method, trade_no, pay_status, paid_at, created_at)
                VALUES
                    (rid, v_fee, ELT((i + k) % 3 + 1, 'wechat','alipay','bank'),
                     CONCAT('PAY2026', LPAD(rid, 8, '0')), 1,
                     DATE_ADD(v_sub, INTERVAL 3 HOUR), DATE_ADD(v_sub, INTERVAL 3 HOUR));
            END IF;

            -- 考场编排 + 准考证 + 签到
            IF st IN (4, 5) THEN
                IF v_seat > 50 THEN SET v_room = v_room + 1; SET v_seat = 1; END IF;
                SET v_aid = v_aid + 1;
                SET v_ticket = CONCAT('ZK2026', LPAD(v_aid - 3000, 6, '0'));
                INSERT IGNORE INTO room_arrangement (id, registration_id, room_id, seat_no, ticket_no, created_at)
                VALUES (v_aid, rid, v_room, v_seat, v_ticket, IF(pid = 3, '2026-09-08 10:00:00', NOW()));
                INSERT IGNORE INTO admission_ticket (registration_id, ticket_no, generated_at)
                VALUES (rid, v_ticket, IF(pid = 3, '2026-09-08 10:00:00', NOW()));
                IF pid = 3 THEN
                    -- 已考完：90%已签到 / 6%缺考 / 4%未签到
                    SET sm = (i * 2 + k) % 50;
                    IF sm < 45 THEN
                        INSERT INTO exam_signin (arrangement_id, signin_time, signin_type, status)
                        VALUES (v_aid, DATE_ADD('2026-09-10 07:40:00', INTERVAL (i * 17 + k * 3) % 70 MINUTE), 1 + sm % 2, 1);
                    ELSEIF sm < 48 THEN
                        INSERT INTO exam_signin (arrangement_id, signin_time, signin_type, status)
                        VALUES (v_aid, NULL, 0, 2);
                    ELSE
                        INSERT INTO exam_signin (arrangement_id, signin_time, signin_type, status)
                        VALUES (v_aid, NULL, 0, 0);
                    END IF;
                ELSE
                    INSERT INTO exam_signin (arrangement_id, signin_time, signin_type, status)
                    VALUES (v_aid, NULL, 0, 0);
                END IF;
                SET v_seat = v_seat + 1;
            END IF;

            -- 成绩与证书（仅计划3已考完的考生）
            IF pid = 3 THEN
                SET v_theory   = 55 + (i * 7) % 45;
                SET v_practice = 50 + (i * 11) % 50;
                SET v_comp     = ROUND(v_theory * 0.40 + v_practice * 0.60, 1);
                SET v_result   = IF(v_comp >= 60, 1, 2);
                INSERT IGNORE INTO score
                    (id, registration_id, theory_score, practice_score, comprehensive_score,
                     theory_weight, practice_weight, result, published, published_at, created_by, created_at)
                VALUES
                    (4000 + rid - 2000, rid, v_theory, v_practice, v_comp, 0.40, 0.60,
                     v_result, 1, '2026-09-15 10:00:00', 2, '2026-09-12 15:00:00');
                IF (i + k) % 9 = 0 THEN
                    INSERT INTO score_change_log (score_id, old_value, new_value, reason, operator_id, created_at)
                    VALUES (4000 + rid - 2000, v_comp, v_comp + 2, '成绩复核：理论成绩录入更正', 2, '2026-09-16 11:00:00');
                END IF;
                IF v_result = 1 THEN
                    INSERT IGNORE INTO certificate (registration_id, certificate_no, trade_name, level_name, issued_at, issuer, status)
                    SELECT rid, CONCAT('SKLCERT2026', LPAD(rid, 8, '0')), t.trade_name, l.level_name,
                           '2026-09-18', '市职业技能鉴定中心', 1
                    FROM exam_plan p JOIN trade t ON p.trade_id = t.id JOIN skill_level l ON p.level_id = l.id
                    WHERE p.id = pid;
                    INSERT INTO message_notification (user_id, title, content, is_read, created_at)
                    VALUES (uid, '证书已发放', CONCAT('恭喜！您的职业技能等级证书已发放，证书编号：SKLCERT2026', LPAD(rid, 8, '0'), '，可在个人中心查看。'), 0, '2026-09-18 10:00:00');
                END IF;
            END IF;

            -- 站内消息（每条报名1条状态消息）
            INSERT INTO message_notification (user_id, title, content, is_read, created_at) VALUES
                (uid,
                 CASE st WHEN 0 THEN '报名表尚未提交' WHEN 1 THEN '报名资料已提交' WHEN 2 THEN '资格审核通过'
                         WHEN 3 THEN '报名审核退回' WHEN 4 THEN '缴费成功' ELSE '考场编排完成' END,
                 CASE st
                     WHEN 0 THEN CONCAT('您报考的《', v_plan_name, '》报名表尚未提交，请及时完善并提交。')
                     WHEN 1 THEN CONCAT('您的《', v_plan_name, '》报名资料已提交成功，请等待管理员审核。')
                     WHEN 2 THEN CONCAT('您的《', v_plan_name, '》报名已通过资格审核，请及时完成缴费。')
                     WHEN 3 THEN CONCAT('您的报名被退回：', v_reason, ' 请修改后重新提交。')
                     WHEN 4 THEN CONCAT('您已成功缴纳《', v_plan_name, '》考试费用，等待考场编排。')
                     ELSE CONCAT('您的考场已编排完成，准考证号：', v_ticket, '，请在个人中心查看准考证。')
                 END,
                 IF((i + k) % 3 = 0, 1, 0),
                 IF(v_sub IS NULL, DATE_SUB(NOW(), INTERVAL (i * k) % 48 HOUR), v_sub));
            SET k = k + 1;
        END WHILE;
        SET i = i + 1;
    END WHILE;

    -- ============ 3. 登录日志 500条（近15天） ============
    SET j = 1;
    WHILE j <= 500 DO
        SET v_num = j % 300;
        IF v_num = 0 THEN SET v_num = 300; END IF;
        INSERT INTO sys_login_log
            (user_id, username, login_type, login_result, client_ip, fail_reason, created_at)
        VALUES
            (100 + v_num, CONCAT('stu', LPAD(v_num, 3, '0')),
             IF(j % 13 = 0, 2, 1), IF(j % 9 = 0, 0, 1),
             CONCAT('192.168.', j % 250, '.', (j * 7) % 250 + 1),
             IF(j % 9 = 0, '密码错误', NULL),
             DATE_SUB(NOW(), INTERVAL (j * 43) % 21600 MINUTE));
        SET j = j + 1;
    END WHILE;

    -- ============ 4. 操作日志 80条（近30天） ============
    SET j = 1;
    WHILE j <= 80 DO
        INSERT INTO operation_log
            (operator_id, operator_name, operation_type, target, detail, client_ip, created_at)
        VALUES
            (ELT(j % 3 + 1, 1, 2, 3),
             ELT(j % 3 + 1, '系统管理员','考务人员','超级管理员'),
             ELT(j % 10 + 1, '新增考试计划','更新考试计划','审核报名','缴费确认','考场编排','成绩录入','发布成绩','发放证书','重置用户密码','更新系统配置'),
             CONCAT(ELT(j % 4 + 1, '考试计划#', '报名记录#', '用户#', '考场#'), j % 24 + 1),
             ELT(j % 10 + 1,
                 '创建新的职业技能等级认定考试计划并发布',
                 '调整考试计划的报名截止时间与费用标准',
                 '对考生报名材料进行资格审核',
                 '确认考生缴费到账并更新报名状态',
                 '对已缴费考生执行考场座位自动编排',
                 '录入考生理论与实操成绩',
                 '发布批次成绩并通知考生查询',
                 '为合格考生制作并发放等级证书',
                 '重置考生登录密码并邮件通知',
                 '更新系统参数配置项'),
             CONCAT('10.0.', j % 20, '.', j % 250 + 2),
             DATE_SUB(NOW(), INTERVAL (j * 9) % 43200 MINUTE));
        SET j = j + 1;
    END WHILE;

    -- ============ 5. AI问答记录 60条 ============
    SET j = 1;
    WHILE j <= 60 DO
        SET v_num = j % 300;
        IF v_num = 0 THEN SET v_num = 300; END IF;
        INSERT INTO ai_qa_log (user_id, question, answer, category, created_at) VALUES
            (100 + v_num,
             ELT(j % 6 + 1,
                 '报名需要准备哪些材料？', '支持哪些缴费方式？', '考场和座位是怎么分配的？',
                 '成绩什么时候公布？在哪里查询？', '证书如何领取？多久能拿到？', '忘记密码怎么办？'),
             ELT(j % 6 + 1,
                 '报名需准备：本人身份证正反面照片、学历证明、工作年限证明（按报考条件）、近期免冠证件照。请在上传材料页依次上传，单个文件不超过10MB，支持JPG/PNG/PDF格式。',
                 '目前支持微信支付、支付宝和银行转账三种方式，缴费成功后报名状态将实时更新为"已缴费"。',
                 '系统根据您报考的计划自动编排考场与座位，编排完成后可在个人中心查看准考证号、考场地点及座位号。',
                 '考试结束后约10个工作日公布成绩，届时可在个人中心"我的成绩"页面查询理论和实操成绩及综合评定结果。',
                 '成绩合格后，电子证书可在个人中心直接查看和下载，纸质证书将于考后30个工作日内制作并邮寄发放。',
                 '可在登录页点击"忘记密码"，通过注册邮箱获取验证码重置密码；也可使用注册时生成的16位账号恢复令牌找回。'),
             ELT(j % 6 + 1, 'registration', 'payment', 'exam_venue', 'results', 'certificate', 'password'),
             DATE_SUB(NOW(), INTERVAL (j * 61) % 14400 MINUTE));
        SET j = j + 1;
    END WHILE;

    -- ============ 6. 售后工单 25条 ============
    SET j = 1;
    WHILE j <= 25 DO
        SET v_num = j % 300;
        IF v_num = 0 THEN SET v_num = 300; END IF;
        INSERT INTO after_sales_ticket
            (user_id, registration_id, title, content, type, status, reply, replied_by, replied_at, created_at)
        VALUES
            (100 + v_num, NULL,
             ELT(j % 5 + 1, '申请退还考试费用','如何修改报名信息','成绩复核申请咨询','证书发放进度咨询','考试当天时间冲突申诉'),
             ELT(j % 5 + 1,
                 '本人因工作调动无法按时参加考试，申请退还已缴纳的考试费用，望审核处理。',
                 '报名时工作单位填写有误，现需要修改为单位最新名称，请问如何操作？',
                 '本人的实操成绩与预期差距较大，申请成绩复核并说明复核流程。',
                 '证书显示已发放但尚未收到邮寄，想咨询当前发放进度。',
                 '考试时间与单位安排的出差时间冲突，申请协调其他批次或延期参考。'),
             ELT(j % 5 + 1, 'refund','consult','consult','consult','complaint'),
             j % 4,
             IF(j % 4 >= 2,
                ELT(j % 5 + 1,
                    '经核实符合退费条件，费用将在3个工作日内原路退回，请注意查收。',
                    '您好，请携带单位证明材料到鉴定中心前台办理信息变更，或联系客服协助线上修改。',
                    '已收到您的复核申请，复核结果将在10个工作日内通过站内消息和邮件反馈。',
                    '证书已交由邮政EMS寄出，单号将发送至您的邮箱，请注意查收。',
                    '已记录您的情况，将协调下一批次优先安排，结果以站内消息通知为准。'),
                NULL),
             IF(j % 4 >= 2, 1, NULL),
             IF(j % 4 >= 2, DATE_SUB(NOW(), INTERVAL (j * 11) % 7200 MINUTE), NULL),
             DATE_SUB(NOW(), INTERVAL (j * 17) % 10080 MINUTE));
        SET j = j + 1;
    END WHILE;

    -- ============ 7. 邮件发送记录 80条 ============
    SET j = 1;
    WHILE j <= 80 DO
        SET v_num = j % 300;
        IF v_num = 0 THEN SET v_num = 300; END IF;
        INSERT INTO sys_email_record
            (to_email, subject, content, send_status, send_time, user_id, verify_code, expire_at, used, created_at)
        VALUES
            (CONCAT('stu', LPAD(v_num, 3, '0'), '@qq.com'),
             ELT(j % 5 + 1, '注册账号验证码','报名提交成功通知','资格审核结果通知','缴费成功通知','成绩发布通知'),
             ELT(j % 5 + 1,
                 '您正在注册职业技能等级认定考试报名系统账号，验证码5分钟内有效，请勿泄露给他人。',
                 '您的报名资料已提交成功，请登录系统在"我的报名流程"中查看审核进度。',
                 '您的报名资格审核已有结果，请登录系统查看详情并完成后续操作。',
                 '您的考试费用已缴纳成功，请保留支付凭证，考场编排完成后将另行通知。',
                 '本批次考试成绩已公布，请登录个人中心"我的成绩"查看详细成绩信息。'),
             1,
             DATE_SUB(NOW(), INTERVAL (j * 23) % 14400 MINUTE),
             100 + v_num,
             IF(j % 5 = 1, LPAD((j * 37) % 1000000, 6, '0'), NULL),
             IF(j % 5 = 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL (j * 23) % 14400 MINUTE), INTERVAL 5 MINUTE), NULL),
             IF(j % 5 = 1, 1, 0),
             DATE_SUB(NOW(), INTERVAL (j * 23) % 14400 MINUTE));
        SET j = j + 1;
    END WHILE;
END $$

DELIMITER ;

-- 执行造数
CALL bulk_seed();
DROP PROCEDURE IF EXISTS bulk_seed;

-- ---------- 收尾：刷新计划报名人数、监考次数 ----------
UPDATE exam_plan p
SET p.current_count = (SELECT COUNT(*) FROM registration r WHERE r.plan_id = p.id AND r.status IN (1,2,4,5));
UPDATE invigilator SET invigilate_count = 2 + id % 5 WHERE invigilate_count = 0;

-- ---------- 结果概览 ----------
SELECT '考生用户' AS item, COUNT(*) AS cnt FROM sys_user WHERE id BETWEEN 101 AND 400
UNION ALL SELECT '报名总数',   COUNT(*) FROM registration
UNION ALL SELECT '待提交',     COUNT(*) FROM registration WHERE status = 0
UNION ALL SELECT '待审核',     COUNT(*) FROM registration WHERE status = 1
UNION ALL SELECT '审核通过',   COUNT(*) FROM registration WHERE status = 2
UNION ALL SELECT '审核退回',   COUNT(*) FROM registration WHERE status = 3
UNION ALL SELECT '已缴费',     COUNT(*) FROM registration WHERE status = 4
UNION ALL SELECT '已确认',     COUNT(*) FROM registration WHERE status = 5
UNION ALL SELECT '已取消',     COUNT(*) FROM registration WHERE status = 6
UNION ALL SELECT '缴费记录',   COUNT(*) FROM payment_record
UNION ALL SELECT '审核记录',   COUNT(*) FROM audit_record
UNION ALL SELECT '考场编排',   COUNT(*) FROM room_arrangement
UNION ALL SELECT '准考证',     COUNT(*) FROM admission_ticket
UNION ALL SELECT '签到记录',   COUNT(*) FROM exam_signin
UNION ALL SELECT '成绩',       COUNT(*) FROM score
UNION ALL SELECT '证书',       COUNT(*) FROM certificate
UNION ALL SELECT '站内消息',   COUNT(*) FROM message_notification
UNION ALL SELECT '登录日志',   COUNT(*) FROM sys_login_log
UNION ALL SELECT '操作日志',   COUNT(*) FROM operation_log
UNION ALL SELECT 'AI问答',     COUNT(*) FROM ai_qa_log
UNION ALL SELECT '售后工单',   COUNT(*) FROM after_sales_ticket
UNION ALL SELECT '邮件记录',   COUNT(*) FROM sys_email_record
UNION ALL SELECT '监考员',     COUNT(*) FROM invigilator;
