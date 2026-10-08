# -*- coding: utf-8 -*-
"""生成 skill_exam 数据库表结构与字段说明 Word 文档。"""
import os
from docx import Document
from docx.shared import Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'skill_exam数据库表结构说明.docx')

# ============ 文档数据：(表名, 中文名, 行数, 用途, [(字段,类型,含义)...]) ============
TABLES = [
("一、账户与权限", [
 ("sys_user","用户账号表","307","全站登录主体，考生/管理员/考务/超管四种角色共用，含认证锁定与个人资料。",[
  ("id","bigint","主键"),
  ("username","varchar(50)","用户名（唯一；字母开头 4-30 位字母/数字/下划线）"),
  ("password","varchar(128)","BCrypt 哈希密码（$2a$10$ 前缀，非明文）"),
  ("real_name","varchar(50)","真实姓名"),
  ("id_card","varchar(18)","身份证号（唯一，18 位末位可为 X）"),
  ("phone","varchar(11)","手机号（唯一，也可用于登录）"),
  ("email","varchar(100)","电子邮箱（接收验证码/通知）"),
  ("recovery_token","varchar(64)","16 位账号恢复令牌（注册时生成仅显示一次，用后清空）"),
  ("gender","tinyint","性别：0 女 1 男"),
  ("work_unit","varchar(100)","工作单位"),
  ("age","int","年龄"),
  ("occupation","varchar(100)","职业"),
  ("income_range","varchar(50)","收入范围"),
  ("region","varchar(100)","所在地区"),
  ("avatar_url","varchar(255)","头像地址"),
  ("role","tinyint","角色：0 考生 1 管理员 2 考务 3 超管"),
  ("status","tinyint","状态：0 禁用 1 启用"),
  ("must_change_pwd","tinyint","是否需下次登录改密（管理员重置后置 1）"),
  ("login_fail_count","int","连续登录失败次数（达 5 次锁定 30 分钟）"),
  ("lock_until","datetime","锁定截止时间"),
  ("last_login_time","datetime","最近登录时间"),
  ("last_login_ip","varchar(50)","最近登录 IP"),
  ("created_at / updated_at","datetime","创建时间 / 更新时间"),
 ]),
 ("sys_role","角色字典表","4","固定四种系统角色。",[
  ("id","bigint","主键"),
  ("role_name","varchar(50)","角色名称（考生/管理员/考务人员/超级管理员）"),
  ("role_code","varchar(50)","角色编码（唯一：CANDIDATE/ADMIN/EXAM_STAFF/SUPER_ADMIN）"),
  ("description","varchar(200)","角色描述"),
  ("sort_order","int","排序"),
  ("status","tinyint","状态：0 禁用 1 启用"),
  ("created_at","datetime","创建时间"),
 ]),
 ("sys_login_log","登录日志表","673","每次登录/登出及成败的安全审计记录。",[
  ("id","bigint","主键"),
  ("user_id","bigint","账号 ID（账号不存在时为空）"),
  ("username","varchar(50)","尝试登录的用户名"),
  ("login_type","tinyint","类型：1 登录 2 登出"),
  ("login_result","tinyint","结果：0 失败 1 成功"),
  ("client_ip","varchar(50)","客户端 IP"),
  ("fail_reason","varchar(200)","失败原因（密码错误第 N 次/账号锁定中等）"),
  ("created_at","datetime","记录时间"),
 ]),
 ("operation_log","操作日志表","204","后台管理操作审计，含操作前后数据快照。",[
  ("id","bigint","主键"),
  ("operator_id","bigint","操作人 ID"),
  ("operator_name","varchar(50)","操作人用户名"),
  ("operation_type","varchar(50)","操作类型（REGISTER/AUDIT 等）"),
  ("target","varchar(100)","操作对象"),
  ("detail","text","操作详情"),
  ("client_ip","varchar(50)","客户端 IP"),
  ("before_value","text","操作前数据快照"),
  ("after_value","text","操作后数据快照"),
  ("created_at","datetime","操作时间"),
 ]),
]),
("二、工种与等级目录", [
 ("trade","工种目录表","20","职业工种主数据（电工/焊工/钳工等）。",[
  ("id","bigint","主键"),
  ("trade_name","varchar(100)","工种名称"),
  ("trade_code","varchar(50)","工种编码（唯一，如 ELECTRICIAN）"),
  ("trade_category","varchar(50)","工种分类"),
  ("description","text","工种描述"),
  ("sort_order","int","排序"),
  ("status","tinyint","状态：0 禁用 1 启用"),
  ("created_at","datetime","创建时间"),
 ]),
 ("skill_level","技能等级表","62","各工种下的等级（五级初级至一级高级技师）。",[
  ("id","bigint","主键"),
  ("trade_id","bigint","所属工种 ID"),
  ("level_name","varchar(50)","等级名称"),
  ("level_code","varchar(20)","等级编码"),
  ("level_rank","int","等级高低排序"),
  ("sort_order","int","展示排序"),
  ("status","tinyint","状态：0 禁用 1 启用"),
  ("created_at","datetime","创建时间"),
 ]),
]),
("三、报名分类与系统配置", [
 ("registration_category","报名类别表","3","报名性质字典：新考/补考/复审。",[
  ("id","bigint","主键"),
  ("category_name","varchar(100)","类别名称"),
  ("category_code","varchar(50)","类别编码（唯一：NEW/RETAKE/REVIEW）"),
  ("description","varchar(200)","类别说明"),
  ("sort_order","int","排序"),
  ("status","tinyint","状态：0 禁用 1 启用"),
  ("created_at","datetime","创建时间"),
 ]),
 ("system_config","系统参数表","3","键值对形式的可调业务参数。",[
  ("id","bigint","主键"),
  ("config_key","varchar(100)","参数键（唯一）"),
  ("config_value","text","参数值"),
  ("config_name","varchar(100)","参数名称（如 登录最大失败次数=5）"),
  ("remark","varchar(200)","备注"),
  ("updated_at","datetime","更新时间"),
 ]),
]),
("四、考试计划", [
 ("exam_plan","考试计划表","24","一场认定考试的定义，报名与编排的总入口；首页计划卡数据源。",[
  ("id","bigint","主键"),
  ("plan_name","varchar(200)","计划名称"),
  ("plan_code","varchar(50)","计划编号（唯一）"),
  ("trade_id","bigint","工种 ID"),
  ("level_id","bigint","等级 ID"),
  ("category_id","bigint","报名类别 ID"),
  ("register_start_time","datetime","报名开始时间"),
  ("register_end_time","datetime","报名截止时间（首页倒计时徽章依据）"),
  ("exam_time","datetime","考试开始时间"),
  ("exam_end_time","datetime","考试结束时间"),
  ("exam_location","varchar(200)","考试地点"),
  ("max_candidates","int","名额上限（0 不限）"),
  ("current_count","int","当前已报人数（首页热度条依据；悲观锁防并发超招）"),
  ("fee","decimal(10,2)","报考费用"),
  ("condition_desc","text","申报条件说明"),
  ("remark","text","备注"),
  ("status","tinyint","状态：0 草稿 1 已发布 2 已暂停 3 已关闭"),
  ("created_by","bigint","创建管理员 ID"),
  ("created_at / updated_at","datetime","创建时间 / 更新时间"),
 ]),
]),
("五、报名与审核", [
 ("registration","报名记录表","902","核心业务表：一个考生对一个计划的一次报名。",[
  ("id","bigint","主键"),
  ("user_id","bigint","考生 ID"),
  ("plan_id","bigint","考试计划 ID"),
  ("work_years","int","从业年限"),
  ("education","varchar(50)","学历"),
  ("emergency_contact","varchar(50)","紧急联系人"),
  ("emergency_phone","varchar(11)","紧急联系电话"),
  ("status","tinyint","0 待提交 1 待审核 2 审核通过 3 审核退回 4 已缴费 5 已确认 6 已取消（前端 7 步时间线依据）"),
  ("reject_reason","text","退回原因"),
  ("first_audit_by / first_audit_at","bigint/datetime","初审人 / 初审时间"),
  ("second_audit_by / second_audit_at","bigint/datetime","复审人 / 复审时间"),
  ("submitted_at","datetime","提交时间"),
  ("created_at / updated_at","datetime","创建时间 / 更新时间"),
 ]),
 ("audit_record","审核记录表","939","每次初审/复审的留痕，一次报名可有多条。",[
  ("id","bigint","主键"),
  ("registration_id","bigint","报名记录 ID"),
  ("auditor_id","bigint","审核人 ID"),
  ("audit_level","tinyint","审核级别：1 初审 2 复审"),
  ("audit_result","tinyint","审核结果：1 通过 2 退回"),
  ("reason","text","审核意见/退回原因"),
  ("created_at","datetime","审核时间"),
 ]),
 ("registration_material","报名材料附件表","1","报名时上传的证明文件。",[
  ("id","bigint","主键"),
  ("registration_id","bigint","报名记录 ID"),
  ("file_name","varchar(200)","文件名"),
  ("file_path","varchar(500)","文件存储路径"),
  ("file_size","bigint","文件大小（字节）"),
  ("file_type","varchar(50)","文件类型"),
  ("created_at","datetime","上传时间"),
 ]),
]),
("六、缴费", [
 ("payment_record","支付流水表","325","一条报名对应一笔费用，条件更新防重复支付。",[
  ("id","bigint","主键"),
  ("registration_id","bigint","报名记录 ID"),
  ("amount","decimal(10,2)","支付金额"),
  ("pay_method","varchar(50)","支付方式：wechat 微信 / alipay 支付宝 / bank 银行"),
  ("trade_no","varchar(100)","第三方交易号"),
  ("pay_status","tinyint","状态：0 待支付 1 已支付 2 已退款"),
  ("paid_at","datetime","支付时间"),
  ("created_at","datetime","创建时间"),
 ]),
]),
("七、考场编排", [
 ("arrangement_config","编排参数表","3","按计划配置的座位编排规则。",[
  ("id","bigint","主键"),
  ("plan_id","bigint","计划 ID（唯一）"),
  ("default_seat_count","int","每考场默认座位数（现 30）"),
  ("seat_gap","int","相邻座位间隔（现 1）"),
  ("shuffle_unit","tinyint","同单位是否打散（现 1）"),
  ("updated_at","datetime","更新时间"),
 ]),
 ("arrangement_task","编排任务表","0","编排批处理执行记录（当前无历史任务）。",[
  ("id","bigint","主键"),
  ("plan_id","bigint","计划 ID"),
  ("total_candidates","int","考生总数"),
  ("arranged_count","int","已安排人数"),
  ("room_used","int","使用考场数"),
  ("status","tinyint","状态：0 进行中 1 完成 2 失败"),
  ("started_at / finished_at","datetime","开始时间 / 完成时间"),
  ("operator_id","bigint","操作人 ID"),
 ]),
 ("exam_room","考场表","21","物理考场主数据，考务端座位可视化网格据此渲染。",[
  ("id","bigint","主键"),
  ("room_code","varchar(50)","考场编号（唯一，如 A-101）"),
  ("building","varchar(100)","楼栋"),
  ("classroom","varchar(50)","教室"),
  ("seat_count","int","座位数"),
  ("status","tinyint","状态：0 禁用 1 启用"),
  ("created_at","datetime","创建时间"),
 ]),
 ("room_arrangement","座位编排结果表","326","每个考生最终排到的具体座位。",[
  ("id","bigint","主键"),
  ("registration_id","bigint","报名记录 ID（唯一，一人一条）"),
  ("room_id","bigint","考场 ID"),
  ("seat_no","int","座位号"),
  ("ticket_no","varchar(80)","准考证号（唯一）"),
  ("exam_date","date","考试日期（唯一键 room_id+seat_no+exam_date，支持跨日期复用座位）"),
  ("adjusted","tinyint","是否手动调座"),
  ("adjust_reason","varchar(200)","调座原因（调座时两人自动互换）"),
  ("created_at","datetime","编排时间"),
 ]),
 ("admission_ticket","准考证表","326","编排完成后生成的可打印准考证。",[
  ("id","bigint","主键"),
  ("registration_id","bigint","报名记录 ID（唯一）"),
  ("ticket_no","varchar(80)","准考证号（唯一）"),
  ("generated_at","datetime","生成时间"),
  ("printed_count","int","打印次数"),
 ]),
]),
("八、考试实施", [
 ("exam_signin","签到记录表","326","每个编排考生的到场记录，唯一索引保证签到幂等。",[
  ("id","bigint","主键"),
  ("arrangement_id","bigint","编排记录 ID（唯一）"),
  ("signin_time","datetime","签到时间"),
  ("signin_type","tinyint","签到方式：0 未签到 1 扫码 2 人脸"),
  ("status","tinyint","状态：0 未签到 1 已签到 2 缺考"),
  ("created_at","datetime","创建时间"),
 ]),
 ("invigilator","监考人员表","15","监考员主数据。",[
  ("id","bigint","主键"),
  ("name","varchar(50)","姓名"),
  ("phone","varchar(11)","手机号"),
  ("invigilate_count","int","累计监考场次"),
  ("created_at","datetime","创建时间"),
 ]),
]),
("九、成绩与证书", [
 ("score","成绩表","39","理论/实操/综合成绩，发布后对考生可见。",[
  ("id","bigint","主键"),
  ("registration_id","bigint","报名记录 ID（唯一）"),
  ("theory_score","decimal(5,2)","理论成绩"),
  ("practice_score","decimal(5,2)","实操成绩"),
  ("comprehensive_score","decimal(5,2)","综合成绩"),
  ("theory_weight","decimal(3,2)","理论权重"),
  ("practice_weight","decimal(3,2)","实操权重"),
  ("result","tinyint","结果：1 合格 2 不合格"),
  ("published","tinyint","是否已发布（0 否 1 是）"),
  ("published_at","datetime","发布时间"),
  ("created_by","bigint","录入人 ID"),
  ("created_at / updated_at","datetime","创建时间 / 更新时间"),
 ]),
 ("score_change_log","成绩修改日志表","4","成绩修改留痕，修改原因必填。",[
  ("id","bigint","主键"),
  ("score_id","bigint","成绩记录 ID"),
  ("old_value","varchar(100)","原值"),
  ("new_value","varchar(100)","新值"),
  ("reason","varchar(300)","修改原因"),
  ("operator_id","bigint","操作人 ID"),
  ("created_at","datetime","修改时间"),
 ]),
 ("certificate","证书表","36","成绩合格自动发证，支持公开查验。",[
  ("id","bigint","主键"),
  ("registration_id","bigint","报名记录 ID（唯一）"),
  ("certificate_no","varchar(80)","证书编号（唯一，查验入口输入）"),
  ("trade_name","varchar(100)","工种名称（发证快照）"),
  ("level_name","varchar(50)","等级名称（发证快照）"),
  ("issued_at","date","发证日期"),
  ("issuer","varchar(200)","发证机构"),
  ("status","tinyint","状态：0 待发放 1 已发放"),
  ("created_at","datetime","创建时间"),
 ]),
]),
("十、消息与邮件", [
 ("message_notification","站内消息表","940","考生端“我的消息”数据。",[
  ("id","bigint","主键"),
  ("user_id","bigint","接收人 ID"),
  ("title","varchar(200)","消息标题"),
  ("content","text","消息内容"),
  ("is_read","tinyint","是否已读：0 未读 1 已读（未读角标依据）"),
  ("created_at","datetime","发送时间"),
 ]),
 ("sys_email_record","邮件记录表","128","全部外发邮件台账，验证码功能依赖此表。",[
  ("id","bigint","主键"),
  ("to_email","varchar(100)","收件邮箱"),
  ("subject","varchar(200)","邮件标题"),
  ("content","text","邮件正文"),
  ("send_status","tinyint","状态：0 待发送 1 已发送 2 失败（异步 SMTP 回写）"),
  ("send_time","datetime","发送时间"),
  ("error_msg","varchar(200)","失败原因"),
  ("user_id","bigint","关联用户（通知邮件场景）"),
  ("verify_code","varchar(10)","邮箱验证码"),
  ("expire_at","datetime","验证码过期时间（默认 5 分钟）"),
  ("used","tinyint","验证码是否已使用（原子核销防双花；60 秒频控查本表）"),
  ("created_at","datetime","创建时间"),
 ]),
]),
("十一、公告、客服、AI 与软著", [
 ("announcement","公告表","10","系统通知公告，首页取已发布数据。",[
  ("id","bigint","主键"),
  ("title","varchar(200)","公告标题"),
  ("content","text","公告内容"),
  ("is_top","tinyint","是否置顶"),
  ("status","tinyint","状态：0 下线 1 已发布"),
  ("created_by","bigint","发布人 ID"),
  ("created_at / updated_at","datetime","创建时间 / 更新时间"),
 ]),
 ("after_sales_ticket","售后工单表","26","考生提交的咨询/退款/投诉工单。",[
  ("id","bigint","主键"),
  ("user_id","bigint","提交用户 ID"),
  ("registration_id","bigint","关联报名记录（带归属校验）"),
  ("title","varchar(200)","工单标题"),
  ("content","text","问题描述"),
  ("type","varchar(50)","类型：refund 退款 / consult 咨询 / complaint 投诉 / other 其他"),
  ("status","tinyint","状态：0 待处理 1 处理中 2 已解决 3 已关闭"),
  ("reply","text","回复内容"),
  ("replied_by","bigint","回复人 ID"),
  ("replied_at","datetime","回复时间"),
  ("created_at / updated_at","datetime","创建时间 / 更新时间"),
 ]),
 ("ai_qa_log","AI 问答日志表","75","智能问答留痕（每 IP 每日限 50 次）。",[
  ("id","bigint","主键"),
  ("user_id","bigint","提问用户 ID（可空，允许匿名）"),
  ("question","text","问题"),
  ("answer","text","回答"),
  ("category","varchar(50)","问题分类"),
  ("created_at","datetime","提问时间"),
 ]),
 ("software_copyright","软件著作权申请表","1","软著申报子业务。",[
  ("id","bigint","主键"),
  ("user_id","bigint","申报用户 ID"),
  ("software_name","varchar(200)","软件名称"),
  ("version","varchar(50)","版本号"),
  ("development_complete_date","date","开发完成日期"),
  ("first_publish_date","date","首次发表日期"),
  ("software_type","varchar(50)","软件类型：原创/修改/翻译"),
  ("development_method","varchar(50)","开发方式：独立/合作/委托/下达"),
  ("copyright_owner","varchar(100)","著作权人"),
  ("contact_phone","varchar(20)","联系电话"),
  ("contact_email","varchar(100)","联系邮箱"),
  ("description","text","软件简介"),
  ("status","tinyint","状态：0 待审核 1 审核中 2 已受理 3 已驳回"),
  ("reject_reason","text","驳回原因"),
  ("created_at / updated_at","datetime","创建时间 / 更新时间"),
 ]),
]),
]

