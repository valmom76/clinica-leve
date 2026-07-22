INSERT INTO subscription_plans (
    id, code, name, description, billing_cycle, price, trial_days,
    price_guarantee_months, availability_limit, visible, active, display_order,
    created_at, updated_at
)
SELECT
    UUID(), 'PIONEER_MONTHLY', 'Clínica Leve Pioneiro',
    'Condição especial para as primeiras 50 clínicas, com preço garantido por 24 meses.',
    'MONTHLY', 59.90, 30, 24, 50, TRUE, TRUE, 1, NOW(6), NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plans WHERE code = 'PIONEER_MONTHLY'
);

INSERT INTO subscription_plans (
    id, code, name, description, billing_cycle, price, trial_days,
    price_guarantee_months, availability_limit, visible, active, display_order,
    created_at, updated_at
)
SELECT
    UUID(), 'CLINICA_LEVE_MONTHLY', 'Clínica Leve Mensal',
    'Todos os módulos do Clínica Leve, sem cobrança por usuário.',
    'MONTHLY', 79.90, 30, NULL, NULL, TRUE, TRUE, 2, NOW(6), NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plans WHERE code = 'CLINICA_LEVE_MONTHLY'
);

INSERT INTO subscription_plans (
    id, code, name, description, billing_cycle, price, trial_days,
    price_guarantee_months, availability_limit, visible, active, display_order,
    created_at, updated_at
)
SELECT
    UUID(), 'CLINICA_LEVE_YEARLY', 'Clínica Leve Anual',
    'Plano anual com o equivalente a dois meses gratuitos.',
    'YEARLY', 799.00, 30, NULL, NULL, TRUE, TRUE, 3, NOW(6), NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plans WHERE code = 'CLINICA_LEVE_YEARLY'
);
