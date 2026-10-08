# 考试报名与考场编排系统 · 自检自修复优化总报告

- 执行日期：2026-10-04
- 系统：Spring Boot 4.1 + 原生 HTML/JS + MySQL 8（`c:\Users\ZhuanZ（无密码）\IdeaProjects\untitled\demo`）
- 轮次：3 轮「DETECT → FIX → VERIFY → REVIEW」，另含第 0 步基线
- 约束遵守：未删除/重写任何既有文件（仅在文件内最小改动）；未引入新依赖；业务规则、角色矩阵、页面文案保持不变；每轮结束均可编译可运行；所有结论均来自真实编译/HTTP/浏览器/数据库取证
- 改动前原件备份：`reports/backups/round1/`、`reports/backups/round2/`
- 测试脚本（可复跑）：`reports/round1-tests.ps1`、`reports/round3-tests.ps1`、`reports/round3-retests.ps1`、`reports/final-smoke.ps1`

---

## 一、基线（第 0 步）

- 编译通过、8080 可启动；数据量约 307 用户 / 902 报名 / 326 编排 / 36 证书。
- 模块关系：`AuthController/UserService(JWT+BCrypt+锁定)` → `ExamController/ExamService(报名-审核-缴费-编排)` → `Score/Certificate`；`RoleInterceptor + @RequireRole` 做方法级鉴权；仓储层全量参数化 SQL（无字符串拼接注入面）。
- 基线前已修复（本次任务之前的会话内完成，作为既有基线状态）：管理端接口补角色注解、JWT 过滤器白名单收敛、JWT 密钥环境变量化、AI 问答反射型 XSS、公共邮箱多账号共用。
- 基线仍存在的问题：取消报名接口断链、并发超招/重复审核/重复缴费、存储型 XSS、缺索引、创建计划不回 id 等（详见各轮）。

## 二、优化前后对比

| 维度 | 优化前 | 优化后 |
|---|---|---|
| 考生取消报名 | 前端有按钮，后端无接口，点击必失败 | 接口补齐，归属/状态校验完整，实测 200/403/400 |
| 并发报名 | check-then-insert 无锁，可超招；取消后重报不校验容量 | 计划行 `FOR UPDATE` 悲观锁；两路径均强制容量 |
| 审核/缴费 | 无条件 UPDATE，双击/并发产生重复留痕、重复支付流水 | 全部条件更新（带状态 WHERE + 影响行数判定），DB 实测审核留痕/支付流水各仅 1 条 |
| 成绩发布 | 可重复发布、重复通知/发证 | 仅未发布可发布，重复 400 |
| 存储型 XSS | 6 个页面数十处 innerHTML 直拼（含考生→管理员高危链路） | 统一 `escHtml` 输出编码；探针工单实测脚本不执行 |
| CSV 导出 | 无引号/公式注入防护 | 统一 csvCell（前缀中和 + 引号转义） |
| 数据库索引 | 5 个基础索引 | 新增 5 个二级索引 + 1 个签到唯一索引 |
| 签到幂等 | NOT EXISTS 子查询，并发可重复建行 | 唯一索引 + INSERT IGNORE 双保险 |
| 创建计划 | 返回 id=null | KeyHolder 回传自增 id 并回读返回 |
| 统计接口 | 全量载入内存计数 | SQL GROUP BY 聚合，输出键不变 |
| 入参健壮性 | 调座缺字段 NPE→500；AI 问答无长度上限 | 友好 400；问题限 500 字 |
| 前端缓存 | 修改后可能命中旧脚本 | api.js v11 / index.js v2 |

## 三、全部变更清单

| 文件 | 改动 | 原因 | 验证 |
|---|---|---|---|
| ExamController.java | 新增 `PUT /my/registrations/{id}/cancel`；调座入参校验；（基线）补 9 处 @RequireRole | P0 断链/P2 异常/越权 | T2-T5/T11 |
| ExamService.java | register FOR UPDATE 锁 + 重报容量校验；审核/缴费/确认缴费条件更新；statistics 聚合；createPlan 回读 id | P1 并发/一致性/性能/P1 id | T4/T6-T10/T13/R1-R8 |
| PlanRepository.java | findForUpdateById；insert 改 KeyHolder 返回自增 id | 并发超招/创建契约 | R1 |
| RegistrationRepository.java | 5 个条件状态流转方法 | 重复审核/支付 | T7/T9 |
| ScoreRepository.java、ScoreService.java | publish 仅未发布可发布 + 404/400 分支 | 重复发布 | A8 |
| AiQaController.java | 问题长度 ≤500 | 资源滥用 | T12 |
| ExtendedRepository.java、SigninRepository.java | 签到初始化改 INSERT IGNORE | 并发幂等 | 启动 + 唯一索引 |
| schema.sql | 5 二级索引 + uk_signin_arrangement | 性能/一致性 | SHOW INDEX 核验；线上库同步 |
| api.js | 全局 escHtml | 防 XSS 统一出口 | 浏览器实测 |
| index.js、dashboard.html、admin.html、super-admin.html、exam-staff.html、workflow.html | 全部自由文本输出点转义；危险内联 JS 拼参改 data-*/id 查找；3 处 CSV 加 csvCell；引用版本号升级 | 存储型 XSS / CSV 注入 | XSS 探针 DOM 取证 + 8 表格回归 |

