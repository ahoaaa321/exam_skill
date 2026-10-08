package com.demo.common.constant;

/**
 * 平台系统公共邮箱。
 * 该邮箱是注册 / 换绑时唯一允许多个账号共同使用的邮箱；
 * 其他邮箱仍按“一邮箱一账号”强校验。
 */
public final class SystemEmails {

    private SystemEmails() {
    }

    /** 系统公共邮箱（即平台发信邮箱，验证码与系统通知统一汇入该邮箱）。 */
    public static final String SHARED_EMAIL = "3390709428@qq.com";

    /** 判断给定邮箱是否为系统公共邮箱（忽略大小写与首尾空格）。 */
    public static boolean isShared(String email) {
        return email != null && SHARED_EMAIL.equalsIgnoreCase(email.trim());
    }
}
