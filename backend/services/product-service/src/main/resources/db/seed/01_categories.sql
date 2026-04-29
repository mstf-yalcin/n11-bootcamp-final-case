CREATE TABLE IF NOT EXISTS categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO categories (id, name, description, is_active, created_at, updated_at)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'Electronics',      'Phones, computers, accessories',  TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000002', 'Clothing',         'Men, women and kids apparel',     TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000003', 'Books',            'Fiction, non-fiction, education', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000004', 'Food & Beverage',  'Grocery and drinks',              TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000005', 'Home & Garden',    'Furniture and outdoor',           TRUE, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;
