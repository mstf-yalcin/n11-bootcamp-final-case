import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { useState, useEffect, useRef } from "react";
import { categoryApi, productApi } from "@/api/endpoints";
import { ProductCard } from "@/features/products/ProductCard";
import { ProductGridSkeleton } from "@/features/products/ProductCardSkeleton";
import { ApiErrorBox } from "@/components/ApiErrorBox";
import { usePageTitle } from "@/hooks/usePageTitle";
import { Spinner } from "@/components/ui/spinner";
import { SearchX, Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";

const SORT_OPTIONS = [
  { label: "Akıllı sıralama", value: "" },
  { label: "Fiyat — düşükten yükseğe", value: "price,asc" },
  { label: "Fiyat — yüksekten düşüğe", value: "price,desc" },
  { label: "En yeni", value: "createdAt,desc" },
  { label: "Puana göre", value: "ratingAverage,desc" },
];

const PAGE_SIZE = 20;

export default function ProductListPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get("q") ?? "";
  const categorySlug = params.get("category") ?? "";
  const minPrice = params.get("minPrice") ?? "";
  const maxPrice = params.get("maxPrice") ?? "";
  const minRating = params.get("minRating") ?? "";
  const sort = params.get("sort") ?? "";

  const [minLocal, setMinLocal] = useState(minPrice);
  const [maxLocal, setMaxLocal] = useState(maxPrice);
  useEffect(() => setMinLocal(minPrice), [minPrice]);
  useEffect(() => setMaxLocal(maxPrice), [maxPrice]);

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
    staleTime: 5 * 60 * 1000,
  });

  const activeCategory = categoriesQuery.data?.find(
    (c) => c.slug === categorySlug
  );
  // Slug-based URL → categoryId via cached category list. Slug bilinmiyorsa
  // (kategoriler hâlâ yükleniyorsa) categorySlug var, activeCategory yok →
  // query enabled=false ile bekletilir; cache hit'te tek frame gecikme.
  const resolvedCategoryId = activeCategory?.id;
  const categoryFilterReady = !categorySlug || Boolean(resolvedCategoryId);

  const titleParts: string[] = [];
  if (q) titleParts.push(`"${q}" araması`);
  if (activeCategory) titleParts.push(activeCategory.name);
  usePageTitle(titleParts.length ? titleParts.join(" - ") : "Tüm Ürünler");

  const productsQuery = useInfiniteQuery({
    queryKey: ["products", { q, categoryId: resolvedCategoryId ?? "", minPrice, maxPrice, minRating, sort }],
    queryFn: ({ pageParam }) =>
      productApi.list({
        search: q || undefined,
        categoryId: resolvedCategoryId || undefined,
        minPrice: minPrice ? Number(minPrice) : undefined,
        maxPrice: maxPrice ? Number(maxPrice) : undefined,
        minRating: minRating ? Number(minRating) : undefined,
        sort: sort || undefined,
        page: pageParam,
        size: PAGE_SIZE,
      }),
    initialPageParam: 0,
    enabled: categoryFilterReady,
    getNextPageParam: (lastPage) =>
      lastPage.page?.hasNext ? lastPage.page.pageNumber + 1 : undefined,
  });

  const allItems =
    productsQuery.data?.pages.flatMap((p) => p.items) ?? [];
  const totalElements =
    productsQuery.data?.pages[0]?.page?.totalElements ?? 0;

  // Intersection observer for infinite scroll trigger
  const sentinelRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (
          entry?.isIntersecting &&
          productsQuery.hasNextPage &&
          !productsQuery.isFetchingNextPage
        ) {
          productsQuery.fetchNextPage();
        }
      },
      { rootMargin: "300px" }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [productsQuery.hasNextPage, productsQuery.isFetchingNextPage, productsQuery]);

  const setParam = (key: string, value: string | null) => {
    const next = new URLSearchParams(params);
    if (!value) next.delete(key);
    else next.set(key, value);
    setParams(next, { replace: true });
  };

  const applyPriceFilter = (e: React.FormEvent) => {
    e.preventDefault();
    const next = new URLSearchParams(params);
    if (minLocal) next.set("minPrice", minLocal);
    else next.delete("minPrice");
    if (maxLocal) next.set("maxPrice", maxLocal);
    else next.delete("maxPrice");
    setParams(next, { replace: true });
  };

  const clearAllFilters = () => {
    setParams(new URLSearchParams(), { replace: true });
    setMinLocal("");
    setMaxLocal("");
  };

  return (
    <div className="container py-6">
      <div className="mb-4 flex items-baseline justify-between">
        <div>
          <h1 className="text-2xl font-semibold">
            {q ? `"${q}" için sonuçlar` : "Tüm Ürünler"}
          </h1>
          <p className="text-sm text-muted-foreground">
            {productsQuery.isLoading
              ? "Yükleniyor..."
              : `${totalElements} ürün bulundu — ${allItems.length} gösteriliyor`}
          </p>
        </div>
        <select
          value={sort}
          onChange={(e) => setParam("sort", e.target.value)}
          className="h-10 rounded-md border border-input bg-background px-3 text-sm"
        >
          {SORT_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <div className="grid gap-6 lg:grid-cols-[260px_1fr]">
        <aside className="space-y-6 rounded-lg border bg-white p-4 lg:sticky lg:top-20 lg:h-fit">
          <div>
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-sm font-semibold">Filtreler</h3>
              <button
                onClick={clearAllFilters}
                className="text-xs text-n11 hover:underline"
              >
                Temizle
              </button>
            </div>
            <Separator />
          </div>

          <div>
            <h4 className="mb-2 text-sm font-semibold">Kategori</h4>
            <div className="max-h-52 space-y-1.5 overflow-y-auto pr-1">
              <label className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1 hover:bg-accent">
                <input
                  type="radio"
                  name="cat"
                  checked={!categorySlug}
                  onChange={() => setParam("category", null)}
                />
                <span className="text-sm">Tümü</span>
              </label>
              {categoriesQuery.data?.map((cat) => (
                <label
                  key={cat.id}
                  className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1 hover:bg-accent"
                >
                  <input
                    type="radio"
                    name="cat"
                    checked={categorySlug === cat.slug}
                    onChange={() => setParam("category", cat.slug)}
                  />
                  {cat.imageUrl && (
                    <img
                      src={cat.imageUrl}
                      alt=""
                      loading="lazy"
                      className="h-5 w-5 rounded object-cover"
                    />
                  )}
                  <span className="text-sm">{cat.name}</span>
                </label>
              ))}
            </div>
          </div>

          <Separator />

          <form onSubmit={applyPriceFilter}>
            <h4 className="mb-2 text-sm font-semibold">Fiyat aralığı</h4>
            <div className="space-y-2">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <Label htmlFor="minPrice" className="text-xs">
                    Min
                  </Label>
                  <Input
                    id="minPrice"
                    type="number"
                    inputMode="decimal"
                    placeholder="0"
                    value={minLocal}
                    onChange={(e) => setMinLocal(e.target.value)}
                  />
                </div>
                <div>
                  <Label htmlFor="maxPrice" className="text-xs">
                    Max
                  </Label>
                  <Input
                    id="maxPrice"
                    type="number"
                    inputMode="decimal"
                    placeholder="—"
                    value={maxLocal}
                    onChange={(e) => setMaxLocal(e.target.value)}
                  />
                </div>
              </div>
              <Button
                type="submit"
                variant="outline"
                size="sm"
                className="w-full"
              >
                Uygula
              </Button>
            </div>
          </form>

          <Separator />

          <div>
            <h4 className="mb-2 text-sm font-semibold">Puan</h4>
            <div className="space-y-1">
              <label className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1 hover:bg-accent">
                <input
                  type="radio"
                  name="rating"
                  checked={!minRating}
                  onChange={() => setParam("minRating", null)}
                />
                <span className="text-sm">Tümü</span>
              </label>
              {[4, 3, 2, 1].map((r) => (
                <label
                  key={r}
                  className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1 hover:bg-accent"
                >
                  <input
                    type="radio"
                    name="rating"
                    checked={minRating === String(r)}
                    onChange={() => setParam("minRating", String(r))}
                  />
                  <span className="flex items-center gap-0.5">
                    {Array.from({ length: 5 }, (_, i) => (
                      <Star
                        key={i}
                        className={
                          i < r
                            ? "h-4 w-4 fill-amber-400 text-amber-400"
                            : "h-4 w-4 fill-transparent text-amber-400"
                        }
                      />
                    ))}
                    <span className="ml-1 text-sm text-muted-foreground">
                      ve üzeri
                    </span>
                  </span>
                </label>
              ))}
            </div>
          </div>
        </aside>

        <div>
          {productsQuery.isLoading && <ProductGridSkeleton count={15} />}
          {productsQuery.isError && (
            <ApiErrorBox
              error={productsQuery.error}
              onRetry={productsQuery.refetch}
              title="Ürünler yüklenemedi"
            />
          )}

          {productsQuery.data && allItems.length === 0 && (
            <div className="rounded-lg border bg-white p-10 text-center">
              <SearchX className="mx-auto mb-3 h-10 w-10 text-muted-foreground" />
              <p className="mb-1 font-medium">Sonuç bulunamadı</p>
              <p className="text-sm text-muted-foreground">
                Aradığın kriterlere uygun ürün yok. Filtreleri değiştirmeyi
                veya farklı bir arama denemeyi dene.
              </p>
              <Button
                variant="outline"
                size="sm"
                className="mt-4"
                onClick={clearAllFilters}
              >
                Filtreleri Temizle
              </Button>
            </div>
          )}

          {allItems.length > 0 && (
            <>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
                {allItems.map((p) => (
                  <ProductCard key={p.id} product={p} />
                ))}
              </div>

              <div ref={sentinelRef} className="h-1" />

              {productsQuery.isFetchingNextPage && (
                <div className="mt-6 flex items-center justify-center gap-2 text-sm text-muted-foreground">
                  <Spinner size={18} />
                  <span>Daha fazla ürün yükleniyor...</span>
                </div>
              )}

              {!productsQuery.hasNextPage && allItems.length >= PAGE_SIZE && (
                <div className="mt-6 text-center text-xs text-muted-foreground">
                  Tüm ürünleri gördün ✓
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
