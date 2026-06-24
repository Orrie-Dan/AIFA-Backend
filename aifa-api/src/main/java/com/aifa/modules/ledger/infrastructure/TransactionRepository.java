package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.domain.Transaction;
import com.aifa.modules.ledger.domain.TransactionType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    default Page<Transaction> findFiltered(
            UUID userId, Instant from, Instant to, UUID categoryId, Pageable pageable) {
        return findAll(TransactionSpecifications.filtered(userId, from, to, categoryId), pageable);
    }

    @Query("""
            SELECT COALESCE(SUM(ABS(t.amountRwf)), 0) FROM Transaction t
            WHERE t.userId = :userId
              AND t.categoryId = :categoryId
              AND t.type = :type
              AND t.transactionAt >= :from
              AND t.transactionAt < :to
            """)
    long sumExpensesForCategory(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("type") TransactionType type);

    default long sumExpensesForCategory(UUID userId, UUID categoryId, Instant from, Instant to) {
        return sumExpensesForCategory(userId, categoryId, from, to, TransactionType.expense);
    }

    Page<Transaction> findByUserIdOrderByTransactionAtDesc(UUID userId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amountRwf), 0) FROM Transaction t
            WHERE t.userId = :userId
              AND t.type = :type
              AND t.transactionAt >= :from
              AND t.transactionAt < :to
            """)
    long sumIncome(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("type") TransactionType type);

    default long sumIncome(UUID userId, Instant from, Instant to) {
        return sumIncome(userId, from, to, TransactionType.income);
    }

    @Query("""
            SELECT COALESCE(SUM(ABS(t.amountRwf)), 0) FROM Transaction t
            WHERE t.userId = :userId
              AND t.type = :type
              AND t.transactionAt >= :from
              AND t.transactionAt < :to
            """)
    long sumExpenses(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("type") TransactionType type);

    default long sumExpenses(UUID userId, Instant from, Instant to) {
        return sumExpenses(userId, from, to, TransactionType.expense);
    }

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId
              AND t.type = :type
              AND t.transactionAt >= :from
            ORDER BY t.transactionAt DESC
            """)
    java.util.List<Transaction> findIncomeSince(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("type") TransactionType type);

    default java.util.List<Transaction> findIncomeSince(UUID userId, Instant from) {
        return findIncomeSince(userId, from, TransactionType.income);
    }

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