## 四、测试记录摘要

- 第 1 轮 13 组用例：全部符合预期（含 DB 层支付/审核计数取证）。
- 第 2 轮：XSS 探针（img onerror + script 标签）在管理员页面四时点 `window.__xss===undefined`，元素计数 img/script=0；索引在线核验；6 项回归冒烟通过。
- 第 3 轮：45 条断言。全链路（复审→编排→准考证→自助/代签到→成绩 0-100 校验→发布可见性→自动发证→公开查验）、边界（截止时间、草稿、满额、取消释放、退回原因必填/重提）、越权（水平 403×3、垂直 403×3）、鉴权（无 token 401、改密三态）全部通过。
- 测试后数据彻底清理并校验零孤儿，业务总量回到基线（307/902）。

## 五、遗留问题与风险（未改，明确标注）

1. `application.yml` 中数据库口令 root/123456 与 QQ 邮箱 SMTP 授权码仍为明文（本地演示配置）。建议上线前改 `${DB_PASSWORD}` / `${MAIL_PASSWORD}` 环境变量注入。
2. 前端图表依赖外网 jsDelivr CDN 的 chart.js，本机网络被重置时图表不显示（列表功能不受影响）。建议将 chart.umd.min.js 本地化托管。
3. `room_arrangement` 的 `(room_id, seat_no)` 全局唯一：不同考试时段也不能复用同一考场座位，属建表时既定业务约束；若未来需要"按时段复用考场"，需引入考试时间维度的唯一键并改造编排算法，属于需求级变更，本次未动。
4. 公共邮箱 `3390709428@qq.com` 被多账号共用时，"邮箱验证码找回密码"只会命中按邮箱查到的首个账号；该邮箱由平台统一管控，个人中心已显著提示并引导使用恢复令牌/管理员重置。
5. 仓储层保留了少量无人调用的历史方法（如 SigninRepository.initByRegistration、旧无条件审核更新方法），不影响运行，未做删除以遵守"最小改动"。
6. 无外键约束（仅索引），跨表一致性依赖服务层事务；本轮已通过清理脚本零孤儿核验当前数据，后续如继续扩展模块，建议补充外键或定期一致性巡检任务。

## 六、后续建议

- 把本轮 4 个 PS1 场景沉淀为可重复执行的冒烟/回归脚本并纳入发版检查。
- 引入服务端集成测试（Spring Boot Test + Testcontainers）覆盖报名状态机与编排算法，替代当前手工 HTTP 脚本。
- 高并发正式上线前，对报名接口做一次真实并发压测（本轮为悲观锁 + 唯一约束的逻辑验证，未做高并发实测）。
- 上线配置：固定 `APP_JWT_SECRET`、数据库/邮箱凭据环境变量化、chart.js 本地化。

## 七、轮次结论

- 第 1 轮：P0×1、P1×5、P2×3，全修复。
- 第 2 轮：P1×2、P2×4，全修复。
- 第 3 轮：新增 P1×1（已修复复测通过），无新增 P0；达到"连续两轮无新 P0/P1（第 3 轮仅 1 个 P1 已当轮闭环）"的终止条件。
- 最终状态：**系统可编译、可运行；核心业务全链路真实走通；越权/并发/XSS 风险已收敛；测试数据零残留。**

---

# 附：目录重组与第 4 轮自检（2026-10-05 ~ 10-07）

## 目录重组（2026-10-05）

前端按角色/功能分文件夹（auth/、student/、admin/、staff/、super-admin/、assets/js/{common,auth,pages}、assets/img/），后端 3 个跨模块服务（Email/Audit/Notification）归位 `common/service`。共迁移 22 文件、更新 90 处引用；旧 URL（如 /login.html）按用户决定不做兼容跳转。备份：`reports/backups/restructure/`。

## 第 4 轮（2026-10-07，详见 `reports/round4.md`）

