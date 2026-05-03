import { useQuery } from "@tanstack/react-query";
import { Clock } from "lucide-react";
import { productApi } from "@/api/endpoints";
import { ProductCard } from "./ProductCard";
import { ProductCardSkeleton } from "./ProductCardSkeleton";
import { useRecentlyViewedStore } from "@/store/recentlyViewedStore";

export function RecentlyViewed() {
  const ids = useRecentlyViewedStore((s) => s.ids);
  const clear = useRecentlyViewedStore((s) => s.clear);

  const sortedKey = [...ids].sort().join(",");
  const { data, isLoading } = useQuery({
    queryKey: ["recently-viewed", sortedKey],
    queryFn: () => productApi.byIds(ids),
    enabled: ids.length > 0,
    staleTime: 60_000,
  });

  if (ids.length === 0) return null;

  // Backend'den gelen ürünleri original sıraya göre düzenle (en son görülen başta)
  const productMap = new Map((data ?? []).map((p) => [p.id, p]));
  const ordered = ids.map((id) => productMap.get(id)).filter(Boolean);

  return (
    <section className="mb-10">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-lg font-semibold">
          <Clock className="h-5 w-5 text-n11" />
          Son Baktıkların
        </h2>
        <button
          onClick={clear}
          className="text-xs text-muted-foreground hover:text-n11 hover:underline"
        >
          Temizle
        </button>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
        {isLoading
          ? Array.from({ length: Math.min(ids.length, 5) }).map((_, i) => (
              <ProductCardSkeleton key={i} />
            ))
          : ordered.map(
              (p) => p && <ProductCard key={p.id} product={p} />
            )}
      </div>
    </section>
  );
}
