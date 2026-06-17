package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.domain.Transaction;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId
              AND (:from IS NULL OR t.transactionAt >= :from)
              AND (:to IS NULL OR t.transactionAt <= :to)
              AND (:categoryId IS NULL OR t.categoryId = :categoryId)
            ORDER BY t.transactionAt DESC
            """)
    Page<Transaction> findFiltered(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(ABS(t.amountRwf)), 0) FROM Transaction t
            WHERE t.userId = :userId
              AND t.categoryId = :categoryId
              AND t.type = com.aifa.modules.ledger.domain.TransactionType.expense
              AND t.transactionAt >= :from
              AND t.transactionAt < :to
            """)
    long sumExpensesForCategory(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    Page<Transaction> findByUserIdOrderByTransactionAtDesc(UUID userId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amountRwf), 0) FROM Transaction t
            WHERE t.userId = :userId
              AND t.type = com.aifa.modules.ledger.domain.TransactionType.income
              AND t.transactionAt >= :from
              AND t.transactionAt < :to
            """)
    long sumIncome(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COALESCE(SUM(ABS(t.amountRwf)), 0) FROM Transaction t
            WHERE t.userId = :userId
              AND t.type = com.aifa.modules.ledger.domain.TransactionType.expense
              AND t.transactionAt >= :from
              AND t.transactionAt < :to
            """)
    long sumExpenses(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId
              AND t.type = com.aifa.modules.ledger.domain.TransactionType.income
              AND t.transactionAt >= :from
            ORDER BY t.transactionAt DESC
            """)
    java.util.List<Transaction> findIncomeSince(@Param("userId") UUID userId, @Param("from") Instant from);

    @Query("SELECT MIN(t.transactionAt) FROM Transaction t WHERE t.userId = :userId")
    Optional<Instant> findEarliestTransactionAt(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(t.amountRwf), 0) FROM Transaction t WHERE t.walletId = :walletId")
    long sumAmountByWalletId(@Param("walletId") UUID walletId);

    @Query("""
            SELECT COALESCE(SUM(t.amountRwf), 0) FROM Transaction t
            WHERE t.walletId = :walletId AND t.transactionAt <= :upTo
            """)
    long sumAmountByWalletIdUpTo(@Param("walletId") UUID walletId, @Param("upTo") Instant upTo);

    long countByWalletId(UUID walletId);

    boolean existsByWalletIdAndExternalRef(UUID walletId, String externalRef);

    java.util.Optional<Transaction> findByWalletIdAndExternalRef(UUID walletId, String externalRef);

    @Query("SELECT MIN(t.transactionAt) FROM Transaction t WHERE t.walletId = :walletId")
    Optional<Instant> findEarliestTransactionAtByWalletId(@Param("walletId") UUID walletId);

    @Query("SELECT DISTINCT t.userId FROM Transaction t")
    java.util.List<UUID> findDistinctUserIds();
}
