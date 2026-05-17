import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { BadgeCheck, TrendingUp } from "lucide-react";
import { productApi } from "@/api/endpoints";
import { ProductCard } from "@/features/products/ProductCard";
import {
  ProductCardSkeleton,
  ProductGridSkeleton,
} from "@/features/products/ProductCardSkeleton";
import { RecentlyViewed } from "@/features/products/RecentlyViewed";
import { ApiErrorBox } from "@/components/ApiErrorBox";
import { BannerCarousel } from "@/components/BannerCarousel";
import { usePageTitle } from "@/hooks/usePageTitle";

export default function HomePage() {
  usePageTitle(null);
  const productsQuery = useQuery({
    queryKey: ["products", { home: true }],
    queryFn: () => productApi.list({ size: 12 }),
  });
  const topRatedQuery = useQuery({
    queryKey: ["products", { home: "top-rated" }],
    queryFn: () => productApi.list({ size: 8, sort: "ratingAverage,desc", minRating: 4.5 }),
    staleTime: 5 * 60 * 1000,
  });

  const hasError = productsQuery.isError || topRatedQuery.isError;

  return (
    <div className="container py-6">
      <div className="mb-8">
        <BannerCarousel />
      </div>

      {hasError && (
        <div className="mb-8">
          <ApiErrorBox
            error={productsQuery.error ?? topRatedQuery.error}
            onRetry={() => {
              productsQuery.refetch();
              topRatedQuery.refetch();
            }}
            title="İçerik yüklenemedi"
          />
        </div>
      )}

      {!hasError &&
        (topRatedQuery.isLoading ||
          (topRatedQuery.data && topRatedQuery.data.items.length > 0)) && (
          <section className="mb-10">
            <div className="mb-4 flex items-baseline justify-between">
              <div className="flex items-center gap-2">
                <BadgeCheck className="h-5 w-5 text-amber-500" />
                <h2 className="text-lg font-semibold">Çok Beğenilenler</h2>
                <span className="text-xs text-muted-foreground">
                  4.5+ puanlı ürünler
                </span>
              </div>
              <Link
                to="/products?minRating=4&sort=ratingAverage,desc"
                className="text-sm text-n11 hover:underline"
              >
                Hepsini gör
              </Link>
            </div>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
              {topRatedQuery.isLoading
                ? Array.from({ length: 8 }).map((_, i) => (
                    <ProductCardSkeleton key={i} />
                  ))
                : topRatedQuery.data!.items
                    .slice(0, 8)
                    .map((p) => <ProductCard key={p.id} product={p} />)}
            </div>
          </section>
        )}

      {!hasError && <RecentlyViewed />}

      {!hasError && (
        <section>
          <div className="mb-4 flex items-baseline justify-between">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-n11" />
              <h2 className="text-lg font-semibold">Öne çıkanlar</h2>
            </div>
            <Link to="/products" className="text-sm text-n11 hover:underline">
              Hepsini gör
            </Link>
          </div>

          {productsQuery.isLoading && <ProductGridSkeleton count={15} />}
          {productsQuery.data && productsQuery.data.items.length === 0 && (
            <div className="rounded-md border bg-white p-10 text-center text-sm text-muted-foreground">
              Henüz ürün eklenmemiş. Birazdan tekrar dene.
            </div>
          )}
          {productsQuery.data && productsQuery.data.items.length > 0 && (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
              {productsQuery.data.items.map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
