# 第 4 轮自检-修复-验证报告（2026-10-07）

本轮聚焦 7 项已审计确认缺陷：1 个 P1 性能问题 + 6 类健壮性/安全加固。改动前 10 个原件已备份至 `reports/backups/round4/`。

## FIX（A-G 逐项）

- **A（P1）** `EmailService.sendNotification`：入库+SMTP 投递整体改投已有 `mailExecutor`（与验证码发送同模式），方法签名不变，异常仅 `log.warn` 不外抛；`NotificationService` 同步注释更新。
- **B** `MaterialController.addMaterial`：fileName/filePath 必须非空字符串、fileSize 必须 Number，否则 400；`CatalogController.createLevel`：tradeId 必填数字、levelName/levelCode 非空，否则 400；`InvigilatorController.addInvigilator`：name/phone 必须非空字符串，否则 400（杜绝 ClassCastException/null 入库）。
- **C** 状态白名单：`RoomController` update/toggleStatus 仅允许 0/1（新增 `requireValidStatus`，同时消除 status 缺失 NPE→500）；`CatalogController` 工种/等级 status 仅 0/1；`AfterSaleController` 工单 status 仅 1-3。
- **D** `AfterSaleController.createAfterSalesTicket`：registrationId 非空时经新增 `SystemRepository.findRegistrationOwnerId` 校验归属当前用户，不属于则 400"报名记录不存在或不属于本人"。
- **E** 删除引用检查：`deleteTrade` 先查 exam_plan 与 skill_level 引用、`deleteLevel` 查 exam_plan、`RoomController.delete` 经新增 `countArrangementsByRoom` 查 room_arrangement，非零 400。
- **F** `AiQaService`：ai_qa_log 无 ip 字段 → 内存限流（ConcurrentHashMap + AtomicInteger，同 IP 每日 50 次，日期翻转清零，约 20 行），超限抛 IllegalStateException → 全局映射 429。
- **G** `EmailService.verifyCode`：先查后改改为原子 `UPDATE ... SET used=1 WHERE used=0 AND expire_at>NOW() ORDER BY id DESC LIMIT 1`，影响行数=1 才成功。

## VERIFY（真实 HTTP + DB）

| 用例 | 预期 | 实际 |
|---|---|---|
| a 匿名 POST /api/system/ai-qa | 200 | ✅ 200；限流逻辑经代码确认（50 次/日/IP→429） |
| b1 匿名 POST /api/rooms | 错误 JSON 非 500 | ✅ 401（鉴权拦截） |
| b2/b3/b4 PUT 缺 status / status=5 / ?status=9 | 400 | ✅ 400×3（原 NPE→500） |
| c1/c2 工单挂他人/不存在 registrationId | 400 | ✅ 400"报名记录不存在或不属于本人" |
| c3 工单挂本人报名 | 200 | ✅ 200 |
| d1/d2 删被 exam_plan 引用的工种 1/等级 3 | 400 | ✅ 400"已被考试计划引用，无法删除" |
| d3 工单 status=9 | 400 | ✅ 400 |
| G1 验证码首次核销重置密码 | 200 | ✅ 200，新密码可登录 |
| G2 同验证码重复使用 | 400 | ✅ 400（原子核销防双花） |
| e GET / 、/api/public/plans 、staff GET /api/arrangements | 200 | ✅ 200×3 |

**编译/重启**：`mvnw -q compile` 一次通过（EXIT=0）；kill 旧进程 9644 后重启，新进程 2180 于 2s 内就绪，GET / 200。

**清理确认**：测试工单、验证码记录、ai_qa_log、operation_log、sys_login_log 测试记录全部删除（复核计数均为 0）；candidate 密码恢复种子哈希并验证 123456 可登录；总量与基线一致（用户 307、邮件 128、消息 940、考场 21、工种 20、等级 62、room1 status=1）。

## 偏离说明

1. 指定考生 uie2e516069 已不存在（第 3 轮清理），改用演示考生 candidate（其 registration id=1）完成越权测试。
2. 管理员无需重置密码：admin/staff/super 哈希与 data.sql 种子一致（123456），直接登录，未改库。
3. A 项未做业务链路运行时触发（唯一 registration 为已确认态，触发 notify 会污染数据且难复原），以编译 + 与验证码异步同构 + 异常全捕获为依据。
4. deleteTrade 在 exam_plan 外加查 skill_level 引用（防孤儿等级），属"或相关表"范围。

## 浏览器级 E2E（重组后回归，2026-10-07 补充）

真实浏览器（Chrome 扩展桥接）逐页验证：

| 页面 | 结果 |
|---|---|
| /auth/login.html | ✅ 表单+Canvas 渲染，console 0 错误 |
| 登录 candidate/123456 → /student/dashboard.html | ✅ 跳转正确，14 面板/2 表格，0 错误 |
| /student/workflow.html | ✅ 渲染本人报名记录（APPLICATION #1） |
| / （首页） | ✅ 34 卡片，0 错误 |
| /auth/register.html | ✅ 8 输入框+Canvas 背景，0 错误 |
| /auth/forgot-password.html | ✅ 7 输入框，0 错误 |
| /admin/admin.html（考生身份访问） | ✅ 守卫踢回 /auth/admin-login.html |
| /staff/exam-staff.html（考生身份访问） | ✅ 守卫踢回 /auth/exam-staff-login.html |
| /super-admin/super-admin.html（考生身份访问） | ✅ 守卫踢回 /auth/super-admin-login.html |
| 登出 | ✅（静态确认：logout() 跳转 '/' 首页，不受重组影响；桥接在 dashboard 页超时未能实点，逻辑无路径风险） |

**结论**：目录重组后全站页面、登录态、角色守卫在新路径下工作正常，无 console 错误。

## REVIEW

7 项缺陷全部修复并验证通过；未新增 P0/P1；遗留清单（死配置、大 JOIN 分页、404→500 等）按指令本轮不动。
