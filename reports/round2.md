# 第 2 轮自检-修复-验证报告（2026-10-04）

## DETECT 问题清单

| # | 级别 | 位置 | 问题 | 影响 |
|---|---|---|---|---|
| 2-1 | P1 | index.js / dashboard/admin/super-admin/exam-staff/workflow 共 6 个页面 | 大量 innerHTML 直接拼接服务端数据：考生真实姓名/身份证/工作单位、售后工单（考生可写，管理员可见）、公告、计划名称/地点/条件、审核意见、证书字段等 | 存储型 XSS。其中「考生写工单→管理员打开」可在管理员上下文执行脚本，属于高危链路 |
| 2-2 | P1 | dashboard.html 证书按钮 `onclick='printCertificate(${JSON.stringify(c)})'`、审核按钮 `showTimeline(id, '${planName}')` | 数据被直接拼进 HTML 属性内的 JS 字符串 | 含单引号/反斜杠的字段可断开属性注入脚本 |
| 2-3 | P2 | admin/exam-staff/super-admin 三处 CSV 导出 | 字段未做 CSV 公式注入防护、未统一引号转义 | 以 = + - @ 开头内容在 Excel 中可被当公式执行 |
| 2-4 | P2 | registration_material / audit_record / payment_record / after_sales_ticket / sys_email_record | 高频外键查询列缺索引（验证码频控按 to_email+时间、材料/审核/支付按 registration_id、工单按 user_id） | 数据量增大后全表扫描 |
| 2-5 | P2 | exam_signin.arrangement_id 无唯一约束；initSignin 系列仅靠 NOT EXISTS 子查询 | 并发编排/补建可插入重复签到行 | 一人多签到记录，签到统计失真 |
| 2-6 | P2 | 静态资源缓存 | api.js 已修改但引用版本仍 v=10/index.js v=1 | 浏览器可能使用旧脚本，转义不生效 |

## FIX 变更

- `api.js`：新增全局 `window.escHtml`（& < > " ' 五字符转义），全站 12 个页面共用；`index.js` 删除自有同名实现，统一调用。
- 6 个页面所有自由文本渲染点改用 `escHtml`：
  - index.js：公告、工种、计划卡/详情、AI 问答（既有反射点）、证书公开查验结果/错误提示；
  - dashboard.html：报名/成绩/消息/材料/签到/工单/个人资料/编辑表单 value/推荐计划/报名弹窗/审核时间线/准考证与证书打印页；
  - admin.html：计划表格与编辑表单、报名列表与详情（含紧急联系人等全部 PII）、审核留痕、成绩、证书、缴费流水、公告、工单列表与详情；
  - exam-staff.html：考场、编排、成绩、签到、监考、计划下拉、考场/调座表单、成绩留痕；
  - super-admin.html：用户、工种、等级、公告、配置（含修复 onclick 内联拼参→data-* 属性传参）、操作日志、登录日志、工种/等级/配置弹窗；
  - workflow.html：报名进度卡与审核意见。
- 3 处 CSV 导出统一增加 `csvCell()`：危险前缀加单引号、双引号包裹、内部引号双写、CRLF 行分隔。
- `schema.sql` 新增 5 个普通索引 + `uk_signin_arrangement` 唯一索引；线上库同步创建（创建前核验 326 条签到无重复）。
- `ExtendedRepository` 两处 / `SigninRepository` 一处签到初始化 SQL 改 `INSERT IGNORE`，与唯一索引配合保证并发幂等。
- 12 个 HTML 的 api.js 引用升 v=11，index.html 的 index.js 升 v=2。
- 备份：`reports/backups/round2/`（9 个文件原件）。

## VERIFY

- Java 编译通过；服务重启 8080 正常；schema.sql 在启动时 `continue-on-error` 模式下执行无破坏性影响。
- 索引：`SHOW INDEX` 确认 uk_signin_arrangement 为 NON_unique=0；5 个新索引在 information_schema 中均存在。
- 存储型 XSS 实测（真实数据 + 浏览器 DOM 取证）：
  - 考生 uifr1a 提交标题 `<img src=x onerror="window.__xss=1">XSS-TITLE`、内容含 `<script>` 的工单；
  - 管理员端列表与详情：尖括号全部渲染为字面文本（DOM 中为 `&lt;img...&gt;`），img/script 元素计数为 0；
  - 四个时点 `window.__xss === undefined`，无脚本执行；无应用级 console 错误；8 个表格正常、无 HTML 双编码乱码；
  - 验证后探针工单已删除。
- 回归冒烟（新代码重启后）：重复缴费 400、统计 200、编排列表 200、调座空 body 400、AI 超长 400、未登录取消 401、本人报名列表 200，全部符合预期。

## REVIEW

- 发现 6 个（P1×2，P2×4），修复 6 个，放弃 0。
- 转义采用「输出编码」而非 CSP，未改变任何页面结构与文案；中文显示经浏览器实测无双重转义。
- 遗留：chart.js 依赖外网 jsDelivr CDN（本机网络被重置，仅影响图表），属第三方资源托管问题，列入第 3 轮观察项，不在本轮改动范围。
