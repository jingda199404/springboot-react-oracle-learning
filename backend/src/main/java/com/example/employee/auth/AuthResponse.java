package com.example.employee.auth;

import java.util.List;

public record AuthResponse(Long id, String username, List<String> permissions, String message) {
}
