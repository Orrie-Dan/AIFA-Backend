-- V1: PostgreSQL extensions and enum-like domain types
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Wallet types
CREATE TYPE wallet_type AS ENUM ('mobile_money', 'bank', 'cash', 'savings');

-- Transaction types
CREATE TYPE transaction_type AS ENUM ('income', 'expense', 'transfer');

-- Category source
CREATE TYPE category_source AS ENUM ('user', 'merchant_table', 'keyword', 'llm', 'uncategorized');

-- Transaction source
CREATE TYPE transaction_source AS ENUM ('manual', 'sms_import', 'api_import');

-- AI mode
CREATE TYPE ai_mode AS ENUM ('smart', 'private');

-- Budget period
CREATE TYPE budget_period AS ENUM ('monthly', 'weekly');

-- Goal status
CREATE TYPE goal_status AS ENUM ('active', 'completed', 'paused', 'cancelled');

-- SMS import batch status
CREATE TYPE sms_import_status AS ENUM ('preview', 'confirmed', 'cancelled');

-- SMS parsed row status
CREATE TYPE sms_row_status AS ENUM ('pending', 'confirmed', 'skipped', 'duplicate');
