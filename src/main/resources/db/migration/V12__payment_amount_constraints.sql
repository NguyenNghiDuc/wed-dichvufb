-- Issue #30: new database writes must preserve valid internal payment amounts.
-- NOT VALID avoids rewriting or rejecting historic legacy rows during rollout.
-- PostgreSQL still enforces these constraints for every NEW insert/update.
-- After a manual legacy-data audit, validate on staging and plan an approved
-- production VALIDATE CONSTRAINT operation (never silently rewrite old data).
ALTER TABLE deposit_request
    ADD CONSTRAINT chk_deposit_request_amount_vnd
    CHECK (amount BETWEEN 1000 AND 1000000000) NOT VALID;

ALTER TABLE payment_receipts
    ADD CONSTRAINT chk_payment_receipt_amount_vnd
    CHECK (amount_vnd BETWEEN 1000 AND 1000000000) NOT VALID;
