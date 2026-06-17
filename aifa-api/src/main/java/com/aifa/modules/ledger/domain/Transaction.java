package com.aifa.modules.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "amount_rwf", nullable = false)
    private long amountRwf;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_type")
    private TransactionType type;

    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "category_source", nullable = false, columnDefinition = "category_source")
    private CategorySource categorySource = CategorySource.uncategorized;

    @Column(name = "merchant_name")
    private String merchantName;

    private String description;

    @Column(name = "transaction_at", nullable = false)
    private Instant transactionAt;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    @Column(name = "is_flagged", nullable = false)
    private boolean flagged;

    @Column(name = "flag_reason")
    private String flagReason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_source")
    private TransactionSource source = TransactionSource.manual;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "reconciled_balance_rwf")
    private Long reconciledBalanceRwf;
}
