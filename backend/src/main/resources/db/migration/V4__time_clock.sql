ALTER TABLE app_users
    ADD COLUMN expected_daily_minutes INT NOT NULL DEFAULT 480 AFTER role;

CREATE TABLE time_clock_entries (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    entry_type VARCHAR(30) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    source VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    created_by_user_id VARCHAR(36) NOT NULL,
    updated_by_user_id VARCHAR(36),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_time_entries_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_time_entries_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_time_entries_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_time_entries_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES app_users (id),
    INDEX ix_time_entries_clinic_occurred (clinic_id, occurred_at),
    INDEX ix_time_entries_user_occurred (user_id, occurred_at)
) ENGINE=InnoDB;
