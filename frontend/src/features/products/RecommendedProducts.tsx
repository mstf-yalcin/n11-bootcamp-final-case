import { useQuery } from "@tanstack/react-query";
import { productApi } from "@/api/endpoints";
import { ProductCard } from "./ProductCard";
import { ProductCardSkeleton } from "./ProductCardSkeleton";

export function RecommendedProducts({
  categoryId,
  excludeProductId,
}: {
  categoryId: string;
  excludeProductId: string;
}) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["recommended", categoryId],
    queryFn: () =>
      productApi.list({ categoryId, size: 10, sort: "ratingAverage,desc" }),
    enabled: Boolean(categoryId),
  });

  if (isError) return null;

  const items =
    data?.items.filter((p) => p.id !== excludeProductId).slice(0, 5) ?? [];

  if (!isLoading && items.length === 0) return null;

  return (
    <section className="mt-10">
      <div className="mb-3 flex items-baseline justify-between">
        <h2 className="text-lg font-semibold">Bunlara da bakanlar</h2>
        <span className="text-xs text-muted-foreground">
          Aynı kategoride çok beğenilenler
        </span>
      </div>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {isLoading
          ? Array.from({ length: 5 }).map((_, i) => (
              <ProductCardSkeleton key={i} />
            ))
          : items.map((p) => <ProductCard key={p.id} product={p} />)}
      </div>
    </section>
  );
}
