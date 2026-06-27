package com.example.employee.accounting;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountingBalanceRequest(
        @NotNull AccountingBalanceType type,
        @NotBlank @Size(max = 100) String accountName,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @Min(1) @Max(31) Integer repaymentDay,
        @Size(max = 100) String repaymentAccountName,
        @Size(max = 255) String memo
) {
}
