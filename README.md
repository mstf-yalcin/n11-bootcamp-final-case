# N11 Bootcamp — E-Ticaret Mikroservis Projesi

> Saga choreography, Outbox + Debezium CDC, Kafka, OAuth2 (RS256 + JWKS), observability ve GCP üzerinde GitHub Actions ile  deploy edilen bir e-ticaret mikroservis projesi.

- **Stack:** Java 21, Spring Boot 3.5, Spring Cloud, PostgreSQL, Elasticsearch, Redis, Kafka, Debezium, React 19, Vite, Tailwind
- **Mimari:** 6 iş servisi + 3 platform servisi + Kafka/CDC + observability (Prometheus, Tempo, Loki, Grafana, Alloy)
- **Deployment:** Docker Compose (local + GCE VM), GitHub Actions + Workload Identity Federation + Artifact Registry

---

**Demo:** [http:34.62.90.83](http:34.62.90.83)

| Rol | Email | Şifre |
|---|---|---|
| User | test@test.com | test123 |
| Admin | admin@test.com | admin123 |

## 1. Ekran Görüntüleri

### 1.1 Anasayfa
![Anasayfa](screenshots/home.png)

### 1.2 Ürün Listesi
![Ürün listesi](screenshots/product-list.png)

### 1.3 Ürün Detay
![Ürün detay](screenshots/product-detail.png)
![Ürün detay](screenshots/product-detail-2.png)

### 1.4 Sepet
![Sepet](screenshots/cart.png)

### 1.5 Checkout / Adres Seçimi
![Checkout](screenshots/checkout.png)

### 1.6 Sipariş Detay & Sipariş Geçmişi
![Sipariş detay](screenshots/order-detail.png)

### 1.7 Kullanıcı Hesabı (Adresler / Ödemeler)
![Hesap](screenshots/account.png)
![Hesap](screenshots/account2.png)
![Hesap](screenshots/account3.png)
![Hesap](screenshots/account4.png)

### 1.8 Login / Register
![Auth](screenshots/login.png)
![Auth](screenshots/register.png)

### 1.9 Admin
![Auth](screenshots/admin.png)
![Auth](screenshots/admin2.png)
![Auth](screenshots/admin3.png)
![Auth](screenshots/admin4.png)
![Auth](screenshots/admin5.png)

### 1.10 Grafana / Dashboard'lar
![Grafana](screenshots/grafana/grafana-dashboard-1.png)
![Grafana](screenshots/grafana/grafana-dashboard-2.png)

### 1.11 Grafana / Alerting
![Grafana](screenshots/grafana/grafana-alert.png)

### 1.12 Kafka UI
![Kafka UI](screenshots/kafkaui1.png)
![Kafka UI](screenshots/kafkaui2.png)

### 1.13 Kibana
![Kibana](screenshots/elastic-kibana.png)

---

## 2. Mimari Genel Bakış

```
                              ┌──────────────┐
                              │   React 19   │
                              │  (Vite + TS) │
                              └──────┬───────┘
                                     │  HTTPS
                              ┌──────▼───────┐
                              │ API Gateway  │  JWT (JWKS), CB, Retry, Fallback
                              │   :8080      │
                              └──────┬───────┘
                                     │ Eureka lb://
        ┌────────────┬────────────┬──┴──────────┬─────────────┬──────────────┐
        │            │            │             │             │              │
   ┌────▼─────┐ ┌────▼─────┐ ┌────▼─────┐ ┌────▼─────┐ ┌─────▼─────┐ ┌──────▼─────┐
   │  user-   │ │ product- │ │  cart-   │ │  stock-  │ │  order-   │ │  payment-  │
   │  :9000   │ │  :9100   │ │  :9300   │ │  :9200   │ │  :9400    │ │  :9500     │
   └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────┬─────┘ └──────┬─────┘
        │            │            │            │             │              │
        └─PG─────────┴─PG/Redis──┬─Redis──────┴─PG──────────┴─PG──────────┘
                                 │
                          ┌──────▼──────┐         ┌────────────────┐
                          │  PostgreSQL │  WAL ──►│   Debezium     │
                          │  wal_logical│         │   Connect      │
                          └─────────────┘         └────────┬───────┘
                                                           │ EventRouter / Products SMT
                                                           ▼
                                                  ┌────────────────┐
                                                  │  Kafka         │  topic: <aggregate>.events
                                                  │                │  topic: products  (CDC)
                                                  └────────┬───────┘
                                                           │
                                    ┌──────────────┬───────┴───────┬──────────────┐
                                    ▼              ▼               ▼              ▼
                                 (consumer)    (consumer)      (consumer)     ES Sink
                                 order-svc     stock-svc       payment-svc    Connector
                                                                                  │
                                                                                  ▼
                                                                       ┌────────────────────┐
                                                                       │  Elasticsearch     │  ◄── product-service
                                                                       │     :9201          │      search query
                                                                       └────────────────────┘      (CQRS read)

Asenkron iletişim: Kafka (3.8) + Debezium CDC + Outbox Pattern
Senkron iletişim:  OpenFeign + Resilience4j (CB / Retry / Fallback)
Service discovery: Eureka (`infrastructure/discovery-server`)
Config:            Spring Cloud Config (`infrastructure/config-server`)
```

