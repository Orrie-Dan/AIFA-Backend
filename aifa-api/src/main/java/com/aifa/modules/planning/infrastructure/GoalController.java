package com.aifa.modules.planning.infrastructure;

import com.aifa.modules.planning.application.GoalService;
import com.aifa.modules.planning.application.dto.ContributeGoalRequest;
import com.aifa.modules.planning.application.dto.CreateGoalRequest;
import com.aifa.modules.planning.application.dto.GoalResponse;
import com.aifa.modules.planning.application.dto.UpdateGoalRequest;
import com.aifa.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;
    private final CurrentUserProvider currentUserProvider;

    public GoalController(GoalService goalService, CurrentUserProvider currentUserProvider) {
        this.goalService = goalService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<GoalResponse> listGoals() {
        return goalService.listGoals(currentUserProvider.getCurrentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse createGoal(@Valid @RequestBody CreateGoalRequest request) {
        return goalService.createGoal(currentUserProvider.getCurrentUserId(), request);
    }

    @PatchMapping("/{id}")
    public GoalResponse updateGoal(@PathVariable UUID id, @RequestBody UpdateGoalRequest request) {
        return goalService.updateGoal(currentUserProvider.getCurrentUserId(), id, request);
    }

    @PostMapping("/{id}/contribute")
    public GoalResponse contribute(@PathVariable UUID id, @Valid @RequestBody ContributeGoalRequest request) {
        return goalService.contribute(currentUserProvider.getCurrentUserId(), id, request);
    }
}
