CREATE TABLE IF NOT EXISTS categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    image_url   VARCHAR(1024),
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

-- Backfill for existing deployments where the columns didn't exist yet.
ALTER TABLE categories ADD COLUMN IF NOT EXISTS slug      VARCHAR(120);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS image_url VARCHAR(1024);
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_slug_key;
ALTER TABLE categories ADD CONSTRAINT categories_slug_key UNIQUE (slug);

-- Existing IDs 1-5 are remapped to the closest new category so that
-- referencing products keep their FK valid. ON CONFLICT (id) DO UPDATE
-- ensures already-seeded DBs are migrated to the new naming/imagery.
INSERT INTO categories (id, name, slug, description, image_url, is_active, created_at, updated_at)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'Elektronik',                'elektronik',                'Telefon, bilgisayar, kulaklık ve daha fazlası',           'https://n11scdn.akamaized.net/a1/30/22/08/01/53/46/97/55/78/81/00/58/49/31961828233979739225.png', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000002', 'Moda',                      'moda',                      'Kadın, erkek ve çocuk giyim, ayakkabı ve aksesuar',       'https://n11scdn.akamaized.net/a1/30/23/04/27/50/92/89/68/98/82/81/83/46/87911743275366201592.png', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000003', 'Kitap & Müzik',             'kitap-muzik',               'Kitap, dergi, müzik ve film ürünleri',                    'https://n11scdn.akamaized.net/a1/30/22/08/01/86/74/46/47/76/61/89/79/84/34021047252181561848.png', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000004', 'Anne & Bebek',              'anne-bebek',                'Bebek bakım, oyuncak, hamile ve çocuk ürünleri',          'https://n11scdn.akamaized.net/a1/30/22/08/01/77/23/79/89/22/11/13/67/06/1817596495358676750.png',  TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000005', 'Ev & Yaşam',                'ev-yasam',                  'Mobilya, mutfak, dekorasyon ve gıda ürünleri',            'https://n11scdn.akamaized.net/a1/30/22/08/01/86/32/24/29/29/47/16/41/05/57126929366806936432.png', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000006', 'Kozmetik & Kişisel Bakım',  'kozmetik-kisisel-bakim',    'Cilt bakım, makyaj, parfüm ve kişisel bakım',             'https://n11scdn.akamaized.net/a1/30/22/08/01/82/78/22/64/91/17/58/84/05/11823586115151480719.png', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000007', 'Mücevher & Saat',           'mucevher-saat',             'Pırlanta, altın takı ve kol saatleri',                    'https://n11scdn.akamaized.net/a1/30/23/05/30/61/78/87/08/84/35/58/58/07/48911776658065241375.png', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000008', 'Spor & Outdoor',            'spor-outdoor',              'Spor giyim, fitness, kamp ve outdoor ekipmanları',        'https://n11scdn.akamaized.net/a1/30/22/08/01/78/48/54/32/73/75/59/66/38/52455961286243405128.png', TRUE, NOW(), NOW()),
    ('11111111-0000-0000-0000-000000000009', 'Otomotiv & Motorsiklet',    'otomotiv-motorsiklet',      'Oto aksesuar, yedek parça ve motorsiklet ürünleri',       'https://n11scdn.akamaized.net/a1/30/22/08/01/84/83/10/49/65/28/06/29/71/92143688675639577247.png', TRUE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    name        = EXCLUDED.name,
    slug        = EXCLUDED.slug,
    description = EXCLUDED.description,
    image_url   = EXCLUDED.image_url;

-- Backfill slug for any rows that may still be NULL (safety net for older DBs).
UPDATE categories
   SET slug = lower(regexp_replace(name, '[^a-zA-Z0-9]+', '-', 'g'))
 WHERE slug IS NULL;

ALTER TABLE categories ALTER COLUMN slug SET NOT NULL;
