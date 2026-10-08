# 第 5 轮自检-修复-验证报告（2026-10-07）

本轮聚焦 10 项已审计确认问题：2 项体验/异常处理、1 项前端资源本地化、1 项安全流程、2 项性能收敛、3 项死代码清理、2 项 schema 迁移。改动前 19 个原件已备份至 `reports/backups/round5/`，数据库全量备份 `reports/backups/round5/db-backup.sql`（mysqldump --routines，29 表，755KB）。

## FIX（1-10 逐项）

- **1（404→500）** `GlobalExceptionHandler` 新增 `NoResourceFoundException` 处理，返回 404 `{"code":404,"message":"请求的资源不存在"}`（参照既有 ResponseStatusException 写法），静态资源/未知 html/js/css 路径不再落入兜底 500。
- **2（yml 凭据环境变量化）** datasource.password 改 `${DB_PASSWORD:123456}`，mail.password 改 `${MAIL_PASSWORD:mfhggtykvdyhcjcg}`，本地默认行为不变（服务以默认值启动成功）。
- **3（chart.js 本地化）** jsDelivr TLS 握手失败、unpkg 该路径 404，最终从 cdnjs 下载 `chart.umd.min.js`（4.4.1 UMD，200,807 字节，内含 version:"4.4.1"）到 `static/assets/js/vendor/`；admin/staff/super-admin 三页 CDN `<script>` 全部替换为 `/assets/js/vendor/chart.umd.min.js`。
- **4（公共邮箱找回密码）** `UserService.sendResetPasswordCode/resetPasswordByEmail`：`SystemEmails.isShared(email)` 时必须提供 username 且 `findByEmailAndUsername`（邮箱+用户名精确匹配）命中才发码/重置，否则 400"该邮箱为系统公共邮箱，需同时提供用户名"/"用户名与邮箱不匹配"；reset 在核销验证码之前校验（不烧码）。普通邮箱流程逐字不变。`AuthController` 拆出 `ResetCodeRequest`、`ResetPasswordRequest` 加 username；前端 `forgot-password.html` 增必填用户名输入框（占位注明公共邮箱必须填写），api.js/auth.js 透传 username（引用 bump v=12）。
- **5（ArrangementConfig 接入编排）** `ExamService.arrange()` 注入 `ArrangementConfigRepository`，按计划读取配置（无配置默认 30/1/1）：shuffleUnit=1 保持现状（同单位打散+洗牌+打散）、=0 不打散（按报名顺序）；seatGap 用于同考场相邻考生座位号间隔（`nextFreeSeat` 指针步进 `max(1,seatGap)`，默认 1=紧邻同现状）；defaultSeatCount 作为 `seat_count<=0` 考场容量上限（现网 21 考场均有座位数，不触发，行为不变）。考务页配置区块保存/加载逻辑未动，接口联通验证 200。
- **6（大 JOIN 与全量工单收敛）** `ArrangementRepository.findAll(planId)`：可选 `WHERE r.plan_id=?`，尾部 `ORDER BY rm.room_code, ra.seat_no LIMIT 2000`；`ExamController` GET /api/arrangements 增可选 planId 透传；`SystemRepository.afterSalesTickets` 全量分支加 `LIMIT 2000`。前端不传参即旧行为。
- **7（ExtendedService 删除）** 删前 grep 复核：无任何调用方、无路由注册（@Service 非 Controller），文件已删除。
- **8（无调用方法清理）** 逐一 grep 复核后删除：`SigninRepository.initByRegistration`、`SigninRepository.deleteOrphanSignins`（ExamService 实际调用的是 ExtendedRepository 同名方法）、`RegistrationRepository.updateStatus/updateFirstAudit/updateSecondAudit/reject`（旧的无条件审核更新，已被条件更新 IfPending/IfPaid 替代）。`ArrangementRepository.findAllPlacements` 无调用方，改造为 `findPlacementsByDate` 服务修复 9。
- **9（座位唯一键按时段化）** 线上迁移：ADD `exam_date DATE` → UPDATE 从 `exam_plan.exam_time`（实际日期列，DATETIME）取 `DATE()` 回填 → 0 行 NULL → `MODIFY NOT NULL` → DROP `uk_room_seat` → CREATE UNIQUE `uk_room_seat_date(room_id,seat_no,exam_date)`。代码：INSERT 编排写入 exam_date（取计划考试日期）；"已占用座位"由全量大 JOIN 改为 `findPlacementsByDate`（按日期过滤，否则时段化形同虚设）；调座 `findByRoomSeat` 加日期判冲突；`SeatPlacement`/Mapper 同步。schema.sql 同步定义。重置编排 SQL 无显式列名插入，无需改动。
- **10（补外键）** 11 项孤儿检查全部为 0（reg→user/plan、arr→reg/room、signin→arr、score/cert/material/payment/ticket→reg、ticket→user），11 条约束全部创建：`fk_signin_arr` ON DELETE CASCADE（签到为编排纯子数据），其余默认 RESTRICT。schema.sql 各 CREATE TABLE 同步 CONSTRAINT 子句（IF NOT EXISTS 模式下老库靠线上 ALTER 已完成）。

## VERIFY（真实 HTTP + DB）

