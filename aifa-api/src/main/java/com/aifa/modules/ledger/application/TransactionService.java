package com.aifa.modules.ledger.application;

import com.aifa.modules.ledger.application.dto.CreateTransactionRequest;
import com.aifa.modules.ledger.application.dto.TransactionResponse;
import com.aifa.modules.ledger.application.dto.UpdateTransactionCategoryRequest;
import com.aifa.modules.ledger.domain.CategorySource;
import com.aifa.modules.ledger.domain.Transaction;
import com.aifa.modules.ledger.domain.TransactionSource;
import com.aifa.modules.ledger.domain.TransactionType;
import com.aifa.modules.ledger.infrastructure.CategoryRepository;
import com.aifa.modules.ledger.infrastructure.TransactionRepository;
import com.aifa.modules.ledger.infrastructure.WalletRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.BadRequestException;
import com.aifa.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final long MAX_FUTURE_DAYS = 1;

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final WalletService walletService;
    private final AuditLogger auditLogger;
    private final Clock clock;
    private final MerchantLookupService merchantLookupService;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            CategoryRepository categoryRepository,
            WalletService walletService,
            AuditLogger auditLogger,
            Clock clock,
            MerchantLookupService merchantLookupService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
        this.walletService = walletService;
        this.auditLogger = auditLogger;
        this.clock = clock;
        this.merchantLookupService = merchantLookupService;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(
            UUID userId, Instant from, Instant to, UUID categoryId, Pageable pageable) {
        return transactionRepository
                .findFiltered(userId, from, to, categoryId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public TransactionResponse createTransaction(UUID userId, CreateTransactionRequest request) {
        validateTransactionDate(request.transactionAt());
        walletRepository
                .findByIdAndUserId(request.walletId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        CategorySource categorySource = CategorySource.uncategorized;
        UUID resolvedCategoryId = request.categoryId();
        if (resolvedCategoryId != null) {
            categoryRepository
                    .findById(resolvedCategoryId)
                    .filter(category -> category.getUserId() == null || category.getUserId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            categorySource = CategorySource.user;
        } else if (request.merchantName() != null && !request.merchantName().isBlank()) {
            var merchantMatch = merchantLookupService.resolveCategory(request.merchantName());
            if (merchantMatch.isPresent()) {
                resolvedCategoryId = merchantMatch.get().categoryId();
                categorySource = merchantMatch.get().source();
            }
        }

        long signedAmount = toSignedAmount(request.amountRwf(), request.type());

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setWalletId(request.walletId());
        transaction.setAmountRwf(signedAmount);
        transaction.setType(request.type());
        transaction.setCategoryId(resolvedCategoryId);
        transaction.setCategorySource(categorySource);
        transaction.setMerchantName(request.merchantName());
        transaction.setDescription(request.description());
        transaction.setTransactionAt(request.transactionAt());
        transaction.setSource(TransactionSource.manual);
        transaction.setCreatedAt(clock.instant());
        transactionRepository.save(transaction);

        walletService.updateBalanceFromLedger(request.walletId(), userId, signedAmount);
        auditLogger.logAction(userId, "CREATE", "transaction", transaction.getId().toString());
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse createImportedTransaction(
            UUID userId,
            UUID walletId,
            long signedAmount,
            TransactionType type,
            UUID categoryId,
            String merchantName,
            String description,
            Instant transactionAt) {
        validateTransactionDate(transactionAt);
        walletRepository
                .findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        CategorySource categorySource = CategorySource.uncategorized;
        UUID resolvedCategoryId = categoryId;
        if (resolvedCategoryId != null) {
            categoryRepository
                    .findById(resolvedCategoryId)
                    .filter(category -> category.getUserId() == null || category.getUserId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            categorySource = CategorySource.user;
        } else if (merchantName != null && !merchantName.isBlank()) {
            var merchantMatch = merchantLookupService.resolveCategory(merchantName);
            if (merchantMatch.isPresent()) {
                resolvedCategoryId = merchantMatch.get().categoryId();
                categorySource = merchantMatch.get().source();
            }
        }

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setWalletId(walletId);
        transaction.setAmountRwf(signedAmount);
        transaction.setType(type);
        transaction.setCategoryId(resolvedCategoryId);
        transaction.setCategorySource(categorySource);
        transaction.setMerchantName(merchantName);
        transaction.setDescription(description);
        transaction.setTransactionAt(transactionAt);
        transaction.setSource(TransactionSource.sms_import);
        transaction.setCreatedAt(clock.instant());
        transactionRepository.save(transaction);

        walletService.updateBalanceFromLedger(walletId, userId, signedAmount);
        return toResponse(transaction);
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        walletService.updateBalanceFromLedger(
                transaction.getWalletId(), userId, -transaction.getAmountRwf());
        transactionRepository.delete(transaction);
        auditLogger.logAction(userId, "DELETE", "transaction", transactionId.toString());
    }

    @Transactional
    public TransactionResponse updateCategory(
            UUID userId, UUID transactionId, UpdateTransactionCategoryRequest request) {
        Transaction transaction = transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        categoryRepository
                .findById(request.categoryId())
                .filter(category -> category.getUserId() == null || category.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        transaction.setCategoryId(request.categoryId());
        transaction.setCategorySource(CategorySource.user);
        transactionRepository.save(transaction);
        auditLogger.logAction(userId, "UPDATE", "transaction_category", transactionId.toString());
        return toResponse(transaction);
    }

    private void validateTransactionDate(Instant transactionAt) {
        Instant maxFuture = clock.instant().plus(MAX_FUTURE_DAYS, ChronoUnit.DAYS);
        if (transactionAt.isAfter(maxFuture)) {
            throw new BadRequestException("Transaction date cannot be far in the future");
        }
    }

    private long toSignedAmount(long amountRwf, TransactionType type) {
        return switch (type) {
            case income -> amountRwf;
            case expense -> -amountRwf;
            case transfer -> amountRwf;
        };
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getWalletId(),
                transaction.getAmountRwf(),
                transaction.getType(),
                transaction.getCategoryId(),
                transaction.getCategorySource(),
                transaction.getMerchantName(),
                transaction.getDescription(),
                transaction.getTransactionAt(),
                transaction.getSource(),
                transaction.getCreatedAt());
    }
}
