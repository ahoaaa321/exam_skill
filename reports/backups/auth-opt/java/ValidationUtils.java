package com.demo.common.util;

import java.util.regex.Pattern;

/**
 * 统一的表单格式校验工具，避免正则散落在各 Service / Controller 中。
 */
public final class ValidationUtils {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");

    private static final int MIN_PASSWORD_LENGTH = 8;

    private ValidationUtils() {
    }

    public static boolean isPhone(String value) {
        return value != null && PHONE.matcher(value).matches();
    }

    public static boolean isIdCard(String value) {
        return value != null && ID_CARD.matcher(value).matches();
    }

    public static boolean isEmail(String value) {
        return value != null && EMAIL.matcher(value).matches();
    }

    /** 密码至少 8 位，且必须同时包含字母和数字。 */
    public static boolean isStrongPassword(String value) {
        return value != null
                && value.length() >= MIN_PASSWORD_LENGTH
                && LETTER.matcher(value).matches()
                && DIGIT.matcher(value).matches();
    }
}
