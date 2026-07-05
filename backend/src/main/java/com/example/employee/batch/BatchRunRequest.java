package com.example.employee.batch;

import java.time.LocalDate;

public record BatchRunRequest(LocalDate targetDate) {
}