# ============ 构建文档 ============
doc = Document()

def ea_style(style, name='微软雅黑'):
    """为样式安全设置中文（东亚）字体。"""
    rpr = style.element.get_or_add_rPr()
    rf = rpr.find(qn('w:rFonts'))
    if rf is None:
        rf = OxmlElement('w:rFonts')
        rpr.append(rf)
    rf.set(qn('w:eastAsia'), name)

# 默认正文样式（中英文字体）
normal = doc.styles['Normal']
normal.font.name = 'Calibri'
normal.font.size = Pt(10.5)
ea_style(normal)

for sname, size, color in [('Heading 1', 16, RGBColor(0x1F,0x3B,0x73)),
                           ('Heading 2', 13, RGBColor(0x1F,0x4E,0x9B)),
                           ('Heading 3', 11.5, RGBColor(0x33,0x33,0x33))]:
    st = doc.styles[sname]
    st.font.name = 'Calibri'
    st.font.size = Pt(size)
    st.font.color.rgb = color
    ea_style(st)

def ea(run, name='微软雅黑'):
    """为 run 安全设置中文（东亚）字体。"""
    rpr = run._element.get_or_add_rPr()
    rf = rpr.find(qn('w:rFonts'))
    if rf is None:
        rf = OxmlElement('w:rFonts')
        rpr.append(rf)
    rf.set(qn('w:eastAsia'), name)
    run.font.name = 'Calibri'

