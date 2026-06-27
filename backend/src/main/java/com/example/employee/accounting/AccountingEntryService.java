package com.example.employee.accounting;

import com.example.employee.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountingEntryService {

    private final AccountingEntryRepository repository;

    public AccountingEntryService(AccountingEntryRepository repository) {
        this.repository = repository;
    }

    public List<AccountingEntry> findAll(Long userId) {
        return repository.findByUserId(userId, Sort.by(Sort.Direction.DESC, "entryDate").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    public AccountingEntry findById(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("記帳データが見つかりません：" + id));
    }

    @Transactional
    public AccountingEntry create(AccountingEntryRequest request, Long userId) {
        return repository.save(new AccountingEntry(
                request.entryDate(),
                request.type(),
                request.category(),
                request.amount(),
                request.paymentMethod(),
                cleanMemo(request.memo()),
                userId
        ));
    }

    @Transactional
    public AccountingEntry update(Long id, AccountingEntryRequest request, Long userId) {
        AccountingEntry entry = findById(id, userId);
        entry.setEntryDate(request.entryDate());
        entry.setType(request.type());
        entry.setCategory(request.category());
        entry.setAmount(request.amount());
        entry.setPaymentMethod(request.paymentMethod());
        entry.setMemo(cleanMemo(request.memo()));
        return entry;
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
}
