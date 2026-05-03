import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { AlertTriangle, Minus, Plus, ShoppingBag, Trash2, X } from "lucide-react";
import { toast } from "sonner";
import { productApi } from "@/api/endpoints";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import { CartItemSkeleton } from "@/components/ListSkeleton";
import { useRemoveFromCartDialog } from "@/components/RemoveFromCartDialog";
import { usePageTitle } from "@/hooks/usePageTitle";
import { formatTRY } from "@/lib/utils";
import { useAuthStore } from "@/store/authStore";
import { useAnonymousCartStore } from "@/store/anonymousCartStore";
import { useFavoritesStore } from "@/store/favoritesStore";
import {
  useCart,
  useRemoveCartItem,
  useUpdateCartItem,
} from "@/features/cart/queries";
import { useProductLookups } from "@/features/products/useProductLookups";
import { FavoritesList } from "@/features/cart/FavoritesList";
import { MAX_QUANTITY_PER_CART_ITEM } from "@/lib/cart-constants";
import type { CartItem } from "@/types/api";

const FALLBACK_IMG = "https://placehold.co/200x200/fff3eb/ff6000?text=n11";

function useResolvedCartItems(): {
  items: CartItem[];
  isLoading: boolean;
  totalAmount: number;
} {
  const accessToken = useAuthStore((s) => s.accessToken);
  const anonItems = useAnonymousCartStore((s) => s.items);
  const cartQuery = useCart();

  const anonProductIds = anonItems.map((i) => i.productId);
  const anonProductsQuery = useQuery({
    queryKey: ["products", "batch", anonProductIds.sort().join(",")],
    queryFn: () => productApi.byIds(anonProductIds),
    enabled: !accessToken && anonProductIds.length > 0,
  });

  if (accessToken) {
    return {
      items: cartQuery.data?.items ?? [],
      isLoading: cartQuery.isLoading,
      totalAmount: cartQuery.data?.totalAmount ?? 0,
    };
  }

  if (anonItems.length === 0) {
    return { items: [], isLoading: false, totalAmount: 0 };
  }

  if (anonProductsQuery.isLoading) {
    return { items: [], isLoading: true, totalAmount: 0 };
  }

  const productMap = new Map((anonProductsQuery.data ?? []).map((p) => [p.id, p]));
  const items: CartItem[] = [];
  for (const ai of anonItems) {
    const product = productMap.get(ai.productId);
    if (!product) continue;
    items.push({
      productId: product.id,
      productName: product.name,
      imageUrl: product.imageUrl,
      unitPrice: Number(product.price),
      currency: product.currency,
      quantity: ai.quantity,
      subtotal: Number(product.price) * ai.quantity,
      stockStatus: product.stockStatus,
      availableQuantity: product.availableQuantity ?? null,
    });
  }

  const totalAmount = items.reduce((sum, i) => sum + i.subtotal, 0);
  return { items, isLoading: false, totalAmount };
}

type ItemStockState = {
  isOutOfStock: boolean;
  isOverCart: boolean;
  isAtLimit: boolean;
  warningText: string | null;
  cap: number;
};

function computeStockState(item: CartItem): ItemStockState {
  const isOutOfStock =
    item.stockStatus === "OUT_OF_STOCK" || item.availableQuantity === 0;
  const available = item.availableQuantity ?? null;
  const isOverCart =
    !isOutOfStock && available != null && item.quantity > available;
  const isAtLimit = item.quantity >= MAX_QUANTITY_PER_CART_ITEM;

  // Adet butonunun cap'i — stok varsa min(stok, 10), yoksa sadece 10.
  const cap =
    available != null && available > 0
      ? Math.min(MAX_QUANTITY_PER_CART_ITEM, available)
      : MAX_QUANTITY_PER_CART_ITEM;

  let warningText: string | null = null;
  if (isOutOfStock) {
    warningText = "Bu ürün stokta yok. Sepetten çıkar veya favorilere ekle.";
  } else if (isOverCart && available != null) {
    warningText = `Stokta ${available} adet kaldı, adet otomatik güncelleniyor.`;
  }

  return { isOutOfStock, isOverCart, isAtLimit, warningText, cap };
}

