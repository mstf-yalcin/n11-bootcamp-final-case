import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { ExternalLink, Pencil, Plus, RotateCcw, Search, Trash2 } from "lucide-react";
import { adminProductApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { DataTable, type Column, type SortState } from "@/components/DataTable";
import { useConfirm } from "@/components/ConfirmDialog";
import { AdminProductFormDialog } from "./AdminProductFormDialog";
import { formatTRY } from "@/lib/utils";
import { usePageTitle } from "@/hooks/usePageTitle";
import type { Product } from "@/types/api";

const PAGE_SIZE = 20;
const FALLBACK_IMG = "https://placehold.co/80x80/fff3eb/ff6000?text=n11";

function parseSort(raw: string | null): SortState | null {
  if (!raw) return null;
  const [key, direction] = raw.split(",");
  if (!key) return null;
  return { key, direction: direction === "desc" ? "desc" : "asc" };
}

export default function AdminProductsPage() {
  usePageTitle("Admin · Ürünler");
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") ?? "0");
  const search = params.get("search") ?? "";
  const sort = parseSort(params.get("sort"));
  const sortParam = sort ? `${sort.key},${sort.direction}` : undefined;
  const [searchLocal, setSearchLocal] = useState(search);

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

  const queryClient = useQueryClient();
  const { confirm, dialog: confirmDialog } = useConfirm();

  const [editing, setEditing] = useState<Product | null>(null);
  const [creating, setCreating] = useState(false);

  const productsQuery = useQuery({
    queryKey: ["admin", "products", { search, page, sort: sortParam }],
    queryFn: () =>
      adminProductApi.list({
        search: search || undefined,
        page,
        size: PAGE_SIZE,
        sort: sortParam,
      }),
  });

  const removeMutation = useMutation({
    mutationFn: (id: string) => adminProductApi.remove(id),
    onSuccess: () => {
      toast.success("Ürün silindi");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
    onError: (err) => notifyApiError(err, "Silme başarısız"),
  });

  const restoreMutation = useMutation({
    mutationFn: (id: string) => adminProductApi.restore(id),
    onSuccess: () => {
      toast.success("Ürün geri yüklendi");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
    onError: (err) => notifyApiError(err, "Geri yükleme başarısız"),
  });

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const next = new URLSearchParams(params);
    if (searchLocal.trim()) next.set("search", searchLocal.trim());
    else next.delete("search");
    next.delete("page");
    setParams(next, { replace: true });
  };

  const setPage = (p: number) => {
    const next = new URLSearchParams(params);
    next.set("page", String(p));
    setParams(next, { replace: true });
  };

  const columns: Column<Product>[] = [
    {
      key: "product",
      header: "Ürün",
      sortKey: "name",
      cell: (p) => (
        <div className="flex items-center gap-3">
          <Link
            to={`/products/${p.slug}`}
            target="_blank"
            rel="noopener noreferrer"
            className="block flex-shrink-0"
            aria-label={`${p.name} detayını yeni sekmede aç`}
            title="Detayda aç"
          >
            <img
              src={p.imageUrl || FALLBACK_IMG}
              alt={p.name}
              onError={(e) => {
                const img = e.currentTarget;
                if (img.src !== FALLBACK_IMG) img.src = FALLBACK_IMG;
              }}
              className="h-10 w-10 rounded-md border bg-secondary/40 object-cover transition-opacity hover:opacity-80"
            />
          </Link>
          <div className="min-w-0">
            <Link
              to={`/products/${p.slug}`}
              target="_blank"
              rel="noopener noreferrer"
              className="font-medium hover:text-n11 hover:underline"
              title="Detayda aç"
            >
              {p.name}
            </Link>
            <div className="mt-0.5 flex items-center gap-2 text-[11px] text-muted-foreground">
              <span className="rounded-full bg-secondary px-2 py-0.5">
                {p.categoryName}
              </span>
              <span className="font-mono">{p.slug}</span>
            </div>
            <code
              className="mt-0.5 block cursor-pointer font-mono text-[10px] text-muted-foreground hover:text-foreground"
              title="UUID'yi kopyalamak için tıkla"
              onClick={(e) => {
                e.stopPropagation();
                e.preventDefault();
                navigator.clipboard.writeText(p.id);
                toast.success("ID kopyalandı");
              }}
            >
              {p.id}
            </code>
          </div>
        </div>
      ),
    },
    {
      key: "price",
      header: "Fiyat",
      sortKey: "price",
      cell: (p) => (
        <span className="font-semibold">
          {formatTRY(p.price, p.currency)}
        </span>
      ),
    },
    {
      key: "tags",
      header: "Etiketler",
      cell: (p) => (
        <div className="flex flex-wrap gap-1">
          {p.tags?.slice(0, 3).map((t) => (
            <Badge key={t.id} variant="secondary" className="text-[10px]">
              {t.name}
            </Badge>
          ))}
        </div>
      ),
    },
    {
      key: "isActive",
      header: "Durum",
      sortKey: "isActive",
      cell: (p) =>
        p.isActive === false ? (
          <Badge variant="destructive">Pasif</Badge>
        ) : (
          <Badge variant="success">Aktif</Badge>
        ),
    },
    {
      key: "actions",
      header: "",
      width: "140px",
      className: "text-right",
      cell: (p) => (
        <div className="flex justify-end gap-1">
          <Link
            to={`/products/${p.slug}`}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded p-1.5 text-muted-foreground hover:bg-accent hover:text-n11"
            aria-label="Yeni sekmede aç"
            title="Yeni sekmede aç"
          >
            <ExternalLink className="h-4 w-4" />
          </Link>
          <button
            onClick={() => setEditing(p)}
            className="rounded p-1.5 text-muted-foreground hover:bg-accent hover:text-n11"
            aria-label="Düzenle"
          >
            <Pencil className="h-4 w-4" />
          </button>
          {p.isActive === false ? (
            <button
              onClick={async () => {
                const ok = await confirm({
                  title: "Ürünü geri yükle",
                  description: `"${p.name}" ürünü tekrar aktif edilecek.`,
                  confirmLabel: "Geri Yükle",
                });
                if (ok) restoreMutation.mutate(p.id);
              }}
              className="rounded p-1.5 text-muted-foreground hover:bg-accent hover:text-n11"
              aria-label="Geri yükle"
              title="Geri yükle"
            >
              <RotateCcw className="h-4 w-4" />
            </button>
          ) : (
            <button
              onClick={async () => {
                const ok = await confirm({
                  title: "Ürünü sil",
                  description: `"${p.name}" ürününü silmek istediğine emin misin? Soft delete uygulanır.`,
                  destructive: true,
                  confirmLabel: "Sil",
                });
                if (ok) removeMutation.mutate(p.id);
              }}
              className="rounded p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
              aria-label="Sil"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      ),
    },
  ];

  const pageInfo = productsQuery.data?.page;

  return (
    <div className="p-8">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Ürünler</h1>
          <p className="text-sm text-muted-foreground">
            {pageInfo
              ? `Toplam ${pageInfo.totalElements} ürün`
              : "Yükleniyor..."}
          </p>
        </div>
        <Button onClick={() => setCreating(true)}>
          <Plus className="mr-1 h-4 w-4" />
          Yeni Ürün
        </Button>
      </div>

      <form onSubmit={onSearch} className="mb-4 flex gap-2">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchLocal}
            onChange={(e) => setSearchLocal(e.target.value)}
            placeholder="Ürün adı, slug veya ID ile ara..."
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
        rows={productsQuery.data?.items}
        rowKey={(p) => p.id}
        isLoading={productsQuery.isLoading}
        isError={productsQuery.isError}
        emptyMessage="Hiç ürün bulunamadı."
        errorMessage="Ürünler yüklenemedi. Backend'e ulaşılamıyor olabilir."
        sort={sort}
        onSort={onSort}
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

      <AdminProductFormDialog
        open={creating}
        onOpenChange={setCreating}
        product={null}
      />
      <AdminProductFormDialog
        open={Boolean(editing)}
        onOpenChange={(o) => !o && setEditing(null)}
        product={editing}
      />
      {confirmDialog}
    </div>
  );
}
