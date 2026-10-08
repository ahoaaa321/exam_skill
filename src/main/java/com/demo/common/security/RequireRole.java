package com.demo.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色要求。标注在 Controller 方法或类上。
 * 未标注的 /api/** 接口仅要求登录，不限制角色。
 * 角色：0考生 1管理员 2考务 3超级管理员
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    int[] value();
}
