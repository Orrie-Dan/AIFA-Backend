-- V3: Ledger — categories, wallets, transactions, merchants stub
CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(50) NOT NULL,
    icon        VARCHAR(50),
    is_system   BOOLEAN NOT NULL DEFAULT TRUE,
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_categories_slug_user UNIQUE (slug, user_id)
);

CREATE TABLE merchants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    category_id     UUID REFERENCES categories(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchants_normalized ON merchants(normalized_name);

CREATE TABLE wallets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    type            wallet_type NOT NULL,
    balance_rwf     BIGINT NOT NULL DEFAULT 0,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_wallets_user_primary ON wallets(user_id) WHERE is_primary = TRUE;

CREATE TABLE transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wallet_id       UUID NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    amount_rwf      BIGINT NOT NULL,
    type            transaction_type NOT NULL,
    category_id     UUID REFERENCES categories(id),
    category_source category_source NOT NULL DEFAULT 'uncategorized',
    merchant_name   VARCHAR(255),
    description     TEXT,
    transaction_at  TIMESTAMPTZ NOT NULL,
    is_recurring    BOOLEAN NOT NULL DEFAULT FALSE,
    is_flagged      BOOLEAN NOT NULL DEFAULT FALSE,
    flag_reason     VARCHAR(50),
    source          transaction_source NOT NULL DEFAULT 'manual',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_at DESC);
CREATE INDEX idx_transactions_category ON transactions(user_id, category_id, transaction_at DESC);
CREATE INDEX idx_transactions_wallet ON transactions(wallet_id, transaction_at DESC);

-- System categories seed
INSERT INTO categories (id, name, slug, icon, is_system, user_id) VALUES
    (gen_random_uuid(), 'Food', 'food', 'restaurant', TRUE, NULL),
    (gen_random_uuid(), 'Transport', 'transport', 'directions_car', TRUE, NULL),
    (gen_random_uuid(), 'Rent', 'rent', 'home', TRUE, NULL),
    (gen_random_uuid(), 'Utilities', 'utilities', 'bolt', TRUE, NULL),
    (gen_random_uuid(), 'Entertainment', 'entertainment', 'movie', TRUE, NULL),
    (gen_random_uuid(), 'Health', 'health', 'local_hospital', TRUE, NULL),
    (gen_random_uuid(), 'Savings', 'savings', 'savings', TRUE, NULL),
    (gen_random_uuid(), 'Other', 'other', 'category', TRUE, NULL);
