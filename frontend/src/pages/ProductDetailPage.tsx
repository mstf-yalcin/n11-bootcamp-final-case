import { useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Loader2, Minus, Package, Plus, ShieldCheck, Star, Truck } from "lucide-react";
import { categoryApi, productApi } from "@/api/endpoints";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { ApiErrorBox } from "@/components/ApiErrorBox";
import { RecommendedProducts } from "@/features/products/RecommendedProducts";
import { usePageTitle } from "@/hooks/usePageTitle";
import { useRecentlyViewedStore } from "@/store/recentlyViewedStore";
import { useEffect } from "react";
import { formatTRY } from "@/lib/utils";
import { useAddToCart, useCart } from "@/features/cart/queries";
import { FavoriteButton } from "@/features/products/FavoriteButton";
import { useAuthStore } from "@/store/authStore";
import { useAnonymousCartStore } from "@/store/anonymousCartStore";
import { MAX_QUANTITY_PER_CART_ITEM } from "@/lib/cart-constants";

const FALLBACK_IMG = "https://placehold.co/600x600/fff3eb/ff6000?text=n11";

export default function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const [qty, setQty] = useState(1);
  const addToCart = useAddToCart();
  const accessToken = useAuthStore((s) => s.accessToken);
  const cartQuery = useCart();
  const anonItems = useAnonymousCartStore((s) => s.items);

  const productQuery = useQuery({
    queryKey: ["product", slug],
    queryFn: () => productApi.bySlug(slug!),
    enabled: Boolean(slug),
  });

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
    staleTime: 5 * 60 * 1000,
  });

  const { data, isLoading, isError, error, refetch } = productQuery;

  usePageTitle(data ? data.name : "Ürün yükleniyor");

  const pushRecentlyViewed = useRecentlyViewedStore((s) => s.push);
  useEffect(() => {
    if (data?.id) pushRecentlyViewed(data.id);
  }, [data?.id, pushRecentlyViewed]);

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner size={28} />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="container py-12">
        <ApiErrorBox
          error={error}
          onRetry={refetch}
          title="Ürün yüklenemedi"
        />
        <div className="mt-4 text-center">
          <Button asChild variant="outline">
            <Link to="/products">Ürünlere dön</Link>
          </Button>
        </div>
      </div>
    );
  }

  const stockStatus = data.stockStatus;
  const available = data.availableQuantity ?? null;
  const isOutOfStock = stockStatus === "OUT_OF_STOCK";
  const isLowStock = stockStatus === "LOW_STOCK";
  const isUnknown = stockStatus === "UNKNOWN" || stockStatus === undefined;

  const currentInCart = accessToken
    ? cartQuery.data?.items.find((i) => i.productId === data.id)?.quantity ?? 0
    : anonItems.find((i) => i.productId === data.id)?.quantity ?? 0;

  // Hard cap: backend kuralı (max 10/item) ile stok değerinin minimumu.
  const stockCap =
    available != null && available >= 0 ? available : MAX_QUANTITY_PER_CART_ITEM;
  const absoluteMax = Math.min(MAX_QUANTITY_PER_CART_ITEM, stockCap);
  const remainingCapacity = Math.max(0, absoluteMax - currentInCart);
  const maxQty = Math.max(1, remainingCapacity || 1);
  const effectiveQty = Math.min(qty, maxQty);
  const reachedLimit = currentInCart + effectiveQty >= MAX_QUANTITY_PER_CART_ITEM;
  const cartFull = remainingCapacity <= 0;

  const handleAddToCart = () => {
    addToCart.mutate({
      productId: data.id,
      quantity: effectiveQty,
      availableQuantity: available,
      currentInCart,
    });
  };

  const handleBuyNow = () => {
    addToCart.mutate(
      {
        productId: data.id,
        quantity: effectiveQty,
        availableQuantity: available,
        currentInCart,
      },
      { onSuccess: () => navigate("/cart") }
    );
  };

  return (
    <div className="container py-6">
      <nav className="mb-4 text-sm text-muted-foreground">
        <Link to="/" className="hover:text-n11">
          Anasayfa
        </Link>
        <span className="mx-2">/</span>
        <Link
          to={(() => {
            const slug = categoriesQuery.data?.find(
              (c) => c.id === data.categoryId
            )?.slug;
            return slug ? `/products?category=${slug}` : "/products";
          })()}
          className="hover:text-n11"
        >
          {data.categoryName}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-foreground">{data.name}</span>
      </nav>

      <div className="grid gap-8 lg:grid-cols-[1.2fr_1fr]">
        <div className="overflow-hidden rounded-lg border bg-white p-6">
          <div className="aspect-square">
            <img
              src={data.imageUrl || FALLBACK_IMG}
              alt={data.name}
              onError={(e) => {
                (e.target as HTMLImageElement).src = FALLBACK_IMG;
              }}
              className="h-full w-full object-contain"
            />
          </div>
        </div>

        <div className="space-y-4">
          <div className="text-xs uppercase text-muted-foreground">
            {data.categoryName}
          </div>
          <h1 className="text-2xl font-semibold">{data.name}</h1>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
            <span className="font-medium text-foreground">
              {Number(data.ratingAverage).toFixed(1)}
            </span>
            <span>({data.ratingCount} değerlendirme)</span>
          </div>

          {data.tags && data.tags.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {data.tags.map((t) => (
                <Badge key={t.id} variant="secondary">
                  {t.name}
                </Badge>
              ))}
            </div>
          )}

          <Separator />

          <div className="rounded-lg border bg-white p-4">
            <div className="text-3xl font-bold text-foreground">
              {formatTRY(data.price, data.currency)}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              KDV dahil — kargo bedava
            </p>

            <div className="mt-3 flex items-center gap-2">
              <Package className="h-4 w-4" />
              {isOutOfStock ? (
                <Badge variant="destructive">Stokta yok</Badge>
              ) : isLowStock && available != null ? (
                <Badge variant="warning">
                  Son {available} ürün — acele et!
                </Badge>
              ) : isUnknown ? (
                <Badge variant="secondary">Stok bilgisi alınamadı</Badge>
              ) : (
                <Badge variant="success">Stokta var</Badge>
              )}
            </div>

            <div className="mt-4 flex items-center gap-3">
              <span className="text-sm font-medium">Adet:</span>
              <div className="flex items-center rounded-md border">
                <button
                  onClick={() => setQty((q) => Math.max(1, q - 1))}
                  className="flex h-9 w-9 items-center justify-center hover:bg-accent disabled:opacity-50"
                  disabled={isOutOfStock || cartFull}
                  aria-label="Azalt"
                >
                  <Minus className="h-4 w-4" />
                </button>
                <span className="w-10 text-center text-sm font-medium">
                  {effectiveQty}
                </span>
                <button
                  onClick={() => setQty((q) => Math.min(maxQty, q + 1))}
                  className="flex h-9 w-9 items-center justify-center hover:bg-accent disabled:opacity-50"
                  disabled={isOutOfStock || cartFull || effectiveQty >= maxQty}
                  aria-label="Arttır"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>
              {reachedLimit && !isOutOfStock && !cartFull && (
                <span className="text-xs text-muted-foreground">
                  Bu üründen sepete en fazla {MAX_QUANTITY_PER_CART_ITEM} adet
                  eklenebilir
                </span>
              )}
            </div>

            {currentInCart > 0 && !isOutOfStock && (
              <p className="mt-2 text-xs text-muted-foreground">
                Sepetinde bu üründen {currentInCart} adet var.
              </p>
            )}
            {cartFull && !isOutOfStock && (
              <p className="mt-2 text-xs text-amber-600">
                Bu üründen sepete eklenebilecek maksimum adete ulaştın.
              </p>
            )}

            <div className="mt-4 grid gap-2 sm:grid-cols-2">
              <Button
                variant="outline"
                size="lg"
                onClick={handleAddToCart}
                disabled={addToCart.isPending || isOutOfStock || cartFull}
              >
                {addToCart.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Sepete Ekleniyor...
                  </>
                ) : isOutOfStock ? (
                  "Stokta Yok"
                ) : cartFull ? (
                  "Sepette Maksimum"
                ) : (
                  "Sepete Ekle"
                )}
              </Button>
              <Button
                size="lg"
                onClick={handleBuyNow}
                disabled={addToCart.isPending || isOutOfStock || cartFull}
              >
                {addToCart.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Hazırlanıyor...
                  </>
                ) : (
                  "Hemen Al"
                )}
              </Button>
            </div>

            <div className="mt-3">
              <FavoriteButton product={data} variant="button" size="md" />
            </div>
          </div>

          <div className="grid gap-3 rounded-lg border bg-white p-4 text-sm">
            <div className="flex items-center gap-3">
              <Truck className="h-5 w-5 text-n11" />
              <span>1-3 iş günü içinde teslim</span>
            </div>
            <div className="flex items-center gap-3">
              <ShieldCheck className="h-5 w-5 text-n11" />
              <span>14 gün koşulsuz iade</span>
            </div>
          </div>
        </div>
      </div>

      {data.description && (
        <section className="mt-10">
          <h2 className="mb-3 text-lg font-semibold">Ürün Açıklaması</h2>
          <div className="rounded-lg border bg-white p-6 text-sm leading-relaxed">
            {data.description}
          </div>
        </section>
      )}

      <section className="mt-10">
        <h2 className="mb-3 text-lg font-semibold">
          Değerlendirmeler
          <span className="ml-2 text-sm font-normal text-muted-foreground">
            ({data.ratingCount})
          </span>
        </h2>
        <div className="rounded-lg border bg-white p-6">
          {data.ratingCount === 0 ? (
            <p className="text-sm text-muted-foreground">
              Bu ürün için henüz değerlendirme yapılmamış.
            </p>
          ) : (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-0.5">
                {Array.from({ length: 5 }, (_, i) => {
                  const filled = i < Math.round(Number(data.ratingAverage));
                  return (
                    <Star
                      key={i}
                      className={
                        filled
                          ? "h-5 w-5 fill-amber-400 text-amber-400"
                          : "h-5 w-5 fill-transparent text-amber-400"
                      }
                    />
                  );
                })}
              </div>
              <span className="text-sm">
                <span className="text-base font-semibold text-foreground">
                  {Number(data.ratingAverage).toFixed(1)}
                </span>
                <span className="text-muted-foreground"> / 5</span>
                <span className="ml-2 text-xs text-muted-foreground">
                  ({data.ratingCount} değerlendirme)
                </span>
              </span>
            </div>
          )}
        </div>
      </section>

      <RecommendedProducts
        categoryId={data.categoryId}
        excludeProductId={data.id}
      />
    </div>
  );
}