| 用例 | 预期 | 实际 |
|---|---|---|
| mvnw -q compile；kill 2180 重启 | 0 通过、服务就绪 | ✅ EXIT=0 一次通过；服务 2s 就绪 |
| /assets/js/no-such-file.js、/no-such-page.html、/css/no-such.css | 404（不再 500） | ✅ 404×3，`{"code":404,...}`（旧代码 500） |
| /no-such-page-xyz | （见偏离 1） | 401：JwtAuthFilter 拦截，修复前后一致 |
| GET /、/api/public/plans | 200 | ✅ 200×2 |
| GET /assets/js/vendor/chart.umd.min.js；三页引用 | 200；已更新 | ✅ 200（200,807B v4.4.1）；三页均 `/assets/js/vendor/chart.umd.min.js` |
| SHOW CREATE TABLE room_arrangement；exam_date NULL 数 | 含 uk_room_seat_date；0 | ✅ `UNIQUE KEY uk_room_seat_date (room_id,seat_no,exam_date)`；0 NULL |
| 跨日期复用/同日期拦截（事务内） | 不同日期可同座位；同日期 1062 | ✅ 2030-01-01 与 2026-10-25 同座位共存；`ERROR 1062 ... uk_room_seat_date`；ROLLBACK 零残留 |
| information_schema 外键计数 | 11 | ✅ 11 |
| CASCADE 链路（事务内，任务预案） | 删编排连带删签到且不违约 | ✅ DELETE id=4 → 326/326→325/325 → ROLLBACK 恢复 326/326 |
| staff 登录；GET /api/arrangements | 200；旧行为 | ✅ 200 role=2；326 行 |
| GET /api/arrangements?planId=1 / ?planId=2 | 200 且只含该计划 | ✅ 14 行 / 13 行，均单一 plan_name |
| POST /api/arrangements（增量） | 200 无新增，总数一致 | ✅ "没有待编排的考生"，326→326（配置接入后默认行为不变） |
| GET /api/arrangements/config/1 | 200 联通 | ✅ 200，30/1/1（库内 3 条配置均为默认值） |
| 公共邮箱发码：无 username / 错误 username | 400 | ✅ 400×2（"需同时提供用户名"/"用户名与邮箱不匹配"） |
| 公共邮箱发码：正确 username（kkluv） | 200 | ✅ 200 sent=true（邮件记录已清理） |
| reset 不带 username | 400（不烧验证码） | ✅ 400，验证码未被核销 |
| 普通未注册邮箱发码 | 404 原行为 | ✅ 404"该邮箱未注册" |
| grep ExtendedService（src） | 0 引用 | ✅ 0（仅历史报告文本与备份原件） |
| 基线复核 | 与 round4 一致 | ✅ 用户307/邮件128/消息940/考场21/工种20/等级62/报名902/编排326/签到326/准考证326/流水325/工单26/操作日志203/登录日志655/配置3/计划24 |

**清理确认**：测试邮件记录（id=221）、staff 登录日志（id=1214）已删除，复核计数回到基线；事务测试全部 ROLLBACK，`TEST-R5-%` 零残留；编排调用因待编排池为空未产生任何数据变更；staff-token.txt 及临时文件已删。

## 偏离说明

1. 404 验证路径：任务示例 `/no-such-page-xyz` 实际被 `JwtAuthFilter` 以 401 拦截（未命中公开白名单且非静态后缀，属鉴权层行为，修复前后一致）。故改用能真正到达资源处理器的 `/assets/**`、`*.html`、`*.css` 路径验证 404 修复。
2. chart.js：jsDelivr（TLS 连接被重置）与 unpkg（该文件 404）均不可达，改用 cdnjs（cloudflare）下载同版本 4.4.1 UMD 构建成功，已达成"本地化"目标。
3. "重置编排"链路：当前库有 326 条真实编排，按任务预案未真实执行重置接口，改为事务内 `BEGIN; DELETE 一条 room_arrangement; ROLLBACK;` 验证 CASCADE 连带删除与约束无损。
4. shuffleUnit=0 语义按"不打散"字面实现：完全跳过同单位打散与随机洗牌，按报名先后顺序编排；=1 与现状逐位一致（库内现有 3 条配置均为默认值，现网行为零变化）。
5. 正确 username 发码产生 1 封真实邮件（投递至系统公共邮箱自身）与 1 条邮件记录，记录测后已删，邮件本体无法撤回（系统邮箱收件，无业务影响）。

## REVIEW

10 项全部修复并真实验证通过；schema 迁移（座位时段化 + 11 条外键）线上执行成功且与 schema.sql 一致；默认行为零回归（编排默认配置、无参查询、普通邮箱流程均与现状一致）；未新增 P0/P1。

---

## 总结（第 5 轮）

10 项修复全部完成：①静态资源缺失由 500 改为 404；②数据库与邮箱密码环境变量化并保留本地默认；③chart.js 4.4.1 本地化（jsDelivr/unpkg 不可达改用 cdnjs），三个管理页引用切换完毕；④公共邮箱找回密码强制"用户名+邮箱"双匹配，前端补用户名必填框，普通邮箱流程不变；⑤编排配置（座位数/间隔/打散）真正接入 arrange()，默认值 30/1/1 与现状逐位一致，实测编排无新增、无回归；⑥编排大 JOIN 支持 planId 过滤并加 LIMIT 2000，工单全量查询同步收敛；⑦ExtendedService 整类删除；⑧复核删除 6 个无调用仓储方法（签到初始化/孤儿清理各 1、报名旧无条件审核更新 4）。schema 迁移两项：座位唯一键由 (room_id,seat_no) 全局化改为 (room_id,seat_no,exam_date) 时段化——迁移前全局唯一索引 uk_room_seat、无 exam_date、零外键；迁移后新索引 uk_room_seat_date 生效、exam_date NOT NULL 且 0 NULL，并实测跨日期同座位可复用、同日期被 1062 拦截；11 对关系孤儿检查全 0，补 11 条外键（签到→编排 CASCADE，事务内删编排连带删签到后回滚验证；其余 RESTRICT），schema.sql 同步。全部验证真实执行：编译一次通过、服务重启就绪、404/200/编排/公共邮箱各用例符合预期，测试数据（邮件、登录日志）已清理，16 项基线计数与 round4 完全一致。