## 3. Saga ve Outbox Akış Diyagramları

### 3.1 Saga — Happy Path
![Saga happy path](screenshots/architecture/saga-happy-path.png)

### 3.2 Saga — Debezium ile Yakın Plan
![Saga happy path](screenshots/architecture/saga-happy-path-with-debezium.png)

### 3.3 Saga — Compensation
![Saga happy path](screenshots/architecture/saga-compensation.png)

### 3.4 Order State Machine
![Order state machine](screenshots/architecture/order-state-machine.png)

### 3.5 Mimari Genel Görünüm
![Architecture overview](screenshots/architecture/architecture-overview.png)

---

## 4. Servisler ve Portlar

### 4.1 Spring İş Servisleri

| Servis | Port  | Sorumluluk |
|---|---|-|
| `user-service` | `9000` | Kullanıcı, adres, RS256 JWT issuer + `/.well-known/jwks.json` |
| `product-service` | `9100` | Ürün/Kategori/Tag CRUD, slug, batch endpoint |
| `stock-service` | `9200` | Stok + rezervasyon, pessimistic lock, saga consumer |
| `cart-service` | `9300` | Pure Redis sepet (`cart:{userId}`, 30 gün TTL) |
| `order-service` | `9400` | Saga producer + consumer, order state machine |
| `payment-service` | `9500`  | Iyzico (sandbox/mock), Payment Intent rendezvous |

### 4.2 Platform Servisleri

| Bileşen | Port | Görev |
|---|---|---|
| `api-gateway` | `8080` | Tek giriş noktası — JWT JWKS doğrulama, CB, Retry, Fallback |
| `config-server` | `8888` | Spring Cloud Config (`n11-config/`) |
| `discovery-server` | `8761` | Eureka registry — `lb://` için |

### 4.3 3rd-Party Altyapı

| Bileşen | Host port | Not |
|---|---|-|
| `postgres` (debezium/postgres:16) | `5432` | `wal_level=logical`, çoklu DB |
| `redis` | `6379` | Cart + Product cache |
| `kafka` (host listener) | `9092` | Host CLI tools için |
| `kafka` (internal) | — | `PLAINTEXT://kafka:29092` — container'lar arası |
| `debezium-connect` | `8083` | CDC + ES Sink connector REST API. Multi-stage Dockerfile ile Confluent ES Sink 14.1.7 image'a gömülü (`n11/debezium-connect:2.7.3-es14.1.7`) |
| `kafka-ui` | `8090` | Provectus Kafka UI |
| `elasticsearch` | `9201` (host) → `9200` (container) | Product search projection — stock-service 9200 çakıştığı için host port farklı |
| `kibana` | `5601` | Elasticsearch UI / index management |

### 4.4 Observability

| Bileşen | Host port | Not |
|---|---|---|
| `prometheus` | `9090`| Metric scrape + TSDB UI |
| `tempo` (HTTP) | `3200` | Grafana datasource |
| `tempo` (OTLP gRPC) | `4317` | OTLP gRPC exporter |
| `tempo` (OTLP HTTP) | `4318` | Spring Boot exporter buraya yazar |
| `loki` | `3100` | Log storage |
| `alloy` | `12345` | Docker SD → Loki forwarder |
| `grafana` | `3000` | UI (admin/admin demo) |

