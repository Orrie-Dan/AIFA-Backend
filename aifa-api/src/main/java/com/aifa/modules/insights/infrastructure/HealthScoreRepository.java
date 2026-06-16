package com.aifa.modules.insights.infrastructure;

import com.aifa.modules.insights.domain.HealthScore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthScoreRepository extends JpaRepository<HealthScore, UUID> {

    Optional<HealthScore> findFirstByUserIdOrderByComputedAtDesc(UUID userId);
}
