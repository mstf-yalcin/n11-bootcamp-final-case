CREATE TABLE IF NOT EXISTS products (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           VARCHAR(255)    NOT NULL UNIQUE,
    name           VARCHAR(255)    NOT NULL,
    description    TEXT,
    price          NUMERIC(10, 2)  NOT NULL,
    currency       VARCHAR(3)      NOT NULL DEFAULT 'TRY',
    rating_count   INTEGER         NOT NULL DEFAULT 0,
    rating_average NUMERIC(3, 2)   NOT NULL DEFAULT 0.00,
    image_url      VARCHAR(512),
    is_active      BOOLEAN         NOT NULL DEFAULT TRUE,
    category_id    UUID            NOT NULL REFERENCES categories(id),
    created_at     TIMESTAMP WITH TIME ZONE,
    updated_at     TIMESTAMP WITH TIME ZONE
);

INSERT INTO products (id, slug, name, description, price, currency, rating_count, rating_average, image_url, is_active, category_id, created_at, updated_at)
VALUES
    ('33333333-0000-0000-0000-000000000001', 'wireless-headphones',               'Wireless Headphones',             'Noise-cancelling over-ear headphones',       999.99,  'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),
    ('33333333-0000-0000-0000-000000000002', 'mechanical-keyboard',               'Mechanical Keyboard',             'TKL layout, red switches',                  1499.00, 'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),
    ('33333333-0000-0000-0000-000000000003', 'usb-c-hub-7-in-1',                  'USB-C Hub 7-in-1',                'HDMI, USB-A, SD card, PD charging',          349.00,  'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000004', 'classic-white-t-shirt',             'Classic White T-Shirt',           '100% cotton, unisex fit',                    129.90,  'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),
    ('33333333-0000-0000-0000-000000000005', 'slim-fit-jeans',                    'Slim Fit Jeans',                  'Stretch denim, dark blue',                   399.00,  'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),
    ('33333333-0000-0000-0000-000000000006', 'running-jacket',                    'Running Jacket',                  'Windproof, lightweight',                     599.00,  'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000007', 'clean-code',                        'Clean Code',                      'R. Martin — software craftsmanship',          89.90,   'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),
    ('33333333-0000-0000-0000-000000000008', 'designing-data-intensive-applications', 'Designing Data-Intensive Applications', 'M. Kleppmann',                    149.00,  'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000009', 'organic-green-tea',                 'Organic Green Tea',               '50 bags, single-origin Rize',                 49.90,   'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),
    ('33333333-0000-0000-0000-000000000010', 'extra-virgin-olive-oil',            'Extra Virgin Olive Oil',          '500ml, cold-pressed',                         89.00,   'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000011', 'desk-lamp-led',                     'Desk Lamp LED',                   'Adjustable color temp, USB charging',        279.00,  'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),
    ('33333333-0000-0000-0000-000000000012', 'ceramic-plant-pot-set',             'Ceramic Plant Pot Set',           'Set of 3, matte finish',                     159.00,  'TRY', 0, 0.00, NULL, TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;