### 4.5 Servisler Arası IP / Erişim

- Container'lar arası: **service name + internal port** (örn. `http://product-service:9100`, `kafka:29092`, `postgres:5432`).
- Servisler arası HTTP: **Eureka discovery + `lb://`** — Feign client `@FeignClient(name = "stock-service")` yazar, port hardcode etmez.
- Frontend: yalnızca `http://localhost:8080` (gateway). VM deploy'unda gateway için firewall `8080`, frontend için `80` açıktır

---

## 5. Tech Stack

### 5.1 Backend

| Katman | Teknoloji | Versiyon |
|---|---|---|
| Dil | Java | 21 |
| Framework | Spring Boot | 3.5.14 |
| Cloud Stack | Spring Cloud | 2025.0.2 |
| Build | Maven Multi-Module | 3.9+ |
| Service discovery | Eureka | (Spring Cloud) |
| Config | Spring Cloud Config | (Spring Cloud) |
| API Gateway | Spring Cloud Gateway (WebFlux) | (Spring Cloud) |
| Auth | OAuth2 Resource Server (RS256 + JWKS) | Spring Security |
| Persistence | PostgreSQL + Hibernate / JPA | 16 / 6 |
| CDC | Debezium PostgreSQL connector | 2.x |
| Messaging | Apache Kafka (KRaft, Zookeeper'sız) | 3.8.0 |
| Cache | Redis | 7 |
| Search projection | Elasticsearch + Spring Data Elasticsearch + Confluent ES Sink | server 8.18.2 / starter 5.5.x / sink 14.1.7 |
| Resilience | Resilience4j (CB / Retry / TimeLimiter) | (Spring Cloud) |
| HTTP client | OpenFeign + Resilience4j | (Spring Cloud) |
| Mapping | MapStruct | 1.6.3 |
| Test | JUnit 5 + Mockito + Testcontainers + Awaitility | — |
| API doc | springdoc-openapi | 2.8.16 |
| Container | Jib (no Dockerfile gereken servisler için) | 3.5.1 |
| Observability | Micrometer + Prometheus + Micrometer Tracing + OpenTelemetry OTLP | — |
| Structured log | Logback + LogstashEncoder (JSON, MDC) | — |

### 5.2 Observability ve Ops

| Bileşen | Image |
|---|---|
| Prometheus | `prom/prometheus:v3.1.0` |
| Tempo | `grafana/tempo:2.7.1` |
| Loki | `grafana/loki:3.3.2` |
| Alloy | `grafana/alloy:v1.5.1` |
| Grafana | `grafana/grafana:11.5.2` |
| Kafka UI | `provectuslabs/kafka-ui` |

### 5.3 CI / CD ve Cloud

- **GitHub Actions** — `ci.yml` (PR'larda mvn verify + npm build), `deploy.yml` (master'a merge → deploy)
- **GCP Artifact Registry** — image registry
- **GCE VM** (Ubuntu 24.04, e2-standard-4) — `docker compose pull && up -d`
- **Jib** — backend image build (Dockerfile yok)
- **Docker buildx** — frontend image

---

## 6. Klasör Yapısı (File Structure)

```text
n11bootcamp-final/                                  # Repo root (Fullstack monorepo)
├── README.md                                       # Bu dosya
├── .github/
│   └── workflows/
│       ├── ci.yml                                  # PR — mvn verify + npm build
│       └── deploy.yml                              # master push — Jib + GAR + VM SCP/SSH
│
├── frontend/                                       # React 19 + Vite + Tailwind
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── nginx.conf                                  # Production serve config
│   ├── Dockerfile
│   └── src/
│       ├── api/                                    # axios client + endpoint sarmalayıcılar
│       ├── components/                             # ui/, layout/, ApiErrorBox, ConfirmDialog, ErrorBoundary, ListSkeleton, BannerCarousel, Logo
│       ├── features/                               # auth/, cart/, checkout/, orders/, products/
│       ├── hooks/, lib/, pages/, routes/, store/, types/
│
├── backend/                                        # Java mikroservis monorepo
│   ├── pom.xml                                     # Root parent POM (Spring Boot 3.5, Cloud 2025)
│   │
│   ├── common-lib/                                 # Tüm servislerin paylaştığı kütüphane
│   │   └── src/main/java/com/n11/bootcamp/common_lib/
│   │       ├── auth/                               # UserPrincipal, UserPrincipalConverter
│   │       ├── config/                             # JpaAuditingConfig, FeignConfig
│   │       ├── entity/                             # BaseEntity (id UUID, createdAt, updatedAt, isActive)
│   │       ├── event/                              # OutboxEventBase, payload records (saf business)
│   │       ├── exception/                          # BaseException, GlobalExceptionHandler
│   │       ├── filter/                             # CorrelationIdFilter
│   │       ├── idempotency/                        # @Idempotent + AOP aspect + Redis SETNX (L1 — Client/HTTP)
│   │       ├── logging/                            # LoggingAspect (@RestController around)
│   │       ├── response/                           # ApiResponse<T>
│   │       ├── enums/                              # EventType, AggregateType, OrderStatus, CancelReason
│   │       └── resources/logback-spring.xml        # Profile-aware (local plain / prod JSON)
│   │
│   ├── services/                                   # İş servisleri
│   │   ├── user-service/                           # :9000  RS256 JWT, JWKS, register/login/refresh
│   │   ├── product-service/                        # :9100  Product/Category/Tag, slug, Resilience4j → stock
│   │   ├── stock-service/                          # :9200  Stock + reservation, pessimistic lock, saga consumer
│   │   ├── cart-service/                           # :9300  Pure Redis cart, hydration, merge
│   │   ├── order-service/                          # :9400  Saga producer + consumer, state machine
│   │   └── payment-service/                        # :9500  Iyzico (sandbox/mock), Payment Intent rendezvous
│   │
│   ├── infrastructure/                             # Platform servisleri
│       ├── api-gateway/                            # :8080  Spring Cloud Gateway WebFlux + JWKS + CB
│       ├── config-server/                          # :8888  Spring Cloud Config
│       └── discovery-server/                       # :8761  Eureka
│
└── deploy/                                         # Runtime / ops (compose + config + ops scripts)
    ├── docker-compose.yml                          # Ana stack (app + infra + obs)
    ├── docker-compose.gcp.yml                      # GCP Artifact Registry image override
    ├── common-config.yml                           # Compose `extends:` template (microservice-base)
    ├── debezium/                                   # Outbox connector JSON'ları
    │   ├── README.md
    │   ├── order-outbox-connector.json
    │   ├── stock-outbox-connector.json
    │   └── payment-outbox-connector.json
    ├── observability/
    │   ├── alloy/config.alloy                      # Docker SD → Loki
    │   ├── grafana/                                # datasource + dashboard/alert provisioning
    │   ├── loki/loki-config.yaml
    │   ├── prometheus/prometheus.yml
    │   └── tempo/tempo.yml
    └── scripts/
        ├── build-images.sh                         # Lokal Jib build
        ├── deploy-vm.sh                            # VM compose pull + up
        └── gcp-bootstrap.sh                        # GCP/GAR/VM/WIF tek seferlik kurulum

scripts/                                            # Dev/test/verification scripts (deploy ops dışı)
└── test-idempotency.sh                             # Idempotency test (3 senaryo: same-key, diff-key, race)
```

---

## 7. Saga Akışları

Choreography pattern: **merkezi orkestratör yok**, her servis kendi event'ini tüketir/üretir. State machine `order-service`'tedir.

### 7.1 Order State Machine

```
PENDING → STOCK_RESERVED → PAYMENT_PROCESSING → CONFIRMED → SHIPPED → DELIVERED

Cancel paths (cancel_reason ile):
PENDING            → CANCELLED  (STOCK_UNAVAILABLE)
STOCK_RESERVED     → CANCELLED  (PAYMENT_FAILED)   [+ stock release]
PAYMENT_PROCESSING → CANCELLED  (USER_CANCELLED)   [+ stock release]
CONFIRMED          → CANCELLED  (manual)           [+ refund + release]
```

### 7.2 Happy Path

```
[Client] POST /api/v1/orders
   ▼
[order-service] @Transactional
   ├─ Feign → cart-service       (sepet hydration)
   ├─ Feign → product-service    (anlık fiyat snapshot)
   ├─ Feign → user-service       (adres + buyer snapshot)
   ├─ INSERT order (status=PENDING) + INSERT order_items (snapshot fiyat/isim)
   └─ INSERT outbox_order (ORDER_CREATED)
   ▼
[Debezium] WAL → Kafka topic: order.events  (EventRouter SMT, header: eventType, correlationId)
   ▼
[stock-service] consumer (ORDER_CREATED)
   @Transactional
   ├─ SELECT ... FOR UPDATE                (pessimistic lock, ORDER BY productId — deadlock azaltır)
   ├─ available < quantity? → STOCK_FAILED (compensation)
   └─ INSERT stock_reservation + UPDATE stocks SET reserved += qty
                                         + INSERT outbox_stock (STOCK_RESERVED)
   ▼ Kafka topic: stock.events
   ▼
[order-service] consumer (STOCK_RESERVED) → UPDATE order SET status=STOCK_RESERVED
[payment-service] consumer (STOCK_RESERVED) → Payment satırının "stock ayağı" set edilir
                                              (rendezvous: ORDER_CREATED + STOCK_RESERVED ikisi de gelince Iyzico'ya gidilir)
   ▼
[payment-service] (her iki ayak hazır)
   - Iyzico Sandbox / mock
   - INSERT outbox_payment (PAYMENT_COMPLETED veya PAYMENT_FAILED)
   ▼ Kafka topic: payment.events
   ▼
[order-service] consumer (PAYMENT_COMPLETED)
   @Transactional
   ├─ UPDATE order SET status=CONFIRMED
   └─ INSERT outbox_order (ORDER_CONFIRMED)
   - Feign → cart-service: DELETE Redis cart:{userId}
```

### 7.3 Compensation — Stok Yetersiz

```
order-service: ORDER_CREATED
   ▼
stock-service: available < qty
   └─ INSERT outbox_stock (STOCK_FAILED)
   ▼
order-service consumer (STOCK_FAILED):
   UPDATE order SET status=CANCELLED, cancel_reason='STOCK_UNAVAILABLE'
```

### 7.4 Compensation — Ödeme Başarısız

```
stock-service: STOCK_RESERVED OK
   ▼
payment-service: Iyzico FAIL → INSERT outbox_payment (PAYMENT_FAILED)
   ▼
stock-service consumer (PAYMENT_FAILED):
   @Transactional
   ├─ UPDATE stock_reservation SET status=RELEASED
   ├─ UPDATE stocks SET reserved -= qty (atomik UPDATE, WHERE reserved >= qty)
   └─ INSERT outbox_stock (STOCK_RELEASED)
   ▼
order-service consumer (PAYMENT_FAILED):
   UPDATE order SET status=CANCELLED, cancel_reason='PAYMENT_FAILED'
```

### 7.5 Compensation — Kullanıcı İptali (CONFIRMED öncesi)

```
PUT /api/v1/orders/{id}/cancel
   ▼
order-service:
   IF status IN (PENDING, STOCK_RESERVED):
     UPDATE order SET status=CANCELLED, cancel_reason='USER_CANCELLED'
     INSERT outbox_order (ORDER_CANCELLED)
   ELSE 409 Conflict
   ▼ Kafka topic: order.events
   ▼
stock-service consumer (ORDER_CANCELLED):
   IF reservation EXISTS AND status != RELEASED:
     UPDATE stock_reservation SET status=RELEASED + UPDATE stocks SET reserved -= qty
```

### 7.6 Idempotency — 3 Layers

Sistemde her boundary'de idempotency garantisi var.

```
Frontend → Backend HTTP        ← L1: Client / HTTP        (Idempotency-Key + Redis SETNX)
            │
            ▼
       DB write + outbox        ← L2: Producer            (Outbox pattern)
            │
       Debezium WAL → Kafka
            │
            ▼
       Consumer service         ← L3: Consumer            (Domain-based)
```

**L1 — Client / HTTP** (`@Idempotent` + Redis SETNX)
Frontend checkout mount'unda UUID üretir, `Idempotency-Key` header ile gönderir. Backend'de common-lib'deki `@Idempotent` annotation + AOP aspect cache'i kontrol eder.

```java
// order-service/OrderController
@PostMapping
@Idempotent
public ResponseEntity<ApiResponse<OrderResponse>> createOrder(...) { ... }
```

- Aynı key 2x → cache hit, aynı orderId
- Farklı key → yeni sipariş
- 5 paralel aynı key → SETNX atomic, tek sipariş (smoke test ile doğrulandı)
- Header yok → backwards compatible, aspect bypass

**L2 — Producer** (Outbox Pattern)
Application doğrudan Kafka producer kullanmaz; DB transaction'ı outbox satırıyla atomik. Debezium WAL'i okuyup Kafka'ya basar (offset tracking + idempotent producer).

**L3 — Consumer** (Domain-based)
Kafka at-least-once delivery'sine karşı `existsByOrderId`, status transition gibi doğal idempotency ile kontrol eder.

```java
// stock-service/StockService.reserveStock
if (reservationRepository.existsByOrderId(order.orderId())) {
    return;  // duplicate event → no-op
}
```

- 3 kez retry'dan sonra:
  - **Business event** → compensation event yayınlar (`STOCK_FAILED` gibi)
  - **Compensation event** → DLT topic'e yazar (`*.DLT`)

**Idempotency test:**
```bash
bash scripts/test-idempotency.sh
```
3 senaryo: same-key dedup, different-key isolation, 5 paralel race condition. PASS/FAIL output.

---

## 8. Outbox + Debezium CDC

Dual-write tutarsızlığı yerine: event'i Kafka'ya servis basmaz, **outbox tablosuna business kayıtla aynı transaction'da** yazar; Debezium WAL'den okuyup Kafka'ya basar.

```
@Transactional {
    orderRepo.save(order);        ── DB write
    outboxRepo.save(outboxEvent); ── AYNI TX
}
        │  
        ▼
   Debezium Connect  ── EventRouter (aggregate_type → topic)
        │
        ▼
   Kafka topic: order.events   (key: aggregate_id, headers: eventType, correlationId)
```

### 8.1 Outbox Tablosu (Şablon)

```sql
CREATE TABLE outbox_<service> (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,   -- "order","stock","payment","shipment"
    aggregate_id    VARCHAR(100) NOT NULL,   -- saga aggregate root id (genelde orderId)
    event_type      VARCHAR(100) NOT NULL,   -- ORDER_CREATED, STOCK_RESERVED ...
    payload         TEXT         NOT NULL,   -- saf business JSON, wrapper YOK
    correlation_id  VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### 8.2 Topic Listesi

| Topic | Producer | Consumer'lar |
|---|---|---|
| `order.events` | order-service | stock, payment, shipment, notification |
| `stock.events` | stock-service | order, notification |
| `payment.events` | payment-service | order, stock, notification |
| `shipment.events` | shipment-service | order, notification |
| `products` | Debezium (products tablosu CDC) | Confluent ES Sink → Elasticsearch |

---

## 9. Elasticsearch — Ürün Search Projection (CQRS Hybrid)

Product search'ün performansı ve fuzzy match'i için Elasticsearch entegre edildi. **Aynı Debezium altyapısı**, **outbox tablosu yerine ürün tablosunu doğrudan** dinler — saga'nın yanında Debezium'un ikinci kullanım vakası.

### 9.1 Akış

```
products tablosu (PostgreSQL)
    │  Hibernate save() / update() / soft-delete
    ▼
Debezium product-cdc-connector
    │  unwrap(drop) + rename + dropFields(id) + routeTopic SMT zinciri
    │  → kirli artifact'lar source'ta temizlenir, Kafka mesajı clean JSON
    ▼
Kafka topic: products
    │
    ▼
Confluent Elasticsearch Sink Connector 14.1.7
    │  ExtractField$Key SMT → ES doc _id = UUID
    ▼
Elasticsearch index: products  (ProductIndexInitializer ile explicit mapping)
    │  - name (turkish analyzer + search_as_you_type autocomplete sub-field)
    │  - description (turkish analyzer)
    │  - categoryId, slug, currency (Keyword)
    │  - price, ratingAverage (Double)
    │
    ▼  ElasticProductService.search()  ─►  matching ID listesi
    │   (multi_match fuzzy + bool_prefix + match_phrase_prefix)
    │                                       │
    │                                       ▼
    │                                  ProductRepository.findAllByIdInAndIsActiveTrue()
    │                                       │  PG'den taze veri + stock enrichment
    │                                       ▼
    │                                  ProductResponse
```

**CQRS hybrid**: Elastic sadece filter + sort, asıl `Product` PostgreSQL'den çekilir. Fiyat/stok/isActive gibi taze veri stale olamaz.

**Search query 3 paralel mekanizma:**
- `multi_match fuzziness=AUTO prefixLength=1` — typo toleransı ("kaehve" → "kahve")
- `multi_match type=bool_prefix` on `name.autocomplete*` — anlık prefix ("pors" → "porselen")
- `match_phrase_prefix` on `description` — description'da yarım kelime fallback

### 9.2 Decorator Pattern (kod tarafı)

```
ProductService (interface)
    ├─ JpaProductService              # default — tüm operasyonlar PG
    └─ ElasticProductService          # @Primary, app.product.search.engine=elastic
         ├─ JpaProductService delegate   # CRUD + tekil okumalar PG'ye delege
         └─ ElasticsearchOperations      # sadece list/search Elastic'te
```

| Operasyon | engine=jpa | engine=elastic |
|---|---|---|
| `getProducts` / `getAdminProducts` | PG ILIKE | **Elastic** bool query → PG bulk fetch |
| `getProductById` / `getProductBySlug` | PG | PG (delegate) |
| `createProduct` / `updateProduct` / `deleteProduct` | PG | PG (delegate) — Debezium ES'i besler |

### 9.3 Plugin Yönetimi — Multi-stage Dockerfile

Debezium image'ında Confluent ES Sink connector yok. `deploy/debezium/Dockerfile` ile **multi-stage build** kullanılır:

```
Stage 1: confluentinc/cp-kafka-connect-base:7.7.0
    │  confluent-hub install confluentinc/kafka-connect-elasticsearch:14.1.7
    ▼
/plugins/confluentinc-kafka-connect-elasticsearch
    │
    ▼ COPY --from=stage1
Stage 2: debezium/connect:2.7.3.Final
    │  /kafka/connect altında plugin hazır
    │  CONNECT_PLUGIN_PATH=/kafka/connect
    ▼
Final image: n11/debezium-connect:2.7.3-es14.1.7
```

`docker-compose.yml`:
```yaml
debezium-connect:
  build:
    context: ./debezium
  image: n11/debezium-connect:2.7.3-es14.1.7
```

### 9.4 Engine Switch

`docker-compose.yml` `product-service` env'inde default:
```yaml
PRODUCT_SEARCH_ENGINE: ${PRODUCT_SEARCH_ENGINE:-elastic}
```

JPA'ya dönmek için:
```bash
PRODUCT_SEARCH_ENGINE=jpa docker compose up -d product-service
```

### 9.5 Doğrulama

```bash
# Cluster health
curl http://localhost:9201/_cluster/health

# Index mapping
curl http://localhost:9201/products/_mapping

# Index doc count (snapshot sonrası)
curl http://localhost:9201/products/_count

# Connector listesi
curl http://localhost:8083/connectors

# Gateway üzerinden search
curl "http://localhost:8080/api/v1/products?search=kahve&size=5"

# Did-you-mean suggester
curl "http://localhost:8080/api/v1/products/suggest?q=kahvve"

# Kibana UI
open http://localhost:5601
```

---

## 10. Hızlı Başlangıç

### 10.1 Ön Koşullar

- **Java 21** + **Maven 3.9+** 
- **Docker Desktop** 
- `curl`, `jq` 

### 10.2 Lokal Çalıştırma

```bash
# Repo root'tan
# (Opsiyonel) Env override'ları için
cp deploy/.env.example deploy/.env

cd backend
mvn clean install -DskipTests jib:dockerBuild

cd ../deploy
docker compose up -d

docker compose ps
```

Servisler ayağa kalktıktan sonra:

| URL | Açıklama |
|---|---|
| `http://localhost:8080/swagger-ui.html` | API Gateway aggregate Swagger |
| `http://localhost:8761` | Eureka Dashboard |
| `http://localhost:8090` | Kafka UI (Provectus) |
| `http://localhost:8083/connectors` | Debezium connector listesi |
| `http://localhost:9201` | Elasticsearch HTTP API |
| `http://localhost:5601` | Kibana (Elasticsearch UI) |
| `http://localhost:3000` | Grafana (admin/admin) |
| `http://localhost:9090` | Prometheus |
| `http://localhost` | Frontend |

### 10.3 Sadece Frontend

```bash
cd frontend
npm install
npx vite   # http://localhost:5173
```

`.env`:

```
VITE_API_BASE_URL=http://localhost:8080
```

## 11. Observability

| Sütun | Üretici | Toplayıcı | Storage / UI |
|---|---|---|---|
| **Logging** | Logback + LogstashEncoder (JSON, MDC) | Alloy (Docker socket SD) | Loki → Grafana |
| **Metrics** | Micrometer + Prometheus registry → `/actuator/prometheus` | Prometheus (pull, 15s) | TSDB → Grafana |
| **Tracing** | Micrometer Tracing + OTel bridge → OTLP HTTP | Tempo distributor | Tempo → Grafana |

Cross-link:

- **Metric exemplar** → trace (Prometheus exemplarTraceIdDestinations)
- **Trace** → log (Loki `service` tag eşleşmesi, tracesToLogsV2)
- **Log** → trace (Loki derivedField `traceId` → Tempo)

`correlationId` (business-level) ve `traceId` paralel çalışır; ikisi de her log satırında top-level field. Saga'da correlationId Kafka header üzerinden propagate olur.

---

## 12. Güvenlik

- **RS256 + JWKS** — `user-service` private key (`certs/private.pem`, PKCS8) ile imzalar, `/.well-known/jwks.json` endpoint'i public key'i servis eder.
- **Auth Server vs Resource Server** — `user-service` Auth Server, `api-gateway` ve downstream servisler Resource Server. Her downstream servis token'ı **kendi başına** doğrular (Zero Trust — gateway bypass edilse bile servis yetkisiz isteği reddeder).
- **Yeni servis eklerken:** `application.yaml`'a tek satır:
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            jwk-set-uri: ${USER_SERVICE_URL:http://localhost:9000}/.well-known/jwks.json
  ```
- **Common-lib içerikleri**: `UserPrincipal`, `UserPrincipalConverter`, `CorrelationIdFilter`, `BaseException`, `GlobalExceptionHandler` — hazır.

---

## 13. Live Config Refresh (Cloud Bus + Kafka)

`config-server` → Kafka `springCloudBus` → tüm servisler → `@RefreshScope` bean'leri restart-suz refresh.

```
git push (n11-config repo)
   │  webhook
   ▼
config-server :8888  /monitor   ── spring-cloud-config-monitor
   │  RefreshRemoteApplicationEvent
   ▼
Kafka topic: springCloudBus
   │
   ├─► user-service     (Bus consumer, common-lib transitif)
   ├─► product-service
   ├─► cart-service
   ├─► order-service
   ├─► payment-service
   ├─► stock-service
   └─► api-gateway
```

- **Manuel mod (lokal):** `curl -X POST http://localhost:8888/actuator/busrefresh` — webhook gerekmez.
- **GitHub webhook:** `POST <vm-ip>:8888/monitor` — değişen dosyalardan etkilenen servisleri target'lar (broadcast değil).
- **Tek servis target:** `POST /actuator/busrefresh/cart-service`.

Yapılandırma:
- Tüm servislerde `spring.kafka.bootstrap-servers` + `management.endpoints.web.exposure.include: ...,busrefresh,refresh`
- `config-server`'da `monitor,busrefresh` expose; `native` (lokal default) ve `git` profile'ı yan yana — `SPRING_PROFILES_ACTIVE=git` ile geçilir.
- `deploy/common-config.yml` → `microservice-platform`'a `KAFKA_BOOTSTRAP_SERVERS=kafka:29092` eklendiç

---

## 14. CI/CD

```
master push
   │
   ├─► CI (PR'larda)            ./mvnw verify + npm build
   │
   └─► Deploy (master'a merge)
         ├─ Jib build + push  → GCP Artifact Registry (her servis paralel)
         ├─ Docker buildx     → GAR (frontend)
         └─ SCP + SSH         → GCE VM: docker compose pull && up -d
```

