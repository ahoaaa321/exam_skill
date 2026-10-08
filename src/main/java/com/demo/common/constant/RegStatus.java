package com.demo.common.constant;

/** 报名记录状态（registration.status）。 */
public final class RegStatus {

    /** 待审核（含退回后重新提交） */
    public static final int PENDING = 1;
    /** 初审通过，待缴费 */
    public static final int FIRST_PASSED = 2;
    /** 审核退回 */
    public static final int REJECTED = 3;
    /** 已缴费，待复审/编排 */
    public static final int PAID = 4;
    /** 复审通过 / 编排已确认 */
    public static final int CONFIRMED = 5;
    /** 考生已取消 */
    public static final int CANCELED = 6;

    private RegStatus() {
    }
}
