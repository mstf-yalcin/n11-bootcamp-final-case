import { useState } from "react";
import { useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { ExternalLink, Pencil, Plus, Search } from "lucide-react";
import { adminProductApi, adminStockApi, productApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { FloatingInput } from "@/components/ui/floating-input";
import { DataTable, type Column, type SortState } from "@/components/DataTable";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { formatDate } from "@/lib/utils";
import { usePageTitle } from "@/hooks/usePageTitle";
import type { Product, StockResponse } from "@/types/api";

type StockRow = StockResponse & { product?: Product };

const FALLBACK_IMG = "https://placehold.co/64x64/fff0fe/ff25f5?text=n11";
const PAGE_SIZE = 20;

function parseSort(raw: string | null): SortState | null {
  if (!raw) return null;
  const [key, direction] = raw.split(",");
  if (!key) return null;
  return { key, direction: direction === "desc" ? "desc" : "asc" };
}

export default function AdminStocksPage() {
  usePageTitle("Admin · Stoklar");
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<StockRow | null>(null);
  const [creating, setCreating] = useState(false);

  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") ?? "0");
  const search = params.get("search") ?? "";
  const sort = parseSort(params.get("sort"));
  const sortParam = sort ? `${sort.key},${sort.direction}` : undefined;
  const [searchLocal, setSearchLocal] = useState(search);

  const setPage = (p: number) => {
    const next = new URLSearchParams(params);
    next.set("page", String(p));
    setParams(next, { replace: true });
  };

  const onSort = (key: string) => {
    const next = new URLSearchParams(params);
    let direction: "asc" | "desc" = "asc";
    if (sort?.key === key) {
      direction = sort.direction === "asc" ? "desc" : "asc";
    }
    next.set("sort", `${key},${direction}`);
    next.delete("page");
    setParams(next, { replace: true });
  };

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const next = new URLSearchParams(params);
    const trimmed = searchLocal.trim();
    if (trimmed) next.set("search", trimmed);
    else next.delete("search");
    next.delete("page"); // arama yapılınca sayfa sıfırla
    setParams(next, { replace: true });
  };

  const productsForSearchQuery = useQuery({
    queryKey: ["products", "for-stock-search", search, page],
    queryFn: () => productApi.list({ search, page, size: PAGE_SIZE }),
    enabled: Boolean(search),
  });

  const filteredProductIds = search
    ? productsForSearchQuery.data?.items.map((p) => p.id)
    : undefined;

  const stocksQuery = useQuery({
    queryKey: [
      "admin",
      "stocks",
      page,
      search,
      filteredProductIds?.join(",") ?? "",
      sortParam ?? "",
    ],
    queryFn: () =>
      adminStockApi.list({
        page: search ? 0 : page,
        size: PAGE_SIZE,
        productIds: filteredProductIds,
        sort: search ? undefined : sortParam,
      }),
    enabled:
      !search ||
      (productsForSearchQuery.isSuccess &&
        Boolean(filteredProductIds?.length)),
  });

  const productIds = stocksQuery.data?.items.map((s) => s.productId) ?? [];
  const productsQuery = useQuery({
    queryKey: ["products", "batch", productIds.sort().join(",")],
    queryFn: () => productApi.byIds(productIds),
    enabled: productIds.length > 0,
  });

  // Tüm ürünleri çek (stoksuzları "Yeni Stok" picker'ında göstermek için)
  const allProductsQuery = useQuery({
    queryKey: ["admin", "products", { all: true }],
    queryFn: () => adminProductApi.list({ size: 200 }),
    enabled: creating,
  });

  const stockedIdsQuery = useQuery({
    queryKey: ["admin", "stocks", "product-ids"],
    queryFn: adminStockApi.stockedProductIds,
    enabled: creating,
  });

  const productMap = new Map(
    (productsQuery.data ?? []).map((p) => [p.id, p])
  );

  const searchYieldedZero =
    Boolean(search) &&
    productsForSearchQuery.isSuccess &&
    !filteredProductIds?.length;

  const searchMatchButNoStock =
    Boolean(search) &&
    productsForSearchQuery.isSuccess &&
    Boolean(filteredProductIds?.length) &&
    stocksQuery.isSuccess &&
    (stocksQuery.data?.items.length ?? 0) === 0;

  const rows: StockRow[] | undefined = searchYieldedZero
    ? []
    : stocksQuery.data?.items.map((s) => ({
        ...s,
        product: productMap.get(s.productId),
      }));

  const pageInfo = search
    ? productsForSearchQuery.data?.page
    : stocksQuery.data?.page;

  const stockedProductIds = new Set(stockedIdsQuery.data ?? []);
  const productsWithoutStock = (allProductsQuery.data?.items ?? []).filter(
    (p) => p.isActive !== false && !stockedProductIds.has(p.id)
  );

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["admin", "stocks"] });
    queryClient.invalidateQueries({ queryKey: ["stock"] });
  };

  const updateMutation = useMutation({
    mutationFn: (vars: { productId: string; quantity: number }) =>
      adminStockApi.update(vars.productId, { quantity: vars.quantity }),
    onSuccess: () => {
      toast.success("Stok güncellendi");
      invalidate();
      setEditing(null);
    },
    onError: (err) => notifyApiError(err, "Güncelleme başarısız"),
  });

  const createMutation = useMutation({
    mutationFn: (vars: { productId: string; quantity: number }) =>
      adminStockApi.create({ productId: vars.productId, quantity: vars.quantity }),
    onSuccess: () => {
      toast.success("Stok kaydı oluşturuldu");
      invalidate();
      setCreating(false);
    },
    onError: (err) => notifyApiError(err, "Stok eklenemedi"),
  });

  const columns: Column<StockRow>[] = [
    {
      key: "product",
      header: "Ürün",
      cell: (s) => (
        <div className="flex items-center gap-3">
          <img
            src={s.product?.imageUrl || FALLBACK_IMG}
            alt={s.product?.name ?? ""}
            onError={(e) => {
              (e.target as HTMLImageElement).src = FALLBACK_IMG;
            }}
            className="h-10 w-10 flex-shrink-0 rounded-md border bg-secondary/40 object-cover"
          />
          <div className="min-w-0">
            <div className="flex items-center gap-1.5">
              <span className="font-medium">
                {s.product?.name ?? "—"}
              </span>
              {s.product?.slug && (
                <Link
                  to={`/products/${s.product.slug}`}
                  target="_blank"
                  className="text-muted-foreground hover:text-n11"
                  aria-label="Ürün sayfasında aç"
                  onClick={(e) => e.stopPropagation()}
                >
                  <ExternalLink className="h-3 w-3" />
                </Link>
              )}
            </div>
            <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
              {s.product?.categoryName && (
                <span className="rounded-full bg-secondary px-2 py-0.5">
                  {s.product.categoryName}
                </span>
              )}
              <code
                className="cursor-pointer font-mono text-[10px] hover:text-foreground"
                title="UUID'yi kopyalamak için tıkla"
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  navigator.clipboard.writeText(s.productId);
                  toast.success("ID kopyalandı");
                }}
              >
                {s.productId}
              </code>
            </div>
          </div>
        </div>
      ),
    },
    {
      key: "quantity",
      header: "Toplam Stok",
      sortKey: "quantity",
      cell: (s) => <span className="font-semibold">{s.quantity}</span>,
    },
    {
      key: "reserved",
      header: "Rezerv",
      sortKey: "reserved",
      cell: (s) => <span className="text-muted-foreground">{s.reserved}</span>,
    },
    {
      key: "available",
      header: "Mevcut",
      cell: (s) => (
        <Badge
          variant={
            s.available === 0
              ? "destructive"
              : s.available <= 5
                ? "warning"
                : "success"
          }
        >
          {s.available}
        </Badge>
      ),
    },
    {
      key: "updatedAt",
      header: "Güncelleme",
      sortKey: "updatedAt",
      cell: (s) => (
        <span className="text-xs text-muted-foreground">
          {formatDate(s.updatedAt)}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "60px",
      className: "text-right",
      cell: (s) => (
        <button
          onClick={() => setEditing(s)}
          className="rounded p-1.5 text-muted-foreground hover:bg-accent hover:text-n11"
          aria-label="Düzenle"
        >
          <Pencil className="h-4 w-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="p-8">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Stoklar</h1>
          <p className="text-sm text-muted-foreground">
            {pageInfo
              ? search
                ? `"${search}" için ${pageInfo.totalElements} ürün`
                : `${pageInfo.totalElements} ürünün stok kaydı`
              : "Yükleniyor..."}
          </p>
        </div>
        <Button onClick={() => setCreating(true)}>
          <Plus className="mr-1 h-4 w-4" />
          Yeni Stok
        </Button>
      </div>

      <form onSubmit={onSearch} className="mb-4 flex gap-2">
        <div className="relative max-w-md flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchLocal}
            onChange={(e) => setSearchLocal(e.target.value)}
            placeholder="Ürün adı, slug, açıklama veya ID ile ara..."
            className="pl-10"
          />
        </div>
        <Button type="submit" variant="outline">
          Ara
        </Button>
        {search && (
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              setSearchLocal("");
              const next = new URLSearchParams(params);
              next.delete("search");
              next.delete("page");
              setParams(next, { replace: true });
            }}
          >
            Temizle
          </Button>
        )}
      </form>

      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(s) => s.id}
        isLoading={stocksQuery.isLoading || productsForSearchQuery.isLoading}
        isError={stocksQuery.isError || productsForSearchQuery.isError}
        emptyMessage={
          !search
            ? "Henüz stok kaydı yok."
            : searchMatchButNoStock
              ? `"${search}" ile eşleşen ürünlerin stok kaydı henüz yok. + Yeni Stok ile ekleyebilirsin.`
              : `"${search}" için ürün bulunamadı.`
        }
        errorMessage="Stoklar yüklenemedi. Backend'e ulaşılamıyor olabilir."
        sort={search ? null : sort}
        onSort={search ? undefined : onSort}
      />

      {pageInfo && pageInfo.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={!pageInfo.hasPrevious}
            onClick={() => setPage(page - 1)}
          >
            Önceki
          </Button>
          <span className="text-sm text-muted-foreground">
            Sayfa {pageInfo.pageNumber + 1} / {pageInfo.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={!pageInfo.hasNext}
            onClick={() => setPage(page + 1)}
          >
            Sonraki
          </Button>
        </div>
      )}

      <StockEditDialog
        stock={editing}
        onClose={() => setEditing(null)}
        onSubmit={(quantity) =>
          editing &&
          updateMutation.mutate({ productId: editing.productId, quantity })
        }
        isPending={updateMutation.isPending}
      />

      <StockCreateDialog
        open={creating}
        onClose={() => setCreating(false)}
        availableProducts={productsWithoutStock}
        loading={allProductsQuery.isLoading}
        onSubmit={(productId, quantity) =>
          createMutation.mutate({ productId, quantity })
        }
        isPending={createMutation.isPending}
      />
    </div>
  );
}

