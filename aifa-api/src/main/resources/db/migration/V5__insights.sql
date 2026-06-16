-- V5: Financial insights — health score history
CREATE TABLE health_scores (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score            SMALLINT NOT NULL CHECK (score BETWEEN 0 AND 100),
    savings_rate     SMALLINT NOT NULL CHECK (savings_rate BETWEEN 0 AND 100),
    emergency_fund   SMALLINT NOT NULL CHECK (emergency_fund BETWEEN 0 AND 100),
    budget_adherence SMALLINT NOT NULL CHECK (budget_adherence BETWEEN 0 AND 100),
    income_stability SMALLINT NOT NULL CHECK (income_stability BETWEEN 0 AND 100),
    debt_servicing   SMALLINT NOT NULL CHECK (debt_servicing BETWEEN 0 AND 100),
    band_label       VARCHAR(30) NOT NULL,
    top_driver       VARCHAR(100) NOT NULL,
    top_improvement  VARCHAR(255) NOT NULL,
    computed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_scores_user_date ON health_scores(user_id, computed_at DESC);
