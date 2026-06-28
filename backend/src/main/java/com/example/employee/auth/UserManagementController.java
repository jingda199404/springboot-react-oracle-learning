package com.example.employee.auth;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final UserExcelService excelService;

    public UserManagementController(AuthService authService, CurrentUser currentUser, UserExcelService excelService) {
        this.authService = authService;
        this.currentUser = currentUser;
        this.excelService = excelService;
    }

    @GetMapping
    public List<UserPermissionResponse> findAll(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        currentUser.requirePermission(userId, AuthService.PERMISSION_SETTING_PERMISSION);
        return authService.findAllManagedUsers();
    }

    @PutMapping("/{targetUserId}/password")
    public UserPermissionResponse updatePassword(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        return authService.updateUserPassword(
                currentUser.requirePermission(userId, AuthService.PERMISSION_SETTING_PERMISSION),
                targetUserId,
                request.password()
        );
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        currentUser.requirePermission(userId, AuthService.PERMISSION_SETTING_PERMISSION);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user-upload-template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelService.createTemplate());
    }

    @PostMapping("/import")
    public UserImportResponse importUsers(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        currentUser.requirePermission(userId, AuthService.PERMISSION_SETTING_PERMISSION);
        return excelService.importUsers(file);
    }
}
