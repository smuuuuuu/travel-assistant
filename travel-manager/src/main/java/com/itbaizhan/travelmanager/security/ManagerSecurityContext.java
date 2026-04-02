package com.itbaizhan.travelmanager.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 读取当前管理员（需配合 {@link ManagerJwtAuthenticationFilter} 使用 {@link ManagerPrincipal}）。
 */
public final class ManagerSecurityContext {

    private ManagerSecurityContext() {
    }

    public static ManagerPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object p = auth.getPrincipal();
        if (p instanceof ManagerPrincipal mp) {
            return mp;
        }
        return null;
    }

    public static Long currentManagerId() {
        ManagerPrincipal mp = currentPrincipal();
        return mp != null && mp.getManagerUser().getId() != null ? mp.getManagerUser().getId() : null;
    }

    public static String currentManagerName() {
        ManagerPrincipal mp = currentPrincipal();
        return mp != null ? mp.getUsername() : null;
    }
}
