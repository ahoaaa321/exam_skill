package com.demo.common;

/**
 * 线程级别的当前登录用户上下文
 */
public class UserContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public record CurrentUser(Long id, String username, int role) {}

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Long currentUserId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.id();
    }

    public static int currentRole() {
        CurrentUser u = HOLDER.get();
        return u == null ? -1 : u.role();
    }
}
