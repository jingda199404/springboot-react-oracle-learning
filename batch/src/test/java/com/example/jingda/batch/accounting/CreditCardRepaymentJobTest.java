package com.example.jingda.batch.accounting;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.batch.target-date=2099-01-01")
class CreditCardRepaymentJobTest {

    @Autowired
    private AccountingBalanceRepository repository;

    @Autowired
    private CreditCardRepaymentJob job;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

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

        assertThat(job.repayDueCards(repaymentDate)).isEqualTo(1);
        assertThat(job.repayDueCards(repaymentDate)).isZero();

        AccountingBalance repaidBank = repository.findById(bank.getId()).orElseThrow();
        AccountingBalance repaidCard = repository.findById(card.getId()).orElseThrow();
        assertThat(repaidBank.getAmount()).isEqualByComparingTo("75000.00");
        assertThat(repaidCard.getAmount()).isEqualByComparingTo("0.00");
        assertThat(repaidCard.getLastRepaymentDate()).isEqualTo(repaymentDate);
    }
}
