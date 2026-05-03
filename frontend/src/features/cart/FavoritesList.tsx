import { Link } from "react-router-dom";
import { Heart, ShoppingCart, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useFavoritesStore } from "@/store/favoritesStore";
import { useAddToCart, useAddManyToCart } from "@/features/cart/queries";
import { formatTRY } from "@/lib/utils";

const FALLBACK_IMG = "https://placehold.co/200x200/fff3eb/ff6000?text=n11";

export function FavoritesList({
  showHeader = true,
  topMargin = true,
}: {
  showHeader?: boolean;
  topMargin?: boolean;
} = {}) {
  const items = useFavoritesStore((s) => s.items);
  const remove = useFavoritesStore((s) => s.remove);
  const clear = useFavoritesStore((s) => s.clear);
  const addToCart = useAddToCart();
  const addManyToCart = useAddManyToCart();

  if (items.length === 0) return null;

  const handleAddToCart = (productId: string) => {
    // queries.ts seviyesinde başarılı eklemede otomatik favoriden çıkıyor
    addToCart.mutate({ productId, quantity: 1 });
  };

  const handleAddAllToCart = () => {
    const batch = items.map((i) => ({ productId: i.productId, quantity: 1 }));
    // queries.ts seviyesinde başarılı eklenenler otomatik favoriden çıkıyor
    addManyToCart.mutate(batch);
  };

  return (
    <section className={topMargin ? "mt-10" : undefined}>
      {(showHeader || items.length > 1) && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          {showHeader ? (
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              <Heart className="h-5 w-5 text-n11" fill="currentColor" />
              Favoriler ({items.length})
            </h2>
          ) : (
            <span />
          )}
          <div className="flex items-center gap-2">
            {items.length > 1 && (
              <Button
                size="sm"
                onClick={handleAddAllToCart}
                disabled={addManyToCart.isPending}
                className="h-8 gap-1 px-3 text-xs"
              >
                <ShoppingCart className="h-3.5 w-3.5" />
                {addManyToCart.isPending
                  ? "Ekleniyor..."
                  : "Tümünü Sepete Ekle"}
              </Button>
            )}
            {items.length > 1 && (
              <button
                onClick={() => {
                  if (confirm("Tüm favoriler temizlensin mi?")) {
                    clear();
                    toast.success("Favoriler temizlendi");
                  }
                }}
                className="text-xs text-muted-foreground hover:text-destructive"
              >
                Tümünü Temizle
              </button>
            )}
          </div>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((fav) => {
          const isPending =
            addToCart.isPending &&
            addToCart.variables?.productId === fav.productId;
          return (
            <div
              key={fav.productId}
              className="group relative flex gap-3 rounded-lg border bg-white p-3 transition-shadow hover:shadow-sm"
            >
              <button
                onClick={() => remove(fav.productId)}
                aria-label="Favorilerden çıkar"
                className="absolute right-2 top-2 flex h-6 w-6 items-center justify-center rounded-full text-muted-foreground opacity-0 transition-opacity hover:bg-destructive/10 hover:text-destructive group-hover:opacity-100"
              >
                <X className="h-3.5 w-3.5" />
              </button>

              <Link
                to={`/products/${fav.slug}`}
                className="flex-shrink-0"
                aria-label={`${fav.name} detayı`}
              >
                <img
                  src={fav.imageUrl || FALLBACK_IMG}
                  alt={fav.name}
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = FALLBACK_IMG;
                  }}
                  className="h-20 w-20 rounded-md object-cover transition-opacity hover:opacity-85"
                />
              </Link>

              <div className="flex min-w-0 flex-1 flex-col justify-between">
                <Link
                  to={`/products/${fav.slug}`}
                  className="line-clamp-2 pr-6 text-sm font-medium leading-tight hover:text-n11 hover:underline"
                  title={fav.name}
                >
                  {fav.name}
                </Link>
                <div className="mt-2 flex items-center justify-between gap-2">
                  <span className="font-bold text-foreground">
                    {formatTRY(fav.price, fav.currency)}
                  </span>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={isPending}
                    onClick={() => handleAddToCart(fav.productId)}
                    className="h-7 gap-1 px-2 text-xs"
                  >
                    <ShoppingCart className="h-3 w-3" />
                    {isPending ? "Ekleniyor..." : "Sepete Ekle"}
                  </Button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
