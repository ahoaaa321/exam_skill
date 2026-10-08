package com.demo.module.exam.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.demo.common.constant.PayStatus;
import com.demo.common.constant.PlanStatus;
import com.demo.common.constant.RegStatus;
import com.demo.common.constant.UserRole;
import com.demo.common.util.MaskUtils;
import com.demo.module.exam.entity.ExamPlan;
import com.demo.module.exam.entity.Registration;
import com.demo.module.exam.repository.ArrangementConfigRepository;
import com.demo.module.exam.repository.ExtendedRepository;
import com.demo.module.exam.repository.PaymentRepository;
import com.demo.module.exam.repository.PlanRepository;
import com.demo.module.exam.repository.RegistrationRepository;
import com.demo.module.room.entity.ExamRoom;
import com.demo.module.room.entity.SeatPlacement;
import com.demo.module.room.repository.ArrangementRepository;
import com.demo.module.room.repository.RoomRepository;
import com.demo.module.room.repository.SigninRepository;
import com.demo.common.service.AuditService;
import com.demo.common.service.NotificationService;

@Service
public class ExamService {

    private final PlanRepository planRepository;
    private final RegistrationRepository registrationRepository;
    private final RoomRepository roomRepository;
    private final ArrangementRepository arrangementRepository;
    private final PaymentRepository paymentRepository;
    private final SigninRepository signinRepository;
    private final ExtendedRepository extendedRepository;
    private final ArrangementConfigRepository arrangementConfigRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public ExamService(PlanRepository planRepository, RegistrationRepository registrationRepository,
                       RoomRepository roomRepository, ArrangementRepository arrangementRepository,
                       PaymentRepository paymentRepository, SigninRepository signinRepository,
                       ExtendedRepository extendedRepository, ArrangementConfigRepository arrangementConfigRepository,
                       NotificationService notificationService, AuditService auditService) {
        this.planRepository = planRepository;
        this.registrationRepository = registrationRepository;
        this.roomRepository = roomRepository;
        this.arrangementRepository = arrangementRepository;
        this.paymentRepository = paymentRepository;
        this.signinRepository = signinRepository;
        this.extendedRepository = extendedRepository;
        this.arrangementConfigRepository = arrangementConfigRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public List<ExamPlan> publishedPlans() {
        return planRepository.findPublished();
    }

    public List<ExamPlan> allPlans() {
        return planRepository.findAll();
    }

    public ExamPlan createPlan(ExamPlan plan, Long createdBy) {
        ExamPlan p = new ExamPlan(null, plan.planName(), plan.planCode(), plan.tradeId(), plan.levelId(),
                plan.categoryId(), plan.registerStartTime(), plan.registerEndTime(), plan.examTime(),
                plan.examEndTime(), plan.examLocation(), plan.maxCandidates(), plan.fee(),
                plan.conditionDesc(), PlanStatus.DRAFT, 0, createdBy, null, null, null, null);
        Long newId = (long) planRepository.insert(p);
        auditService.log("PLAN_CREATE", plan.planCode(), "创建考试计划：" + plan.planName());
        return planRepository.findById(newId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "计划创建后读取失败"));
    }

