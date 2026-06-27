package com.example.employee.auth;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public PermissionController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<PermissionDto> findAll(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        currentUser.requirePermission(userId, AuthService.PERMISSION_SETTING_PERMISSION);
        return authService.findAllPermissions();
    }

    @GetMapping("/users")
    public List<UserPermissionResponse> findAllUsers(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        currentUser.requirePermission(userId, AuthService.PERMISSION_SETTING_PERMISSION);
        return authService.findAllUserPermissions();
    }

    @PutMapping("/users/{targetUserId}")
    public UserPermissionResponse updateUserPermissions(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody PermissionUpdateRequest request
    ) {
        return authService.updateUserPermissions(
                currentUser.requirePermission(userId, AuthService.PERMISSION_SETTING_PERMISSION),
                targetUserId,
                request.permissions()
        );
    }
}
