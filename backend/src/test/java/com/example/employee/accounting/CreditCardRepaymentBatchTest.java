package com.example.employee.accounting;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CreditCardRepaymentBatchTest {

    @Autowired
    private AccountingBalanceRepository repository;

    @Autowired
    private CreditCardRepaymentBatch batch;

    @Test
    void repaysDueLiabilityFromConfiguredAssetAccountOncePerDay() {
        LocalDate repaymentDate = LocalDate.of(2026, 6, 27);
        AccountingBalance bank = repository.save(new AccountingBalance(
                repaymentDate,
                AccountingBalanceType.ASSET,
                "返済用銀行",
                "資産",
                new BigDecimal("100000.00"),
                null,
                "",
                null,
                "",
                101L
        ));
        AccountingBalance card = repository.save(new AccountingBalance(
                repaymentDate,
                AccountingBalanceType.LIABILITY,
                "テストカード",
                "負債",
                new BigDecimal("25000.00"),
                27,
                "返済用銀行",
                null,
                "",
                101L
        ));

        assertThat(batch.repayDueCards(repaymentDate)).isEqualTo(1);
        assertThat(batch.repayDueCards(repaymentDate)).isZero();

        AccountingBalance repaidBank = repository.findById(bank.getId()).orElseThrow();
        AccountingBalance repaidCard = repository.findById(card.getId()).orElseThrow();
        assertThat(repaidBank.getAmount()).isEqualByComparingTo("75000.00");
        assertThat(repaidCard.getAmount()).isEqualByComparingTo("0.00");
        assertThat(repaidCard.getLastRepaymentDate()).isEqualTo(repaymentDate);
    }
}
