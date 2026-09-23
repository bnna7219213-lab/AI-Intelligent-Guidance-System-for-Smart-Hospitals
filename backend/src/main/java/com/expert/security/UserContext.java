package com.expert.security;

/**
 * 当前用户上下文 (基于 ThreadLocal)
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    /**
     * 设置当前用户信息到上下文
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色
     */
    public static void set(Long userId, String username, String role) {
        USER_ID.set(userId);
        USERNAME.set(username);
        ROLE.set(role);
    }

    /**
     * 获取当前用户ID
     *
     * @return userId
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取当前用户名
     *
     * @return username
     */
    public static String getUsername() {
        return USERNAME.get();
    }

    /**
     * 获取当前用户角色
     *
     * @return role
     */
    public static String getRole() {
        return ROLE.get();
    }

    /**
     * 清除当前用户上下文
     */
    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
    }
}
