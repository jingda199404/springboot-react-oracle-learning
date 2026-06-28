package com.example.employee.auth;

import com.example.employee.error.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final AuthService authService;
    private final HttpServletRequest request;

    public CurrentUser(AuthService authService, HttpServletRequest request) {
        this.authService = authService;
        this.request = request;
    }

    public Long requireUserId(Long userId) {
        if (userId == null) {
            throw new AuthenticationException("ログインが必要です");
        }
        authService.requireActiveSession(userId, sessionToken());
        return userId;
    }

    public Long requirePermission(Long userId, String permission) {
        if (userId == null) {
            throw new AuthenticationException("ログインが必要です");
        }
        authService.requirePermission(userId, sessionToken(), permission);
        return userId;
    }

    private String sessionToken() {
        return request.getHeader("X-Session-Token");
    }
}
