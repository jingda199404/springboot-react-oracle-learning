package com.example.employee.stock;

import com.example.employee.auth.AuthService;
import com.example.employee.auth.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/stock-trades")
public class StockTradeController {

    private final StockTradeService service;
    private final CurrentUser currentUser;

    public StockTradeController(StockTradeService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public StockTradePageResponse findAll(
            @RequestParam(required = false) Integer year,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return service.findAll(currentUser.requirePermission(userId, AuthService.STOCK_TRADING_PERMISSION), year);
    }

    @PostMapping("/import")
    public StockTradeImportResponse importExcel(
            @RequestParam Integer year,
            @RequestParam MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return service.importExcel(file, year, currentUser.requirePermission(userId, AuthService.STOCK_TRADING_PERMISSION));
    }

    @PostMapping
    public StockTrade create(
            @RequestBody StockTradeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return service.create(request, currentUser.requirePermission(userId, AuthService.STOCK_TRADING_PERMISSION));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        service.delete(id, currentUser.requirePermission(userId, AuthService.STOCK_TRADING_PERMISSION));
        return ResponseEntity.noContent().build();
    }
}
