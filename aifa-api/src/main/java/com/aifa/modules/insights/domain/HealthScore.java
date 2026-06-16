package com.aifa.modules.insights.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "health_scores")
@Getter
@Setter
public class HealthScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private short score;

    @Column(name = "savings_rate", nullable = false)
    private short savingsRate;

    @Column(name = "emergency_fund", nullable = false)
    private short emergencyFund;

    @Column(name = "budget_adherence", nullable = false)
    private short budgetAdherence;

    @Column(name = "income_stability", nullable = false)
    private short incomeStability;

    @Column(name = "debt_servicing", nullable = false)
    private short debtServicing;

    @Column(name = "band_label", nullable = false)
    private String bandLabel;

    @Column(name = "top_driver", nullable = false)
    private String topDriver;

    @Column(name = "top_improvement", nullable = false)
    private String topImprovement;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
