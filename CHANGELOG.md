# Changelog

## Unreleased — Ledger balance model + SMS import

### Core ledger model
- Balance is now derived: `opening_balance_rwf + SUM(transactions)`.
- `balance_rwf` on wallets is a transactionally recomputed cache (full `SUM` on multi-row mutations).
- New wallet fields: `openingBalanceRwf`, `ledgerFloorAt`.
- Migration `V8` backfills opening balance so existing wallets keep the same balance.

### Negative-balance guard (scoped)
- `INSUFFICIENT_WALLET_BALANCE` applies only to **manual** transactions within ~1 day of now (real-time spend).
- SMS import and backdated manual entries never hit the spend-prevention guard.
- Removed `applyLedgerDeltaForSmsImport()` workaround.

### SMS import confirm
- Derives `opening_balance_rwf` from SMS anchor rows (`balanceRwf` on any row).
- Backfill: importing rows older than `ledger_floor_at` shifts opening balance algebraically.
- Idempotency via `external_ref` fingerprint on transactions (duplicate confirm = no-op).
- Reconciliation: confirm response includes `discrepancies[]` when SMS balance ≠ computed (±100 RWF).
- Confirm allowed on already-confirmed batches for idempotent retries.

### SMS import preview
- New fields: `suggestedOpeningBalanceRwf`, `anchorRowIndex`.

### API
- `WalletResponse`: added `openingBalanceRwf`, `ledgerFloorAt`.
- `PATCH /wallets/{id}`: accepts `openingBalanceRwf` (balance is derived, not directly patchable).
- `SmsImportConfirmResponse`: added `openingBalanceRwf`, `computedBalanceRwf`, `ledgerFloorAt`, `discrepancies`, `alreadyImportedRowIndexes`.

### Parser (`mtn_momo_v2`)
- See prior changelog entry for SMS parser expansion.

### Example flow

```bash
curl -X POST http://localhost:8080/api/v1/import/sms \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"messages":"You have paid RWF 8500 to Shop. Remaining balance: RWF 407600","walletId":"WALLET_UUID"}'

curl -X POST http://localhost:8080/api/v1/import/sms/confirm \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"batchId":"BATCH_UUID","rowIndexes":[0],"walletId":"WALLET_UUID"}'
```
