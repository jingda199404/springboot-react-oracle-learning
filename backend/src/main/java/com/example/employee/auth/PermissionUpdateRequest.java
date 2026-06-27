package com.example.employee.auth;

import java.util.List;

public record PermissionUpdateRequest(List<String> permissions) {
}
