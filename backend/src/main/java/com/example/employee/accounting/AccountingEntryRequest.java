package com.example.employee.accounting;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountingEntryRequest(
        @NotNull LocalDate entryDate,
        @NotNull AccountingEntryType type,
        @NotBlank @Size(max = 80) String category,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(max = 100) String paymentMethod,
        @Size(max = 255) String memo
) {
}
