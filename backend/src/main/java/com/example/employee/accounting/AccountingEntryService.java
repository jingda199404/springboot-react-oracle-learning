package com.example.employee.accounting;

import com.example.employee.common.TextUtils;
import com.example.employee.error.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountingEntryService {

    private static final Logger log = LoggerFactory.getLogger(AccountingEntryService.class);

    private final AccountingEntryRepository repository;
    private final AccountingBalanceRepository balanceRepository;

    public AccountingEntryService(AccountingEntryRepository repository, AccountingBalanceRepository balanceRepository) {
        this.repository = repository;
        this.balanceRepository = balanceRepository;
    }

    public List<AccountingEntry> findAll(Long userId) {
        return repository.findByUserId(userId);
    }

    public AccountingEntry findById(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("記帳データが見つかりません：" + id));
    }

    @Transactional
    public AccountingEntry create(AccountingEntryRequest request, Long userId) {
        AccountingEntry created = repository.save(new AccountingEntry(
                request.entryDate(),
                request.type(),
                request.category(),
                request.amount(),
                request.paymentMethod(),
                TextUtils.trimToEmpty(request.memo()),
                userId
        ));
        applyBalanceImpact(created.getType(), created.getAmount(), created.getPaymentMethod(), userId, false);
        log.info("Accounting entry created. userId={}, entryId={}, type={}, amount={}, paymentMethod={}",
                userId, created.getId(), created.getType(), created.getAmount(), created.getPaymentMethod());
        return created;
    }

    @Transactional
    public AccountingEntry update(Long id, AccountingEntryRequest request, Long userId) {
        AccountingEntry entry = findById(id, userId);
        applyBalanceImpact(entry.getType(), entry.getAmount(), entry.getPaymentMethod(), userId, true);
        entry.setEntryDate(request.entryDate());
        entry.setType(request.type());
        entry.setCategory(request.category());
        entry.setAmount(request.amount());
        entry.setPaymentMethod(request.paymentMethod());
        entry.setMemo(TextUtils.trimToEmpty(request.memo()));
        entry = repository.save(entry);
        applyBalanceImpact(entry.getType(), entry.getAmount(), entry.getPaymentMethod(), userId, false);
        log.info("Accounting entry updated. userId={}, entryId={}, type={}, amount={}, paymentMethod={}",
                userId, entry.getId(), entry.getType(), entry.getAmount(), entry.getPaymentMethod());
        return entry;
    }

    @Transactional
    public void delete(Long id, Long userId) {
        AccountingEntry entry = findById(id, userId);
        applyBalanceImpact(entry.getType(), entry.getAmount(), entry.getPaymentMethod(), userId, true);
        repository.delete(entry);
        log.info("Accounting entry deleted. userId={}, entryId={}", userId, id);
    }

    private void applyBalanceImpact(AccountingEntryType entryType, BigDecimal amount, String paymentMethod, Long userId, boolean reverse) {
        String accountName = TextUtils.requireText(paymentMethod, "入出金方法を選択してください");
        AccountingBalance balance = balanceRepository.findFirstByUserIdAndAccountName(userId, accountName)
                .orElseThrow(() -> new IllegalArgumentException("入出金方法に対応する資産・負債データが見つかりません：" + accountName));
        BigDecimal delta = AccountingAmountUtils.balanceDelta(entryType, balance.getType(), amount);
        if (reverse) {
            delta = delta.negate();
        }
        balance.setAmount(balance.getAmount().add(delta));
        balance.setAsOfDate(LocalDate.now());
        balanceRepository.save(balance);
        log.debug("Accounting balance adjusted. userId={}, balanceId={}, accountName={}, delta={}, reverse={}",
                userId, balance.getId(), balance.getAccountName(), delta, reverse);
    }
}
