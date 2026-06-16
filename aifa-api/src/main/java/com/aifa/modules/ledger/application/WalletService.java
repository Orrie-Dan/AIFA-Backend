package com.aifa.modules.ledger.application;

import com.aifa.modules.ledger.application.dto.CreateWalletRequest;
import com.aifa.modules.ledger.application.dto.UpdateWalletRequest;
import com.aifa.modules.ledger.application.dto.WalletResponse;
import com.aifa.modules.ledger.domain.Wallet;
import com.aifa.modules.ledger.infrastructure.WalletRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.BadRequestException;
import com.aifa.shared.exception.ConflictException;
import com.aifa.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public WalletService(WalletRepository walletRepository, AuditLogger auditLogger, Clock clock) {
        this.walletRepository = walletRepository;
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

        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setName(request.name().trim());
        wallet.setType(request.type());
        wallet.setPrimary(request.primary());
        wallet.setBalanceRwf(0L);
        wallet.setCreatedAt(clock.instant());
        wallet.setUpdatedAt(clock.instant());
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
    public WalletResponse updateBalanceFromLedger(UUID walletId, UUID userId, long delta) {
        Wallet wallet = walletRepository
                .findByIdAndUserIdForUpdate(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        long newBalance = wallet.getBalanceRwf() + delta;
        if (newBalance < 0) {
            throw new BadRequestException("Insufficient wallet balance");
        }
        wallet.setBalanceRwf(newBalance);
        wallet.setUpdatedAt(clock.instant());
        walletRepository.save(wallet);
        return toResponse(wallet);
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getName(),
                wallet.getType(),
                wallet.getBalanceRwf(),
                wallet.isPrimary(),
                wallet.getCreatedAt());
    }
}
