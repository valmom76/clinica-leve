ALTER TABLE app_users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 AFTER expected_daily_minutes,
    ADD COLUMN credentials_updated_at DATETIME(6) NULL AFTER token_version;

CREATE TABLE account_action_tokens (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    requested_ip VARCHAR(64),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_account_tokens_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_account_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT uk_account_tokens_hash UNIQUE (token_hash),
    INDEX ix_account_tokens_user (clinic_id, user_id, purpose, created_at),
    INDEX ix_account_tokens_expiry (expires_at, used_at)
) ENGINE=InnoDB;
