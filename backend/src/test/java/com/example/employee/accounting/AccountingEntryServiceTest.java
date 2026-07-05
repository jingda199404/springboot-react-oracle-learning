package com.example.employee.accounting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.employee.error.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AccountingEntryServiceTest {

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;

    @Autowired
    private AccountingEntryService entryService;

    @Autowired
    private AccountingBalanceService balanceService;

    @Autowired
    private AccountingEntryRepository entryRepository;

    @Autowired
    private AccountingBalanceRepository balanceRepository;

    @BeforeEach
    void setUp() {
        entryRepository.deleteAll();
        balanceRepository.deleteAll();
    }

    @Test
    void entryChangesAreAppliedToBalances() {
        AccountingBalance bank = balanceRepository.save(new AccountingBalance(
                LocalDate.now(),
                AccountingBalanceType.ASSET,
                "銀行預金",
                "資産",
                new BigDecimal("1000.00"),
                null,
                "",
                null,
                "",
                USER_ID
        ));
        AccountingBalance card = balanceRepository.save(new AccountingBalance(
                LocalDate.now(),
                AccountingBalanceType.LIABILITY,
                "Dカード",
                "負債",
                new BigDecimal("0.00"),
                10,
                "銀行預金",
                null,
                "",
                USER_ID
        ));

        AccountingEntry entry = entryService.create(new AccountingEntryRequest(
                LocalDate.now(),
                AccountingEntryType.EXPENSE,
                "食費",
                new BigDecimal("300.00"),
                "銀行預金",
                ""
        ), USER_ID);

        assertThat(balanceRepository.findById(bank.getId()).orElseThrow().getAmount()).isEqualByComparingTo("700.00");
        assertThat(balanceRepository.findById(card.getId()).orElseThrow().getAmount()).isEqualByComparingTo("0.00");

        entryService.update(entry.getId(), new AccountingEntryRequest(
                LocalDate.now(),
                AccountingEntryType.EXPENSE,
                "通信",
                new BigDecimal("500.00"),
                "Dカード",
                ""
        ), USER_ID);

        assertThat(balanceRepository.findById(bank.getId()).orElseThrow().getAmount()).isEqualByComparingTo("1000.00");
        assertThat(balanceRepository.findById(card.getId()).orElseThrow().getAmount()).isEqualByComparingTo("500.00");

        entryService.delete(entry.getId(), USER_ID);

        assertThat(balanceRepository.findById(bank.getId()).orElseThrow().getAmount()).isEqualByComparingTo("1000.00");
        assertThat(balanceRepository.findById(card.getId()).orElseThrow().getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void accountingDataIsSeparatedByUserId() {
        AccountingBalance userBank = balanceRepository.save(new AccountingBalance(
                LocalDate.now(),
                AccountingBalanceType.ASSET,
                "銀行預金",
                "資産",
                new BigDecimal("1000.00"),
                null,
                "",
                null,
                "",
                USER_ID
        ));
        AccountingBalance otherUserBank = balanceRepository.save(new AccountingBalance(
                LocalDate.now(),
                AccountingBalanceType.ASSET,
                "銀行預金",
                "資産",
                new BigDecimal("5000.00"),
                null,
                "",
                null,
                "",
                OTHER_USER_ID
        ));

        AccountingEntry userEntry = entryService.create(new AccountingEntryRequest(
                LocalDate.now(),
                AccountingEntryType.EXPENSE,
                "食費",
                new BigDecimal("200.00"),
                "銀行預金",
                ""
        ), USER_ID);
        AccountingEntry otherUserEntry = entryService.create(new AccountingEntryRequest(
                LocalDate.now(),
                AccountingEntryType.EXPENSE,
                "食費",
                new BigDecimal("800.00"),
                "銀行預金",
                ""
        ), OTHER_USER_ID);

        assertThat(entryService.findAll(USER_ID)).extracting(AccountingEntry::getId).containsExactly(userEntry.getId());
        assertThat(entryService.findAll(OTHER_USER_ID)).extracting(AccountingEntry::getId).containsExactly(otherUserEntry.getId());
        assertThat(balanceService.findAll(USER_ID)).extracting(AccountingBalance::getId).containsExactly(userBank.getId());
        assertThat(balanceService.findAll(OTHER_USER_ID)).extracting(AccountingBalance::getId).containsExactly(otherUserBank.getId());
        assertThat(balanceRepository.findById(userBank.getId()).orElseThrow().getAmount()).isEqualByComparingTo("800.00");
        assertThat(balanceRepository.findById(otherUserBank.getId()).orElseThrow().getAmount()).isEqualByComparingTo("4200.00");

        assertThatThrownBy(() -> entryService.findById(userEntry.getId(), OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> balanceService.findById(userBank.getId(), OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
