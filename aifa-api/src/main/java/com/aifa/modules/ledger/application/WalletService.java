package com.aifa.modules.ledger.application;

import com.aifa.modules.ledger.application.dto.CreateWalletRequest;
import com.aifa.modules.ledger.application.dto.UpdateWalletRequest;
import com.aifa.modules.ledger.application.dto.WalletResponse;
import com.aifa.modules.ledger.domain.Wallet;
import com.aifa.modules.ledger.infrastructure.TransactionRepository;
import com.aifa.modules.ledger.infrastructure.WalletRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.BadRequestException;
import com.aifa.shared.exception.ConflictException;
import com.aifa.shared.exception.ErrorCode;
import com.aifa.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    /** Manual expenses at or after this window are subject to real-time overspend protection. */
    private static final long REAL_TIME_SPEND_WINDOW_DAYS = 1;

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public WalletService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            AuditLogger auditLogger,
            Clock clock) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> listWallets(UUID userId) {
        return walletRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WalletResponse createWallet(UUID userId, CreateWalletRequest request) {
        if (request.primary() && walletRepository.existsByUserIdAndPrimaryTrue(userId)) {
            throw new ConflictException("User already has a primary wallet");
        }

        Instant now = clock.instant();
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setName(request.name().trim());
        wallet.setType(request.type());
        wallet.setPrimary(request.primary());
        wallet.setBalanceRwf(0L);
        wallet.setOpeningBalanceRwf(0L);
        wallet.setLedgerFloorAt(now);
        wallet.setCreatedAt(now);
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        auditLogger.logAction(userId, "CREATE", "wallet", wallet.getId().toString());
        return toResponse(wallet);
    }

    @Transactional
    public WalletResponse updateWallet(UUID userId, UUID walletId, UpdateWalletRequest request) {
        Wallet wallet = walletRepository
                .findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (request.name() != null && !request.name().isBlank()) {
            wallet.setName(request.name().trim());
        }
        if (request.primary() != null) {
            if (request.primary() && !wallet.isPrimary()
                    && walletRepository.existsByUserIdAndPrimaryTrue(userId)) {
                throw new ConflictException("User already has a primary wallet");
            }
            wallet.setPrimary(request.primary());
        }
        if (request.openingBalanceRwf() != null) {
            wallet.setOpeningBalanceRwf(request.openingBalanceRwf());
            recomputeAndPersistBalance(wallet);
        }
        wallet.setUpdatedAt(clock.instant());
        walletRepository.save(wallet);
        auditLogger.logAction(userId, "UPDATE", "wallet", walletId.toString());
        return toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWalletForUser(UUID walletId, UUID userId) {
        return walletRepository
                .findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    @Transactional
    public Wallet getWalletForUserForUpdate(UUID walletId, UUID userId) {
        return walletRepository
                .findByIdAndUserIdForUpdate(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    /**
     * Applies a single manual-transaction delta. Overspend protection applies only to
     * near-now manual spend; backdated entries recompute the cache without blocking.
     */
    @Transactional
    public void applyManualLedgerDelta(UUID walletId, UUID userId, long delta, Instant transactionAt) {
        Wallet wallet = getWalletForUserForUpdate(walletId, userId);
        if (requiresRealTimeSpendGuard(transactionAt)) {
            long newBalance = wallet.getBalanceRwf() + delta;
            if (newBalance < 0) {
                throw new BadRequestException(
                        "Insufficient wallet balance", ErrorCode.INSUFFICIENT_WALLET_BALANCE);
            }
            wallet.setBalanceRwf(newBalance);
        } else {
            recomputeAndPersistBalance(wallet);
        }
        wallet.setUpdatedAt(clock.instant());
        walletRepository.save(wallet);
    }

    @Transactional
    public void recomputeBalanceCache(UUID walletId, UUID userId) {
        Wallet wallet = getWalletForUserForUpdate(walletId, userId);
        recomputeAndPersistBalance(wallet);
        wallet.setUpdatedAt(clock.instant());
        walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public long computeBalanceAt(UUID walletId, UUID userId, Instant upTo) {
        Wallet wallet = getWalletForUser(walletId, userId);
        return wallet.getOpeningBalanceRwf() + transactionRepository.sumAmountByWalletIdUpTo(walletId, upTo);
    }

    boolean requiresRealTimeSpendGuard(Instant transactionAt) {
        Instant windowStart = clock.instant().minus(REAL_TIME_SPEND_WINDOW_DAYS, ChronoUnit.DAYS);
        return !transactionAt.isBefore(windowStart);
    }

    private void recomputeAndPersistBalance(Wallet wallet) {
        long computed = wallet.getOpeningBalanceRwf() + transactionRepository.sumAmountByWalletId(wallet.getId());
        wallet.setBalanceRwf(computed);
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getName(),
                wallet.getType(),
                wallet.getBalanceRwf(),
                wallet.getOpeningBalanceRwf(),
                wallet.getLedgerFloorAt(),
                wallet.isPrimary(),
                wallet.getCreatedAt());
    }
}
