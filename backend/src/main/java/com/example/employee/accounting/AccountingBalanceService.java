package com.example.employee.accounting;

import com.example.employee.error.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountingBalanceService {

    private final AccountingBalanceRepository repository;

    public AccountingBalanceService(AccountingBalanceRepository repository) {
        this.repository = repository;
    }

    public List<AccountingBalance> findAll(Long userId) {
        return repository.findByUserId(userId, Sort.by(Sort.Direction.ASC, "type").and(Sort.by(Sort.Direction.ASC, "id")));
    }

    public AccountingBalance findById(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("資産・負債データが見つかりません：" + id));
    }

    @Transactional
    public AccountingBalance create(AccountingBalanceRequest request, Long userId) {
        return repository.save(new AccountingBalance(
                LocalDate.now(),
                request.type(),
                request.accountName(),
                defaultCategory(request.type()),
                request.amount(),
                repaymentDay(request),
                repaymentAccountName(request, userId),
                null,
                cleanMemo(request.memo()),
                userId
        ));
    }

    @Transactional
    public AccountingBalance update(Long id, AccountingBalanceRequest request, Long userId) {
        AccountingBalance balance = findById(id, userId);
        balance.setAsOfDate(LocalDate.now());
        balance.setType(request.type());
        balance.setAccountName(request.accountName());
        balance.setCategory(defaultCategory(request.type()));
        balance.setAmount(request.amount());
        balance.setRepaymentDay(repaymentDay(request));
        balance.setRepaymentAccountName(repaymentAccountName(request, userId));
        balance.setMemo(cleanMemo(request.memo()));
        return balance;
    }

    @Transactional
    public void delete(Long id, Long userId) {
        repository.delete(findById(id, userId));
    }

    private String cleanMemo(String memo) {
        if (memo == null || memo.isBlank()) {
            return "";
        }
        return memo.trim();
    }

    private String defaultCategory(AccountingBalanceType type) {
        return type == AccountingBalanceType.ASSET ? "資産" : "負債";
    }

    private Integer repaymentDay(AccountingBalanceRequest request) {
        if (request.type() != AccountingBalanceType.LIABILITY) {
            return null;
        }
        return request.repaymentDay();
    }

    private String repaymentAccountName(AccountingBalanceRequest request, Long userId) {
        if (request.type() != AccountingBalanceType.LIABILITY) {
            return "";
        }
        String repaymentAccountName = cleanMemo(request.repaymentAccountName());
        if (request.repaymentDay() != null && repaymentAccountName.isBlank()) {
            throw new IllegalArgumentException("返済日を設定する場合は返済口座を選択してください");
        }
        if (!repaymentAccountName.isBlank()) {
            repository.findFirstByUserIdAndTypeAndAccountName(userId, AccountingBalanceType.ASSET, repaymentAccountName)
                    .orElseThrow(() -> new IllegalArgumentException("返済口座は登録済みの資産口座から選択してください"));
        }
        return repaymentAccountName;
    }
}
