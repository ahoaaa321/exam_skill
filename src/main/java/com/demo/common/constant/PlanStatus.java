package com.demo.common.constant;

/** 考试计划状态（exam_plan.status），合法范围 0-3。 */
public final class PlanStatus {

    /** 草稿/未发布 */
    public static final int DRAFT = 0;
    /** 已发布，可报名 */
    public static final int PUBLISHED = 1;

    private PlanStatus() {
    }
}
