package com.example.employee.batch;

import com.example.employee.accounting.CreditCardRepaymentBatch;
import com.example.employee.auth.AuthService;
import com.example.employee.auth.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private static final String CREDIT_CARD_REPAYMENT = "credit-card-repayment";
    private static final String CREDIT_CARD_REPAYMENT_LABEL = "クレジットカード返済";

    private final CreditCardRepaymentBatch repaymentBatch;
    private final CurrentUser currentUser;

    public BatchController(CreditCardRepaymentBatch repaymentBatch, CurrentUser currentUser) {
        this.repaymentBatch = repaymentBatch;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<BatchDefinition> findAll(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        currentUser.requirePermission(userId, AuthService.ACCOUNTING_PERMISSION);
        return List.of(creditCardRepaymentDefinition());
    }

    @PostMapping("/{code}/run")
    public BatchRunResponse run(@PathVariable String code, @RequestBody BatchRunRequest request, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Long currentUserId = currentUser.requirePermission(userId, AuthService.ACCOUNTING_PERMISSION);
        LocalDate targetDate = request.targetDate() == null ? LocalDate.now() : request.targetDate();
        if (!CREDIT_CARD_REPAYMENT.equals(code)) {
            throw new IllegalArgumentException("指定された batch は存在しません：" + code);
        }

        int processedCount = repaymentBatch.repayDueCards(targetDate, currentUserId);
        return new BatchRunResponse(
                CREDIT_CARD_REPAYMENT,
                CREDIT_CARD_REPAYMENT_LABEL,
                targetDate,
                processedCount,
                processedCount + " 件の返済処理を実行しました。",
                LocalDateTime.now()
        );
    }

    private BatchDefinition creditCardRepaymentDefinition() {
        return new BatchDefinition(
                CREDIT_CARD_REPAYMENT,
                CREDIT_CARD_REPAYMENT_LABEL,
                "指定した実行日が返済日のクレジットカード負債を、設定済みの返済口座から自動返済します。",
                List.of(new BatchParameter("targetDate", "実行日", "date", true, LocalDate.now().toString()))
        );
    }
}
