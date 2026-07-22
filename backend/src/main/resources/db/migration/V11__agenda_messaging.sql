ALTER TABLE appointments
    ADD COLUMN confirmation_requested_at DATETIME(6) NULL AFTER notes,
    ADD COLUMN confirmed_at DATETIME(6) NULL AFTER confirmation_requested_at,
    ADD COLUMN reschedule_requested_at DATETIME(6) NULL AFTER confirmed_at;

ALTER TABLE patients
    ADD COLUMN whatsapp_opt_in BOOLEAN NOT NULL DEFAULT FALSE AFTER phone,
    ADD COLUMN whatsapp_opt_in_at DATETIME(6) NULL AFTER whatsapp_opt_in,
    ADD COLUMN whatsapp_opt_in_recorded_by VARCHAR(36) NULL AFTER whatsapp_opt_in_at,
    ADD CONSTRAINT fk_patients_whatsapp_opt_in_user FOREIGN KEY (whatsapp_opt_in_recorded_by) REFERENCES app_users (id);

CREATE TABLE appointment_messaging_settings (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    confirmation_template_name VARCHAR(160) NOT NULL DEFAULT 'consulta_confirmacao',
    reminder_template_name VARCHAR(160) NOT NULL DEFAULT 'consulta_lembrete',
    language_code VARCHAR(20) NOT NULL DEFAULT 'pt_BR',
    confirmation_preview VARCHAR(1000) NOT NULL,
    reminder_preview VARCHAR(1000) NOT NULL,
    first_reminder_hours INT NOT NULL DEFAULT 24,
    second_reminder_hours INT NULL DEFAULT 2,
    max_attempts INT NOT NULL DEFAULT 3,
    retry_minutes INT NOT NULL DEFAULT 15,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_appointment_messaging_settings_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT uk_appointment_messaging_settings_clinic UNIQUE (clinic_id)
) ENGINE=InnoDB;

CREATE TABLE appointment_messages (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    appointment_id VARCHAR(36) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    recipient VARCHAR(30) NOT NULL,
    template_name VARCHAR(160),
    scheduled_at DATETIME(6) NOT NULL,
    next_attempt_at DATETIME(6),
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    provider_message_id VARCHAR(200),
    response_provider_message_id VARCHAR(200),
    response_action VARCHAR(30),
    error_message VARCHAR(1000),
    sent_at DATETIME(6),
    delivered_at DATETIME(6),
    read_at DATETIME(6),
    responded_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_appointment_messages_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_appointment_messages_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT uk_appointment_messages_provider UNIQUE (provider_message_id),
    CONSTRAINT uk_appointment_messages_response UNIQUE (response_provider_message_id),
    INDEX ix_appointment_messages_queue (status, next_attempt_at),
    INDEX ix_appointment_messages_history (clinic_id, appointment_id, created_at)
) ENGINE=InnoDB;
