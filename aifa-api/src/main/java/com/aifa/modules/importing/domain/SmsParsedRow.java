package com.aifa.modules.importing.domain;

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
@Table(name = "sms_parsed_rows")
@Getter
@Setter
public class SmsParsedRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    @Column(name = "raw_text", nullable = false)
    private String rawText;

    @Column(name = "amount_rwf")
    private Long amountRwf;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "phone_hash")
    private String phoneHash;

    @Column(name = "balance_rwf")
    private Long balanceRwf;

    @Column(name = "transaction_at")
    private Instant transactionAt;

    private String direction;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "sms_row_status")
    private SmsRowStatus status = SmsRowStatus.pending;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "parse_error")
    private String parseError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
