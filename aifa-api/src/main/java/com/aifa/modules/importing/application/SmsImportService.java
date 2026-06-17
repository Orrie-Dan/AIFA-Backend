package com.aifa.modules.importing.application;

import com.aifa.modules.importing.application.dto.ParsedSmsRow;
import com.aifa.modules.importing.application.dto.SmsBalanceDiscrepancy;
import com.aifa.modules.importing.application.dto.SmsImportConfirmRequest;
import com.aifa.modules.importing.application.dto.SmsImportConfirmResponse;
import com.aifa.modules.importing.application.dto.SmsImportPreviewRequest;
import com.aifa.modules.importing.application.dto.SmsImportPreviewResponse;
import com.aifa.modules.importing.domain.SmsImportBatch;
import com.aifa.modules.importing.domain.SmsImportStatus;
import com.aifa.modules.importing.domain.SmsParsedRow;
import com.aifa.modules.importing.domain.SmsRowStatus;
import com.aifa.modules.importing.infrastructure.SmsImportBatchRepository;
import com.aifa.modules.importing.infrastructure.SmsParsedRowRepository;
import com.aifa.modules.ledger.application.OpeningBalanceCalculator;
import com.aifa.modules.ledger.application.TransactionService;
import com.aifa.modules.ledger.application.WalletService;
import com.aifa.modules.ledger.domain.TransactionType;
import com.aifa.modules.ledger.domain.Wallet;
import com.aifa.modules.ledger.infrastructure.CategoryRepository;
import com.aifa.modules.ledger.infrastructure.TransactionRepository;
import com.aifa.modules.ledger.infrastructure.WalletRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.BadRequestException;
import com.aifa.shared.exception.ErrorCode;
import com.aifa.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsImportService {

    private static final Logger log = LoggerFactory.getLogger(SmsImportService.class);
    private static final long BALANCE_TOLERANCE_RWF = 100L;

    private final SmsParseService smsParseService;
    private final SmsImportBatchRepository batchRepository;
    private final SmsParsedRowRepository rowRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public SmsImportService(
            SmsParseService smsParseService,
            SmsImportBatchRepository batchRepository,
            SmsParsedRowRepository rowRepository,
            WalletService walletService,
            WalletRepository walletRepository,
            TransactionService transactionService,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            AuditLogger auditLogger,
            Clock clock) {
        this.smsParseService = smsParseService;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional
    public SmsImportPreviewResponse preview(UUID userId, SmsImportPreviewRequest request) {
        if (request.walletId() == null) {
            throw new BadRequestException("Wallet is required for SMS import preview", ErrorCode.WALLET_REQUIRED);
        }
        walletService.getWalletForUser(request.walletId(), userId);

        List<ParsedSmsRow> parsed = smsParseService.parseBatch(request.messages(), clock.instant());
        OpeningSuggestion suggestion = computeOpeningSuggestion(parsed);

        SmsImportBatch batch = new SmsImportBatch();
        batch.setUserId(userId);
        batch.setWalletId(request.walletId());
        batch.setRawMessages(request.messages());
        batch.setParserVersion(SmsParseService.PARSER_VERSION);
        batch.setStatus(SmsImportStatus.preview);
        batch.setCreatedAt(clock.instant());
        batchRepository.save(batch);

        List<SmsParsedRow> entities = new ArrayList<>();
        for (ParsedSmsRow row : parsed) {
            SmsParsedRow entity = new SmsParsedRow();
            entity.setBatchId(batch.getId());
            entity.setRowIndex(row.rowIndex());
            entity.setRawText(row.rawText());
            entity.setAmountRwf(row.amountRwf());
            entity.setSenderName(row.counterpartyName());
            entity.setPhoneHash(row.phoneHash());
            entity.setBalanceRwf(row.balanceRwf());
            entity.setTransactionAt(row.transactionAt() != null ? row.transactionAt() : clock.instant());
            entity.setDirection(row.direction());
            entity.setParseError(row.parseError());
            entity.setStatus(row.parsed() ? SmsRowStatus.pending : SmsRowStatus.skipped);
            entity.setCreatedAt(clock.instant());
            entities.add(entity);
        }
        rowRepository.saveAll(entities);

        auditLogger.logAction(userId, "PREVIEW", "sms_import", batch.getId().toString());
        return new SmsImportPreviewResponse(
                batch.getId(),
                batch.getStatus(),
                batch.getParserVersion(),
                parsed,
                batch.getCreatedAt(),
                suggestion.openingBalanceRwf(),
                suggestion.anchorRowIndex());
    }

    @Transactional
    public SmsImportConfirmResponse confirm(UUID userId, SmsImportConfirmRequest request) {
        SmsImportBatch batch = batchRepository
                .findById(request.batchId())
                .filter(b -> b.getUserId().equals(userId))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Import batch not found", ErrorCode.IMPORT_BATCH_NOT_FOUND));

        if (batch.getStatus() != SmsImportStatus.preview && batch.getStatus() != SmsImportStatus.confirmed) {
            throw new BadRequestException("Import batch already processed", ErrorCode.IMPORT_BATCH_EXPIRED);
        }

        UUID walletId = batch.getWalletId();
        if (walletId == null) {
            walletId = request.walletId();
        }
        if (walletId == null) {
            throw new BadRequestException(
                    "Wallet must be set before confirming import", ErrorCode.WALLET_REQUIRED);
        }
        if (batch.getWalletId() == null) {
            batch.setWalletId(walletId);
            batchRepository.save(batch);
        }

        if (request.categoryId() != null) {
            categoryRepository
                    .findById(request.categoryId())
                    .filter(category -> category.getUserId() == null || category.getUserId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Set<Integer> selected = Set.copyOf(request.rowIndexes());
        List<SmsParsedRow> allRows = rowRepository.findByBatchIdOrderByRowIndexAsc(batch.getId());
        List<SmsParsedRow> selectedRows = allRows.stream()
                .filter(row -> selected.contains(row.getRowIndex()))
                .sorted(Comparator.comparing(SmsParsedRow::getTransactionAt))
                .toList();

        List<Integer> alreadyImportedRowIndexes = new ArrayList<>();
        List<SmsParsedRow> newRows = new ArrayList<>();
        for (SmsParsedRow row : selectedRows) {
            if (row.getStatus() == SmsRowStatus.confirmed) {
                alreadyImportedRowIndexes.add(row.getRowIndex());
                continue;
            }
            if (row.getStatus() != SmsRowStatus.pending || row.getAmountRwf() == null) {
                throw new BadRequestException("Row " + row.getRowIndex() + " cannot be imported");
            }
            newRows.add(row);
        }

        Wallet wallet = walletService.getWalletForUserForUpdate(walletId, userId);
        long existingCount = transactionRepository.countByWalletId(walletId);

        if (!newRows.isEmpty()) {
            adjustOpeningBalance(wallet, newRows, existingCount);
            walletRepository.save(wallet);
        }

        List<UUID> transactionIds = new ArrayList<>();
        for (SmsParsedRow row : selectedRows) {
            if (row.getStatus() == SmsRowStatus.confirmed && row.getTransactionId() != null) {
                transactionIds.add(row.getTransactionId());
            }
        }

        for (SmsParsedRow row : newRows) {
            String fingerprint = SmsImportFingerprint.compute(
                    walletId,
                    row.getAmountRwf(),
                    row.getDirection(),
                    row.getSenderName(),
                    row.getTransactionAt(),
                    row.getBalanceRwf());

            TransactionType type = isIncome(row.getDirection()) ? TransactionType.income : TransactionType.expense;
            long signedAmount =
                    isIncome(row.getDirection()) ? row.getAmountRwf() : -row.getAmountRwf();

            var transaction = transactionService.createImportedTransaction(
                    userId,
                    walletId,
                    signedAmount,
                    type,
                    request.categoryId(),
                    row.getSenderName(),
                    "MoMo SMS import",
                    row.getTransactionAt(),
                    fingerprint,
                    row.getBalanceRwf());

            if (row.getStatus() != SmsRowStatus.confirmed) {
                row.setStatus(SmsRowStatus.confirmed);
                row.setTransactionId(transaction.id());
                rowRepository.save(row);
            }
            if (!transactionIds.contains(transaction.id())) {
                transactionIds.add(transaction.id());
            }
        }

        if (!newRows.isEmpty()) {
            walletService.recomputeBalanceCache(walletId, userId);
        }

        wallet = walletService.getWalletForUser(walletId, userId);
        List<SmsBalanceDiscrepancy> discrepancies = computeDiscrepancies(wallet, newRows);

        if (batch.getStatus() != SmsImportStatus.confirmed) {
            batch.setStatus(SmsImportStatus.confirmed);
            batch.setConfirmedAt(clock.instant());
            batchRepository.save(batch);
        }

        auditLogger.logAction(userId, "CONFIRM", "sms_import", batch.getId().toString());
        return new SmsImportConfirmResponse(
                batch.getId(),
                newRows.size(),
                transactionIds,
                wallet.getOpeningBalanceRwf(),
                wallet.getBalanceRwf(),
                wallet.getLedgerFloorAt(),
                discrepancies,
                alreadyImportedRowIndexes);
    }

    private void adjustOpeningBalance(Wallet wallet, List<SmsParsedRow> newRows, long existingCount) {
        Instant earliestNewTxnAt =
                newRows.stream().map(SmsParsedRow::getTransactionAt).min(Instant::compareTo).orElseThrow();
        Optional<SmsParsedRow> anchorRow = OpeningBalanceCalculator.findAnchorRow(newRows);

        if (earliestNewTxnAt.isBefore(wallet.getLedgerFloorAt()) && existingCount > 0) {
            OpeningBalanceCalculator.applyBackfillFloorShift(wallet, newRows, earliestNewTxnAt);
            if (anchorRow.isPresent()) {
                long existingSum = transactionRepository.sumAmountByWalletIdUpTo(
                        wallet.getId(), anchorRow.get().getTransactionAt());
                long recomputed = OpeningBalanceCalculator.deriveOpeningFromAnchor(
                        anchorRow.get(), newRows, existingSum);
                if (Math.abs(recomputed - wallet.getOpeningBalanceRwf()) > BALANCE_TOLERANCE_RWF) {
                    log.warn(
                            "Backfill anchor mismatch for wallet {}: stored={} recomputed={}",
                            wallet.getId(),
                            wallet.getOpeningBalanceRwf(),
                            recomputed);
                }
            }
        } else if (OpeningBalanceCalculator.isFirstImport(wallet, existingCount) && anchorRow.isPresent()) {
            wallet.setOpeningBalanceRwf(
                    OpeningBalanceCalculator.deriveOpeningFromAnchor(anchorRow.get(), newRows, 0L));
            wallet.setLedgerFloorAt(earliestNewTxnAt);
        }
    }

    private List<SmsBalanceDiscrepancy> computeDiscrepancies(Wallet wallet, List<SmsParsedRow> newRows) {
        List<SmsBalanceDiscrepancy> discrepancies = new ArrayList<>();
        for (SmsParsedRow row : newRows) {
            if (row.getBalanceRwf() == null) {
                continue;
            }
            long expected = wallet.getOpeningBalanceRwf()
                    + transactionRepository.sumAmountByWalletIdUpTo(wallet.getId(), row.getTransactionAt());
            long delta = expected - row.getBalanceRwf();
            if (Math.abs(delta) > BALANCE_TOLERANCE_RWF) {
                discrepancies.add(new SmsBalanceDiscrepancy(
                        row.getRowIndex(), row.getBalanceRwf(), expected, delta));
            }
        }
        return discrepancies;
    }

    private OpeningSuggestion computeOpeningSuggestion(List<ParsedSmsRow> parsed) {
        List<ParsedSmsRow> importable = parsed.stream().filter(ParsedSmsRow::parsed).toList();
        Optional<ParsedSmsRow> anchor = importable.stream()
                .filter(row -> row.balanceRwf() != null)
                .min(Comparator.comparing(ParsedSmsRow::transactionAt));
        if (anchor.isEmpty()) {
            return new OpeningSuggestion(null, null);
        }
        long sumToAnchor = importable.stream()
                .filter(row -> !row.transactionAt().isAfter(anchor.get().transactionAt()))
                .mapToLong(this::signedAmountFromParsed)
                .sum();
        return new OpeningSuggestion(anchor.get().balanceRwf() - sumToAnchor, anchor.get().rowIndex());
    }

    private long signedAmountFromParsed(ParsedSmsRow row) {
        return isIncome(row.direction()) ? row.amountRwf() : -row.amountRwf();
    }

    private boolean isIncome(String direction) {
        if (direction == null) {
            return false;
        }
        return "IN".equalsIgnoreCase(direction)
                || "credit".equalsIgnoreCase(direction)
                || "income".equalsIgnoreCase(direction);
    }

    private record OpeningSuggestion(Long openingBalanceRwf, Integer anchorRowIndex) {}
}
