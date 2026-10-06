package com.expert.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要认证的接口注解。
 * 可用于标记需要登录才能访问的方法，支持指定角色。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAuth {

    /**
     * 允许访问的角色列表，为空表示仅需要登录即可。
     *
     * @return 角色数组
     */
    String[] role() default {};
}
