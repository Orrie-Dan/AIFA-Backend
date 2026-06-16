package com.aifa.modules.insights.infrastructure;

import com.aifa.modules.insights.application.HealthScoreService;
import com.aifa.modules.ledger.infrastructure.TransactionRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HealthScoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthScoreScheduler.class);

    private final TransactionRepository transactionRepository;
    private final HealthScoreService healthScoreService;

    public HealthScoreScheduler(TransactionRepository transactionRepository, HealthScoreService healthScoreService) {
        this.transactionRepository = transactionRepository;
        this.healthScoreService = healthScoreService;
    }

    @Scheduled(cron = "${aifa.insights.health-score-cron:0 0 2 * * *}")
    public void computeNightlyScores() {
        List<UUID> userIds = transactionRepository.findDistinctUserIds();
        log.info("Computing health scores for {} users", userIds.size());
        for (UUID userId : userIds) {
            try {
                healthScoreService.computeAndPersist(userId);
            } catch (Exception ex) {
                log.warn("Failed to compute health score for user {}", userId, ex);
            }
        }
    }
}
