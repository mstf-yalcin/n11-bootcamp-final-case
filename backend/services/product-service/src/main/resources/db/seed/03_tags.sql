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
    -- Mevcut ID'ler TR isimlere remap'lenir; FK'lar id üzerinden korunur.
    ('22222222-0000-0000-0000-000000000001', 'Elektronik',          'elektronik',          TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000002', 'Ses',                 'ses',                 TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000003', 'Bilgisayar Aksesuarı','bilgisayar-aksesuari',TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000004', 'Günlük',              'gunluk',              TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000005', 'Spor Giyim',          'spor-giyim',          TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000006', 'Yazılım',             'yazilim',             TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000007', 'Mimari',              'mimari',              TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000008', 'Organik',             'organik',             TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000009', 'Yeni Ürün',           'yeni-urun',           TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000010', 'İndirimde',           'indirimde',           TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000011', 'Öne Çıkan',           'one-cikan',           TRUE, NOW(), NOW()),
    -- Yeni tag'ler
    ('22222222-0000-0000-0000-000000000012', 'Bestseller',          'bestseller',          TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000013', 'Kadın',               'kadin',               TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000014', 'Erkek',               'erkek',               TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000015', 'Çocuk',               'cocuk',               TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000016', 'Premium',             'premium',             TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000017', 'Bluetooth',           'bluetooth',           TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000018', 'Kablosuz',            'kablosuz',            TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000019', 'Gaming',              'gaming',              TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000020', 'Mutfak',              'mutfak',              TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000021', 'Dekorasyon',          'dekorasyon',          TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000022', 'Bebek',               'bebek',               TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000023', 'Oyuncak',             'oyuncak',             TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000024', 'Cilt Bakımı',         'cilt-bakimi',         TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000025', 'Makyaj',              'makyaj',              TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000026', 'Parfüm',              'parfum',              TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000027', 'Saat',                'saat',                TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000028', 'Pırlanta',            'pirlanta',            TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000029', 'Altın',               'altin',               TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000030', 'Gümüş',               'gumus',               TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000031', 'Fitness',             'fitness',             TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000032', 'Kamp',                'kamp',                TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000033', 'Yoga',                'yoga',                TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000034', 'Koşu',                'kosu',                TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000035', 'Oto Aksesuar',        'oto-aksesuar',        TRUE, NOW(), NOW()),
    ('22222222-0000-0000-0000-000000000036', 'Motorsiklet',         'motorsiklet',         TRUE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    slug = EXCLUDED.slug;
