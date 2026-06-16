package com.aifa.modules.planning.infrastructure;

import com.aifa.modules.planning.domain.GoalContribution;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

    List<GoalContribution> findByGoalIdOrderByContributedAtDesc(UUID goalId);
}
