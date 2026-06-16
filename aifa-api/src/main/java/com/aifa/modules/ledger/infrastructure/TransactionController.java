package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.application.TransactionService;
import com.aifa.modules.ledger.application.dto.CreateTransactionRequest;
import com.aifa.modules.ledger.application.dto.TransactionResponse;
import com.aifa.modules.ledger.application.dto.UpdateTransactionCategoryRequest;
import com.aifa.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserProvider currentUserProvider;

    public TransactionController(TransactionService transactionService, CurrentUserProvider currentUserProvider) {
        this.transactionService = transactionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public Page<TransactionResponse> listTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID category,
            @PageableDefault(size = 20) Pageable pageable) {
        return transactionService.listTransactions(
                currentUserProvider.getCurrentUserId(), from, to, category, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.createTransaction(currentUserProvider.getCurrentUserId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable UUID id) {
        transactionService.deleteTransaction(currentUserProvider.getCurrentUserId(), id);
    }

    @PatchMapping("/{id}/category")
    public TransactionResponse updateCategory(
            @PathVariable UUID id, @Valid @RequestBody UpdateTransactionCategoryRequest request) {
        return transactionService.updateCategory(currentUserProvider.getCurrentUserId(), id, request);
    }
}
