CREATE TABLE IF NOT EXISTS products (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           VARCHAR(255)    NOT NULL UNIQUE,
    name           VARCHAR(255)    NOT NULL,
    description    TEXT,
    price          NUMERIC(10, 2)  NOT NULL,
    currency       VARCHAR(3)      NOT NULL DEFAULT 'TRY',
    rating_count   INTEGER         NOT NULL DEFAULT 0,
    rating_average NUMERIC(3, 2)   NOT NULL DEFAULT 0.00,
    image_url      VARCHAR(1024),
    is_active      BOOLEAN         NOT NULL DEFAULT TRUE,
    category_id    UUID            NOT NULL REFERENCES categories(id),
    created_at     TIMESTAMP WITH TIME ZONE,
    updated_at     TIMESTAMP WITH TIME ZONE
);

ALTER TABLE products ALTER COLUMN image_url TYPE VARCHAR(1024);

INSERT INTO products (id, slug, name, description, price, currency, rating_count, rating_average, image_url, is_active, category_id, created_at, updated_at)
VALUES
    -- ============================================================
    --  ELEKTRONİK  (category_id = 11111111-...-001)
    -- ============================================================
    ('33333333-0000-0000-0000-000000000001', 'kablosuz-kulaklik-bluetooth',     'Kablosuz Kulaklık Bluetooth 5.3',
     'Aktif gürültü engelleme (ANC) destekli kulak üstü Bluetooth 5.3 kulaklık. 30 saate kadar pil ömrü, hızlı şarj ve katlanabilir tasarımıyla seyahat için ideal.',
     1899.00, 'TRY', 1248, 4.60, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000002', 'mekanik-klavye-rgb',              'Mekanik RGB Oyuncu Klavyesi',
     'TKL düzen, kırmızı switch ve programlanabilir RGB aydınlatma ile özel olarak oyuncular için tasarlanmış mekanik klavye. Anti-ghosting ve N-key rollover destekli.',
     1499.00, 'TRY', 542, 4.40, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000003', 'usb-c-hub-7-in-1',                'USB-C Hub 7-in-1',
     'HDMI 4K çıkış, 3 USB-A 3.0, SD/microSD okuyucu ve 100W PD şarj destekli kompakt USB-C hub. MacBook ve tüm modern dizüstüler ile uyumlu.',
     449.00, 'TRY', 318, 3.50, 'https://images.unsplash.com/photo-1625948515291-69613efd103f?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000001', 'akilli-telefon-256gb',            'Akıllı Telefon 256 GB 5G',
     '6.7 inç AMOLED 120Hz ekran, üçlü 50MP kamera ve 5000 mAh batarya. 5G destekli işlemci ve 12 GB RAM ile akıcı performans sunar.',
     34990.00, 'TRY', 2103, 4.70, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000002', 'dizustu-bilgisayar-14-inc',       'Dizüstü Bilgisayar 14" 16 GB RAM',
     '14 inç Retina ekran, 8 çekirdekli işlemci, 16 GB unified memory ve 512 GB SSD. Tüm gün süren pil ömrü ve fansız sessiz çalışma.',
     42999.00, 'TRY', 876, 4.80, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000003', 'akilli-saat-spor',                'Akıllı Saat Spor Sürümü',
     'Kalp atışı, SpO2 ve uyku takibi yapan AMOLED ekranlı akıllı saat. 70+ spor modu, suya dayanıklılık ve 14 güne kadar pil ömrü.',
     2799.00, 'TRY', 1554, 4.50, 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000004', 'kablosuz-kulakici-tws',           'Kablosuz Kulakiçi TWS',
     'Aktif gürültü engelleme, şarj kutusuyla 30 saat pil, IPX5 suya dayanıklılık ve dokunmatik kontrol. Çift cihaz eşleme desteği bulunur.',
     1349.00, 'TRY', 1986, 4.40, 'https://images.unsplash.com/photo-1572569511254-d8f925fe2cbb?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000005', 'gaming-monitor-27',               'Oyuncu Monitörü 27" 165 Hz',
     '27 inç QHD IPS panel, 165 Hz tazeleme hızı, 1 ms tepki süresi ve FreeSync Premium. HDR400 desteği ile gerçekçi renkler ve canlı oyun deneyimi.',
     8990.00, 'TRY', 412, 4.60, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000006', 'bluetooth-hoparlor-tasinabilir',  'Taşınabilir Bluetooth Hoparlör',
     'IPX7 suya dayanıklı, 20 saat çalma süresi ve PartyBoost özelliği ile birden fazla hoparlörü eşleştirerek geniş ses sahası oluşturur.',
     2199.00, 'TRY', 980, 4.50, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000007', 'harici-ssd-1tb',                  'Harici SSD 1 TB Type-C',
     '1050 MB/sn okuma hızı sunan kompakt harici SSD. USB 3.2 Gen 2 Type-C ile geniş cihaz uyumluluğu, darbeye dayanıklı metal kasa.',
     1799.00, 'TRY', 233, 4.70, 'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000008', 'kablosuz-mouse-ergonomik',        'Kablosuz Ergonomik Mouse',
     '2.4 GHz alıcı ve Bluetooth çift bağlantı, 4000 DPI hassasiyet ve sessiz tıklama. Ergonomik gövde uzun kullanımda bilek yorgunluğunu azaltır.',
     549.00, 'TRY', 612, 3.40, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000009', 'tablet-11-inc',                   'Tablet 11" 128 GB Wi-Fi',
     '11 inç IPS ekran, sekiz çekirdekli işlemci ve 8 GB RAM. Çocuk profili, ebeveyn kontrolü ve dahili kalem desteği ile çok yönlü kullanım sağlar.',
     12999.00, 'TRY', 444, 4.40, 'https://images.unsplash.com/photo-1561154464-82e9adf32764?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000001', NOW(), NOW()),

    -- ============================================================
    --  MODA  (category_id = 11111111-...-002)
    -- ============================================================
    ('33333333-0000-0000-0000-000000000004', 'klasik-beyaz-tisort',             'Klasik Beyaz Tişört',
     '%100 organik pamuktan üretilmiş, unisex regular fit basic tişört. Boyamada solmaya dayanıklı, tüm kombinlerle kolayca eşleşir.',
     199.90, 'TRY', 832, 3.60, 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000005', 'slim-fit-kot-pantolon',           'Slim Fit Kot Pantolon',
     'Streç denim kumaştan slim fit kalıp, koyu mavi indigo yıkama. Gün boyu konforlu kullanım için %2 elastan içerir.',
     549.00, 'TRY', 410, 4.30, 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000006', 'kosu-rüzgarliği',                 'Koşu Rüzgarlığı',
     'Hafif, rüzgar geçirmez ve su itici dış cephe kumaşı. Reflektif detaylar gece koşularında güvenliği artırır, sırt fermuarlı cep telefonu için pratik.',
     799.00, 'TRY', 327, 4.40, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000010', 'erkek-deri-ceket',                'Erkek Deri Biker Ceket',
     'Suni deri biker model erkek ceket. Astarlı iç yüzey, fonksiyonel cepler ve metal fermuar detayları ile hem rock hem klasik tarza uyum sağlar.',
     2199.00, 'TRY', 248, 4.50, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000011', 'spor-ayakkabi-koleksiyon',        'Erkek Spor Ayakkabı',
     'Hava yastıklı taban, nefes alabilen örgü üst yüzey ve esnek kalıp. Hem koşu hem günlük kullanım için hafif ve konforlu sneaker.',
     1499.00, 'TRY', 1872, 4.60, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000012', 'kadin-deri-omuz-cantasi',         'Kadın Deri Omuz Çantası',
     'Hakiki vegan deri omuz çantası, ayarlanabilir askı, 3 bölmeli iç tasarım. Günlük ve iş kullanımına uygun zarif duruşlu medium boy.',
     899.00, 'TRY', 391, 4.40, 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000013', 'ipek-bluz-kadin',                 'İpek Görünümlü Bluz',
     'Akıcı dökümlü saten kumaş, klasik yaka ve uzun kollu kesim. Ofis ve davet kombinlerinde zarif bir alternatif sunar.',
     449.00, 'TRY', 156, 4.30, 'https://images.unsplash.com/photo-1551048632-24e444b48a3e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000014', 'midi-etek-kadin',                 'Midi Pileli Etek',
     'Lastikli bel detayı ve dökümlü pileli kesim. Bot, sneaker veya topuklu ile farklı stillerde kombinlenebilir.',
     379.00, 'TRY', 198, 2.10, 'https://images.unsplash.com/photo-1583496661160-fb5886a13d27?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000015', 'yazlik-elbise-cicekli',           'Yazlık Çiçekli Elbise',
     'Hafif viskon kumaş, ayarlanabilir askı ve fırfırlı etek detayı. Yaz tatilleri ve günlük şehir kombinleri için ideal.',
     629.00, 'TRY', 274, 3.40, 'https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000016', 'erkek-chino-pantolon',            'Erkek Chino Pantolon',
     'Slim fit kesim, %98 pamuk %2 elastan kumaş. Gardırobun temel parçası — gömlek ile şık, tişört ile spor kombinler için uygun.',
     499.00, 'TRY', 311, 3.50, 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000017', 'yun-kazak-unisex',                'Yün Karışımlı Kazak',
     'Yumuşak akrilik-yün karışımı, balıkçı yaka kazak. Soğuk havalarda uzun süreli sıcaklık ve şık görünüm sağlar.',
     689.00, 'TRY', 187, 3.30, 'https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000018', 'beyzbol-sapkasi',                 'Beyzbol Şapkası',
     'Ayarlanabilir arka kapamalı, %100 pamuklu beyzbol şapkası. Spor, kamp ve günlük kullanım için klasik kesim.',
     249.00, 'TRY', 432, 2.50, 'https://images.unsplash.com/photo-1521369909029-2afed882baee?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000002', NOW(), NOW()),

    -- ============================================================
    --  KİTAP & MÜZİK  (category_id = 11111111-...-003)
    -- ============================================================
    ('33333333-0000-0000-0000-000000000007', 'temiz-kod-clean-code',            'Temiz Kod (Clean Code)',
     'Robert C. Martin''ın yazılım zanaatkarlığı klasiği. İyi kodun nasıl yazılacağı, ne zaman refactor edileceği ve okunabilirliğin önemine dair pratik öneriler.',
     189.90, 'TRY', 1342, 4.80, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000008', 'designing-data-intensive-apps',   'Designing Data-Intensive Applications',
     'Martin Kleppmann''dan veri sistemlerinin temellerine derinlemesine bir bakış. Replikasyon, partitioning, transactions ve stream processing konularını kapsar.',
     349.00, 'TRY', 2150, 4.90, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000019', 'pragmatik-programci',             'Pragmatik Programcı',
     'Andrew Hunt ve David Thomas''tan zamansız bir yazılım geliştirme klasiği. Pratik ipuçları, kariyer rehberi ve yazılım felsefesi ile dolu.',
     249.00, 'TRY', 678, 4.70, 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000020', 'domain-driven-design',            'Domain-Driven Design',
     'Eric Evans''ın yazılım modellemesini iş alanı odaklı yaklaşan başyapıt eseri. Bounded context, aggregate ve event storming için temel kaynak.',
     449.00, 'TRY', 411, 4.80, 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000021', 'sapiens-yuval-noah-harari',       'Sapiens: Hayvanlardan Tanrılara',
     'Yuval Noah Harari''nin insanlık tarihini bilişsel devrimden günümüze taşıyan dünyaca bestseller eseri. 100''den fazla dilde çevrildi.',
     189.00, 'TRY', 3201, 4.70, 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000022', 'suc-ve-ceza-dostoyevski',         'Suç ve Ceza',
     'Dostoyevski''nin psikolojik gerilim klasiği. Petersburg sokaklarında bir öğrencinin işlediği cinayet ve onu izleyen vicdani sorgulama.',
     145.00, 'TRY', 1890, 4.80, 'https://images.unsplash.com/photo-1474932430478-367dbb6832c1?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000023', 'beyaz-dis-jack-london',           'Beyaz Diş',
     'Jack London''ın doğa ve hayatta kalma teması üzerine kurduğu klasiği. Vahşi yaşamla evcilleşmenin sınırında bir kurt-köpek hikayesi.',
     89.00, 'TRY', 645, 4.60, 'https://images.unsplash.com/photo-1535905557558-afc4877a26fc?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000024', 'vinyl-the-beatles-abbey-road',    'The Beatles - Abbey Road (Vinyl)',
     'Efsanevi Abbey Road albümünün remastered 180g vinyl baskısı. Koleksiyonerler için yüksek kaliteli üretim ve orijinal kapak tasarımı.',
     899.00, 'TRY', 312, 4.90, 'https://images.unsplash.com/photo-1535992165812-68d1861aa71e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000025', 'vinyl-pink-floyd-dark-side',      'Pink Floyd - The Dark Side of the Moon (Vinyl)',
     '50. yıl yeniden basım vinyl. Audiophile kalite, gatefold kapak ve büyük boy poster içerikli özel sürüm.',
     1099.00, 'TRY', 256, 4.90, 'https://images.unsplash.com/photo-1488841714725-bb4c32d1ac94?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000026', 'akustik-gitar-baslangic',         'Akustik Gitar Başlangıç Seti',
     '4/4 boy klasik akustik gitar, kılıf, akort aleti, yedek tel ve pena seti dahil. Yeni başlayanlar için pratik komple paket.',
     2299.00, 'TRY', 421, 4.50, 'https://images.unsplash.com/photo-1510915361894-db8b60106cb1?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000027', 'dijital-piyano-88-tus',           'Dijital Piyano 88 Tuş',
     '88 tam boy hammer-action tuş, 200+ enstrüman sesi, 50 ritim ve dahili Bluetooth. USB-MIDI ile bilgisayar bağlantısı.',
     8499.00, 'TRY', 178, 4.70, 'https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000028', 'roman-bestseller-set-5li',        '5''li Bestseller Roman Seti',
     'Geçtiğimiz yılın en çok satan 5 modern romanından oluşan özel set. Hem hediye hem kişisel okuma için ekonomik paket fiyatı.',
     549.00, 'TRY', 287, 4.40, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000003', NOW(), NOW()),

    -- ============================================================
    --  ANNE & BEBEK  (category_id = 11111111-...-004)
    -- ============================================================
    ('33333333-0000-0000-0000-100000000029', 'bebek-arabasi-3-tekerlekli',      'Bebek Arabası 3 Tekerlekli',
     'Tek elle katlanır, alüminyum şasi 3 tekerlekli puset. 5 noktalı emniyet kemeri, ayarlanabilir sırt ve büyük güneşlik dahil.',
     5499.00, 'TRY', 432, 4.60, 'https://images.unsplash.com/photo-1492725764893-90b379c2b6e7?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000030', 'mama-sandalyesi-ahsap',           'Bebek Mama Sandalyesi (Ahşap)',
     '6 ay - 6 yaş arası kullanılabilen yüksekliği ayarlanabilir ahşap mama sandalyesi. Çıkarılabilir tepsi ve yıkanabilir minder.',
     2199.00, 'TRY', 287, 4.70, 'https://images.unsplash.com/photo-1545558014-8692077e9b5c?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000031', 'bebek-bezi-3lu-paket',            'Bebek Bezi 3''lü Paket',
     '4 numara (7-18 kg) bebek bezi, 3 paket (toplam 168 adet). Süper emici, yumuşak iç yüzey ve sızdırmaz kanal sistemi.',
     899.00, 'TRY', 1543, 3.70, 'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000032', 'biberon-seti-3lu',                'Biberon Seti 3''lü (PPSU)',
     'BPA içermeyen PPSU malzemeden üretilmiş anti-kolik vanalı biberon seti. 150ml ve 240ml boyutlarda, S/M/L emzik içerir.',
     549.00, 'TRY', 768, 3.40, 'https://images.unsplash.com/photo-1607924400119-c1eaedd7415e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000033', 'bebek-battaniyesi-mussolin',      'Bebek Battaniyesi Müslin',
     '%100 organik pamuk müslin kumaş, çok katlı yumuşak dokulu battaniye. Yıkamaya dayanıklı, hipoalerjenik ve hava geçirgen.',
     279.00, 'TRY', 411, 4.70, 'https://images.unsplash.com/photo-1522771930-78848d9293e8?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000034', 'pelus-ayi-buyuk-boy',             'Peluş Ayı 80 cm',
     'Yumuşak peluş kumaş, sünger dolgulu büyük boy oyuncak ayı. CE sertifikalı, makinede yıkanabilir, hipoalerjenik.',
     449.00, 'TRY', 632, 4.80, 'https://images.unsplash.com/photo-1559454403-b8fb88521f17?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000035', 'lego-classic-yaratici',           'LEGO Classic Yaratıcı Kutu',
     '484 parçalı LEGO Classic seti. Sınırsız yaratıcılık için 33 farklı renkte parça, 4 yaş ve üzeri çocuklar için ideal.',
     799.00, 'TRY', 1287, 4.90, 'https://images.unsplash.com/photo-1580477667995-2b94f01c9516?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000036', 'bebek-bakim-cantasi',             'Bebek Bakım Çantası',
     'Çok bölmeli, su itici kumaşlı anne çantası. Termal şişe bölmesi, ıslak mendil cebi ve omuz/sırt çantası dönüşümlü askı.',
     689.00, 'TRY', 234, 4.50, 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000037', 'bebek-tulumu-3lu',                'Yenidoğan Tulum 3''lü Set',
     '%100 organik pamuktan üretilmiş yenidoğan (0-3 ay) tulum seti. Çıtçıt detaylı, etiketsiz iç yüzey, GOTS sertifikalı.',
     349.00, 'TRY', 543, 4.70, 'https://images.unsplash.com/photo-1519689680058-324335c77eba?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000038', 'emzik-seti-2li',                  'Emzik Seti 2''li',
     '0-6 ay için silikon emzik, ortodontik tasarım. Steril edilebilir, BPA-free, tutucu zincir hediye.',
     129.00, 'TRY', 387, 2.50, 'https://images.unsplash.com/photo-1622279488973-f29a6dbe6c44?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000039', 'bebek-yatagi-park-besik',         'Park Beşik (Çantalı)',
     'Katlanır park beşik, taşıma çantası dahil. Yan zip kapı, oyun matı ve tekerlekli pratik kullanım. 0-3 yaş için uygun.',
     2899.00, 'TRY', 198, 4.50, 'https://images.unsplash.com/photo-1555252333-9f8e92e65df9?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000040', 'oyuncak-arabalar-set',            'Oyuncak Arabalar 10''lu Set',
     'Dökme metal gövdeli 10''lu oyuncak araba seti. 1:64 ölçek, kolay tutuş ve sağlam yapı. 3 yaş ve üzeri için uygun.',
     299.00, 'TRY', 712, 4.60, 'https://images.unsplash.com/photo-1594787318286-3d835c1d207f?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000004', NOW(), NOW()),

    -- ============================================================
    --  EV & YAŞAM  (category_id = 11111111-...-005)
    -- ============================================================
    ('33333333-0000-0000-0000-000000000009', 'organik-yesil-cay',               'Organik Yeşil Çay (50 Poşet)',
     'Tek menşe Rize''den hasat edilen organik yeşil çay. 50 poşetlik kutuda, kafein dengeli ve antioksidan açısından zengin.',
     89.90, 'TRY', 432, 4.40, 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000010', 'sizma-zeytinyagi-500ml',          'Sızma Zeytinyağı 500 ml',
     'Erken hasat, soğuk sıkım Ayvalık sızma zeytinyağı. 0.3 asit oranı, koyu yeşil cam şişe ile ışıktan korumalı.',
     249.00, 'TRY', 768, 4.60, 'https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000011', 'led-masa-lambasi',                'LED Masa Lambası',
     'Renk sıcaklığı ayarlanabilir LED masa lambası, USB şarj çıkışlı. Göz koruması için kırpışmasız aydınlatma ve dokunmatik kontrol.',
     449.00, 'TRY', 234, 3.40, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-000000000012', 'seramik-saksi-3lu',               'Seramik Saksı Seti (3''lü)',
     'Modern minimalist tasarımlı 3''lü mat finish seramik saksı seti. Bambu altlık dahil, küçük-orta ve büyük boyutlarda.',
     299.00, 'TRY', 187, 4.50, 'https://images.unsplash.com/photo-1485955900006-10f4d324d411?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000041', 'kanepe-3-kisilik',                '3 Kişilik Modern Kanepe',
     'Keten dokuma kumaş, kuş gözü çelik gövde. Yüksek yoğunluklu sünger oturma ve geri yaslanır arkalık. Sökülebilir kılıf, makinede yıkanabilir.',
     12999.00, 'TRY', 142, 4.50, 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000042', 'yemek-takimi-12-parca',           'Porselen Yemek Takımı (12 Parça)',
     '4 kişilik, 12 parça porselen yemek takımı. Mikrodalga ve bulaşık makinesi dayanıklı, modern minimal tasarım.',
     1199.00, 'TRY', 412, 4.70, 'https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000043', 'nevresim-takimi-cift-kisilik',    'Çift Kişilik Nevresim Takımı',
     'Saten pamuk nevresim takımı. Renkli baskı solmaz, ranforce kalitesi ile yumuşak doku. 4 parça (nevresim, alt, 2 yastık kılıfı).',
     899.00, 'TRY', 651, 4.60, 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000044', 'kahve-makinesi-otomatik',         'Tam Otomatik Kahve Makinesi',
     '15 bar basınç, entegre süt köpürtücü ve programlanabilir filtre. Espresso, cappuccino ve latte için tek tuşla hazırlama.',
     6999.00, 'TRY', 387, 4.70, 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000045', 'blender-set-1500w',               'Blender Set 1500W',
     '1500W güçlü motor, 8 hızlı kontrol ve buz kırma fonksiyonu. Smoothie hazneli ek aparatla yanınızda taşıyabilirsiniz.',
     1899.00, 'TRY', 245, 4.40, 'https://images.unsplash.com/photo-1570222094714-d96eb6d70bb1?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000046', 'dekoratif-cerceve-set',           'Dekoratif Çerçeve Seti (5''li)',
     'Farklı boyutlarda 5''li ahşap fotoğraf çerçevesi seti. Mat siyah finish, duvar veya raf için askılı/ayaklı.',
     449.00, 'TRY', 318, 4.50, 'https://images.unsplash.com/photo-1513519245088-0e12902e5a38?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000047', 'el-yapimi-mum-sandal',            'El Yapımı Aromaterapi Mumu',
     'Doğal soya wax''tan el yapımı sandal ağacı kokulu mum. 40 saat yanma süresi, geri dönüştürülebilir cam kavanoz.',
     189.00, 'TRY', 421, 3.50, 'https://images.unsplash.com/photo-1602874801006-cf6e2e9d4d1c?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000005', NOW(), NOW()),

    -- ============================================================
    --  KOZMETİK & KİŞİSEL BAKIM  (category_id = 11111111-...-006)
    -- ============================================================
    ('33333333-0000-0000-0000-100000000048', 'hyaluronik-asit-serum',           'Hyaluronik Asit Yüz Serumu',
     'Cildi nem ile dolduran %2 hyaluronik asit ve B5 vitamini içerikli yoğun nemlendirici serum. Tüm cilt tipleri için uygun.',
     349.00, 'TRY', 1287, 4.70, 'https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000049', 'mat-ruj-kirmizi',                 'Mat Likit Ruj - Kırmızı',
     'Uzun süre kalıcı, transferleşmez mat formül. Yüksek pigment yoğunluğu, dudakta kurutmayan E vitamini içerikli ruj.',
     189.00, 'TRY', 743, 2.80, 'https://images.unsplash.com/photo-1586495777744-4413f21062fa?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000050', 'erkek-parfum-100ml',              'Erkek Parfüm 100 ml EDP',
     'Odunsu ve baharatlı notalarla tasarlanmış erkek parfümü. Üst notalar bergamot ve karabiber, kalp odu gül, taban patçuli ve amber.',
     1599.00, 'TRY', 542, 3.70, 'https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000051', 'anti-aging-yuz-kremi',            'Anti-Aging Gece Yüz Kremi',
     'Retinol ve peptit içerikli gece bakım kremi. İnce çizgileri azaltır, cildi sıkılaştırır ve sabaha dinç bir cilt bırakır.',
     679.00, 'TRY', 432, 4.50, 'https://images.unsplash.com/photo-1571781926291-c477ebfd024b?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000052', 'makyaj-fircasi-12li-set',         'Makyaj Fırçası 12''li Set',
     'Sentetik kıllı, ergonomik saplı 12 parça profesyonel makyaj fırça seti. Yüz, göz ve dudak için tüm temel fırçalar dahil.',
     449.00, 'TRY', 612, 4.60, 'https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000053', 'gunes-kremi-spf50',               'Güneş Kremi SPF50+ (50 ml)',
     'Geniş spektrumlu UVA/UVB koruması sağlayan, suya dayanıklı ve makyaj altına uygun hafif formül. Hassas ciltler için uygun.',
     279.00, 'TRY', 854, 3.80, 'https://images.unsplash.com/photo-1556228720-87ea7d49d4ad?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000054', 'goz-far-paleti-renkli',           'Göz Farı Paleti (35 Renk)',
     '35 farklı renkte mat ve şimmer göz farı paleti. Yüksek pigment, kolay blendlenebilir ve uzun kalıcılık sunar.',
     549.00, 'TRY', 743, 4.50, 'https://images.unsplash.com/photo-1583241475880-083f84372725?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000055', 'el-kremi-organik-shea',           'Organik Shea Yağlı El Kremi',
     '%100 vegan, paraben içermeyen shea yağı ve E vitamini içerikli yoğun nemlendirici el kremi. Hızlı emilir, yapışkan bırakmaz.',
     129.00, 'TRY', 1043, 4.70, 'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000056', 'sampuan-volume-500ml',            'Hacim Veren Şampuan 500 ml',
     'İnce ve cansız saçlar için biotin ve keratin içerikli hacim veren şampuan. Tuzsuz formül, renk koruyucu.',
     189.00, 'TRY', 654, 3.60, 'https://images.unsplash.com/photo-1556227702-d1e4e7b5c232?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000057', 'argan-yagi-100ml',                'Saf Argan Yağı 100 ml',
     'Soğuk sıkım, %100 saf argan yağı. Saç ve cilt bakımı için çok amaçlı, hızlı emilen ve yapışkan olmayan formül.',
     259.00, 'TRY', 421, 4.60, 'https://images.unsplash.com/photo-1602228048741-a1c8b56cb39c?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000058', 'maskara-suya-dayanikli',          'Suya Dayanıklı Maskara',
     'Hacim ve uzunluk sağlayan, smudge-proof formüllü suya dayanıklı maskara. Konik fırça ile her kirpiği ayrı ayrı kaplar.',
     219.00, 'TRY', 832, 3.60, 'https://images.unsplash.com/photo-1631214540242-3cd8c4b0b3a8?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000059', 'kadin-parfum-50ml',               'Kadın Parfüm 50 ml EDP',
     'Beyaz çiçek ve frezya esintili tatlı-pudralı kadın parfümü. Üst notalar yasemin, kalp odu beyaz misk, taban vanilya.',
     1399.00, 'TRY', 743, 4.60, 'https://images.unsplash.com/photo-1541643600914-78b084683601?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000006', NOW(), NOW()),

    -- ============================================================
    --  MÜCEVHER & SAAT  (category_id = 11111111-...-007)
    -- ============================================================
    ('33333333-0000-0000-0000-100000000060', 'pirlanta-tek-tas-yuzuk',          'Pırlanta Tek Taş Yüzük (0.30 ct)',
     '14 ayar beyaz altın yuvada 0.30 karat IGI sertifikalı pırlanta. Klasik 6 tırnaklı solitaire tasarım, sertifika ve özel kutu dahil.',
     24999.00, 'TRY', 87, 4.90, 'https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000061', 'altin-kalp-kolye-14k',            'Altın Kalp Kolye 14 Ayar',
     '14 ayar sarı altın kalp uçlu kolye. 45 cm zincir, parlak finish ve hediye kutusu. Doğum günü ve özel günler için ideal.',
     5499.00, 'TRY', 432, 4.80, 'https://images.unsplash.com/photo-1611652022419-a9419f74343d?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000062', 'gumus-bilezik-italyan',           'İtalyan Gümüş Bilezik',
     '925 ayar gerçek gümüş, italyan zincir örgü bilezik. Mat-parlak finish, kararma önleyici kaplama, ayarlanabilir uzunluk.',
     749.00, 'TRY', 287, 4.60, 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000063', 'mekanik-kol-saati-erkek',         'Erkek Mekanik Otomatik Kol Saati',
     '316L paslanmaz çelik kasa, safir kristal cam ve otomatik kalibre. Su geçirmezlik 10 ATM, deri kayış dahil.',
     7999.00, 'TRY', 198, 4.70, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000064', 'spor-erkek-saati-cap',            'Erkek Spor Saati (Çelik Kayış)',
     'Quartz mekanizmalı, paslanmaz çelik kasa ve kayış. Kronograf, takvim ve ışıklı ibre fonksiyonları. 50m suya dayanıklı.',
     2299.00, 'TRY', 412, 4.50, 'https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000065', 'incili-kupe-klasik',              'Klasik İnci Küpe',
     'Tatlısu incisi, 925 ayar gümüş çiviler. 8mm yuvarlak inciler ile zarafetin simgesi olan zamansız tasarım.',
     549.00, 'TRY', 354, 4.60, 'https://images.unsplash.com/photo-1535632787350-4e68ef0ac584?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000066', 'altin-bilezik-22-ayar',           '22 Ayar Hasır Altın Bilezik',
     '22 ayar saf altın hasır örgü kadın bilezik (yaklaşık 8 gr). Geleneksel Türk işçiliği, garantili ayar belgesi ve sertifika dahil.',
     34999.00, 'TRY', 132, 4.90, 'https://images.unsplash.com/photo-1611591437281-460bfbe1220a?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000067', 'pirlanta-kupe-rose-gold',         'Pırlanta Küpe Rose Gold',
     '14 ayar rose gold, 0.10 ct toplam pırlanta küpe. Pırlanta tek taş tasarım, çitli kapanış ve sertifika dahil.',
     8999.00, 'TRY', 96, 4.80, 'https://images.unsplash.com/photo-1535632066274-30c97a52f0c8?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000068', 'nazar-kolye-altin',               'Altın Nazar Kolye',
     '14 ayar altın, mavi mineli nazar boncuklu kolye. 42-45 cm ayarlanabilir zincir, hediye kutusu içinde gönderilir.',
     2799.00, 'TRY', 543, 2.90, 'https://images.unsplash.com/photo-1611652022419-a9419f74343d?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000069', 'kadin-altin-saat',                'Kadın Altın Renkli Kol Saati',
     'Mineral cam, paslanmaz çelik kasa, taşlı kadran ve mesh çelik kayış. Quartz mekanizma, 3 ATM su geçirmezliği.',
     2499.00, 'TRY', 312, 4.50, 'https://images.unsplash.com/photo-1622434641406-a158123450f9?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000070', 'gumus-tasli-yuzuk',               'Gümüş Taşlı Yüzük',
     '925 ayar gümüş, zirkon taşlı zarif kadın yüzüğü. Beyaz altın kaplama, yan taş detaylı klasik tasarım.',
     449.00, 'TRY', 234, 2.20, 'https://images.unsplash.com/photo-1603561591411-07134e71a2a9?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000071', 'altin-kelepce-bileklik',          '14 Ayar Altın Kelepçe Bileklik',
     '14 ayar sarı altın kelepçe model bileklik. Mat ve parlak finish kombinasyonu, klasik ve modern tarza uygun.',
     6999.00, 'TRY', 178, 4.70, 'https://images.unsplash.com/photo-1573408301185-9146fe634ad0?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000007', NOW(), NOW()),

    -- ============================================================
    --  SPOR & OUTDOOR  (category_id = 11111111-...-008)
    -- ============================================================
    ('33333333-0000-0000-0000-100000000072', 'yoga-mati-6mm-tpe',               'Yoga Matı 6mm TPE',
     'Çift katmanlı TPE malzeme, kaymaz yüzey ve hafif yapı. 6mm kalınlık ile diz koruma, taşıma kayışı dahil.',
     449.00, 'TRY', 1187, 4.70, 'https://images.unsplash.com/photo-1545205597-3d9d02c29597?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000073', 'kosu-ayakkabisi-air-yastik',      'Hava Yastıklı Koşu Ayakkabısı',
     'Knit üst yüzey, EVA ara taban ve hava yastığı. Şehir içi koşu ve günlük spor kullanım için hafif ve nefes alabilir.',
     2199.00, 'TRY', 1432, 4.60, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000074', 'dambil-set-2x10kg',               'Dambıl Seti 2x10 kg',
     'Kauçuk kaplı altıgen dambıl, 2''li set. Anti-rolling tasarım, ergonomik krom sap. Ev antrenmanları için ideal.',
     899.00, 'TRY', 387, 4.50, 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000075', 'kettlebell-16kg',                 'Kettlebell 16 kg',
     'Tek parça döküm demir, vinyl kaplama. Yumuşak tutuş ve grip için ergonomik sap, antrenman için profesyonel ağırlık.',
     749.00, 'TRY', 234, 4.60, 'https://images.unsplash.com/photo-1571902943202-507ec2618e8f?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000076', 'spor-tisort-erkek-dryfit',        'Erkek Dry-Fit Spor Tişörtü',
     'Nemi hızla emen ve uçuran teknik kumaş, anti-bakteriyel iç astar. Antrenman ve günlük spor için uygun rahat kesim.',
     299.00, 'TRY', 654, 3.20, 'https://images.unsplash.com/photo-1556906781-9a412961c28c?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000077', 'spor-corap-3lu',                  'Antrenman Çorabı 3''lü Paket',
     'Pamuklu, anti-bakteriyel ve nem yönetimi sağlayan teknik örgü çorap. Yastıklı taban ve esnek bilek detayı.',
     199.00, 'TRY', 421, 2.60, 'https://images.unsplash.com/photo-1586350977771-2a1dafd9bcf6?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000078', 'kamp-cadiri-3-kisilik',           'Kamp Çadırı 3 Kişilik',
     'Hafif fiberglas çubuk yapı, su geçirmez 3000mm dış cephe. Çift kapı, sineklik ve yağmurluk dahil. 3 mevsim kullanıma uygun.',
     2499.00, 'TRY', 287, 4.60, 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000079', 'uyku-tulumu-mumya',               'Mumya Tipi Uyku Tulumu',
     '-5°C konfor, -15°C ekstrem sıcaklık. Mumya tipi kesim ile maksimum ısı tutumu, taşıma kılıfı dahil.',
     1499.00, 'TRY', 198, 4.70, 'https://images.unsplash.com/photo-1496080174650-637e3f22fa03?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000080', 'mountain-bike-26-jant',           'Mountain Bike 26 Jant 21 Vites',
     'Alüminyum kadro, ön süspansiyon, Shimano 21 vites grup ve disk fren. Ofroad ve şehir içi sportif kullanım için.',
     11999.00, 'TRY', 145, 4.50, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000081', 'silikon-tabanlik-spor',           'Silikon Spor Tabanlık',
     'Şok emici silikon arka tabanlık, ortopedik destek. Düz tabanlılar ve uzun süreli ayakta kalanlar için ideal konfor.',
     249.00, 'TRY', 543, 2.40, 'https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000082', 'pilates-topu-65cm',               'Pilates Topu 65 cm',
     'Anti-burst patlamayı engelleyen güvenli yapı, kaymaz yüzey. Pompa dahil, yoga ve pilates antrenmanları için uygun.',
     349.00, 'TRY', 312, 2.80, 'https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000083', 'trekking-bot-su-gecirmez',        'Trekking Bot (Su Geçirmez)',
     'Hakiki deri ve nubuk dış cephe, Vibram benzeri kaymaz taban. Su geçirmez membran, bilek desteği için yüksek bilek.',
     2299.00, 'TRY', 287, 4.70, 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000008', NOW(), NOW()),

    -- ============================================================
    --  OTOMOTİV & MOTORSİKLET  (category_id = 11111111-...-009)
    -- ============================================================
    ('33333333-0000-0000-0000-100000000084', 'motorsiklet-kaski-tam',           'Motorsiklet Kask (Tam Yüz)',
     'ECE 22.06 sertifikalı, ABS dış kabuk, çıkarılabilir iç astar. Pinlock hazır vizör, anti-buğu ve havalandırma sistemi.',
     2299.00, 'TRY', 287, 4.70, 'https://images.unsplash.com/photo-1623856977017-da252c7770a7?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000085', 'aku-12v-60ah',                    'Akü 12V 60Ah',
     'Bakım gerektirmeyen kalsiyum-kalsiyum teknolojisi. 540 CCA marş gücü, sızdırmaz kasa, 24 ay üretici garantisi.',
     2199.00, 'TRY', 198, 4.50, 'https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000086', 'motor-yagi-5w-30-4lt',            'Tam Sentetik Motor Yağı 5W-30 (4 lt)',
     'API SP performans, ACEA C3 onayı tam sentetik motor yağı. Düşük SAPS formül, modern dizel ve benzin motorları için uygun.',
     1199.00, 'TRY', 432, 4.80, 'https://images.unsplash.com/photo-1635770776537-0c8b3e4f0d9c?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000087', 'lastik-yaz-205-55-r16',           'Yaz Lastiği 205/55 R16',
     'Eko yakıt sınıfı C, ıslak zemin tutuşu A, gürültü 70 dB. AB etiket sertifikalı premium yaz lastiği.',
     1899.00, 'TRY', 234, 4.50, 'https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000088', 'silecek-takimi-set',              'Silecek Takımı (Çiftli)',
     'Aerodinamik tasarım, doğal kauçuk silici. Sessiz çalışma, kar/buz koşullarına dayanıklı. Üniversal montaj.',
     349.00, 'TRY', 543, 2.30, 'https://images.unsplash.com/photo-1591488543493-0db7b03b0eb3?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000089', 'oto-koltuk-kilifi-set',           'Oto Koltuk Kılıfı (Komple Set)',
     'Üniversal kalıp, deri görünümlü PU malzeme. 11 parça komple set, hava yastığı uyumlu, kolay takılır.',
     1499.00, 'TRY', 312, 4.30, 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000090', 'bagaj-kilifi-su-gecirmez',        'Bagaj Kılıfı Su Geçirmez',
     'Üniversal kalıp, dayanıklı 600D oxford kumaş. Bagajınızı yıkama, çamur ve evcil hayvan tüylerinden korur.',
     449.00, 'TRY', 187, 1.90, 'https://images.unsplash.com/photo-1520031441872-265e4ff70366?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000091', 'motor-eldiveni-yaz',              'Motorsiklet Eldiveni (Yaz)',
     'Hava akımı sağlayan örgü dokuma, eklem koruyucu kabuk. Dokunmatik ekran uyumlu parmak ucu, ergonomik kalıp.',
     549.00, 'TRY', 156, 4.60, 'https://images.unsplash.com/photo-1591376344935-de7c44eee574?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000092', 'arac-deri-kokulu',                'Araç Deri Kokulu',
     'Lüks deri esansı, klima ızgarası montajlı koku. 60 güne kadar etkili, ayarlanabilir yoğunluk kontrolü.',
     89.00, 'TRY', 654, 1.80, 'https://images.unsplash.com/photo-1550355291-bbee04a92027?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000093', 'kit-lastik-anahtar-set',          'Lastik Anahtarı + Kriko Seti',
     'Hidrolik kriko (2 ton), lastik anahtarı ve uzatma kolu. Çelik taşıma kutusu, ev araçları için tüm temel ihtiyaçlar.',
     899.00, 'TRY', 234, 4.50, 'https://images.unsplash.com/photo-1597267018896-e0e6e3a2cf90?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000094', 'araba-cilasi-nano',               'Araba Cilası Nano (500 ml)',
     'Nano teknolojisi seramik koruma cilası. UV koruması, su iticilik (hidrofobik), 12 ay etkili. Mikrofiber bez dahil.',
     449.00, 'TRY', 312, 4.70, 'https://images.unsplash.com/photo-1605559424843-9e4c228bf1c2?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW()),

    ('33333333-0000-0000-0000-100000000095', 'motor-tisort-erkek',              'Motor Tişörtü (Erkek)',
     'Pamuklu tişört, motorcu tasarım baskı. Regular fit, yuvarlak yaka, makinede yıkanabilir.',
     299.00, 'TRY', 178, 4.40, 'https://images.unsplash.com/photo-1611601679706-87b30b1f8b30?auto=format&fit=crop&w=600&q=80',
     TRUE, '11111111-0000-0000-0000-000000000009', NOW(), NOW())

ON CONFLICT (id) DO UPDATE SET
    slug           = EXCLUDED.slug,
    name           = EXCLUDED.name,
    description    = EXCLUDED.description,
    price          = EXCLUDED.price,
    rating_count   = EXCLUDED.rating_count,
    rating_average = EXCLUDED.rating_average,
    image_url      = EXCLUDED.image_url,
    category_id    = EXCLUDED.category_id;
