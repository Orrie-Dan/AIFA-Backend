package com.aifa.modules.planning.infrastructure;

import com.aifa.modules.planning.domain.Goal;
import com.aifa.modules.planning.domain.GoalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, GoalStatus status);

    List<Goal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);
}
