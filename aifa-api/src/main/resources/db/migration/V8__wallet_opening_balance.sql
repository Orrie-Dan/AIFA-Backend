-- Opening balance + ledger floor for derived balance model; SMS idempotency on transactions

ALTER TABLE wallets
    ADD COLUMN opening_balance_rwf BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN ledger_floor_at TIMESTAMPTZ;

UPDATE wallets SET ledger_floor_at = created_at WHERE ledger_floor_at IS NULL;

ALTER TABLE wallets ALTER COLUMN ledger_floor_at SET NOT NULL;

ALTER TABLE transactions
    ADD COLUMN external_ref VARCHAR(128),
    ADD COLUMN reconciled_balance_rwf BIGINT;

CREATE UNIQUE INDEX idx_transactions_wallet_external_ref
    ON transactions (wallet_id, external_ref)
    WHERE external_ref IS NOT NULL;

-- Backfill: opening_balance_rwf + sum(txns) = current balance_rwf (reproduces today's balances)
UPDATE wallets w
SET opening_balance_rwf = w.balance_rwf - COALESCE(
        (SELECT SUM(t.amount_rwf) FROM transactions t WHERE t.wallet_id = w.id), 0),
    ledger_floor_at = COALESCE(
        (SELECT MIN(t.transaction_at) FROM transactions t WHERE t.wallet_id = w.id),
        w.created_at);