    public ExamPlan updatePlan(Long id, ExamPlan plan) {
        ExamPlan existing = planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考试计划不存在"));
        if (plan.maxCandidates() != null && plan.maxCandidates() > 0
                && plan.maxCandidates() < existing.currentCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "招考人数不能小于当前已报名人数（" + existing.currentCount() + "）");
        }
        ExamPlan p = new ExamPlan(id, plan.planName(), plan.planCode(), plan.tradeId(), plan.levelId(),
                plan.categoryId(), plan.registerStartTime(), plan.registerEndTime(), plan.examTime(),
                plan.examEndTime(), plan.examLocation(), plan.maxCandidates(), plan.fee(),
                plan.conditionDesc(), existing.status(), existing.currentCount(), existing.createdBy(),
                existing.tradeName(), existing.levelName(), existing.categoryName(), existing.createdAt());
        planRepository.update(p);
        auditService.log("PLAN_UPDATE", plan.planCode(), "修改考试计划：" + plan.planName());
        return p;
    }

    public void deletePlan(Long id) {
        ExamPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考试计划不存在"));
        if (registrationRepository.countByPlanId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该计划已有考生报名，不可删除，可改为关闭");
        }
        planRepository.delete(id);
        auditService.log("PLAN_DELETE", plan.planCode(), "删除考试计划：" + plan.planName());
    }

    public ExamPlan updatePlanStatus(Long id, int status) {
        ExamPlan old = planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考试计划不存在"));
        if (status < 0 || status > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的计划状态");
        }
        planRepository.updateStatus(id, status);
        auditService.logWithSnapshot("PLAN_STATUS", String.valueOf(id), "调整计划状态",
                "status=" + old.status(), "status=" + status);
        return planRepository.findById(id).orElse(null);
    }

    public List<Registration> allRegistrations() {
        return registrationRepository.findAll();
    }

    public List<Registration> myRegistrations(Long userId) {
        return registrationRepository.findByUserId(userId);
    }

    @Transactional
    public Registration register(Long userId, Long planId, Integer workYears, String education,
                                 String emergencyContact, String emergencyPhone) {
        // 对计划行加悲观锁，串行化同一计划的并发报名，防止超招
        ExamPlan plan = planRepository.findForUpdateById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考试计划不存在"));
        if (plan.status() == null || plan.status() != PlanStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该计划未发布，不可报名");
        }
        if (!plan.isRegisterOpen()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名时间已过或未开始");
        }

        var existing = registrationRepository.findByUserAndPlan(userId, planId);
        Registration saved;
        boolean reOccupySeat = false;
        if (existing.isPresent()) {
            Registration old = existing.get();
            int st = old.status() == null ? 0 : old.status();
            if (st == RegStatus.REJECTED) {
                // 审核退回 → 重新提交，沿用原名额（退回未释放名额）
                registrationRepository.reactivate(old.id());
                saved = registrationRepository.findById(old.id()).orElseThrow();
            } else if (st == RegStatus.CANCELED) {
                // 已取消 → 重新报名，需重新校验容量并重新占用名额
                if (plan.maxCandidates() > 0 && plan.currentCount() >= plan.maxCandidates()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名人数已满");
                }
                registrationRepository.reactivate(old.id());
                planRepository.incrementCurrentCount(planId);
                saved = registrationRepository.findById(old.id()).orElseThrow();
                reOccupySeat = true;
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "您已报名该考试计划，当前状态：" + old.statusName());
            }
        } else {
            if (plan.maxCandidates() > 0 && plan.currentCount() >= plan.maxCandidates()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名人数已满");
            }
            Registration reg = new Registration(null, userId, planId, workYears, education,
                    emergencyContact, emergencyPhone, RegStatus.PENDING, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            registrationRepository.insert(reg);
            planRepository.incrementCurrentCount(planId);
            saved = registrationRepository.findByUserAndPlan(userId, planId).orElseThrow();
            reOccupySeat = true;
        }
        if (reOccupySeat) {
            notificationService.notify(userId, "报名提交成功",
                    "您已成功提交【" + plan.planName() + "】的报名申请，请等待资格审核。");
        } else {
            notificationService.notify(userId, "报名重新提交成功",
                    "您已重新提交【" + plan.planName() + "】的报名申请，请等待资格审核。");
        }
        return saved;
    }

    /** 考生取消报名（仅待审核/审核退回可取消），释放名额 */
    @Transactional
    public void cancelRegistration(Long registrationId, Long userId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在"));
        if (!reg.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作他人报名");
        }
        int st = reg.status() == null ? 0 : reg.status();
        if (st != RegStatus.PENDING && st != RegStatus.FIRST_PASSED && st != RegStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前状态不可取消报名");
        }
        registrationRepository.cancel(registrationId);
        planRepository.decrementCurrentCount(reg.planId());
        auditService.log("REG_CANCEL", String.valueOf(registrationId), "考生取消报名：" + reg.planName());
        notificationService.notify(userId, "报名已取消",
                "您报名的【" + reg.planName() + "】已取消，报名名额已释放。");
    }

    /** 审核进度时间线（管理员可查任意；考生仅可查本人） */
    public List<Map<String, Object>> auditRecords(Long registrationId, Long userId, int role) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在"));
        if (role == UserRole.CANDIDATE && !reg.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看他人报名记录");
        }
        return registrationRepository.findAuditRecords(registrationId);
    }

    @Transactional
    public Registration audit(Long registrationId, boolean approved, String reason, Long auditorId, boolean isSecondAudit) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在"));
        if (isSecondAudit) {
            // 复审：已缴费 → 已确认
            if (reg.status() == null || reg.status() != RegStatus.PAID) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已缴费的报名才能复审");
            }
            if (approved) {
                if (registrationRepository.updateSecondAuditIfPaid(registrationId, auditorId) == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名状态已变化，复审未生效，请刷新后重试");
                }
                registrationRepository.insertAuditRecord(registrationId, auditorId, 2, 1, null);
                notificationService.notify(reg.userId(), "资格复审通过",
                        "您报名的【" + reg.planName() + "】已通过复审，等待考场编排与准考证生成。");
                auditService.log("AUDIT_SECOND_PASS", String.valueOf(registrationId), reg.realName() + " 复审通过");
            } else {
                if (reason == null || reason.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "退回必须填写原因");
                }
                if (registrationRepository.rejectIfPaid(registrationId, reason) == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名状态已变化，复审退回未生效，请刷新后重试");
                }
                registrationRepository.insertAuditRecord(registrationId, auditorId, 2, 2, reason);
                paymentRepository.markRefund(registrationId);
                notificationService.notify(reg.userId(), "资格复审未通过",
                        "您报名的【" + reg.planName() + "】复审未通过，原因：" + reason + "。报名费用将原路退回。");
                auditService.log("AUDIT_SECOND_REJECT", String.valueOf(registrationId),
                        reg.realName() + " 复审退回：" + reason);
            }
        } else {
            // 初审：待审核 → 审核通过/退回
            if (reg.status() == null || reg.status() != RegStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前状态不可初审");
            }
            if (approved) {
                if (registrationRepository.updateFirstAuditIfPending(registrationId, auditorId) == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名状态已变化，初审未生效，请刷新后重试");
                }
                registrationRepository.insertAuditRecord(registrationId, auditorId, 1, 1, null);
                notificationService.notify(reg.userId(), "资格初审通过",
                        "您报名的【" + reg.planName() + "】已通过初审，请及时完成缴费。");
                auditService.log("AUDIT_FIRST_PASS", String.valueOf(registrationId), reg.realName() + " 初审通过");
            } else {
                if (reason == null || reason.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "退回必须填写原因");
                }
                if (registrationRepository.rejectIfPending(registrationId, reason) == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名状态已变化，初审退回未生效，请刷新后重试");
                }
                registrationRepository.insertAuditRecord(registrationId, auditorId, 1, 2, reason);
                notificationService.notify(reg.userId(), "报名资格审核退回",
                        "您报名的【" + reg.planName() + "】被退回，原因：" + reason + "。可修改后重新提交。");
                auditService.log("AUDIT_FIRST_REJECT", String.valueOf(registrationId),
                        reg.realName() + " 初审退回：" + reason);
            }
        }
        return registrationRepository.findById(registrationId).orElse(null);
    }

    /** 管理员线下确认缴费 */
    @Transactional
    public Registration confirmPay(Long registrationId, Long adminId) {
        Registration reg = requireStatus(registrationId, RegStatus.FIRST_PASSED, "只有审核通过后才能确认缴费");
        // 条件更新抢占状态，避免重复确认；确认成功后再补线下流水
        if (registrationRepository.markPaidIfFirstPassed(registrationId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缴费确认未生效，报名状态可能已被处理，请刷新后重试");
        }
        paymentRepository.findByRegistrationId(registrationId).orElseGet(() -> {
            paymentRepository.insert(registrationId, reg.fee(), "offline",
                    "OFF" + System.currentTimeMillis(), PayStatus.PAID);
            return null;
        });
        notificationService.notify(reg.userId(), "缴费确认成功",
                "您报名的【" + reg.planName() + "】缴费已确认（线下核验），等待复审与考场编排。");
        auditService.log("PAY_CONFIRM", String.valueOf(registrationId),
                reg.realName() + " 线下缴费确认 ¥" + reg.fee());
        return registrationRepository.findById(registrationId).orElse(null);
    }

    /** 考生线上模拟缴费，写入支付流水 */
    @Transactional
    public Registration pay(Long registrationId, Long userId, String payMethod) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在"));
        if (!reg.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作他人报名");
        }
        if (reg.status() == null || reg.status() != RegStatus.FIRST_PASSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有审核通过后才能缴费");
        }
        // 条件更新抢占状态：并发/重复点击时只有一笔请求成功，杜绝重复支付流水
        if (registrationRepository.markPaidIfFirstPassed(registrationId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缴费未生效，报名状态可能已变化，请刷新后重试");
        }
        String method = (payMethod == null || payMethod.isBlank()) ? "wechat" : payMethod;
        String tradeNo = method.toUpperCase().charAt(0) + String.valueOf(System.currentTimeMillis());
        paymentRepository.insert(registrationId, reg.fee(), method, tradeNo, PayStatus.PAID);
        notificationService.notify(reg.userId(), "在线支付成功",
                "您已成功支付【" + reg.planName() + "】报名费 ¥" + reg.fee() + "，流水号：" + tradeNo + "。");
        auditService.log("PAY_ONLINE", String.valueOf(registrationId),
                reg.realName() + " 在线缴费 ¥" + reg.fee() + " (" + method + ")");
        return registrationRepository.findById(registrationId).orElse(null);
    }

    private Registration requireStatus(Long registrationId, int status, String msg) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在"));
        if (reg.status() != status) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        return reg;
    }

    /**
     * 增量考场编排：
     * 仅编排“已缴费且尚未编排”的考生；按考试计划分组，组内同单位打散 + 随机洗牌，
     * 依次填入启用考场的空座位，不影响已编排考生。座位不足时整体回滚。
     * 座位唯一性按考试日期时段化：同一天内座位不可复用，不同考试日期可复用同一座位。
     * 编排参数读取计划维度的 arrangement_config（无配置用默认值：座位数30/间隔1/打散1，与历史行为一致）。
     */
    @Transactional
    public Map<String, Object> arrange() {
        List<Registration> pool = registrationRepository.findPaidNotArranged();
        int capacity = roomRepository.totalSeats();
        if (pool.isEmpty()) {
            return Map.of("total", 0, "capacity", capacity, "usedRooms", 0,
                    "alreadyArranged", arrangementRepository.countAll(),
                    "message", "没有待编排的考生");
        }

        List<ExamRoom> rooms = roomRepository.findEnabled();

        // 每个考试日期的下一个候选座位指针与已占用座位（同一日期内共享，不同日期互不冲突）
        Map<java.time.LocalDate, Map<Long, Integer>> pointersByDate = new LinkedHashMap<>();
        Map<java.time.LocalDate, java.util.Set<String>> occupiedByDate = new LinkedHashMap<>();

        String year = String.valueOf(java.time.LocalDate.now().getYear());
        int assigned = 0;
        int usedRooms = 0;
        java.util.Set<Long> usedRoomIds = new java.util.HashSet<>();
        List<Long> notifiedUserIds = new ArrayList<>();

        // 按计划分组
        Map<Long, List<Registration>> byPlan = new LinkedHashMap<>();
        for (Registration r : pool) {
            byPlan.computeIfAbsent(r.planId(), k -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<Long, List<Registration>> entry : byPlan.entrySet()) {
            Long planId = entry.getKey();
            List<Registration> group = entry.getValue();
            ExamPlan plan = planRepository.findById(planId).orElse(null);
            java.time.LocalDate examDate = plan == null || plan.examTime() == null
                    ? null : plan.examTime().toLocalDate();

            // 编排配置：未配置时用默认值（座位30/间隔1/打散1）
            Map<String, Object> config = arrangementConfigRepository.findByPlan(planId);
            int defaultSeatCount = intConfig(config, "default_seat_count", 30);
            int seatGap = intConfig(config, "seat_gap", 1);
            int shuffleUnit = intConfig(config, "shuffle_unit", 1);

            // 该考试日期的已占用座位（含历史编排），保证增量不冲突
            java.util.Set<String> occupied = occupiedByDate.computeIfAbsent(examDate, d -> {
                java.util.Set<String> set = new java.util.HashSet<>();
                for (SeatPlacement p : arrangementRepository.findPlacementsByDate(d)) {
                    set.add(p.roomId() + ":" + p.seatNo());
                }
                return set;
            });
            Map<Long, Integer> pointers = pointersByDate.computeIfAbsent(examDate, d -> {
                Map<Long, Integer> p = new LinkedHashMap<>();
                rooms.forEach(r -> p.put(r.id(), 1));
                return p;
            });

            List<Registration> ordered;
            if (shuffleUnit == 1) {
                // 同单位打散后再整体洗牌，兼顾随机与分散（历史默认行为）
                ordered = interleaveByUnit(group);
                Collections.shuffle(ordered);
                ordered = interleaveByUnit(ordered);
            } else {
                // 未启用打散：按报名先后顺序直接编排
                ordered = group;
            }
            for (Registration r : ordered) {
                Long[] seat = nextFreeSeat(rooms, pointers, occupied, seatGap, defaultSeatCount);
                if (seat == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "可用座位不足，当前已编排 " + assigned + " 人，请先增加考场或重置后重试");
                }
                Long roomId = seat[0];
                int seatNo = seat[1].intValue();
                occupied.add(roomId + ":" + seatNo);
                usedRoomIds.add(roomId);
                String ticketNo = buildTicketNo(year, roomId, seatNo);
                arrangementRepository.insert(r.id(), roomId, seatNo, ticketNo, examDate);
                arrangementRepository.generateAdmissionTicket(r.id(), ticketNo);
                arrangementRepository.updateRegistrationStatusToConfirmed(r.id());
                // 同步创建签到记录，保证“编排→签到”链路闭环
                extendedRepository.initSigninByRegistration(r.id());
                notifiedUserIds.add(r.userId());
                assigned++;
            }
        }
        usedRooms = usedRoomIds.size();

        for (Long uid : notifiedUserIds) {
            notificationService.notify(uid, "考场编排完成",
                    "您的考场座位已编排完成，准考证已生成，请登录个人中心查看并打印。");
        }
        auditService.log("ARRANGE", "ALL", "增量编排完成，本次编排 " + assigned + " 人，使用考场 " + usedRooms + " 间");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", assigned);
        result.put("capacity", capacity);
        result.put("usedRooms", usedRooms);
        result.put("alreadyArranged", arrangementRepository.countAll());
        result.put("message", "编排完成，本次新增 " + assigned + " 个座位，累计编排 " + arrangementRepository.countAll() + " 人");
        return result;
    }

    /** 读取编排配置整型字段，缺失或非法时回退默认值 */
    private int intConfig(Map<String, Object> config, String key, int defaultValue) {
        Object value = config == null ? null : config.get(key);
        return value instanceof Number n ? n.intValue() : defaultValue;
    }

    /** 同单位打散：按 work_unit 分桶后轮询取，降低相邻座位同单位概率 */
    private List<Registration> interleaveByUnit(List<Registration> input) {
        Map<String, List<Registration>> buckets = new LinkedHashMap<>();
        for (Registration r : input) {
            String unit = r.workUnit() == null || r.workUnit().isBlank() ? "~" : r.workUnit();
            buckets.computeIfAbsent(unit, k -> new ArrayList<>()).add(r);
        }
        List<Registration> result = new ArrayList<>();
        boolean added = true;
        while (added) {
            added = false;
            for (List<Registration> bucket : buckets.values()) {
                if (!bucket.isEmpty()) {
                    result.add(bucket.remove(0));
                    added = true;
                }
            }
        }
        return result;
    }

    /** 在启用考场中寻找下一个未占用座位，返回 [roomId, seat]；seatGap 为同考场相邻考生座位间隔，defaultSeatCount 为未显式配置座位数考场的容量上限 */
    private Long[] nextFreeSeat(List<ExamRoom> rooms, Map<Long, Integer> pointers, java.util.Set<String> occupied,
                                int seatGap, int defaultSeatCount) {
        for (ExamRoom room : rooms) {
            int cap = room.seatCount() > 0 ? room.seatCount() : defaultSeatCount;
            int ptr = pointers.getOrDefault(room.id(), 1);
            while (ptr <= cap && occupied.contains(room.id() + ":" + ptr)) {
                ptr++;
            }
            if (ptr <= cap) {
                pointers.put(room.id(), ptr + Math.max(1, seatGap));
                return new Long[]{room.id(), (long) ptr};
            }
            pointers.put(room.id(), ptr);
        }
        return null;
    }

    private String buildTicketNo(String year, Long roomId, int seatNo) {
        return year + "-" + String.format("%03d", roomId) + "-" + String.format("%02d", seatNo);
    }

    /** 清空全部编排，已确认考生回退为已缴费，便于重新编排 */
    @Transactional
    public Map<String, Object> resetArrangements() {
        int n = arrangementRepository.countAll();
        arrangementRepository.deleteAllTickets();
        arrangementRepository.deleteAllArrangements();
        extendedRepository.deleteOrphanSignins();
        registrationRepository.resetArrangedStatus();
        auditService.log("ARRANGE_RESET", "ALL", "清空全部编排，共 " + n + " 条");
        return Map.of("cleared", n, "message", "已清空全部编排结果，" + n + " 名考生回退为待编排");
    }

    /** 手动调座：目标座位空闲则移动，被占则与占有人对调，并同步准考证号 */
    @Transactional
    public void updateArrangement(Long arrangementId, Long targetRoomId, int seatNo) {
        SeatPlacement a = arrangementRepository.findArrangementById(arrangementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "编排记录不存在"));
        ExamRoom targetRoom = roomRepository.findById(targetRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目标考场不存在"));
        if (targetRoom.status() == null || targetRoom.status() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标考场已停用");
        }
        if (seatNo < 1 || seatNo > targetRoom.seatCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "座位号超出该考场容量（1-" + targetRoom.seatCount() + "）");
        }

        Long regA = a.registrationId();
        Long roomA = a.roomId();
        int seatA = a.seatNo();
        String year = String.valueOf(java.time.LocalDate.now().getYear());

        if (roomA.equals(targetRoomId) && seatA == seatNo) {
            return; // 无变化
        }

        var occupiedOpt = arrangementRepository.findByRoomSeat(targetRoomId, seatNo, a.examDate());
        if (occupiedOpt.isPresent()) {
            SeatPlacement b = occupiedOpt.get();
            long arrB = b.id();
            Long regB = b.registrationId();
            // A → 目标座位
            String ticketA = buildTicketNo(year, targetRoomId, seatNo);
            arrangementRepository.updatePlacement(arrangementId, targetRoomId, seatNo, ticketA);
            arrangementRepository.syncTicketNo(regA, ticketA);
            // B → A 原座位
            String ticketB = buildTicketNo(year, roomA, seatA);
            arrangementRepository.updatePlacement(arrB, roomA, seatA, ticketB);
            arrangementRepository.syncTicketNo(regB, ticketB);
            auditService.log("SEAT_SWAP", String.valueOf(arrangementId),
                    "调座交换：编排#" + arrangementId + " 与 #" + arrB + " 互换座位");
        } else {
            String ticketA = buildTicketNo(year, targetRoomId, seatNo);
            arrangementRepository.updatePlacement(arrangementId, targetRoomId, seatNo, ticketA);
            arrangementRepository.syncTicketNo(regA, ticketA);
            auditService.log("SEAT_MOVE", String.valueOf(arrangementId),
                    "调座：编排#" + arrangementId + " 调整至 " + targetRoom.roomCode() + " " + seatNo + " 座");
        }
    }

    public Map<String, Object> getTicket(Long registrationId, Long userId, int role) {
        Registration r = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在"));
        if (role == 0 && !r.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看他人准考证");
        }
        if (r.ticketNo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "准考证尚未生成，请等待考场编排");
        }
        ExamPlan plan = planRepository.findById(r.planId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ticketNo", r.ticketNo());
        map.put("candidate", r.realName());
        map.put("phone", r.phone());
        map.put("plan", r.planName());
        map.put("trade", r.tradeName());
        map.put("level", r.levelName());
        map.put("room", r.roomCode() + " (" + r.building() + " " + r.classroom() + ")");
        map.put("roomCode", r.roomCode());
        map.put("seat", r.seatNo());
        map.put("idCard", MaskUtils.maskMiddle(r.idCard()));
        if (plan != null) {
            map.put("examTime", plan.examTime() == null ? null : plan.examTime().toString().replace('T', ' '));
            map.put("examEndTime", plan.examEndTime() == null ? null : plan.examEndTime().toString().replace('T', ' '));
            map.put("examLocation", plan.examLocation());
        }
        return map;
    }

    public List<Map<String, Object>> arrangements(Long planId) {
        return arrangementRepository.findAll(planId);
    }

    public Map<String, Object> statistics() {
        // 状态分布走数据库聚合，避免把全量报名载入内存计数（输出键保持不变）
        Map<String, Object> counts = registrationRepository.statusCounts();
        Map<String, Object> map = new LinkedHashMap<>(counts);
        map.put("totalFee", paymentRepository.sumPaidAmount());
        map.put("arranged", arrangementRepository.countAll());
        map.put("byTrade", arrangementRepository.statisticsByTrade());
        map.put("byLevel", arrangementRepository.statisticsByLevel());
        map.put("monthlyTrend", arrangementRepository.monthlyTrend());
        map.put("statusDist", arrangementRepository.countGroupByStatus());
        return map;
    }
}
