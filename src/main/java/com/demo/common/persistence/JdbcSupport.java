package com.demo.common.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate RowMapper 通用辅助：时间转换、LEFT JOIN 可空列读取、首条记录提取。
 */
public final class JdbcSupport {

    private JdbcSupport() {
    }

    public static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    /**
     * 读取可能不存在于结果集中的列（如未加对应 JOIN 时的扩展字段），列缺失时返回 null 而非抛异常。
     */
    public static String getString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    /** 列表取首条，空列表转为 empty。 */
    public static <T> Optional<T> firstOrEmpty(List<T> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
