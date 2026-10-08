# 第 1 轮自检-修复-验证报告（2026-10-04）

## DETECT 问题清单

| # | 级别 | 位置 | 问题 | 影响 |
|---|---|---|---|---|
| 1-1 | P0 | `ExamController`（缺失映射）/ `ExamService.cancelRegistration`（孤儿方法） | 前端 dashboard「取消报名」调用 `PUT /api/my/registrations/{id}/cancel`，后端无对应 Controller 映射 | 考生取消报名必然 404/405，功能完全不可用 |
| 1-2 | P1 | `ExamService.register` + `PlanRepository` | 容量校验「先查 current_count 再插入」无行锁，默认隔离级别下并发报名可超招 | 热门计划并发报名突破 max_candidates |
| 1-3 | P1 | `RegistrationRepository.updateFirstAudit/updateSecondAudit/reject` | 审核流转为无条件 UPDATE，双击/并发可插入重复审核记录、重复通知 | 审核留痕重复、状态机被并发踩踏 |
| 1-4 | P1 | `ExamService.pay` | 缴费「先查状态再插流水」无抢占，双击/并发可生成重复支付流水 | 模拟支付重复记账 |
| 1-5 | P1 | `ScoreRepository.publish` | 发布为无条件 UPDATE，重复点击重复发通知、重复触发自动发证 | 重复通知/发证尝试 |
| 1-6 | P1 | 已取消报名重新报名分支（`register` CANCELED 分支） | 重新占位时**不做容量校验** | 满额后可借「取消→重报」绕过容量限制 |
| 1-7 | P2 | `ExamController.updateArrangement` | 直接强转 body 中 roomId/seatNo，缺字段即 NPE → 500 | 错误输入暴露为服务端异常（虽被全局处理兜底为通用提示） |
| 1-8 | P2 | `ExamService.statistics` | 全量载入 Registration 到内存做 stream 计数（仓储层已有 `statusCounts()` 聚合未使用） | 数据量增长后无谓的内存/DB 开销 |
| 1-9 | P2 | `AiQaController` | 问题内容无长度上限，直接落库 ai_qa_log | 可被刷入超长文本 |

## FIX 变更

- `ExamController`：新增 `PUT /my/registrations/{id}/cancel`（本人取消，登录即可，归属校验在 Service）；调座入参校验缺失返回 400。
- `PlanRepository`：新增 `findForUpdateById`（`SELECT ... FOR UPDATE`）。
- `ExamService.register`：事务内对计划行加悲观锁；CANCELED 重报分支补容量校验。
- `RegistrationRepository`：新增 `updateFirstAuditIfPending / updateSecondAuditIfPaid / rejectIfPending / rejectIfPaid / markPaidIfFirstPassed` 条件更新（均带状态 WHERE，返回影响行数）。
- `ExamService`：初审/复审通过与退回、线上缴费、线下确认缴费全部改为「条件更新成功后才写留痕/流水/通知」，0 行返回友好 400。
- `ScoreRepository.publish` / `ScoreService.publishScore`：仅未发布可发布；重复发布 400，成绩不存在 404。
- `statistics()`：改用 `statusCounts()` 数据库聚合，输出键与取值保持完全一致。
- `AiQaController`：question 长度上限 500。
- 备份：改动前原件存于 `reports/backups/round1/`。

## VERIFY（真实 HTTP + 数据库校验，脚本 reports/round1-tests.ps1）

| 用例 | 输入 | 预期 | 实际 |
|---|---|---|---|
| T1 报名 | 考生 A → 计划7 | 200 待审核 | ✅ 200，regId=2907，status=1 |
| T2 取消本人报名 | A cancel 2907 | 200 | ✅ 200 |
| T3 取消他人报名 | B cancel 2907 | 403 | ✅ 403 |
| T4 取消后重新报名 | A 再报计划7 | 200，status=1 | ✅ 200/1 |
| T5 未登录取消 | 无 token | 401 | ✅ 401 |
| T6 初审 | admin approve | 200 | ✅ 200 |
| T7 重复初审 | admin 再 approve | 400 | ✅ 400，DB 审核留痕仅 1 条 |
| T8 缴费 | A wechat pay | 200 | ✅ 200 |
| T9 重复缴费 | A 再 pay | 400 | ✅ 400，DB 支付流水仅 1 条 |
| T10 已缴费取消 | A cancel | 400 | ✅ 400 |
| T11 调座空 body | staff PUT {} | 400 | ✅ 400（原会 500） |
| T12 AI 问答 501 字 | 匿名 | 400 | ✅ 400；正常问题 200 分类正确 |
| T13 统计 | admin | 七状态键齐全 | ✅ 13 个键齐全，total=903 |
| 容量计数 | 计划7 current_count | 与 COUNT(status<>6) 一致 | ✅ 26 = 26 |

编译：`mvnw compile` 通过；服务重启后 8080 正常运行。

## REVIEW

- 发现 9 个（P0×1，P1×5，P2×3），修复 9 个，放弃 0。
- 未改变任何既有业务规则（状态机语义、角色矩阵、页面文案均保持原样）。
- 遗留：存储型 XSS（公告/计划名/工单等 innerHTML 渲染）、若干缺失索引、签到表并发幂等——列入第 2 轮。
