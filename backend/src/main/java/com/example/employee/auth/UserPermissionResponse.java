package com.example.employee.auth;

import java.util.List;

public record UserPermissionResponse(Long id, String username, List<String> permissions) {
}
