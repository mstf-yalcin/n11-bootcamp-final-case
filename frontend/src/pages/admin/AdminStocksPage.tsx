import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { ExternalLink, Pencil, Plus, Search } from "lucide-react";
import { adminProductApi, adminStockApi, productApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { FloatingInput } from "@/components/ui/floating-input";
import { DataTable, type Column } from "@/components/DataTable";
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

export default function AdminStocksPage() {
  usePageTitle("Admin · Stoklar");
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<StockRow | null>(null);
  const [creating, setCreating] = useState(false);
  const [search, setSearch] = useState("");

  const stocksQuery = useQuery({
    queryKey: ["admin", "stocks"],
    queryFn: adminStockApi.list,
  });

  const productIds = stocksQuery.data?.map((s) => s.productId) ?? [];
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

  const productMap = new Map(
    (productsQuery.data ?? []).map((p) => [p.id, p])
  );
  const allRows: StockRow[] | undefined = stocksQuery.data?.map((s) => ({
    ...s,
    product: productMap.get(s.productId),
  }));

  const rows = useMemo(() => {
    if (!allRows) return undefined;
    const q = search.trim().toLowerCase();
    if (!q) return allRows;
    return allRows.filter((r) => {
      const name = r.product?.name?.toLowerCase() ?? "";
      const slug = r.product?.slug?.toLowerCase() ?? "";
      const category = r.product?.categoryName?.toLowerCase() ?? "";
      return (
        name.includes(q) ||
        slug.includes(q) ||
        category.includes(q)
      );
    });
  }, [allRows, search]);

  const stockedProductIds = new Set(stocksQuery.data?.map((s) => s.productId));
  const productsWithoutStock = (allProductsQuery.data?.items ?? []).filter(
    (p) => !stockedProductIds.has(p.id)
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
              <span className="font-mono">
                {s.productId.slice(0, 8)}
              </span>
            </div>
          </div>
        </div>
      ),
    },
    {
      key: "quantity",
      header: "Toplam Stok",
      cell: (s) => <span className="font-semibold">{s.quantity}</span>,
    },
    {
      key: "reserved",
      header: "Rezerv",
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
            {allRows
              ? search
                ? `${rows?.length ?? 0} / ${allRows.length} ürün`
                : `${allRows.length} ürün`
              : "Yükleniyor..."}
          </p>
        </div>
        <Button onClick={() => setCreating(true)}>
          <Plus className="mr-1 h-4 w-4" />
          Yeni Stok
        </Button>
      </div>

      <div className="mb-4 max-w-md">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Ürün adı, slug veya kategori ara..."
            className="pl-10"
          />
        </div>
      </div>

      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(s) => s.id}
        isLoading={stocksQuery.isLoading}
        isError={stocksQuery.isError}
        emptyMessage={
          search
            ? `"${search}" için stok bulunamadı.`
            : "Henüz stok kaydı yok."
        }
        errorMessage="Stoklar yüklenemedi. Backend'e ulaşılamıyor olabilir."
      />

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
