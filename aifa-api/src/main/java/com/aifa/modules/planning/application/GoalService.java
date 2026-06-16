package com.aifa.modules.planning.application;

import com.aifa.modules.insights.application.AffordabilityService;
import com.aifa.modules.planning.application.dto.ContributeGoalRequest;
import com.aifa.modules.planning.application.dto.CreateGoalRequest;
import com.aifa.modules.planning.application.dto.GoalResponse;
import com.aifa.modules.planning.application.dto.UpdateGoalRequest;
import com.aifa.modules.planning.domain.Goal;
import com.aifa.modules.planning.domain.GoalContribution;
import com.aifa.modules.planning.domain.GoalStatus;
import com.aifa.modules.planning.infrastructure.GoalContributionRepository;
import com.aifa.modules.planning.infrastructure.GoalRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.BadRequestException;
import com.aifa.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final AffordabilityService affordabilityService;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public GoalService(
            GoalRepository goalRepository,
            GoalContributionRepository goalContributionRepository,
            AffordabilityService affordabilityService,
            AuditLogger auditLogger,
            Clock clock) {
        this.goalRepository = goalRepository;
        this.goalContributionRepository = goalContributionRepository;
        this.affordabilityService = affordabilityService;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals(UUID userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GoalResponse createGoal(UUID userId, CreateGoalRequest request) {
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setName(request.name().trim());
        goal.setTargetRwf(request.targetRwf());
        goal.setTargetDate(request.targetDate());
        goal.setStatus(GoalStatus.active);
        goal.setCreatedAt(clock.instant());
        goal.setUpdatedAt(clock.instant());
        goalRepository.save(goal);

        auditLogger.logAction(userId, "CREATE", "goal", goal.getId().toString());
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(UUID userId, UUID goalId, UpdateGoalRequest request) {
        Goal goal = goalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (request.name() != null && !request.name().isBlank()) {
            goal.setName(request.name().trim());
        }
        if (request.targetRwf() != null) {
            if (request.targetRwf() <= 0) {
                throw new BadRequestException("Target amount must be positive");
            }
            goal.setTargetRwf(request.targetRwf());
        }
        if (request.targetDate() != null) {
            goal.setTargetDate(request.targetDate());
        }
        if (request.status() != null) {
            goal.setStatus(request.status());
        }
        goal.setUpdatedAt(clock.instant());
        goalRepository.save(goal);
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse contribute(UUID userId, UUID goalId, ContributeGoalRequest request) {
        Goal goal = goalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (goal.getStatus() != GoalStatus.active) {
            throw new BadRequestException("Cannot contribute to inactive goal");
        }

        GoalContribution contribution = new GoalContribution();
        contribution.setGoalId(goalId);
        contribution.setUserId(userId);
        contribution.setAmountRwf(request.amountRwf());
        contribution.setNote(request.note());
        contribution.setContributedAt(clock.instant());
        contribution.setCreatedAt(clock.instant());
        goalContributionRepository.save(contribution);

        goal.setCurrentRwf(goal.getCurrentRwf() + request.amountRwf());
        if (goal.getCurrentRwf() >= goal.getTargetRwf()) {
            goal.setStatus(GoalStatus.completed);
        }
        goal.setUpdatedAt(clock.instant());
        goalRepository.save(goal);

        auditLogger.logAction(userId, "CONTRIBUTE", "goal", goalId.toString());
        return toResponse(goal);
    }

    private GoalResponse toResponse(Goal goal) {
        double progress = goal.getTargetRwf() == 0 ? 0 : (goal.getCurrentRwf() * 100.0) / goal.getTargetRwf();
        long remaining = Math.max(0, goal.getTargetRwf() - goal.getCurrentRwf());
        Integer monthsToGoal = goal.getStatus() == GoalStatus.active
                ? affordabilityService.estimateMonthsToGoal(goal.getUserId(), remaining)
                : null;
        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetRwf(),
                goal.getCurrentRwf(),
                Math.min(progress, 100.0),
                goal.getTargetDate(),
                goal.getStatus(),
                monthsToGoal);
    }
}
