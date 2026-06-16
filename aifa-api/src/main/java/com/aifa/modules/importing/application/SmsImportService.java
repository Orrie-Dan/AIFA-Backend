package com.aifa.modules.importing.application;

import com.aifa.modules.importing.application.dto.ParsedSmsRow;
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
import com.aifa.modules.ledger.application.TransactionService;
import com.aifa.modules.ledger.application.WalletService;
import com.aifa.modules.ledger.domain.TransactionType;
import com.aifa.modules.ledger.infrastructure.CategoryRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.BadRequestException;
import com.aifa.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsImportService {

    private final SmsParseService smsParseService;
    private final SmsImportBatchRepository batchRepository;
    private final SmsParsedRowRepository rowRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public SmsImportService(
            SmsParseService smsParseService,
            SmsImportBatchRepository batchRepository,
            SmsParsedRowRepository rowRepository,
            WalletService walletService,
            TransactionService transactionService,
            CategoryRepository categoryRepository,
            AuditLogger auditLogger,
            Clock clock) {
        this.smsParseService = smsParseService;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.categoryRepository = categoryRepository;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional
    public SmsImportPreviewResponse preview(UUID userId, SmsImportPreviewRequest request) {
        if (request.walletId() != null) {
            walletService.getWalletForUser(request.walletId(), userId);
        }

        List<ParsedSmsRow> parsed = smsParseService.parseBatch(request.messages(), clock.instant());

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
                batch.getId(), batch.getStatus(), batch.getParserVersion(), parsed, batch.getCreatedAt());
    }

    @Transactional
    public SmsImportConfirmResponse confirm(UUID userId, SmsImportConfirmRequest request) {
        SmsImportBatch batch = batchRepository
                .findById(request.batchId())
                .filter(b -> b.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Import batch not found"));

        if (batch.getStatus() != SmsImportStatus.preview) {
            throw new BadRequestException("Import batch already processed");
        }
        if (batch.getWalletId() == null) {
            throw new BadRequestException("Wallet must be set before confirming import");
        }

        if (request.categoryId() != null) {
            categoryRepository
                    .findById(request.categoryId())
                    .filter(category -> category.getUserId() == null || category.getUserId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Set<Integer> selected = Set.copyOf(request.rowIndexes());
        List<SmsParsedRow> rows = rowRepository.findByBatchIdOrderByRowIndexAsc(batch.getId());
        List<UUID> transactionIds = new ArrayList<>();

        for (SmsParsedRow row : rows) {
            if (!selected.contains(row.getRowIndex())) {
                continue;
            }
            if (row.getStatus() != SmsRowStatus.pending || row.getAmountRwf() == null) {
                throw new BadRequestException("Row " + row.getRowIndex() + " cannot be imported");
            }

            TransactionType type = "credit".equals(row.getDirection()) ? TransactionType.income : TransactionType.expense;
            long signedAmount = "credit".equals(row.getDirection()) ? row.getAmountRwf() : -row.getAmountRwf();

            var transaction = transactionService.createImportedTransaction(
                    userId,
                    batch.getWalletId(),
                    signedAmount,
                    type,
                    request.categoryId(),
                    row.getSenderName(),
                    "MoMo SMS import",
                    row.getTransactionAt());

            row.setStatus(SmsRowStatus.confirmed);
            row.setTransactionId(transaction.id());
            rowRepository.save(row);
            transactionIds.add(transaction.id());
        }

        batch.setStatus(SmsImportStatus.confirmed);
        batch.setConfirmedAt(clock.instant());
        batchRepository.save(batch);

        auditLogger.logAction(userId, "CONFIRM", "sms_import", batch.getId().toString());
        return new SmsImportConfirmResponse(batch.getId(), transactionIds.size(), transactionIds);
    }
}
