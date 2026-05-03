# Frontend Design System

> Renk, tipografi, UI pattern'leri ve marka kararları. Renk değişikliği gerekirse `tailwind.config.js` + `src/index.css` ikilisi tek kaynak.

---

## 1. Marka Rengi (n11 Pink/Magenta)

### Mevcut Renk

| Token | Değer | Kullanım |
|---|---|---|
| `n11.DEFAULT` | `#ff25f5` (HSL 303° 100% 57%) | Ana brand — butonlar, "n11" yazısı, badge'ler, focus ring, link |
| `n11.dark` | `#d10ec5` | Hover state'leri, vurgular |
| `n11.light` | `#ffe6fd` | Açık zemin, badge background |
| `n11yellow.DEFAULT` | `#fff100` | Kampanya rozetleri (örn. "250 TL İndirim") |
| `n11yellow.dark` | `#e6d900` | Sarı için hover |

### CSS Değişkenleri (`src/index.css`)

```css
--primary: 303 100% 57%;        /* aynı renk HSL formatı */
--primary-foreground: 0 0% 100%;
--accent: 303 100% 96%;
--accent-foreground: 303 100% 32%;
--ring: 303 100% 57%;           /* focus ring rengi */
```

### Denenmiş alternatifler

| Renk | HSL | Sonuç |
|---|---|---|
| `#f72585` | 333° 92% 56% | İlk deneme — daha kırmızıya kaçan, "klasik pembe". Marka magenta'sından uzak |
| `#ff44ee` | 305° 100% 63% | Favicon SVG'siyle birebir (`#f4e`). Beyaz zeminde hafif solgun durabiliyor |
| `#ff25f5` | 303° 100% 57% | **Şu anki seçim** — daha vivid, daha yüksek kontrast |

> Renk değiştirmek için: tailwind config `n11.DEFAULT/dark/light` + `src/index.css`'deki `--primary --accent --accent-foreground --ring` HSL değerleri. İki dosya da güncellenmeli.

---

## 2. Logo

`src/components/Logo.tsx` — SVG data URL'iyle, `<img>` olarak render edilir. `size` prop'u (default 36px) ile boyut, `withText` prop'u ile yanına "n11" yazısı.

Aynı SVG `public/favicon.svg` olarak kayıtlı, `index.html`'de `<link rel="icon">` ile referans.

**Auth sayfalarında logo + yazı pattern'i:**

```tsx
<Logo size={32} />
<span className="text-3xl font-extrabold">n<span className="text-n11">11</span></span>
<span className="ml-1 inline-block h-3 w-3 rounded-full bg-n11" />
```

> Login/Register sayfalarındaki bu ikili (icon + text + dot) tasarım kullanıcı tarafından onaylandı, koru.

---

## 3. Tipografi

- Font: `system-ui` stack (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, ...`)
- Heading'ler: `font-semibold` veya `font-bold`
- Body: default Tailwind text-sm (14px)
- Small accents: `text-[11px] uppercase font-medium text-muted-foreground` (label'lar, eyebrow'lar)

---

## 4. UI Component Pattern'leri

### `FloatingInput` (`components/ui/floating-input.tsx`)

n11-style label-içeride pattern:
- Input boş + odak yok → label tam ortada (placeholder gibi)
- Odak veya içerik varsa → label `top-2`'ye kayar, `text-[11px]`, uppercase, n11 pembe
- 150ms transition
- `suffix` prop'u ile sağ tarafa eklenti (örn. şifre eye butonu)

> **Geçmiş bug uyarısı:** Eski versiyonda h-12 + pt-4 idi; cursor label resting position ile aynı yere düşüyordu. Şimdi h-14 + pt-6 → cursor label transition path'inden uzakta.

### Hover Pattern'leri

Header üst seviye butonları (Adres / Sepet / Hesap) → `hover:bg-accent`. Açılır menü item'ları (Profilim, Siparişlerim vb.) → aynı `hover:bg-accent`. Üye Ol/Giriş Yap text link'leri → `hover:text-n11 hover:underline`.

### `ApiErrorBox`

Network hatasıyla server hatasını ikonla ayırır: `WifiOff` vs `AlertTriangle`. `onRetry` callback'i ile "Tekrar Dene" butonu. Mesaj `extractApiError()` ile API response'tan friendly Türkçe'ye çevrilir.

### `ConfirmDialog` / `useConfirm()`

Promise-based:
```ts
const ok = await confirm({ title, description, destructive: true });
if (ok) doDangerousThing();
```

Native `confirm()` yerine sipariş iptal, adres silme vb. tüm yıkıcı işlemlerde bu kullanılır.

### Skeleton'lar

`components/ListSkeleton.tsx` içinde `CartItemSkeleton`, `OrderRowSkeleton`, `AddressCardSkeleton`, `PaymentRowSkeleton`. Spinner yerine sayfanın gerçek layout iskeletini gösterir → perceived performance.

### `OrderTimeline`

Saga state'lerini görsel olarak gösterir: PENDING → STOCK_RESERVED → CONFIRMED → SHIPPED → DELIVERED. Tamamlanan adımlar yeşil + check icon, mevcut adım dönen `CircleDashed`. CANCELLED ise iptal sebebiyle kırmızı kart.

---

## 5. Renk Kullanım Kuralları

| Rol | Renk | Örnek |
|---|---|---|
| Primary CTA | `bg-n11 text-white` | "Sepete Ekle", "Üye Ol" |
| Auth CTA | `bg-foreground text-white` | Auth tab aktif state, "Üye Ol/Giriş Yap" submit |
| Secondary CTA | `outline` | "İptal", "Vazgeç" |
| Destructive | `destructive` | "Sil", "İptal Et" |
| Success badge | `success` (emerald) | "Stokta var", "Tamamlandı" |
| Warning badge | `warning` (amber) | "Son N ürün", "Bekliyor" |
| Info badge | `info` (sky) | "Kargoda", "İade Edildi" |

---

## 6. Layout Boşlukları (Spacing)

- Container: `container mx-auto px-4` (Tailwind default + 1400px max)
- Section gap: `mb-6` veya `mb-8`
- Card padding: `p-4` veya `p-5`
- Form field gap: `space-y-3`
- Grid gap: `gap-3` (sıkı), `gap-4` (orta), `gap-6` (gevşek)

---

## 7. Responsive Breakpoint'ler

Tailwind defaultları:
- `sm: 640px` — küçük tablet
- `md: 768px` — tablet (Header'da kişisel detaylar burada açılır)
- `lg: 1024px` — desktop (sidebar layout'lar burada başlar)
- `xl: 1280px` — geniş desktop

Şu an mobile responsive eksik bazı yerlerde (özellikle filtre paneli, account sidebar). Backlog'da.
