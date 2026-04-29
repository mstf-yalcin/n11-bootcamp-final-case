CREATE TABLE IF NOT EXISTS tags (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50)  NOT NULL UNIQUE,
    slug       VARCHAR(50)  NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO tags (id, name, slug, is_active, created_at, updated_at)
VALUES
    ('22222222-0000-0000-0000-000000000001', 'Electronics',  'electronics',  TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000002', 'Audio',        'audio',        TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000003', 'Peripherals',  'peripherals',  TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000004', 'Casual',       'casual',       TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000005', 'Activewear',   'activewear',   TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000006', 'Programming',  'programming',  TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000007', 'Architecture', 'architecture', TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000008', 'Organic',      'organic',      TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000009', 'New Arrival',  'new-arrival',  TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000010', 'Sale',         'sale',         TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000011', 'Featured',     'featured',     TRUE, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;
