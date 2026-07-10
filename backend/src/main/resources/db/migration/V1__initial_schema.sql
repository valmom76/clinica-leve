CREATE TABLE clinics (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    timezone VARCHAR(60) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_clinics_slug UNIQUE (slug)
) ENGINE=InnoDB;

CREATE TABLE app_users (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_users_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT uk_users_clinic_email UNIQUE (clinic_id, email)
) ENGINE=InnoDB;

CREATE TABLE specialties (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    color VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_specialties_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT uk_specialties_clinic_name UNIQUE (clinic_id, name)
) ENGINE=InnoDB;

CREATE TABLE professionals (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    specialty_id VARCHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    council VARCHAR(80),
    email VARCHAR(190),
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_professionals_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_professionals_specialty FOREIGN KEY (specialty_id) REFERENCES specialties (id),
    INDEX ix_professionals_clinic_name (clinic_id, name)
) ENGINE=InnoDB;

CREATE TABLE patients (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    cpf VARCHAR(14),
    birth_date DATE,
    email VARCHAR(190),
    phone VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_patients_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    INDEX ix_patients_clinic_name (clinic_id, name),
    INDEX ix_patients_clinic_phone (clinic_id, phone)
) ENGINE=InnoDB;

CREATE TABLE appointments (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    patient_id VARCHAR(36) NOT NULL,
    professional_id VARCHAR(36) NOT NULL,
    specialty_id VARCHAR(36) NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_appointments_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_professional FOREIGN KEY (professional_id) REFERENCES professionals (id),
    CONSTRAINT fk_appointments_specialty FOREIGN KEY (specialty_id) REFERENCES specialties (id),
    INDEX ix_appointments_clinic_start (clinic_id, start_at),
    INDEX ix_appointments_professional_start (professional_id, start_at)
) ENGINE=InnoDB;
