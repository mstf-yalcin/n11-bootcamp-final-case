# Frontend → Backend API Sözleşmesi

Tüketilen tüm endpoint'ler `src/api/endpoints.ts` içinde sarmalanmış durumda.

> Backend tarafındaki kaynak doküman: `backend/docs/services/*.md`. Burası sadece frontend'in **tükettiği** subset'i gösterir.

---

## Base URL

`VITE_API_BASE_URL` (default `http://localhost:8080` — gateway). Tüm istekler bu base'in altına gider.

## Response Zarfı

Backend `common-lib.ApiResponse<T>` sarmalayıcısı kullanır:

```ts
{ success, data, message?, errorCode?, page?, errors? }
```

Frontend `unwrap()` helper'ıyla `data`'yı açar; sayfalı response'lar için `page` ayrıca okunur.

---

## Endpoint'ler

### Auth — `/api/v1/auth`

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/register` | `{ email, password, firstName, lastName, phone? }` | `TokenResponse { accessToken, refreshToken }` |
| POST | `/login` | `{ email, password }` | `TokenResponse` |
| POST | `/refresh` | `{ refreshToken }` | `TokenResponse` |
| POST | `/logout` | `{ refreshToken }` | void |
| GET | `/me` | — | `UserResponse { id, email, firstName, lastName, phone?, roles }` |

> **Frontend ekstra:** Şifre validation 8-50 karakter + en az 1 rakam + en az 1 büyük/küçük harf. Backend min 6 (frontend daha katı). Telefon backend'de opsiyonel ama frontend zorunlu (RegisterPage).

### Users — `/api/v1/users/me`

| Method | Path | Açıklama |
|---|---|---|
| GET | `/addresses` | Kullanıcının adresleri |
| POST | `/addresses` | Yeni adres |
| PUT | `/addresses/{id}` | Adres güncelle |
| DELETE | `/addresses/{id}` | Adres sil |
| GET | `/checkout-context?addressId={id}` | order-service tarafından çağrılır (frontend doğrudan çağırmaz) |

### Products — `/api/v1/products`

| Method | Path | Açıklama |
|---|---|---|
| GET | `?categoryId&minPrice&maxPrice&search&page&size&sort` | Filtreli, sayfalı liste |
| GET | `/{slug}` | Slug ile detay |
| GET | `/batch?ids=...` | Toplu çekme (RecommendedProducts ve anonim cart hidrasyonu için) |

> **Stock alanları product response'unda gömülü:** `stockStatus` (`IN_STOCK | LOW_STOCK | OUT_OF_STOCK | UNKNOWN`)
> ve `availableQuantity` (number | null). Backend product-service stock-service'i kendisi çağırır:
> - Listing → Redis cache 60sn
> - Detail/batch → direkt Feign (taze)
>
> Frontend `stockApi.byProductId` artık kullanılmıyor — ProductDetailPage stok bilgisini product
> response'undan okur.

### Categories — `/api/v1/categories`

| Method | Path |
|---|---|
| GET | `/` |

### Cart — `/api/v1/cart` (Authenticated)

| Method | Path | Body |
|---|---|---|
| GET | `/` | — |
| POST | `/items` | `{ productId, quantity }` |
| PUT | `/items/{productId}` | `{ quantity }` |
| DELETE | `/items/{productId}` | — |
| POST | `/merge` | `{ items: [{ productId, quantity }] }` |
| DELETE | `/` | — |

> **Cart item kuralları (backend tarafından zorlanır):**
> - Bir üründen sepete max **10 adet** eklenebilir (`MAX_QUANTITY_PER_CART_ITEM` — `src/lib/cart-constants.ts`)
> - Aşılırsa `409 CART_ITEM_QUANTITY_LIMIT_EXCEEDED`
> - Stoktan fazlası istenirse `409 INSUFFICIENT_STOCK`
> - Stock-service down ise `503 STOCK_SERVICE_UNAVAILABLE` (fail-closed, ürün eklenemez)
>
> **Cart response'unda her item:** `stockStatus` + `availableQuantity` alanları product-service'ten gelir
> (cart-service direkt stock-service çağırmaz, product-service `/batch` zaten enrich edilmiş).

### Orders — `/api/v1/orders` (Authenticated)

| Method | Path | Body |
|---|---|---|
| POST | `/` | `{ addressId, identityNumber }` |
| GET | `/` | sayfalı liste |
| GET | `/{id}` | tekil — saga polling endpoint'i |
| PUT | `/{id}/cancel` | `PENDING / STOCK_RESERVED` durumunda |

### Payments — `/api/v1/payments` (Authenticated)

| Method | Path |
|---|---|
| GET | `/orders/{orderId}` |
| GET | `/me?page&size` |

### Stock — `/api/v1/stocks`

Frontend stock-service'i **doğrudan çağırmaz** — backend product-service ve cart-service her ikisinde de
stock enrichment'ı internal Feign ile yapar. Frontend için sadece admin sayfaları (`adminStockApi`)
stock CRUD endpoint'lerini kullanır.

| Method | Path | Frontend kullanımı |
|---|---|---|
| GET | `/availability?productIds=` | Backend internal — frontend çağırmaz |
| GET | `/{productId}` | Sadece admin |
| POST/PUT/DELETE | `/...` | Sadece admin (`adminStockApi`) |

> Stock bilgisi product/cart response'larında gömülü gelir, listing'den detail'a kadar her
> sayfada `stockStatus` + `availableQuantity` field'ları üzerinden okunur.

---

## Bilinen Backend Bağımlılıkları (Gateway Route Gereksinimleri)

`backend/infrastructure/api-gateway/src/main/resources/application.yaml` predicate'leri:

| Path | Servis | Durum |
|---|---|---|
| `/api/v1/auth/**` | user-service | ✅ |
| `/api/v1/users/**` | user-service | ⚠️ — `/api/v1/auth/**` rotalı ama `/api/v1/users/**` rotası eklenmeli |
| `/api/v1/products/**` | product-service | ✅ (`/api/v1/product/**` → `/api/v1/products/**` olarak düzeltildi) |
| `/api/v1/categories/**` | product-service | ⚠️ — eklenmeli |
| `/api/v1/cart/**` | cart-service | ✅ |
| `/api/v1/orders/**` | order-service | ✅ |
| `/api/v1/payments/**` | payment-service | ⚠️ — eklenmeli |
| `/api/v1/stocks/**` | stock-service | ⚠️ — eklenmeli (ama frontend graceful) |

---

## Saga Polling Davranışı

`OrderDetailPage`:
- Order durumu `PENDING / STOCK_RESERVED / PAYMENT_PROCESSING` ise `refetchInterval: 2000`
- Diğer durumlarda polling biter
- Payment query (paralel) `isInProgress` false olunca devreye girer (saga tamamlanınca)

---

## ErrorCode → Türkçe Mesaj Eşleşmesi

`api/client.ts` içindeki `ERROR_CODE_MESSAGES` map'i:

| errorCode | Mesaj |
|---|---|
| **Auth & user** | |
| `EMAIL_ALREADY_EXISTS` | Bu e-posta zaten kayıtlı. Giriş yapmayı dene. |
| `PHONE_ALREADY_EXISTS` | Bu telefon numarası kullanılamaz. |
| `INVALID_CREDENTIALS` | E-posta veya şifre hatalı. |
| `INVALID_TOKEN` | Oturumun süresi doldu, tekrar giriş yap. |
| `USER_NOT_FOUND` | Kullanıcı bulunamadı. |
| `ADDRESS_NOT_FOUND` | Adres bulunamadı. |
| **Cart & order** | |
| `CART_ITEM_NOT_FOUND` | Bu ürün sepetinde değil. |
| `CART_ITEM_QUANTITY_LIMIT_EXCEEDED` | Bir üründen sepete en fazla 10 adet ekleyebilirsin. |
| `EMPTY_CART` | Sepetin boş. Önce ürün ekle. |
| `ORDER_NOT_FOUND` | Sipariş bulunamadı. |
| `INVALID_ORDER_STATE` | Sipariş bu aşamada işlem yapmaya uygun değil. |
| `PRODUCT_SNAPSHOT_MISMATCH` | Sepetindeki bir ürün artık sistemde yok. Sepetini gözden geçir. |
| `PAYMENT_NOT_FOUND` | Ödeme kaydı bulunamadı. |
| **Catalog** | |
| `PRODUCT_NOT_FOUND` | Ürün bulunamadı veya kaldırılmış. |
| `CATEGORY_NOT_FOUND` | Kategori bulunamadı. |
| `TAG_NOT_FOUND` | Etiket bulunamadı. |
| `SLUG_ALREADY_EXISTS` | Bu slug zaten kullanılıyor. |
| `SLUG_GENERATION_FAILED` | Slug oluşturulamadı, başlığı değiştirip tekrar dene. |
| **Stock** | |
| `INSUFFICIENT_STOCK` | Stok yetersiz, ürünü daha az adetle tekrar dene. |
| `STOCK_NOT_FOUND` | Stok kaydı bulunamadı. |
| `STOCK_ALREADY_EXISTS` | Bu ürün için stok kaydı zaten mevcut. |
| `INVALID_STOCK_QUANTITY` | Geçersiz stok miktarı. |
| `STOCK_SERVICE_UNAVAILABLE` | Stok bilgisi alınamıyor, lütfen tekrar dene. |
| **Generic** | |
| `VALIDATION_ERROR` | Form bilgileri geçerli değil. |
| `DUPLICATE_ENTRY` | Bu kayıt zaten mevcut. |
| `NOT_FOUND` | Aradığın şey bulunamadı. |
| `MALFORMED_REQUEST` | Geçersiz istek formatı. |
| `INTERNAL_ERROR` | Sunucu hatası, lütfen tekrar dene. |
| `GATEWAY_ERROR` | Servis hatası, az sonra tekrar dene. |
| `*_SERVICE_UNAVAILABLE` | İlgili servise ulaşılamıyor, az sonra tekrar dene. |

Yeni errorCode eklenince hem backend exception'da hem buradaki map'te güncellenmeli.
