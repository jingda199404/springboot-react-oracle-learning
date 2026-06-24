package com.example.employee.employee;

import java.util.List;

public record EmployeeImportResponse(int imported, int skipped, List<String> errors) {
}
