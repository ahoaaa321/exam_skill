package com.demo.common.util;

/**
 * 敏感信息脱敏工具。
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    /**
     * 保留首尾各 4 位，中间以 10 个 * 代替（用于身份证号等）。
     * 长度不足 8 位时原样返回，避免过度脱敏后反而泄露完整短串。
     */
    public static String maskMiddle(String value) {
        if (value == null || value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + "**********" + value.substring(value.length() - 4);
    }
}
