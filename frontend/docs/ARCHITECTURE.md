# Frontend Architecture

> Bu doküman; mimari kararları, akışları ve uyarıları kalıcı kaynak olarak tutar. Memory'ye özet yazılmaz — her şey burada.

---

## 1. Routing & Layout

`react-router-dom@7` ile nested routes. Bütün sayfalar `lazy()` ile bölünür ve bir `<Suspense fallback={<FullPageSpinner />}>` arkasında render olur.

```
<ErrorBoundary>
  <RootLayout>            ← Header + CategoryBar + Outlet + Footer
    /                     → HomePage
    /products             → ProductListPage
    /products/:slug       → ProductDetailPage
    /cart                 → CartPage
    /login, /register     → LoginPage, RegisterPage
    /checkout             → ProtectedRoute → CheckoutPage
    /orders, /orders/:id  → ProtectedRoute → OrderListPage, OrderDetailPage
    /account              → ProtectedRoute → AccountLayout (sidebar)
      /profile            → AccountProfilePage
      /addresses          → AccountAddressesPage
      /payments           → PaymentHistoryPage
    *                     → NotFoundPage
```

`ProtectedRoute` Zustand'dan token okur, yoksa `/login?next=<path>`'e yönlendirir.

---

## 2. Auth Akışı

| Aşama | Detay |
|---|---|
| Token saklama | `useAuthStore` — `accessToken`, `refreshToken`, `user`. `zustand/persist` ile `localStorage` (`n11-auth` key) |
| Login/Register | `authApi.login` / `authApi.register` token döner → `setTokens()` → `authApi.me()` çağırılır → `setUser()`. Anonim sepet varsa `cartApi.merge()` ile sunucuya birleştirilir |
| Bearer header | `axios.interceptors.request` token varsa `Authorization`'a ekler — **ama `/api/v1/auth/login`, `/register`, `/refresh` hariç tutulur** (aşağıdaki not) |
| 401 → refresh | `axios.interceptors.response`: 401 alırsan auth endpoint'leri hariç **tek seferlik** `refreshAccessToken()` denenir; başarılıysa orijinal istek tekrar çalıştırılır. Başarısızsa `clear()` + `/login?next=...` redirect |
| Tek refresh promise | `refreshPromise` modül-scope'lu; eş zamanlı 401'ler tek refresh paylaşır |

> **Memory feedback:** `localStorage` key adı `n11-auth`'dur — bu sabit her zaman tutarlı kalsın.

### Auth bootstrap endpoint'lerine Bearer GÖNDERME (kritik)

`POST /api/v1/auth/login`, `register`, `refresh` çağrılarında `Authorization: Bearer ...` header'ı **eklenmez** — `client.ts`'deki request interceptor `NO_AUTH_PATHS` listesiyle skip eder. Refresh ayrıca raw `axios.post` ile `api` instance'ını bypass eder.

**Neden kritik:** API gateway OAuth2 Resource Server modunda çalışıyor (`jwk-set-uri` ile JWKS). Spring Security davranışı şöyle:

- `Authorization` header **VAR** + token geçersiz/expired → gateway endpoint permitAll bile olsa **token doğrulamayı zorla yapar** ve 401 döner. Header user-service'e ulaşamaz.
- `Authorization` header **YOK** → gateway permitAll path'leri (auth/**) anonim geçirir, user-service'e ulaşır, normal login akışı çalışır.

Eski expired token'la sayfada kalan kullanıcı login olmaya çalışsa bile bu bypass olmadan gateway 401 atar → kullanıcı oturum açamaz. Bu yüzden auth-bootstrap istekleri Bearer'sız atılır.

---

## 3. Server State — React Query

Tek `QueryClient` `main.tsx`'de yaratılır:

```ts
defaultOptions: {
  queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30s },
  mutations: { retry: 0 },
}
```

### Query key konvansiyonu

