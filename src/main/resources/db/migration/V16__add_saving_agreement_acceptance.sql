-- Add agreement acceptance tracking for savings opening flow

ALTER TABLE savings
    ADD COLUMN agreement_accepted_at TIMESTAMP NULL,
    ADD COLUMN agreement_version VARCHAR(32) NULL;
