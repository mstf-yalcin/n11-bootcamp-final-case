import { Link } from "react-router-dom";
import { Star } from "lucide-react";
import { formatTRY } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { FavoriteButton } from "./FavoriteButton";
import type { Product } from "@/types/api";

const FALLBACK_IMG = "https://placehold.co/400x400/fff3eb/ff6000?text=n11";

export function ProductCard({ product }: { product: Product }) {
  const isOutOfStock = product.stockStatus === "OUT_OF_STOCK";
  const isLowStock = product.stockStatus === "LOW_STOCK";

  return (
    <Link
      to={`/products/${product.slug}`}
      className="group relative flex flex-col overflow-hidden rounded-lg border bg-white transition-shadow hover:shadow-md"
    >
      <div className="relative aspect-square overflow-hidden bg-secondary/40">
        <img
          src={product.imageUrl || FALLBACK_IMG}
          alt={product.name}
          loading="lazy"
          onError={(e) => {
            (e.target as HTMLImageElement).src = FALLBACK_IMG;
          }}
          className={
            "h-full w-full object-cover transition-transform duration-300 group-hover:scale-105 " +
            (isOutOfStock ? "opacity-50 grayscale" : "")
          }
        />
        {isOutOfStock && (
          <div className="absolute left-2 top-2">
            <Badge variant="destructive">Tükendi</Badge>
          </div>
        )}
        {isLowStock && product.availableQuantity != null && (
          <div className="absolute left-2 top-2">
            <Badge variant="warning">
              Son {product.availableQuantity} ürün
            </Badge>
          </div>
        )}
        <div className="absolute right-2 top-2">
          <FavoriteButton product={product} size="md" />
        </div>
      </div>
      <div className="flex flex-1 flex-col gap-2 p-3">
        <div className="text-xs text-muted-foreground">
          {product.categoryName}
        </div>
        <h3 className="line-clamp-2 min-h-[2.5rem] text-sm font-medium">
          {product.name}
        </h3>
        {product.tags && product.tags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {product.tags.slice(0, 2).map((t) => (
              <span
                key={t.id}
                className="rounded-full bg-n11/10 px-2 py-0.5 text-[10px] font-medium text-n11"
              >
                {t.name}
              </span>
            ))}
            {product.tags.length > 2 && (
              <span className="text-[10px] text-muted-foreground">
                +{product.tags.length - 2}
              </span>
            )}
          </div>
        )}
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
          <span>{Number(product.ratingAverage).toFixed(1)}</span>
          <span>({product.ratingCount})</span>
        </div>
        <div className="mt-auto text-lg font-bold text-foreground">
          {formatTRY(product.price, product.currency)}
        </div>
      </div>
    </Link>
  );
}