| Veri | Key |
|---|---|
| Profil | `["auth", "me"]` |
| Sepet | `["cart"]` |
| Ürün listesi | `["products", { search, categoryId, minPrice, maxPrice, sort, page }]` |
| Ürün detay | `["product", slug]` |
| Önerilen ürünler | `["recommended", categoryId]` |
| Stok | `["stock", productId]` |
| Kategoriler | `["categories"]` |
| Adresler | `["addresses"]` |
| Sipariş listesi | `["orders", page]` |
| Sipariş detay | `["order", id]` |
| Ödeme (sipariş) | `["payment", orderId]` |
| Ödeme listesi | `["payments", "me", page]` |

### Saga polling

`OrderDetailPage` `refetchInterval: 2000` döner — ama sadece order durumu `PENDING / STOCK_RESERVED / PAYMENT_PROCESSING` ise. Diğer durumlarda polling biter.

### Cache invalidation

Mutation'lar başarılı olunca:
- `useAddToCart / useUpdateCartItem / useRemoveCartItem` → `invalidate(["cart"])`
- `useLogin` → `invalidate(["cart"])`
- `useLogout` → `queryClient.clear()`
- `createOrder` → `invalidate(["cart"])` + `invalidate(["orders"])`
- Adres CRUD → `invalidate(["addresses"])`

---

## 4. Client State — Zustand

Sadece **kalıcı** olması gereken, server'la senkronize olmayan veri:

| Store | İçerik | Persist |
|---|---|---|
| `useAuthStore` | accessToken, refreshToken, user | ✅ `n11-auth` |
| `useAnonymousCartStore` | `[{ productId, quantity }]` | ✅ `n11-anon-cart` |

> Tüm diğer state (sepet, sipariş, ürün) React Query'de tutulur. Zustand'a koyma.

---

## 5. Anonymous Cart Akışı

1. Login değilken: `useAddToCart` mutation `useAnonymousCartStore.addItem()` çağırır, localStorage'a yazar
2. Header'daki sepet sayacı `accessToken` varsa `cartQuery.data`'dan, yoksa `useAnonymousCartStore.totalCount()`'dan okur
3. Cart sayfasında: login'liyken `cartApi.get()`, anonim'ken `productApi.byIds(ids)` ile hidrasyon yapılır
4. Login olunca `useLogin.onSuccess` → `cartApi.merge(items)` → `useAnonymousCartStore.clear()`. `skippedProductIds` varsa toast warning

---

## 5.1 Stock UX

Backend product-service stock-service'i kendisi enrich ettiği için frontend stok için ayrı sorgu
atmıyor. `Product` ve `CartItem` tiplerinde gömülü iki alan:

```ts
stockStatus?: "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK" | "UNKNOWN";
availableQuantity?: number | null;
```

### UI davranışı

| Sayfa | Davranış |
|---|---|
| **ProductCard (listing)** | `OUT_OF_STOCK` → kart gri + "Tükendi" badge. `LOW_STOCK` → "Son N ürün" badge. Diğerlerinde rozet yok |
| **ProductDetailPage** | Status badge (Tükendi/Son N ürün/Stokta var/Bilinmiyor). Adet input `min(10, availableQuantity)` ile cap'lenir. `+` butonu cap'e ulaşınca disable. `OUT_OF_STOCK` → tüm CTA disable |
| **CartPage** | Item başına badge + `quantity > availableQuantity` ise kırmızı uyarı kutusu ("Stokta sadece N var, sepetinde M var"). Hiçbir item otomatik silinmez. `OUT_OF_STOCK` veya over-cart varsa **checkout disable** |
| **Add-to-cart hatası** | Backend'den 409 dönerse `notifyApiError` toast — `INSUFFICIENT_STOCK` veya `CART_ITEM_QUANTITY_LIMIT_EXCEEDED` errorCode'una göre Türkçe mesaj |

### Hard cap — sepetteki bir üründen max 10 adet

`src/lib/cart-constants.ts`'te `MAX_QUANTITY_PER_CART_ITEM = 10`. Backend'de aynı sayı
(`app.cart.max-quantity-per-item`). Frontend +/- butonlarını bu sayı ile cap'liyor ki kullanıcı
gereksiz 409 hatası almasın. Backend yine de hard kuralı zorlayan tek otorite.

### Stok bilgisinin tazeliği — neden gömülü gelir?

