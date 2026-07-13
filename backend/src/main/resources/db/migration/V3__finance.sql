CREATE TABLE financial_categories (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_financial_categories_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT uk_financial_categories_name UNIQUE (clinic_id, entry_type, name),
    INDEX ix_financial_categories_clinic_type (clinic_id, entry_type)
) ENGINE=InnoDB;

CREATE TABLE financial_entries (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    description VARCHAR(180) NOT NULL,
    counterparty VARCHAR(160),
    amount DECIMAL(14,2) NOT NULL,
    due_date DATE NOT NULL,
    payment_date DATE,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(40),
    notes VARCHAR(500),
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_financial_entries_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_financial_entries_category FOREIGN KEY (category_id) REFERENCES financial_categories (id),
    CONSTRAINT fk_financial_entries_user FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    INDEX ix_financial_entries_clinic_due (clinic_id, due_date),
    INDEX ix_financial_entries_clinic_status (clinic_id, status),
    INDEX ix_financial_entries_clinic_type (clinic_id, entry_type)
) ENGINE=InnoDB;
