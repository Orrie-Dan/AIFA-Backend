package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.domain.Wallet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Wallet> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndPrimaryTrue(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id AND w.userId = :userId")
    Optional<Wallet> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
