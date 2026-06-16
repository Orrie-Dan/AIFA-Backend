package com.aifa.modules.insights.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthScoreCalculatorTest {

    @Test
    void scoresSavingsRateByThresholds() {
        assertThat(HealthScoreCalculator.scoreSavingsRate(0.30)).isEqualTo(100);
        assertThat(HealthScoreCalculator.scoreSavingsRate(0.05)).isEqualTo(40);
        assertThat(HealthScoreCalculator.scoreSavingsRate(0.0)).isEqualTo(0);
    }

    @Test
    void computesWeightedTotalWithBudgetComponent() {
        var components = new HealthScoreCalculator.ComponentScores(80, 80, 60, 80, 100);
        var result = HealthScoreCalculator.compute(components, true, false);
        assertThat(result.totalScore()).isBetween(70, 85);
        assertThat(HealthScoreCalculator.bandLabel(result.totalScore())).isIn("Strong", "Building");
    }

    @Test
    void reweightsWhenBudgetMissing() {
        var components = new HealthScoreCalculator.ComponentScores(100, 100, 0, 100, 100);
        var result = HealthScoreCalculator.compute(components, false, false);
        assertThat(result.totalScore()).isEqualTo(100);
    }
}
