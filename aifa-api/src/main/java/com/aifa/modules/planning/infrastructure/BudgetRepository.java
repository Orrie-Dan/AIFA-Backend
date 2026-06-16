package com.aifa.modules.planning.infrastructure;

import com.aifa.modules.planning.domain.Budget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT b FROM Budget b
            WHERE b.userId = :userId
              AND b.activeFrom <= :date
              AND (b.activeTo IS NULL OR b.activeTo >= :date)
            ORDER BY b.activeFrom DESC
            """)
    List<Budget> findActiveForDate(@Param("userId") UUID userId, @Param("date") LocalDate date);
}
