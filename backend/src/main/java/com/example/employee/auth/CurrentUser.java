package com.example.employee.auth;

import com.example.employee.error.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final AuthService authService;

    public CurrentUser(AuthService authService) {
        this.authService = authService;
    }

    public Long requireUserId(Long userId) {
        if (userId == null) {
            throw new AuthenticationException("ログインが必要です");
        }
        authService.findUser(userId);
        return userId;
    }

    public Long requirePermission(Long userId, String permission) {
        if (userId == null) {
            throw new AuthenticationException("ログインが必要です");
        }
        authService.requirePermission(userId, permission);
        return userId;
    }
}
