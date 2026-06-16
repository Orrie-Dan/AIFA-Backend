-- V4: Planning, goals, and SMS import
CREATE TABLE budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id     UUID NOT NULL REFERENCES categories(id),
    amount_rwf      BIGINT NOT NULL CHECK (amount_rwf > 0),
    period          budget_period NOT NULL DEFAULT 'monthly',
    active_from     DATE NOT NULL,
    active_to       DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_budgets_user_active ON budgets(user_id, active_from DESC);

CREATE TABLE goals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    target_rwf      BIGINT NOT NULL CHECK (target_rwf > 0),
    current_rwf     BIGINT NOT NULL DEFAULT 0 CHECK (current_rwf >= 0),
    target_date     DATE,
    status          goal_status NOT NULL DEFAULT 'active',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_goals_user_status ON goals(user_id, status);

CREATE TABLE goal_contributions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id         UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount_rwf      BIGINT NOT NULL CHECK (amount_rwf > 0),
    contributed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_goal_contributions_goal ON goal_contributions(goal_id, contributed_at DESC);

CREATE TABLE sms_import_batches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wallet_id       UUID REFERENCES wallets(id),
    status          sms_import_status NOT NULL DEFAULT 'preview',
    raw_messages    TEXT NOT NULL,
    parser_version  VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at    TIMESTAMPTZ
);

CREATE INDEX idx_sms_batches_user ON sms_import_batches(user_id, created_at DESC);

CREATE TABLE sms_parsed_rows (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id        UUID NOT NULL REFERENCES sms_import_batches(id) ON DELETE CASCADE,
    row_index       INT NOT NULL,
    raw_text        TEXT NOT NULL,
    amount_rwf      BIGINT,
    sender_name     VARCHAR(255),
    phone_hash      VARCHAR(64),
    balance_rwf     BIGINT,
    transaction_at  TIMESTAMPTZ,
    direction       VARCHAR(10) CHECK (direction IN ('credit', 'debit')),
    status          sms_row_status NOT NULL DEFAULT 'pending',
    transaction_id  UUID REFERENCES transactions(id),
    parse_error     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sms_rows_batch_index UNIQUE (batch_id, row_index)
);

CREATE INDEX idx_sms_rows_batch ON sms_parsed_rows(batch_id, row_index);
