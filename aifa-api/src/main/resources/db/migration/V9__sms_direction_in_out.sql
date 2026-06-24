-- Align sms_parsed_rows.direction with parser values (IN/OUT) instead of credit/debit.
ALTER TABLE sms_parsed_rows DROP CONSTRAINT sms_parsed_rows_direction_check;

UPDATE sms_parsed_rows SET direction = 'IN' WHERE direction = 'credit';
UPDATE sms_parsed_rows SET direction = 'OUT' WHERE direction = 'debit';

ALTER TABLE sms_parsed_rows ADD CONSTRAINT sms_parsed_rows_direction_check
    CHECK (direction IS NULL OR direction IN ('IN', 'OUT'));