export default function CartPage() {
  const navigate = useNavigate();
  const accessToken = useAuthStore((s) => s.accessToken);
  const { items, isLoading, totalAmount } = useResolvedCartItems();
  const { lookups } = useProductLookups(items.map((i) => i.productId));
  usePageTitle(items.length > 0 ? `Sepetim (${items.length})` : "Sepetim");
  const updateMutation = useUpdateCartItem();
  const removeMutation = useRemoveCartItem();
  const { ask: askRemove, dialog: removeDialog } = useRemoveFromCartDialog();
  const addFavorite = useFavoritesStore((s) => s.add);
  const hasFavorite = useFavoritesStore((s) => s.has);
  const [autoFixNotices, setAutoFixNotices] = useState<Map<string, string>>(
    new Map()
  );

  const setNotice = (productId: string, message: string) => {
    setAutoFixNotices((prev) => {
      const next = new Map(prev);
      next.set(productId, message);
      return next;
    });
  };

  const dismissNotice = (productId: string) => {
    setAutoFixNotices((prev) => {
      if (!prev.has(productId)) return prev;
      const next = new Map(prev);
      next.delete(productId);
      return next;
    });
  };

  const pendingProductId =
    (updateMutation.isPending && updateMutation.variables?.productId) ||
    (removeMutation.isPending && removeMutation.variables) ||
    null;

  const handleRemoveWithChoice = async (item: CartItem) => {
    const outcome = await askRemove({
      productName: item.productName,
      alreadyInFavorites: hasFavorite(item.productId),
    });
    if (outcome === "cancel") return;

    if (outcome === "favorite-and-remove") {
      const lookup = lookups.get(item.productId);
      addFavorite({
        productId: item.productId,
        name: item.productName,
        slug: lookup?.slug ?? item.productId,
        imageUrl: item.imageUrl ?? lookup?.imageUrl,
        price: item.unitPrice,
        currency: item.currency,
      });
      toast.success("Favorilere eklendi");
    }
    removeMutation.mutate(item.productId);
  };

  const handleDecrement = async (item: CartItem) => {
    if (item.quantity > 1) {
      dismissNotice(item.productId);
      updateMutation.mutate({
        productId: item.productId,
        quantity: item.quantity - 1,
      });
      return;
    }
    await handleRemoveWithChoice(item);
  };

  const handleIncrement = (item: CartItem) => {
    dismissNotice(item.productId);
    updateMutation.mutate({
      productId: item.productId,
      quantity: item.quantity + 1,
    });
  };

  const handleRemove = async (item: CartItem) => {
    await handleRemoveWithChoice(item);
  };

  // Sepete giren ürünlerin adet aşımını otomatik düzelt — quantity > available ise
  // available'a çek. Stoğu biten ürünleri OTOMATİK silmiyoruz; karar kullanıcıya
  // bırakılır (favorilere ekleme/silme seçenekleri için). Bekleyen mutation varken
  // çalışmaz — auto-fix ile manuel update çakışmasın diye.
  const autoFixedRef = useRef<Set<string>>(new Set());
  const isMutating = updateMutation.isPending || removeMutation.isPending;
  useEffect(() => {
    if (isLoading) return;
    if (isMutating) return;
    for (const item of items) {
      const key = `${item.productId}:${item.quantity}:${item.availableQuantity}:${item.stockStatus}`;
      if (autoFixedRef.current.has(key)) continue;

      // Stoğu biten / out-of-stock ürünler için auto-fix yok; kullanıcı görsün ve
      // kart üzerindeki CTA ile karar versin (Sil veya Favorilere ekle).
      if (item.stockStatus === "OUT_OF_STOCK" || item.availableQuantity === 0) {
        continue;
      }
      const available = item.availableQuantity ?? null;
      if (available != null && available > 0 && item.quantity > available) {
        autoFixedRef.current.add(key);
        updateMutation.mutate({
          productId: item.productId,
          quantity: available,
        });
        setNotice(
          item.productId,
          `Stokta ${available} adet kaldığı için adediniz ${item.quantity} → ${available} olarak güncellendi.`
        );
        // Aynı render içinde sadece bir auto-fix tetikle — sonraki mutation
        // bittiğinde useEffect tekrar çalışıp bir sonraki item'i halleder.
        break;
      }
    }
    // mutations are stable refs; intentional minimal deps
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items, isLoading, isMutating]);

  const blockingItems = items.filter((i) => {
    const s = computeStockState(i);
    return s.isOutOfStock || s.isOverCart;
  });
  const canCheckout = blockingItems.length === 0;

  const handleCheckout = () => {
    if (!canCheckout) return;
    if (!accessToken) {
      navigate("/login?next=/checkout");
    } else {
      navigate("/checkout");
    }
  };

  if (isLoading) {
    return (
      <div className="container py-6">
        <div className="mb-6 h-7 w-40 animate-pulse rounded bg-secondary" />
        <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <CartItemSkeleton key={i} />
            ))}
          </div>
          <div className="hidden h-64 animate-pulse rounded-lg bg-secondary lg:block" />
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-md rounded-lg border bg-white p-10 text-center">
          <ShoppingBag className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
          <h2 className="mb-2 text-xl font-semibold">Sepetin boş</h2>
          <p className="mb-6 text-sm text-muted-foreground">
            Sepetine ekleyecek harika ürünler seni bekliyor.
          </p>
          <Button asChild>
            <Link to="/products">Alışverişe başla</Link>
          </Button>
        </div>
        <FavoritesList />
        {removeDialog}
      </div>
    );
  }

  return (
    <div className="container py-6">
      <h1 className="mb-6 text-2xl font-semibold">Sepetim ({items.length} ürün)</h1>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-3">
          {items.map((item) => {
            const stockState = computeStockState(item);
            const isItemPending = pendingProductId === item.productId;
            const decrementDisabled =
              updateMutation.isPending || removeMutation.isPending;
            const incrementDisabled =
              updateMutation.isPending ||
              stockState.isOutOfStock ||
              item.quantity >= stockState.cap;

            const lookup = lookups.get(item.productId);
            const detailHref = lookup ? `/products/${lookup.slug}` : null;
            const thumbSrc =
              item.imageUrl || lookup?.imageUrl || FALLBACK_IMG;

            return (
              <div
                key={item.productId}
                className="relative flex flex-col gap-2 rounded-lg border bg-white p-4"
              >
                {isItemPending && (
                  <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center rounded-lg bg-white/60 backdrop-blur-[1px]">
                    <Spinner size={24} className="text-n11" />
                  </div>
                )}
                <div className="flex gap-4">
                  {detailHref ? (
                    <Link
                      to={detailHref}
                      className="flex-shrink-0"
                      aria-label={`${item.productName} detayı`}
                    >
                      <img
                        src={thumbSrc}
                        alt={item.productName}
                        onError={(e) => {
                          (e.target as HTMLImageElement).src = FALLBACK_IMG;
                        }}
                        className={
                          "h-24 w-24 rounded-md object-cover transition-opacity hover:opacity-85 " +
                          (stockState.isOutOfStock ? "opacity-50 grayscale" : "")
                        }
                      />
                    </Link>
                  ) : (
                    <img
                      src={thumbSrc}
                      alt={item.productName}
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = FALLBACK_IMG;
                      }}
                      className={
                        "h-24 w-24 flex-shrink-0 rounded-md object-cover " +
                        (stockState.isOutOfStock ? "opacity-50 grayscale" : "")
                      }
                    />
                  )}
                  <div className="flex flex-1 flex-col gap-2">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      {detailHref ? (
                        <Link
                          to={detailHref}
                          className="font-medium leading-tight hover:text-n11 hover:underline"
                        >
                          {item.productName}
                        </Link>
                      ) : (
                        <h3 className="font-medium leading-tight">
                          {item.productName}
                        </h3>
                      )}
                      {stockState.isOutOfStock && (
                        <Badge variant="destructive">Tükendi</Badge>
                      )}
                      {!stockState.isOutOfStock &&
                        item.stockStatus === "LOW_STOCK" &&
                        item.availableQuantity != null && (
                          <Badge variant="warning">
                            Son {item.availableQuantity} ürün
                          </Badge>
                        )}
                    </div>
                    <div className="text-sm text-muted-foreground">
                      Birim: {formatTRY(item.unitPrice, item.currency)}
                    </div>
                    <div className="mt-auto flex items-center">
                      <div className="flex items-center rounded-md border">
                        <button
                          onClick={() => handleDecrement(item)}
                          disabled={decrementDisabled}
                          className="flex h-8 w-8 items-center justify-center hover:bg-accent disabled:opacity-50"
                          aria-label="Azalt"
                        >
                          <Minus className="h-3.5 w-3.5" />
                        </button>
                        <span className="w-10 text-center text-sm font-medium">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => handleIncrement(item)}
                          disabled={incrementDisabled}
                          className="flex h-8 w-8 items-center justify-center hover:bg-accent disabled:opacity-50"
                          aria-label="Arttır"
                        >
                          <Plus className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                  <div className="flex flex-col items-end justify-between">
                    <div className="font-bold text-foreground">
                      {formatTRY(item.subtotal, item.currency)}
                    </div>
                    <button
                      onClick={() => handleRemove(item)}
                      className="flex items-center gap-1 text-xs text-muted-foreground hover:text-destructive"
                    >
                      <Trash2 className="h-3.5 w-3.5" /> Sil
                    </button>
                  </div>
                </div>

                {stockState.warningText && (
                  <div className="rounded-md bg-destructive/10 p-2.5 text-xs text-destructive">
                    <div className="flex items-start gap-2">
                      <AlertTriangle className="mt-0.5 h-3.5 w-3.5 flex-shrink-0" />
                      <span>{stockState.warningText}</span>
                    </div>
                    {stockState.isOutOfStock && (
                      <div className="mt-2 flex gap-2 pl-5">
                        <button
                          onClick={() => {
                            const lookup = lookups.get(item.productId);
                            addFavorite({
                              productId: item.productId,
                              name: item.productName,
                              slug: lookup?.slug ?? item.productId,
                              imageUrl: item.imageUrl ?? lookup?.imageUrl,
                              price: item.unitPrice,
                              currency: item.currency,
                            });
                            removeMutation.mutate(item.productId);
                            toast.success("Favorilere eklendi ve sepetten çıkarıldı");
                          }}
                          disabled={
                            updateMutation.isPending ||
                            removeMutation.isPending ||
                            hasFavorite(item.productId)
                          }
                          className="rounded-md bg-white px-3 py-1.5 text-xs font-medium text-foreground shadow-sm hover:bg-secondary disabled:opacity-50"
                        >
                          {hasFavorite(item.productId)
                            ? "Zaten favorilerde"
                            : "Favorilere Ekle ve Çıkar"}
                        </button>
                        <button
                          onClick={() => removeMutation.mutate(item.productId)}
                          disabled={
                            updateMutation.isPending || removeMutation.isPending
                          }
                          className="rounded-md bg-destructive px-3 py-1.5 text-xs font-medium text-destructive-foreground shadow-sm hover:bg-destructive/90 disabled:opacity-50"
                        >
                          Sepetten Çıkar
                        </button>
                      </div>
                    )}
                  </div>
                )}
                {!stockState.warningText &&
                  autoFixNotices.has(item.productId) && (
                    <div className="flex items-start gap-2 rounded-md bg-amber-50 p-2 text-xs text-amber-800">
                      <AlertTriangle className="mt-0.5 h-3.5 w-3.5 flex-shrink-0" />
                      <span className="flex-1">
                        {autoFixNotices.get(item.productId)}
                      </span>
                      <button
                        onClick={() => dismissNotice(item.productId)}
                        className="flex-shrink-0 rounded p-0.5 hover:bg-amber-100"
                        aria-label="Bildirimi kapat"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  )}
                {!stockState.warningText &&
                  !autoFixNotices.has(item.productId) &&
                  stockState.isAtLimit && (
                    <div className="text-xs text-muted-foreground">
                      Bu üründen sepete en fazla {MAX_QUANTITY_PER_CART_ITEM}{" "}
                      adet ekleyebilirsin.
                    </div>
                  )}
              </div>
            );
          })}
        </div>

        <aside className="lg:sticky lg:top-20 lg:h-fit">
          <div className="space-y-4 rounded-lg border bg-white p-5">
            <h3 className="text-base font-semibold">Sipariş Özeti</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Ara Toplam</span>
                <span>{formatTRY(totalAmount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Kargo</span>
                <span className="text-emerald-600">Ücretsiz</span>
              </div>
            </div>
            <Separator />
            <div className="flex justify-between text-base">
              <span className="font-semibold">Toplam</span>
              <span className="font-bold text-foreground">
                {formatTRY(totalAmount)}
              </span>
            </div>

            {!canCheckout && (
              <div className="flex items-start gap-2 rounded-md bg-amber-50 p-3 text-xs text-amber-700">
                <AlertTriangle className="mt-0.5 h-4 w-4 flex-shrink-0" />
                <span>
                  Sepetinde stoğu olmayan ürünler var. Devam edebilmek için bu
                  ürünleri çıkar veya favorilere ekle.
                </span>
              </div>
            )}

            <Button
              size="lg"
              className="w-full"
              onClick={handleCheckout}
              disabled={!canCheckout}
            >
              {accessToken ? "Ödemeye Geç" : "Giriş Yap ve Devam Et"}
            </Button>
            {!accessToken && canCheckout && (
              <p className="text-center text-xs text-muted-foreground">
                Giriş yapınca sepetin senin hesabınla birleştirilecek.
              </p>
            )}
          </div>
        </aside>
      </div>
      <FavoritesList />
      {removeDialog}
    </div>
  );
}