function StockEditDialog({
  stock,
  onClose,
  onSubmit,
  isPending,
}: {
  stock: StockRow | null;
  onClose: () => void;
  onSubmit: (quantity: number) => void;
  isPending: boolean;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<{ quantity: number }>({ defaultValues: { quantity: 0 } });

  return (
    <Dialog
      open={Boolean(stock)}
      onOpenChange={(o) => {
        if (o && stock) reset({ quantity: stock.quantity });
        if (!o) onClose();
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            Stok Güncelle —{" "}
            <span className="text-n11">{stock?.product?.name ?? "—"}</span>
          </DialogTitle>
        </DialogHeader>
        {stock && (
          <div className="mb-3 grid grid-cols-3 gap-2 rounded-md bg-secondary/40 p-3 text-center text-xs">
            <div>
              <div className="font-bold">{stock.quantity}</div>
              <div className="text-muted-foreground">Mevcut Toplam</div>
            </div>
            <div>
              <div className="font-bold">{stock.reserved}</div>
              <div className="text-muted-foreground">Rezerv</div>
            </div>
            <div>
              <div className="font-bold">{stock.available}</div>
              <div className="text-muted-foreground">Boş</div>
            </div>
          </div>
        )}
        <form
          onSubmit={handleSubmit((data) =>
            onSubmit(Number(data.quantity))
          )}
          className="space-y-3"
        >
          <FloatingInput
            id="qty"
            type="number"
            label="Yeni Toplam Adet"
            error={errors.quantity?.message}
            {...register("quantity", {
              required: "Adet zorunludur",
              valueAsNumber: true,
              min: { value: 0, message: "Negatif olamaz" },
            })}
          />
          <p className="text-xs text-muted-foreground">
            Yeni adet, mevcut rezervlerden ({stock?.reserved ?? 0}) düşük olamaz.
          </p>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose}>
              İptal
            </Button>
            <Button type="submit" disabled={isPending}>
              Güncelle
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function StockCreateDialog({
  open,
  onClose,
  availableProducts,
  loading,
  onSubmit,
  isPending,
}: {
  open: boolean;
  onClose: () => void;
  availableProducts: Product[];
  loading: boolean;
  onSubmit: (productId: string, quantity: number) => void;
  isPending: boolean;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<{ productId: string; quantity: number }>({
    defaultValues: { productId: "", quantity: 0 },
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        if (o) reset({ productId: "", quantity: 0 });
        if (!o) onClose();
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Yeni Stok Kaydı</DialogTitle>
        </DialogHeader>

        {loading ? (
          <div className="py-6 text-center text-sm text-muted-foreground">
            Ürünler yükleniyor...
          </div>
        ) : availableProducts.length === 0 ? (
          <div className="rounded-md border border-dashed bg-secondary/30 p-6 text-center text-sm text-muted-foreground">
            Stok kaydı oluşturulabilecek ürün yok. <br />
            Tüm aktif ürünlerin stok kaydı zaten mevcut.
          </div>
        ) : (
          <form
            onSubmit={handleSubmit((data) =>
              onSubmit(data.productId, Number(data.quantity))
            )}
            className="space-y-3"
          >
            <div>
              <label className="mb-1 block text-xs font-medium uppercase text-muted-foreground">
                Ürün
              </label>
              <select
                {...register("productId", {
                  required: "Ürün seç",
                })}
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
              >
                <option value="">Stok kaydı olmayan ürünü seç...</option>
                {availableProducts.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
              {errors.productId && (
                <p className="mt-1 text-xs text-destructive">
                  {errors.productId.message}
                </p>
              )}
              <p className="mt-1 text-[11px] text-muted-foreground">
                {availableProducts.length} ürün stok kaydı bekliyor
              </p>
            </div>

            <FloatingInput
              id="create-qty"
              type="number"
              label="Başlangıç Adedi"
              error={errors.quantity?.message}
              {...register("quantity", {
                required: "Adet zorunludur",
                valueAsNumber: true,
                min: { value: 0, message: "Negatif olamaz" },
              })}
            />

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={onClose}>
                İptal
              </Button>
              <Button type="submit" disabled={isPending}>
                Stok Oluştur
              </Button>
            </div>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