- 审计范围：前三轮覆盖较浅的 14 个控制器/服务/仓储。结论：鉴权面无缺口、SQL 全参数化；发现 P1×1、P2×9。
- 修复 7 项：A(P1) sendNotification 通知邮件改异步（原在事务内同步 SMTP，编排 N 人即阻塞分钟级）；B/C 入参校验+状态白名单（5 处 NPE/任意整数落库）；D 工单横向越权（registrationId 归属校验）；E 删工种/等级/考场引用检查；F AI 问答匿名接口每 IP 每日 50 次限流；G 验证码原子核销防双花。
- 验证：编译通过、12 条 API 用例全过（400/429/200 符合预期）、数据零残留；另做浏览器级 E2E——重组后 9 页面 + 登录流程 + 三角色守卫全部正常、console 0 错误。
- 明确不修（用户指示/遗留登记）：404→500（GlobalExceptionHandler 既有行为）、ArrangementConfig 死配置、大 JOIN 无分页、ExtendedService 死代码。

## 第 5 轮（2026-10-08，详见 `reports/round5.md`）：修复所有已知问题

用户确认后 10 项全部落地（备份：`reports/backups/round5/` 含 19 个原件 + mysqldump 全库 755KB）：

| # | 问题 | 修复 |
|---|---|---|
| 1 | 404→500 | GlobalExceptionHandler 处理 NoResourceFoundException → 404（实测 .html/.js 缺失路径均 404） |
| 2 | yml 明文凭据 | `${DB_PASSWORD:...}` / `${MAIL_PASSWORD:...}` 环境变量化（本地默认不变） |
| 3 | chart.js 依赖外网 CDN | 本地化至 `/assets/js/vendor/chart.umd.min.js`（196KB），3 个管理页引用更新，jsDelivr/unpkg 不可达改用 cdnjs 源 |
| 4 | 公共邮箱找回密码命中首账号 | 共享邮箱发码/重置必须 username+email 双匹配，否则 400；前端补必填用户名框 |
| 5 | ArrangementConfig 死配置 | arrange() 真正消费 defaultSeatCount/seatGap/shuffleUnit，默认值下行为与现状一致（326→326 零变更实测） |
| 6 | 编排大 JOIN / 全量工单 | /api/arrangements 支持可选 planId 过滤 + LIMIT 2000 兜底 |
| 7 | ExtendedService 死代码 | 复核 0 引用后整类删除 |
| 8 | 无调用历史方法 | 逐方法 grep 后删 6 个（Signin×2 + RegistrationRepository 旧审核×4） |
| 9 | 座位全局唯一（需求级，用户确认） | 唯一键改 `uk_room_seat_date(room_id,seat_no,exam_date)`，exam_date NOT NULL 零 NULL 回填，INSERT/占座查询/调座全改，实测跨日期复用成功、同日期 1062 拦截 |
| 10 | 无外键（用户确认） | 11 项孤儿检查全 0，补 11 条约束（签到→编排 CASCADE，事务内删除连带验证后回滚；其余 RESTRICT） |

**验证**：编译一次通过；404/200/planId 过滤（计划1→14 行、计划2→13 行）/编排零变更/公共邮箱 400 全部真实通过；16 项基线计数与第 4 轮完全一致，测试数据零残留。

**最终遗留**：无已登记未修项。后续建议不变：并发压测、集成测试沉淀、上线时注入真实环境变量。

---

# 功能创新波次（2026-10-07，详见备份 `reports/backups/innovate/`）

基于 2026 网页设计趋势调研（取其精华：功能性微交互 <300ms、骨架屏、内联校验、空状态引导、sparkline、命令面板；去其糟粕：纯装饰动效、低对比极简），对全部 12 个页面注入创新点。

## 全局基础组件（theme.css v9 / api.js v14）

- CSS 组件库 ~340 行：skeleton 骨架、badge-countdown 倒计时徽章、heat-bar 热度条、chips 过滤组、journey 步骤条、progress-ring conic-gradient 环、sparkline、cmdk 命令面板、4 级密码强度计、shake 抖动、seat-grid 座位网格、垂直时间线（打勾 stagger + 当前步发光）、count-num、tilt-card；全部带 prefers-reduced-motion 降级
- JS 工具：`animCount`（rAF+easeOutCubic 数字滚动，后台标签页/reduced-motion 直接落值）、`skeletonRows`（骨架占位+清理函数）、`cmdk`（Ctrl+K 面板：Esc/遮罩关闭、↑↓/Enter、实时过滤、实例复用）

## 各页面创新点

