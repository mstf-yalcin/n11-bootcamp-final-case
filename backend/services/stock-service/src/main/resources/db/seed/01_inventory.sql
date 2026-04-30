-- Seed inventory for product-service products
-- product IDs come from product-service seed (must be kept in sync)

INSERT INTO inventories (id, product_id, quantity, reserved, is_active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000001', 50, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000002', 30, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000003', 20, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000004', 15, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000005', 100, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000006', 75, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000007', 40, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000008', 60, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000009', 25, 0, true, NOW(), NOW()),
    (gen_random_uuid(), 'a1000000-0000-0000-0000-000000000010', 10, 0, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