def shade_cell(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear'); shd.set(qn('w:fill'), hex_color)
    tcPr.append(shd)

def set_cell_text(cell, text, bold=False, size=9.5, color=None, align=None):
    cell.text = ''
    p = cell.paragraphs[0]
    if align: p.alignment = align
    r = p.add_run(text)
    r.font.size = Pt(size); r.font.bold = bold
    r.font.name = 'Calibri'
    ea(r)
    if color: r.font.color.rgb = color

# ---- 封面 ----
t = doc.add_paragraph(); t.alignment = WD_ALIGN_PARAGRAPH.CENTER
for _ in range(4): t.add_run('\n')
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('职业技能等级认定考试报名与考场编排系统')
r.font.size = Pt(22); r.font.bold = True; r.font.color.rgb = RGBColor(0x1F,0x3B,0x73)
ea(r)
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('skill_exam 数据库表结构与字段说明')
r.font.size = Pt(18); r.font.bold = True
ea(r)
for _ in range(2): doc.add_paragraph()
for line in ['数据库：MySQL 8.0.26', '连接地址：localhost:3306 / skill_exam',
             '数据表：29 张    字段：272 个', '统计日期：2026-10-08（行数为实测精确值）']:
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(line); r.font.size = Pt(12)
    ea(r)
doc.add_page_break()

# ---- 概览 ----
doc.add_heading('一、数据库概览', level=1)
doc.add_heading('1.1 连接信息', level=2)
info = [('数据库名称','skill_exam'),('数据库类型','MySQL 8.0.26'),('地址','localhost:3306'),
        ('JDBC URL','jdbc:mysql://localhost:3306/skill_exam?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'),
        ('账号 / 密码','root / 123456（支持环境变量 DB_PASSWORD 覆盖）'),('字符集','utf8mb4'),
        ('建库方式','createDatabaseIfNotExist=true（不存在时自动创建）')]
tb = doc.add_table(rows=0, cols=2); tb.style = 'Table Grid'; tb.alignment = WD_TABLE_ALIGNMENT.CENTER
for k,v in info:
    row = tb.add_row().cells
    set_cell_text(row[0], k, bold=True); shade_cell(row[0], 'E8EEF7')
    set_cell_text(row[1], v)
tb.columns[0].width = Cm(3.5); tb.columns[1].width = Cm(13.5)

doc.add_heading('1.2 核心数据主线', level=2)
flow = ('sys_user（用户） → registration（报名，关联 exam_plan + trade + skill_level） → '
        'audit_record（两级审核） → payment_record（缴费） → room_arrangement（座位编排，关联 exam_room） → '
        'admission_ticket（准考证） → exam_signin（签到） → score（成绩） → certificate（证书）')
doc.add_paragraph(flow)

doc.add_heading('1.3 表清单（29 张，按业务域分组）', level=2)
tb = doc.add_table(rows=1, cols=4); tb.style = 'Table Grid'
hdr = tb.rows[0].cells
for i,h in enumerate(['业务域','表名','中文名','行数']):
    set_cell_text(hdr[i], h, bold=True, color=RGBColor(0xFF,0xFF,0xFF), align=WD_ALIGN_PARAGRAPH.CENTER)
    shade_cell(hdr[i], '1F4E9B')
for group, items in TABLES:
    for (name, cn, rows, _purpose, _fields) in items:
        c = tb.add_row().cells
        set_cell_text(c[0], group.split('、',1)[1])
        set_cell_text(c[1], name)
        set_cell_text(c[2], cn)
        set_cell_text(c[3], rows, align=WD_ALIGN_PARAGRAPH.CENTER)

doc.add_page_break()

# ---- 各表详情 ----
sec_no = 2
for group, items in TABLES:
    doc.add_heading(group, level=1)
    for idx, (name, cn, rows, purpose, fields) in enumerate(items, start=1):
        doc.add_heading('%d.%d  %s（%s）' % (sec_no, idx, name, cn), level=2)
        p = doc.add_paragraph()
        r = p.add_run('用途：'); r.font.bold = True
        ea(r)
        r2 = p.add_run(purpose); ea(r2)
        p = doc.add_paragraph()
        r = p.add_run('当前数据量：%s 行    字段数：%d' % (rows, len(fields))); r.font.size = Pt(9.5)
        r.font.color.rgb = RGBColor(0x66,0x66,0x66)
        ea(r)
        tb = doc.add_table(rows=1, cols=3); tb.style = 'Table Grid'
        hdr = tb.rows[0].cells
        for i,h in enumerate(['字段名','类型','含义']):
            set_cell_text(hdr[i], h, bold=True, color=RGBColor(0xFF,0xFF,0xFF), align=WD_ALIGN_PARAGRAPH.CENTER)
            shade_cell(hdr[i], '1F4E9B')
        for fname, ftype, fmean in fields:
            c = tb.add_row().cells
            set_cell_text(c[0], fname, size=9)
            set_cell_text(c[1], ftype, size=9)
            set_cell_text(c[2], fmean, size=9)
        tb.columns[0].width = Cm(4.2); tb.columns[1].width = Cm(3.0); tb.columns[2].width = Cm(9.8)
        doc.add_paragraph()
    sec_no += 1

# ---- 附：状态约定 ----
doc.add_heading('附：通用字段约定', level=1)
for line in [
    'id：各表自增主键（BIGINT）。',
    'created_at / updated_at：记录创建时间 / 最后更新时间（DATETIME）。',
    'status：绝大多数业务表使用 0=停用/待处理、1=启用/正常，具体含义以各表字段说明为准。',
    '所有金额字段为 DECIMAL，时间为 DATETIME/DATE，布尔语义字段为 TINYINT(0/1)。',
    '表间已建 11 个外键约束：签到→编排为级联删除（CASCADE），其余为限制删除（RESTRICT）。',
]:
    doc.add_paragraph(line, style='List Bullet')

doc.save(OUT)
print('SAVED:', OUT)
print('paragraphs:', len(doc.paragraphs), 'tables:', len(doc.tables))
