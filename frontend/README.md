# n11 Frontend

n11 e-ticaret demo projesi — React 19 + TypeScript + Vite + Tailwind CSS.

## Hızlı Başlangıç

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # Production build (tsc + vite build)
npm run preview  # Production build'i lokal serve et
```

Backend gateway adresi `.env` üzerinden ayarlanır:

```
VITE_API_BASE_URL=http://localhost:8080
```

## Teknoloji Yığını

| Katman | Teknoloji | Neden |
|---|---|---|
| Build | Vite 8 | Hızlı HMR, native ESM, küçük config |
| UI | React 19 + TypeScript | Tip güvenliği + Suspense + lazy routes |
| Stil | Tailwind CSS 3 + CSS variables | Utility-first, tema değiştirmek `--primary` gibi tek değişkene dokunmak |
| UI primitives | Radix UI + shadcn-style sarmalayıcılar | A11y, headless, kendi temamız |
| Server state | TanStack Query 5 | Cache, retry, refetch, polling — tek elden |
| Client state | Zustand (persist) | Auth tokens + anonymous cart için minimal |
| HTTP | Axios + interceptor | Bearer header otomasyonu, 401 → refresh akışı |
| Form | react-hook-form (zod yok) | Built-in `register()` validation kuralları yeter |
| Toast | sonner | shadcn ile uyumlu, kompakt |
| Icons | lucide-react | Tutarlı stroke, küçük bundle |

> **zod neden yok:** Şu anki ihtiyaç `required / minLength / pattern` ile karşılanıyor. `useForm`'un kendi rule sistemi yeterli, ekstra dependency'e gerek yok.

## Klasör Yapısı

```
src/
├── api/              # axios client + endpoint sarmalayıcılar
│   ├── client.ts     # interceptor, errorCode → mesaj map, refresh akışı
│   └── endpoints.ts  # authApi, productApi, cartApi, orderApi, paymentApi, stockApi, addressApi, categoryApi
├── components/       # cross-cutting bileşenler
│   ├── ui/           # shadcn-style primitives (button, input, card, dialog, badge, skeleton, floating-input...)
│   ├── layout/       # Header, Footer, CategoryBar, RootLayout, AccountLayout
│   ├── ApiErrorBox.tsx       # Network/server error fallback + retry
│   ├── ConfirmDialog.tsx     # `useConfirm()` hook — promise-based
│   ├── ErrorBoundary.tsx     # Top-level runtime hata yakalayıcı
│   ├── ListSkeleton.tsx      # CartItem/Order/Address/Payment skeleton'ları
│   ├── BannerCarousel.tsx    # Anasayfa rotating banner
│   └── Logo.tsx              # SVG marka logosu (data URL)
├── features/         # Domain-specific bileşen + hook'lar
│   ├── auth/         # AuthTabs, queries (useLogin/useRegister/useLogout/useMe)
│   ├── cart/         # queries (useCart/useAddToCart/useUpdateCartItem/useRemoveCartItem)
│   ├── checkout/     # AddressForm
│   ├── orders/       # OrderStatusBadge, OrderTimeline
│   └── products/     # ProductCard, ProductCardSkeleton, RecommendedProducts
├── hooks/            # usePageTitle
├── lib/              # utils (cn, formatTRY, formatDate)
├── pages/            # Route component'leri (lazy loaded)
├── routes/           # ProtectedRoute (auth guard)
├── store/            # Zustand store'lar (auth, anonymous cart)
└── types/            # api.ts — backend kontratıyla 1-1 type'lar
```

## Daha Fazla Bilgi

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Mimari kararlar, akışlar, uyarılar
- [docs/DESIGN.md](docs/DESIGN.md) — Renk, tipografi, n11 brand referansı, UI pattern'leri
- [docs/API.md](docs/API.md) — Tüketilen backend endpoint'leri + gateway gereksinimleri
