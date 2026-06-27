package com.example.employee.accounting;

import com.example.employee.auth.AuthService;
import com.example.employee.auth.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounting-entries")
public class AccountingEntryController {

    private final AccountingEntryService service;
    private final CurrentUser currentUser;

    public AccountingEntryController(AccountingEntryService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<AccountingEntry> findAll(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return service.findAll(currentUser.requirePermission(userId, AuthService.ACCOUNTING_PERMISSION));
    }

    @GetMapping("/{id}")
    public AccountingEntry findById(@PathVariable Long id, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return service.findById(id, currentUser.requirePermission(userId, AuthService.ACCOUNTING_PERMISSION));
    }

    @PostMapping
    public ResponseEntity<AccountingEntry> create(@Valid @RequestBody AccountingEntryRequest request, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        AccountingEntry created = service.create(request, currentUser.requirePermission(userId, AuthService.ACCOUNTING_PERMISSION));
        return ResponseEntity.created(URI.create("/api/accounting-entries/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public AccountingEntry update(@PathVariable Long id, @Valid @RequestBody AccountingEntryRequest request, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return service.update(id, request, currentUser.requirePermission(userId, AuthService.ACCOUNTING_PERMISSION));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        service.delete(id, currentUser.requirePermission(userId, AuthService.ACCOUNTING_PERMISSION));
        return ResponseEntity.noContent().build();
    }
}
