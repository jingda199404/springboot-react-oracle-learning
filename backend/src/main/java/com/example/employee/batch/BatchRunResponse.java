package com.example.employee.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BatchRunResponse(String code, String label, LocalDate targetDate, int processedCount, String message, LocalDateTime executedAt) {
}
