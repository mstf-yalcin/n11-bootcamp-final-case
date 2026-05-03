-- Seed stock for product-service products.
-- Ürün UUID'leri product-service'in 02_products.sql ile senkron tutulmalı.
-- Dağılım kuralları:
--   * Çoğunluk:  IN_STOCK   (10+ adet)
--   * Bazıları:  LOW_STOCK  (1-9 adet) — kıt veya premium ürünler
--   * Bazıları:  Stok kaydı YOK (out-of-stock / unknown) — bilinçli atlanır

INSERT INTO stocks (id, product_id, quantity, reserved, is_active, created_at, updated_at)
VALUES
    -- ========== ELEKTRONİK ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000001',  45, 0, true, NOW(), NOW()),  -- kablosuz-kulaklik
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000002',  22, 0, true, NOW(), NOW()),  -- mekanik-klavye
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000003',  80, 0, true, NOW(), NOW()),  -- usb-c-hub
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000001',  18, 0, true, NOW(), NOW()),  -- akilli-telefon
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000002',   3, 0, true, NOW(), NOW()),  -- LOW: dizustu-bilgisayar
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000003',  95, 0, true, NOW(), NOW()),  -- akilli-saat
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000004', 200, 0, true, NOW(), NOW()),  -- kablosuz-kulakici-tws
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000005',  12, 0, true, NOW(), NOW()),  -- gaming-monitor
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000006',  67, 0, true, NOW(), NOW()),  -- bluetooth-hoparlor
    -- (33333333-0000-0000-0000-100000000007)  harici-ssd-1tb         → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000008', 145, 0, true, NOW(), NOW()),  -- kablosuz-mouse
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000009',  22, 0, true, NOW(), NOW()),  -- tablet-11-inc

    -- ========== MODA ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000004', 150, 0, true, NOW(), NOW()),  -- klasik-beyaz-tisort
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000005',  65, 0, true, NOW(), NOW()),  -- slim-fit-jeans
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000006',  30, 0, true, NOW(), NOW()),  -- running-jacket
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000010',  14, 0, true, NOW(), NOW()),  -- erkek-deri-ceket
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000011',  80, 0, true, NOW(), NOW()),  -- spor-ayakkabi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000012',  23, 0, true, NOW(), NOW()),  -- kadin-deri-canta
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000013',   5, 0, true, NOW(), NOW()),  -- LOW: ipek-bluz
    -- (33333333-0000-0000-0000-100000000014)  midi-etek-kadin        → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000015',  78, 0, true, NOW(), NOW()),  -- yazlik-elbise
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000016', 110, 0, true, NOW(), NOW()),  -- erkek-chino
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000017',   2, 0, true, NOW(), NOW()),  -- LOW: yun-kazak
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000018', 220, 0, true, NOW(), NOW()),  -- beyzbol-sapkasi

    -- ========== KİTAP & MÜZİK ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000007',  40, 0, true, NOW(), NOW()),  -- temiz-kod
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000008',  25, 0, true, NOW(), NOW()),  -- ddia
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000019',  35, 0, true, NOW(), NOW()),  -- pragmatik-programci
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000020',  18, 0, true, NOW(), NOW()),  -- ddd
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000021', 145, 0, true, NOW(), NOW()),  -- sapiens
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000022',  90, 0, true, NOW(), NOW()),  -- suc-ve-ceza
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000023',  65, 0, true, NOW(), NOW()),  -- beyaz-dis
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000024',   6, 0, true, NOW(), NOW()),  -- LOW: vinyl-beatles
    -- (33333333-0000-0000-0000-100000000025)  vinyl-pink-floyd       → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000026',  12, 0, true, NOW(), NOW()),  -- akustik-gitar
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000027',   4, 0, true, NOW(), NOW()),  -- LOW: dijital-piyano
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000028',  87, 0, true, NOW(), NOW()),  -- roman-bestseller

    -- ========== ANNE & BEBEK ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000029',  25, 0, true, NOW(), NOW()),  -- bebek-arabasi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000030',  18, 0, true, NOW(), NOW()),  -- mama-sandalyesi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000031', 350, 0, true, NOW(), NOW()),  -- bebek-bezi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000032',  67, 0, true, NOW(), NOW()),  -- biberon-seti
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000033', 134, 0, true, NOW(), NOW()),  -- bebek-battaniyesi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000034',   3, 0, true, NOW(), NOW()),  -- LOW: pelus-ayi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000035',  56, 0, true, NOW(), NOW()),  -- lego-classic
    -- (33333333-0000-0000-0000-100000000036)  bebek-bakim-cantasi    → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000037',  89, 0, true, NOW(), NOW()),  -- bebek-tulumu
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000038', 200, 0, true, NOW(), NOW()),  -- emzik-seti
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000039',  14, 0, true, NOW(), NOW()),  -- bebek-yatagi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000040',  76, 0, true, NOW(), NOW()),  -- oyuncak-arabalar

    -- ========== EV & YAŞAM ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000009', 120, 0, true, NOW(), NOW()),  -- yesil-cay
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000010',   4, 0, true, NOW(), NOW()),  -- LOW: zeytinyagi
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000011',  60, 0, true, NOW(), NOW()),  -- led-masa-lambasi
    (gen_random_uuid(), '33333333-0000-0000-0000-000000000012',  35, 0, true, NOW(), NOW()),  -- seramik-saksi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000041',   6, 0, true, NOW(), NOW()),  -- LOW: kanepe (büyük ürün)
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000042',  45, 0, true, NOW(), NOW()),  -- yemek-takimi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000043',  78, 0, true, NOW(), NOW()),  -- nevresim
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000044',  11, 0, true, NOW(), NOW()),  -- kahve-makinesi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000045',  23, 0, true, NOW(), NOW()),  -- blender
    -- (33333333-0000-0000-0000-100000000046)  dekoratif-cerceve      → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000047', 165, 0, true, NOW(), NOW()),  -- el-yapimi-mum

    -- ========== KOZMETİK & KİŞİSEL BAKIM ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000048', 287, 0, true, NOW(), NOW()),  -- hyaluronik-serum
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000049', 156, 0, true, NOW(), NOW()),  -- mat-ruj
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000050',  38, 0, true, NOW(), NOW()),  -- erkek-parfum
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000051',   7, 0, true, NOW(), NOW()),  -- LOW: anti-aging
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000052',  64, 0, true, NOW(), NOW()),  -- makyaj-fircasi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000053', 215, 0, true, NOW(), NOW()),  -- gunes-kremi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000054',  88, 0, true, NOW(), NOW()),  -- goz-far-paleti
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000055', 320, 0, true, NOW(), NOW()),  -- el-kremi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000056', 175, 0, true, NOW(), NOW()),  -- sampuan
    -- (33333333-0000-0000-0000-100000000057)  argan-yagi             → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000058', 124, 0, true, NOW(), NOW()),  -- maskara
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000059',   4, 0, true, NOW(), NOW()),  -- LOW: kadin-parfum

    -- ========== MÜCEVHER & SAAT ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000060',   2, 0, true, NOW(), NOW()),  -- LOW: pirlanta-tek-tas (lüks)
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000061',  18, 0, true, NOW(), NOW()),  -- altin-kalp-kolye
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000062',  56, 0, true, NOW(), NOW()),  -- gumus-bilezik
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000063',   8, 0, true, NOW(), NOW()),  -- LOW: mekanik-saat
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000064',  34, 0, true, NOW(), NOW()),  -- spor-erkek-saat
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000065',  67, 0, true, NOW(), NOW()),  -- incili-kupe
    -- (33333333-0000-0000-0000-100000000066)  altin-bilezik-22-ayar  → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000067',   5, 0, true, NOW(), NOW()),  -- LOW: pirlanta-kupe
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000068',  89, 0, true, NOW(), NOW()),  -- nazar-kolye
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000069',  22, 0, true, NOW(), NOW()),  -- kadin-altin-saat
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000070', 134, 0, true, NOW(), NOW()),  -- gumus-tasli-yuzuk
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000071',   3, 0, true, NOW(), NOW()),  -- LOW: altin-kelepce

    -- ========== SPOR & OUTDOOR ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000072', 198, 0, true, NOW(), NOW()),  -- yoga-mati
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000073',  76, 0, true, NOW(), NOW()),  -- kosu-ayakkabi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000074',  23, 0, true, NOW(), NOW()),  -- dambil
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000075',  41, 0, true, NOW(), NOW()),  -- kettlebell
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000076', 320, 0, true, NOW(), NOW()),  -- spor-tisort
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000077', 254, 0, true, NOW(), NOW()),  -- spor-corap
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000078',   6, 0, true, NOW(), NOW()),  -- LOW: kamp-cadiri
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000079',  18, 0, true, NOW(), NOW()),  -- uyku-tulumu
    -- (33333333-0000-0000-0000-100000000080)  mountain-bike          → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000081', 167, 0, true, NOW(), NOW()),  -- silikon-tabanlik
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000082',  88, 0, true, NOW(), NOW()),  -- pilates-topu
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000083',  12, 0, true, NOW(), NOW()),  -- trekking-bot

    -- ========== OTOMOTİV & MOTORSİKLET ==========
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000084',  24, 0, true, NOW(), NOW()),  -- motorsiklet-kaski
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000085',  18, 0, true, NOW(), NOW()),  -- aku
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000086', 134, 0, true, NOW(), NOW()),  -- motor-yagi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000087',   5, 0, true, NOW(), NOW()),  -- LOW: lastik-yaz
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000088', 167, 0, true, NOW(), NOW()),  -- silecek
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000089',  56, 0, true, NOW(), NOW()),  -- oto-koltuk-kilifi
    -- (33333333-0000-0000-0000-100000000090)  bagaj-kilifi           → STOK YOK
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000091',  78, 0, true, NOW(), NOW()),  -- motor-eldiveni
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000092', 410, 0, true, NOW(), NOW()),  -- arac-deri-kokulu
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000093',  28, 0, true, NOW(), NOW()),  -- kit-anahtar
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000094',  92, 0, true, NOW(), NOW()),  -- araba-cilasi
    (gen_random_uuid(), '33333333-0000-0000-0000-100000000095',   4, 0, true, NOW(), NOW())   -- LOW: motor-tisort
ON CONFLICT (product_id) DO NOTHING;
