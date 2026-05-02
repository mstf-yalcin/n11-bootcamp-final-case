CREATE TABLE IF NOT EXISTS categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

-- Backfill for existing deployments where the column didn't exist yet.
ALTER TABLE categories ADD COLUMN IF NOT EXISTS slug VARCHAR(120);
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'categories_slug_key'
    ) THEN
        ALTER TABLE categories ADD CONSTRAINT categories_slug_key UNIQUE (slug);
    END IF;
END $$;

INSERT INTO categories (id, name, slug, description, is_active, created_at, updated_at)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'Electronics',      'electronics',      'Phones, computers, accessories',  TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000002', 'Clothing',         'clothing',         'Men, women and kids apparel',     TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000003', 'Books',            'books',            'Fiction, non-fiction, education', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000004', 'Food & Beverage',  'food-beverage',    'Grocery and drinks',              TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000005', 'Home & Garden',    'home-garden',      'Furniture and outdoor',           TRUE, NOW(), NOW())
ON CONFLICT (name) DO UPDATE SET slug = EXCLUDED.slug;

-- Backfill slug for any rows that may still be NULL (safety net for older DBs).
UPDATE categories
   SET slug = lower(regexp_replace(name, '[^a-zA-Z0-9]+', '-', 'g'))
 WHERE slug IS NULL;

ALTER TABLE categories ALTER COLUMN slug SET NOT NULL;
