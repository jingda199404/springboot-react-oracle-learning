package com.example.employee.batch;

import java.util.List;

public record BatchDefinition(String code, String label, String description, List<BatchParameter> parameters) {
}
