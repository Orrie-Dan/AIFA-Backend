-- V6: Merchant lookup seeds for Rwanda (Phase 2.1)
INSERT INTO merchants (name, normalized_name, category_id)
SELECT 'MTN Mobile Money', 'mtn mobile money', id FROM categories WHERE slug = 'other' AND user_id IS NULL;

INSERT INTO merchants (name, normalized_name, category_id)
SELECT 'Airtel Money', 'airtel money', id FROM categories WHERE slug = 'other' AND user_id IS NULL;

INSERT INTO merchants (name, normalized_name, category_id)
SELECT 'Equity Bank', 'equity bank', id FROM categories WHERE slug = 'other' AND user_id IS NULL;

INSERT INTO merchants (name, normalized_name, category_id)
SELECT 'Bank of Kigali', 'bank of kigali', id FROM categories WHERE slug = 'other' AND user_id IS NULL;

INSERT INTO merchants (name, normalized_name, category_id)
SELECT 'Simba Supermarket', 'simba supermarket', id FROM categories WHERE slug = 'food' AND user_id IS NULL;

INSERT INTO merchants (name, normalized_name, category_id)
SELECT 'Nakumatt', 'nakumatt', id FROM categories WHERE slug = 'food' AND user_id IS NULL;

INSERT INTO merchants (name, normalized_name, category_id)
SELECT 'Kigali Bus', 'kigali bus', id FROM categories WHERE slug = 'transport' AND user_id IS NULL;

INSERT INTO merchants (name, normalized_name, category_id)
SELECT 'SP Rwanda', 'sp rwanda', id FROM categories WHERE slug = 'transport' AND user_id IS NULL;
