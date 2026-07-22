ALTER TABLE app_users
    ADD COLUMN professional_id VARCHAR(36) NULL AFTER role,
    ADD CONSTRAINT fk_users_professional FOREIGN KEY (professional_id) REFERENCES professionals (id),
    ADD CONSTRAINT uk_users_clinic_professional UNIQUE (clinic_id, professional_id);

CREATE TABLE clinical_encounters (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    appointment_id VARCHAR(36) NOT NULL,
    patient_id VARCHAR(36) NOT NULL,
    professional_id VARCHAR(36) NOT NULL,
    specialty_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    chief_complaint LONGTEXT,
    subjective_notes LONGTEXT,
    objective_notes LONGTEXT,
    assessment LONGTEXT,
    care_plan LONGTEXT,
    additional_notes LONGTEXT,
    created_by_user_id VARCHAR(36) NOT NULL,
    updated_by_user_id VARCHAR(36) NOT NULL,
    finalized_by_user_id VARCHAR(36),
    finalized_at DATETIME(6),
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_encounters_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_encounters_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_encounters_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_encounters_professional FOREIGN KEY (professional_id) REFERENCES professionals (id),
    CONSTRAINT fk_encounters_specialty FOREIGN KEY (specialty_id) REFERENCES specialties (id),
    CONSTRAINT fk_encounters_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_encounters_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_encounters_finalized_by FOREIGN KEY (finalized_by_user_id) REFERENCES app_users (id),
    CONSTRAINT uk_encounters_appointment UNIQUE (appointment_id),
    INDEX ix_encounters_clinic_patient (clinic_id, patient_id, created_at),
    INDEX ix_encounters_clinic_professional (clinic_id, professional_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE clinical_encounter_versions (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    encounter_id VARCHAR(36) NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    chief_complaint LONGTEXT,
    subjective_notes LONGTEXT,
    objective_notes LONGTEXT,
    assessment LONGTEXT,
    care_plan LONGTEXT,
    additional_notes LONGTEXT,
    author_user_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_encounter_versions_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_encounter_versions_encounter FOREIGN KEY (encounter_id) REFERENCES clinical_encounters (id),
    CONSTRAINT fk_encounter_versions_author FOREIGN KEY (author_user_id) REFERENCES app_users (id),
    CONSTRAINT uk_encounter_versions_number UNIQUE (encounter_id, version_number),
    INDEX ix_encounter_versions_clinic (clinic_id, encounter_id)
) ENGINE=InnoDB;

CREATE TABLE clinical_document_templates (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    type VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    title_template VARCHAR(240) NOT NULL,
    body_template LONGTEXT NOT NULL,
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version_number INT NOT NULL DEFAULT 1,
    created_by_user_id VARCHAR(36),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_document_templates_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_document_templates_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    CONSTRAINT uk_document_templates_name UNIQUE (clinic_id, name),
    INDEX ix_document_templates_clinic_type (clinic_id, type, active)
) ENGINE=InnoDB;

CREATE TABLE clinical_documents (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    encounter_id VARCHAR(36) NOT NULL,
    appointment_id VARCHAR(36) NOT NULL,
    patient_id VARCHAR(36) NOT NULL,
    professional_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(36),
    type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    title VARCHAR(240) NOT NULL,
    content LONGTEXT NOT NULL,
    template_version INT,
    revision_number INT NOT NULL DEFAULT 1,
    parent_document_id VARCHAR(36),
    created_by_user_id VARCHAR(36) NOT NULL,
    updated_by_user_id VARCHAR(36) NOT NULL,
    finalized_by_user_id VARCHAR(36),
    finalized_at DATETIME(6),
    document_hash CHAR(64),
    verification_code VARCHAR(64),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_clinical_documents_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_clinical_documents_encounter FOREIGN KEY (encounter_id) REFERENCES clinical_encounters (id),
    CONSTRAINT fk_clinical_documents_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_clinical_documents_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_clinical_documents_professional FOREIGN KEY (professional_id) REFERENCES professionals (id),
    CONSTRAINT fk_clinical_documents_template FOREIGN KEY (template_id) REFERENCES clinical_document_templates (id),
    CONSTRAINT fk_clinical_documents_parent FOREIGN KEY (parent_document_id) REFERENCES clinical_documents (id),
    CONSTRAINT fk_clinical_documents_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_clinical_documents_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_clinical_documents_finalized_by FOREIGN KEY (finalized_by_user_id) REFERENCES app_users (id),
    CONSTRAINT uk_clinical_documents_verification UNIQUE (verification_code),
    INDEX ix_clinical_documents_encounter (clinic_id, encounter_id, created_at),
    INDEX ix_clinical_documents_patient (clinic_id, patient_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE clinical_audit_events (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    actor_user_id VARCHAR(36) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    details_json LONGTEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_clinical_audit_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_clinical_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_users (id),
    INDEX ix_clinical_audit_entity (clinic_id, entity_type, entity_id, created_at),
    INDEX ix_clinical_audit_actor (clinic_id, actor_user_id, created_at)
) ENGINE=InnoDB;

INSERT INTO clinical_document_templates (
    id, clinic_id, type, name, title_template, body_template, favorite, active,
    version_number, created_by_user_id, created_at, updated_at
)
SELECT UUID(), c.id, 'CLINICAL_REPORT', 'Relatório clínico',
       'Relatório clínico - {{paciente.nome}}',
       'Paciente: {{paciente.nome}}\nData de nascimento: {{paciente.data_nascimento}}\n\nRelato clínico:\n{{consulta.avaliacao}}\n\nConduta:\n{{consulta.plano}}\n\n{{profissional.nome}}\n{{profissional.conselho}}',
       TRUE, TRUE, 1, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM clinics c
WHERE NOT EXISTS (
    SELECT 1 FROM clinical_document_templates t
    WHERE t.clinic_id = c.id AND t.name = 'Relatório clínico'
);

INSERT INTO clinical_document_templates (
    id, clinic_id, type, name, title_template, body_template, favorite, active,
    version_number, created_by_user_id, created_at, updated_at
)
SELECT UUID(), c.id, 'EXAM_REQUEST', 'Solicitação de exames',
       'Solicitação de exames - {{paciente.nome}}',
       'Solicito os exames abaixo para {{paciente.nome}}:\n\n[Descreva os exames solicitados]\n\nIndicação clínica:\n{{consulta.avaliacao}}\n\n{{profissional.nome}}\n{{profissional.conselho}}',
       TRUE, TRUE, 1, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM clinics c
WHERE NOT EXISTS (
    SELECT 1 FROM clinical_document_templates t
    WHERE t.clinic_id = c.id AND t.name = 'Solicitação de exames'
);

INSERT INTO clinical_document_templates (
    id, clinic_id, type, name, title_template, body_template, favorite, active,
    version_number, created_by_user_id, created_at, updated_at
)
SELECT UUID(), c.id, 'MEDICAL_CERTIFICATE', 'Atestado',
       'Atestado - {{paciente.nome}}',
       'Atesto, para os devidos fins, que {{paciente.nome}} esteve sob atendimento profissional em {{consulta.data}}.\n\n[Complete o período de afastamento e demais informações necessárias.]\n\n{{profissional.nome}}\n{{profissional.conselho}}',
       FALSE, TRUE, 1, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM clinics c
WHERE NOT EXISTS (
    SELECT 1 FROM clinical_document_templates t
    WHERE t.clinic_id = c.id AND t.name = 'Atestado'
);

INSERT INTO clinical_document_templates (
    id, clinic_id, type, name, title_template, body_template, favorite, active,
    version_number, created_by_user_id, created_at, updated_at
)
SELECT UUID(), c.id, 'ATTENDANCE_DECLARATION', 'Declaração de comparecimento',
       'Declaração de comparecimento - {{paciente.nome}}',
       'Declaro, para os devidos fins, que {{paciente.nome}} compareceu a esta clínica em {{consulta.data}} para atendimento.\n\n{{profissional.nome}}\n{{profissional.conselho}}',
       FALSE, TRUE, 1, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM clinics c
WHERE NOT EXISTS (
    SELECT 1 FROM clinical_document_templates t
    WHERE t.clinic_id = c.id AND t.name = 'Declaração de comparecimento'
);

INSERT INTO clinical_document_templates (
    id, clinic_id, type, name, title_template, body_template, favorite, active,
    version_number, created_by_user_id, created_at, updated_at
)
SELECT UUID(), c.id, 'PRESCRIPTION', 'Receita simples',
       'Receita - {{paciente.nome}}',
       'Paciente: {{paciente.nome}}\n\n[Informe o medicamento, a apresentação, a via, a dose, a frequência e a duração.]\n\n{{profissional.nome}}\n{{profissional.conselho}}',
       FALSE, TRUE, 1, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM clinics c
WHERE NOT EXISTS (
    SELECT 1 FROM clinical_document_templates t
    WHERE t.clinic_id = c.id AND t.name = 'Receita simples'
);

INSERT INTO clinical_document_templates (
    id, clinic_id, type, name, title_template, body_template, favorite, active,
    version_number, created_by_user_id, created_at, updated_at
)
SELECT UUID(), c.id, 'FREE_DOCUMENT', 'Documento livre',
       'Documento - {{paciente.nome}}',
       'Paciente: {{paciente.nome}}\nData: {{documento.data}}\n\n[Digite o conteúdo do documento.]\n\n{{profissional.nome}}\n{{profissional.conselho}}',
       FALSE, TRUE, 1, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM clinics c
WHERE NOT EXISTS (
    SELECT 1 FROM clinical_document_templates t
    WHERE t.clinic_id = c.id AND t.name = 'Documento livre'
);