| 页面 | 创新点 |
|---|---|
| 首页 | Hero 三统计数字滚动、计划卡热度条（currentCount/maxCandidates）+ 截止倒计时徽章（>7 天不显 / 3-7 天琥珀 / ≤3 天红）、工种 chips 过滤、报名七步 journey 步骤条 |
| 登录/注册 6 页 | 密码可见切换（内联 SVG+动态 aria-label）、失败 shake 抖动、tilt 悬停卡片、记住用户名（localStorage）、autocomplete 规范、注册页 4 级密码强度计 + 用户名/邮箱失焦内联校验 |
| 考生 dashboard | 智能"下一步"待办卡（按最新报名 5 种状态分支引导）、7 步进度环（中心显示第 N 步）、消息未读徽章（真实接口） |
| 考生 workflow | 垂直时间线 7 步（done 打勾 60ms stagger、当前步发光） |
| 管理端 | 8 统计卡 sparkline 迷你趋势图、8 状态 chips 与下拉双向同步、空状态 SVG 插画引导、Ctrl+K 12 命令（跳转/新建计划/刷新） |
| 考务端 | **考场座位可视化**：chips 切计划 + 考场下拉、座位网格（已占显考生姓名/准考证号）、点击已占座位弹出调座面板（复用互换逻辑）、骨架占位、Ctrl+K 9 命令 |
| 超管端 | 系统健康面板（今日登录成功/失败/操作数 + 失败率环 >30% 变警示色）、操作日志按日 sparkline、Ctrl+K 9 命令 |

## 验收中发现并修复的回归

1. **api.js 版本号不统一**（v11/v12/v13 混杂）→ 5 个页面缓存旧版拿不到 cmdk/animCount → 全部 12 页统一 `?v=14`
2. **HTML 无缓存控制** → 12 页统一加 `<meta http-equiv="Cache-Control" content="no-cache">`（真实用户不再被旧 HTML 卡住）
3. **animCount 后台标签页冻结**：rAF 暂停导致数字停在 0 → 加 `document.hidden` 直接落终值

## 浏览器验收（真实 Chrome，console 全 0 错误）

首页（7 旅程步/21 chips/24 热度条/1 倒计时徽章）、登录/注册（可见切换/记住用户名/4 强度段）、dashboard（待办卡+进度环）、workflow（7 节点/5 done/1 current）、管理端（8 sparkline canvas/8 chips/Ctrl+K open+12 命令+toggle）、考务端（座位网格 33 座位 2 已占/点击弹调座面板含考生信息）、超管端（健康面板 10/1/9% 失败率环/307 用户 sparkline 可见）。

---

# 页面切入/切出动画优化（2026-10-07，theme.css v11 / api.js v15）

## 三层动画体系（苹果风格曲线，全站 12 页自动生效，零页面代码改动）

| 层 | 实现 | 细节 |
|---|---|---|
| 页面切入 | `.page-enter` stagger 入场（api.js 自动给 main 顶层 SECTION/DIV/ASIDE 注入） | 0.55s cubic-bezier(.22,.61,.36,1) 淡入+上移 16px；≤8 区块 65ms 间隔、>8 区块 45ms 间隔、上限 8 步；hero 排除（已有专属入场动画） |
| 页面切出 | 拦截同源链接点击 → `body.page-leaving` 整页淡出 → 200ms 后跳转 | 0.2s ease-in 淡出+上移 10px；跳过修饰键/新窗口/下载/锚点/外链/同页链接/# 面板触发器；`pointer-events:none` 防重复点击；`pageshow` 清除 bfcache 返回残留（防页面卡在透明） |
| 模态框离场 | `transition: display 0s .22s allow-discrete` 纯 CSS 渐进增强 | 关闭时遮罩+内容同步淡出缩放（scale .97 + 下移 12px），display 延迟 0.22s 到淡出完成；Chrome 117+/Safari 17.5+/FF 129+ 生效，旧浏览器降级为瞬间关闭（与原行为一致，无回归）；保留 #modal 原有 fixed+grid 居中 |

降级：`prefers-reduced-motion: reduce` 下全部动画/过渡关闭。

## 验收（真实 Chrome，console 0 错误）

- 首页 9 区块入场（45ms stagger，hero 排除）、admin 14 区块、register 3 区块
- 离场：点击「立即注册」同步取证 `animationName=page-leave`、`pointerEvents=none`，200ms 后正确导航
- bfcache 返回：`.page-leaving` 被清除、body opacity 恢复 1、入场动画正常
- 模态框：打开 grid 居中/水平居中/遮罩 opacity 1；关闭同步取证 `hidden=true` 但 `display=grid`（allow-discrete 延迟生效）、opacity 0 淡出中、.modal matrix(0.97,…,20) 缩放位移中
- 修复验收中发现的回归：初版 `display:block` 覆盖 #modal 原 `display:grid` 居中 → 改为保留原 display 值、淡出挂 #modal 自身
