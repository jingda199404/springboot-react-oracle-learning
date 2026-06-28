package com.example.employee.auth;

import java.util.List;

public record UserImportResponse(int imported, int skipped, List<String> errors) {
}
