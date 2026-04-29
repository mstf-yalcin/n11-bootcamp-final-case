CREATE TABLE IF NOT EXISTS product_tags (
    product_id UUID NOT NULL REFERENCES products(id),
    tag_id     UUID NOT NULL REFERENCES tags(id),
    PRIMARY KEY (product_id, tag_id)
);

INSERT INTO product_tags (product_id, tag_id)
SELECT p.id, t.id FROM products p CROSS JOIN tags t
WHERE (p.slug, t.slug) IN (
    ('wireless-headphones',               'audio'),
    ('wireless-headphones',               'electronics'),
    ('wireless-headphones',               'featured'),
    ('mechanical-keyboard',               'peripherals'),
    ('mechanical-keyboard',               'electronics'),
    ('usb-c-hub-7-in-1',                  'peripherals'),
    ('usb-c-hub-7-in-1',                  'electronics'),
    ('usb-c-hub-7-in-1',                  'new-arrival'),
    ('classic-white-t-shirt',             'casual'),
    ('slim-fit-jeans',                    'casual'),
    ('running-jacket',                    'activewear'),
    ('clean-code',                        'programming'),
    ('clean-code',                        'featured'),
    ('designing-data-intensive-applications', 'programming'),
    ('designing-data-intensive-applications', 'architecture'),
    ('organic-green-tea',                 'organic'),
    ('extra-virgin-olive-oil',            'organic'),
    ('extra-virgin-olive-oil',            'sale'),
    ('desk-lamp-led',                     'new-arrival'),
    ('ceramic-plant-pot-set',             'sale')
)
ON CONFLICT (product_id, tag_id) DO NOTHING;