- **Listing:** product-service Redis cache 60sn TTL — yüksek trafik, stale OK
- **Detail / cart hydration:** product-service'in /batch endpoint'i direkt Feign — taze veri
- **Add-to-cart:** cart-service direkt stock-service Feign (cache yok) — anlık doğrulama
- **Frontend hiçbir yerde stock-service'e direkt sorgu atmaz** — UX katmanı backend orchestrator'a güvenir

---

## 6. Hata Yönetimi

| Katman | Strateji |
|---|---|
| Runtime crash | `<ErrorBoundary>` (React class component) → "Yeniden Yükle / Anasayfa" fallback. `componentDidCatch` console'a basar |
| API 401 (auth dışı) | Interceptor refresh dener → başarısızsa logout + `/login?next=` |
| API 4xx/5xx | Sayfa-bazlı `<ApiErrorBox error={...} onRetry={refetch} />` (network ile server hatasını ayrı gösterir) |
| Mutation hatası | `notifyApiError(err)` → sonner toast |
| errorCode → Türkçe mesaj | `ERROR_CODE_MESSAGES` map (`api/client.ts`): `EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `EMPTY_CART`, `INSUFFICIENT_STOCK`, `CART_ITEM_QUANTITY_LIMIT_EXCEEDED`, `STOCK_SERVICE_UNAVAILABLE`, vb. |

---

## 7. Form Validation (zod yok)

Tüm formlar `react-hook-form` + `register()` kurallarıyla:

```ts
register("email", {
  required: "E-posta zorunludur",
  pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: "Geçerli e-posta gir" },
});
```

Şifre validation'ı için `validate: { digit: ..., case: ... }` ile çoklu kural. Canlı feedback için `watch("password")` + render time check.

> **Memory feedback:** `frontend formlarında zod kullanma; react-hook-form built-in register kuralları` — bu kararın gerekçesi.

---

## 8. Custom Hook'lar / Utility'ler

| Hook | Amaç |
|---|---|
| `usePageTitle(title)` | `document.title`'ı route'a göre günceller. `null` → default n11 başlığı |
| `useConfirm()` | Promise-based onay dialog'u (native `confirm()` yerine) |
| `cn(...)` | `clsx + tailwind-merge` — class isim çakışmasız birleştirme |
| `formatTRY(amount, currency?)` | `Intl.NumberFormat("tr-TR")` ile fiyat |
| `formatDate(iso)` | `Intl.DateTimeFormat("tr-TR")` medium+short |

---

## 9. Bilinen Backend Bağımlılıkları (Frontend → Gateway)

Gateway'de bunlar route'lanmalı, yoksa frontend 404 alır:

| Endpoint | Servis | Durum |
|---|---|---|
| `/api/v1/auth/**` | user-service | ✅ |
| `/api/v1/users/**` | user-service | ⚠️ ekle (adresler, checkout-context) |
| `/api/v1/products/**` | product-service | ✅ |
| `/api/v1/categories/**` | product-service | ⚠️ ekle |
| `/api/v1/cart/**` | cart-service | ✅ |
| `/api/v1/orders/**` | order-service | ✅ |
| `/api/v1/payments/**` | payment-service | ⚠️ ekle (`/payments/me`, `/payments/orders/{id}`) |
| `/api/v1/stocks/**` | stock-service | ⚠️ ekle (graceful fallback var ama 404 spam'i temizlenir) |

---

## 10. Test / Build

- `npm run build` → TypeScript proje refs (`tsc -b`) + Vite build. Hata varsa CI burada düşer.
- Manuel test: dev server'ı çalıştır, login → sepet → sipariş akışını dene. Saga polling'i `OrderDetailPage`'de gözlemle.
- E2E framework yok (bootcamp scope).

---

## 11. Not — Performans Notları

- Tüm sayfalar `lazy()` ile chunk'lanır (route bazlı code-split)
- `staleTime: 30s` her query'de default → aynı sayfaya geri dönerken refetch yok
- Lazy image: `<img loading="lazy" />` ProductCard'da
- Skeleton'lar perceived performance için kullanılır
- Banner carousel auto-rotate `setInterval`'ı `useEffect` cleanup ile temizlenir
