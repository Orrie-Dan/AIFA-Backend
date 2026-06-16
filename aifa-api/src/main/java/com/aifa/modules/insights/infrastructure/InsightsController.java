package com.aifa.modules.insights.infrastructure;

import com.aifa.modules.insights.application.AffordabilityService;
import com.aifa.modules.insights.application.HealthScoreService;
import com.aifa.modules.insights.application.RecommendationService;
import com.aifa.modules.insights.application.SpendingAnalysisService;
import com.aifa.modules.insights.application.dto.AffordabilityRequest;
import com.aifa.modules.insights.application.dto.AffordabilityResponse;
import com.aifa.modules.insights.application.dto.HealthScoreResponse;
import com.aifa.modules.insights.application.dto.RecommendationResponse;
import com.aifa.modules.insights.application.dto.SpendingAnalysisResponse;
import com.aifa.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightsController {

    private final SpendingAnalysisService spendingAnalysisService;
    private final HealthScoreService healthScoreService;
    private final AffordabilityService affordabilityService;
    private final RecommendationService recommendationService;
    private final CurrentUserProvider currentUserProvider;

    public InsightsController(
            SpendingAnalysisService spendingAnalysisService,
            HealthScoreService healthScoreService,
            AffordabilityService affordabilityService,
            RecommendationService recommendationService,
            CurrentUserProvider currentUserProvider) {
        this.spendingAnalysisService = spendingAnalysisService;
        this.healthScoreService = healthScoreService;
        this.affordabilityService = affordabilityService;
        this.recommendationService = recommendationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/spending-analysis")
    public SpendingAnalysisResponse spendingAnalysis() {
        return spendingAnalysisService.analyze(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/health-score")
    public HealthScoreResponse healthScore() {
        return healthScoreService.getCurrentScore(currentUserProvider.getCurrentUserId());
    }

    @PostMapping("/affordability")
    public AffordabilityResponse affordability(@Valid @RequestBody AffordabilityRequest request) {
        return affordabilityService.check(currentUserProvider.getCurrentUserId(), request);
    }

    @GetMapping("/recommendations")
    public List<RecommendationResponse> recommendations() {
        return recommendationService.getRecommendations(currentUserProvider.getCurrentUserId());
    }
}
