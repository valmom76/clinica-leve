CREATE TABLE material_categories (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_material_categories_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT uk_material_categories_clinic_name UNIQUE (clinic_id, name)
) ENGINE=InnoDB;

CREATE TABLE materials (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    sku VARCHAR(80),
    unit VARCHAR(30) NOT NULL,
    minimum_stock DECIMAL(14,3) NOT NULL DEFAULT 0,
    current_stock DECIMAL(14,3) NOT NULL DEFAULT 0,
    lot_controlled BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_materials_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_materials_category FOREIGN KEY (category_id) REFERENCES material_categories (id),
    CONSTRAINT uk_materials_clinic_sku UNIQUE (clinic_id, sku),
    INDEX ix_materials_clinic_name (clinic_id, name),
    INDEX ix_materials_clinic_category (clinic_id, category_id)
) ENGINE=InnoDB;

CREATE TABLE material_batches (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    material_id VARCHAR(36) NOT NULL,
    lot_number VARCHAR(80) NOT NULL,
    expiration_date DATE,
    current_quantity DECIMAL(14,3) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_material_batches_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_material_batches_material FOREIGN KEY (material_id) REFERENCES materials (id),
    CONSTRAINT uk_material_batches_lot UNIQUE (clinic_id, material_id, lot_number),
    INDEX ix_material_batches_expiration (clinic_id, expiration_date),
    INDEX ix_material_batches_quantity (material_id, current_quantity)
) ENGINE=InnoDB;

CREATE TABLE stock_movements (
    id VARCHAR(36) NOT NULL,
    clinic_id VARCHAR(36) NOT NULL,
    material_id VARCHAR(36) NOT NULL,
    batch_id VARCHAR(36),
    movement_type VARCHAR(20) NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    balance_after DECIMAL(14,3) NOT NULL,
    reason VARCHAR(300) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT fk_stock_movements_material FOREIGN KEY (material_id) REFERENCES materials (id),
    CONSTRAINT fk_stock_movements_batch FOREIGN KEY (batch_id) REFERENCES material_batches (id),
    CONSTRAINT fk_stock_movements_user FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    INDEX ix_stock_movements_material_date (clinic_id, material_id, occurred_at),
    INDEX ix_stock_movements_clinic_date (clinic_id, occurred_at)
) ENGINE=InnoDB;
