CREATE TABLE subscription_plans (
    id VARCHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    trial_days INT NOT NULL DEFAULT 30,
    price_guarantee_months INT,
    availability_limit INT,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_subscription_plans_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE clinic_billing_profiles (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    legal_name VARCHAR(160) NOT NULL,
    cpf_cnpj VARCHAR(14) NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    postal_code VARCHAR(8),
    address VARCHAR(180),
    address_number VARCHAR(30),
    complement VARCHAR(120),
    province VARCHAR(100),
    asaas_customer_id VARCHAR(40),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_billing_profiles_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT uk_billing_profiles_clinic UNIQUE (clinic_id),
    CONSTRAINT uk_billing_profiles_asaas_customer UNIQUE (asaas_customer_id)
) ENGINE=InnoDB;

CREATE TABLE clinic_subscriptions (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30),
    billing_cycle VARCHAR(20) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    trial_ends_at DATETIME(6),
    next_due_date DATE,
    grace_ends_at DATETIME(6),
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at DATETIME(6),
    asaas_subscription_id VARCHAR(40),
    asaas_checkout_id VARCHAR(80),
    checkout_url VARCHAR(500),
    last_payment_status VARCHAR(40),
    last_payment_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_clinic_subscriptions_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_clinic_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id),
    CONSTRAINT uk_clinic_subscriptions_clinic UNIQUE (clinic_id),
    CONSTRAINT uk_clinic_subscriptions_asaas_subscription UNIQUE (asaas_subscription_id),
    CONSTRAINT uk_clinic_subscriptions_asaas_checkout UNIQUE (asaas_checkout_id),
    INDEX ix_clinic_subscriptions_status (status)
) ENGINE=InnoDB;

CREATE TABLE subscription_payments (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    subscription_id VARCHAR(36) NOT NULL,
    asaas_payment_id VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    billing_type VARCHAR(30),
    value DECIMAL(10, 2) NOT NULL,
    due_date DATE,
    payment_date DATETIME(6),
    invoice_url VARCHAR(500),
    bank_slip_url VARCHAR(500),
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscription_payments_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_subscription_payments_subscription FOREIGN KEY (subscription_id) REFERENCES clinic_subscriptions (id),
    CONSTRAINT uk_subscription_payments_asaas_payment UNIQUE (asaas_payment_id),
    INDEX ix_subscription_payments_clinic_due (clinic_id, due_date),
    INDEX ix_subscription_payments_subscription (subscription_id)
) ENGINE=InnoDB;

CREATE TABLE asaas_webhook_events (
    id VARCHAR(36) NOT NULL,
    asaas_event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80),
    payload LONGTEXT NOT NULL,
    processing_status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    processed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_asaas_webhook_events_event UNIQUE (asaas_event_id),
    INDEX ix_asaas_webhook_events_status (processing_status, created_at)
) ENGINE=InnoDB;

INSERT INTO subscription_plans (
    id, code, name, description, billing_cycle, price, trial_days,
    price_guarantee_months, availability_limit, visible, active, display_order,
    created_at, updated_at
) VALUES
    ('b1f2b3c4-d5e6-47f8-9012-111111111111', 'PIONEER_MONTHLY', 'Clínica Leve Pioneiro',
     'Condição especial para as primeiras 50 clínicas, com preço garantido por 24 meses.',
     'MONTHLY', 59.90, 30, 24, 50, TRUE, TRUE, 1, NOW(6), NOW(6)),
    ('b1f2b3c4-d5e6-47f8-9012-222222222222', 'CLINICA_LEVE_MONTHLY', 'Clínica Leve Mensal',
     'Todos os módulos do Clínica Leve, sem cobrança por usuário.',
     'MONTHLY', 79.90, 30, NULL, NULL, TRUE, TRUE, 2, NOW(6), NOW(6)),
    ('b1f2b3c4-d5e6-47f8-9012-333333333333', 'CLINICA_LEVE_YEARLY', 'Clínica Leve Anual',
     'Plano anual com o equivalente a dois meses gratuitos.',
     'YEARLY', 799.00, 30, NULL, NULL, TRUE, TRUE, 3, NOW(6), NOW(6));
