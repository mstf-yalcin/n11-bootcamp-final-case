import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { ChevronDown, ChevronRight, ExternalLink, Search } from "lucide-react";
import { Link } from "react-router-dom";
import { adminOrderApi } from "@/api/endpoints";
import { API_BASE } from "@/api/client";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DataTable, type Column, type SortState } from "@/components/DataTable";
import { OrderStatusBadge } from "@/features/orders/OrderStatusBadge";
import { useProductLookups } from "@/features/products/useProductLookups";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useForm } from "react-hook-form";
import { formatDate, formatTRY } from "@/lib/utils";
import { usePageTitle } from "@/hooks/usePageTitle";
import type {
  AdminOrderStatusUpdate,
  Order,
  OrderStatus,
} from "@/types/api";

const STATUS_OPTIONS: { value: OrderStatus | ""; label: string }[] = [
  { value: "", label: "Tümü" },
  { value: "PENDING", label: "Onay Bekliyor" },
  { value: "STOCK_RESERVED", label: "Stok Ayrıldı" },
  { value: "PAYMENT_PROCESSING", label: "Ödeme İşleniyor" },
  { value: "CONFIRMED", label: "Onaylandı" },
  { value: "SHIPPED", label: "Kargoda" },
  { value: "DELIVERED", label: "Teslim Edildi" },
  { value: "CANCELLED", label: "İptal Edildi" },
];

const VALID_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ["CANCELLED"],
  STOCK_RESERVED: ["CANCELLED"],
  PAYMENT_PROCESSING: [],
  CONFIRMED: ["SHIPPED", "CANCELLED"],
  SHIPPED: ["DELIVERED"],
  DELIVERED: [],
  CANCELLED: [],
};

const FALLBACK_IMG = "https://placehold.co/80x80/fff3eb/ff6000?text=n11";

function parseSort(raw: string | null): SortState | null {
  if (!raw) return null;
  const [key, direction] = raw.split(",");
  if (!key) return null;
  return { key, direction: direction === "desc" ? "desc" : "asc" };
}

const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: "Onay Bekliyor",
  STOCK_RESERVED: "Stok Ayrıldı",
  PAYMENT_PROCESSING: "Ödeme İşleniyor",
  CONFIRMED: "Onaylandı",
  SHIPPED: "Kargoda",
  DELIVERED: "Teslim Edildi",
  CANCELLED: "İptal Edildi",
};

export default function AdminOrdersPage() {
  usePageTitle("Admin · Siparişler");
  const [params, setParams] = useSearchParams();
  const status = (params.get("status") ?? "") as OrderStatus | "";
  const search = params.get("search") ?? "";
  const page = Number(params.get("page") ?? "0");
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

  const [editing, setEditing] = useState<Order | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const ordersQuery = useQuery({
    queryKey: ["admin", "orders", { status, search, page, sort: sortParam }],
    queryFn: () =>
      adminOrderApi.list({
        status: status || undefined,
        search: search || undefined,
        page,
        size: 20,
        sort: sortParam,
      }),
  });

  const expandedOrder = ordersQuery.data?.items.find(
    (o) => o.id === expandedId
  );
  const expandedProductIds =
    expandedOrder?.items.map((it) => it.productId) ?? [];
  const { lookups } = useProductLookups(expandedProductIds);

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const next = new URLSearchParams(params);
    if (searchLocal.trim()) next.set("search", searchLocal.trim());
    else next.delete("search");
    next.delete("page");
    setParams(next, { replace: true });
  };

  const updateStatusMutation = useMutation({
    mutationFn: (vars: { id: string; body: AdminOrderStatusUpdate }) =>
      adminOrderApi.updateStatus(vars.id, vars.body),
    onSuccess: () => {
      toast.success("Sipariş durumu güncellendi");
      queryClient.invalidateQueries({ queryKey: ["admin", "orders"] });
      setEditing(null);
    },
    onError: (err) => notifyApiError(err, "Güncelleme başarısız"),
  });

  const setStatusFilter = (value: string) => {
    const next = new URLSearchParams(params);
    if (value) next.set("status", value);
    else next.delete("status");
    next.delete("page");
    setParams(next, { replace: true });
  };

  const setPage = (p: number) => {
    const next = new URLSearchParams(params);
    next.set("page", String(p));
    setParams(next, { replace: true });
  };

  const totalQty = (o: Order) =>
    o.items.reduce((sum, it) => sum + it.quantity, 0);

  const columns: Column<Order>[] = [
    {
      key: "expand",
      header: "",
      width: "32px",
      cell: (o) => (
        <button
          onClick={(e) => {
            e.stopPropagation();
            setExpandedId(expandedId === o.id ? null : o.id);
          }}
          className="rounded p-1 text-muted-foreground hover:bg-accent hover:text-foreground"
          aria-label={expandedId === o.id ? "Detayı kapat" : "Detayı aç"}
        >
          {expandedId === o.id ? (
            <ChevronDown className="h-4 w-4" />
          ) : (
            <ChevronRight className="h-4 w-4" />
          )}
        </button>
      ),
    },
    {
      key: "id",
      header: "Sipariş No",
      cell: (o) => (
        <code className="text-xs font-mono">
          {o.id}
        </code>
      ),
    },
    {
      key: "buyer",
      header: "Müşteri",
      sortKey: "buyerFirstName",
      cell: (o) => (
        <div className="text-xs">
          {o.buyerFullName ? (
            <div className="font-medium">{o.buyerFullName}</div>
          ) : null}
          {o.buyerEmail ? (
            <div className="text-muted-foreground">{o.buyerEmail}</div>
          ) : (
            <code className="font-mono text-muted-foreground">
              {o.userId}
            </code>
          )}
        </div>
      ),
    },
    {
      key: "createdAt",
      header: "Tarih",
      sortKey: "createdAt",
      cell: (o) => (
        <span className="text-xs text-muted-foreground">
          {formatDate(o.createdAt)}
        </span>
      ),
    },
    {
      key: "items",
      header: "Ürünler",
      cell: (o) => (
        <div className="text-xs">
          <div className="font-medium">
            {o.items.length} çeşit · {totalQty(o)} adet
          </div>
          {o.items[0] && (
            <div className="mt-0.5 line-clamp-1 text-muted-foreground">
              {o.items[0].productName}
              {o.items.length > 1 && ` +${o.items.length - 1} ürün`}
            </div>
          )}
        </div>
      ),
    },
    {
      key: "amount",
      header: "Tutar",
      sortKey: "totalAmount",
      cell: (o) => (
        <span className="font-semibold">
          {formatTRY(o.totalAmount, o.currency)}
        </span>
      ),
    },
    {
      key: "status",
      header: "Durum",
      sortKey: "status",
      cell: (o) => (
        <OrderStatusBadge status={o.status} cancelReason={o.cancelReason} viewer="admin" />
      ),
    },
    {
      key: "actions",
      header: "",
      width: "100px",
      className: "text-right",
      cell: (o) => (
        <Button
          variant="outline"
          size="sm"
          onClick={() => setEditing(o)}
          disabled={VALID_TRANSITIONS[o.status].length === 0}
        >
          Düzenle
        </Button>
      ),
    },
  ];

  const renderOrderDetails = (o: Order) => (
    <div className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-2">
        {o.shippingCity && (
          <div className="text-xs">
            <div className="font-semibold uppercase text-muted-foreground">
              Teslimat
            </div>
            <div>
              {o.shippingDistrict ? `${o.shippingDistrict}, ` : ""}
              {o.shippingCity}
            </div>
          </div>
        )}
        {o.cancelReason && (
          <div className="text-xs">
            <div className="font-semibold uppercase text-muted-foreground">
              İptal Sebebi
            </div>
            <div>{o.cancelReason}</div>
          </div>
        )}
      </div>

      <div>
        <div className="mb-2 text-xs font-semibold uppercase text-muted-foreground">
          Sipariş Kalemleri ({o.items.length} çeşit · {totalQty(o)} adet)
        </div>
        <div className="overflow-hidden rounded-md border bg-white">
          <table className="w-full text-sm">
            <thead className="bg-secondary/30">
              <tr>
                <th className="px-3 py-2 text-left text-[10px] font-semibold uppercase text-muted-foreground">
                  Ürün
                </th>
                <th className="px-3 py-2 text-right text-[10px] font-semibold uppercase text-muted-foreground">
                  Adet
                </th>
                <th className="px-3 py-2 text-right text-[10px] font-semibold uppercase text-muted-foreground">
                  Birim
                </th>
                <th className="px-3 py-2 text-right text-[10px] font-semibold uppercase text-muted-foreground">
                  Tutar
                </th>
              </tr>
            </thead>
            <tbody>
              {o.items.map((it) => {
                const lookup = lookups.get(it.productId);
                const href = lookup ? `/products/${lookup.slug}` : null;
                const thumb = lookup?.imageUrl || FALLBACK_IMG;
                return (
                  <tr
                    key={it.productId}
                    className="border-t first:border-t-0"
                  >
                    <td className="px-3 py-2">
                      <div className="flex items-center gap-3">
                        {href ? (
                          <Link
                            to={href}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex-shrink-0"
                            aria-label={`${it.productName} detayı`}
                          >
                            <img
                              src={thumb}
                              alt={it.productName}
                              onError={(e) => {
                                (e.target as HTMLImageElement).src =
                                  FALLBACK_IMG;
                              }}
                              className="h-10 w-10 rounded border bg-secondary object-cover transition-opacity hover:opacity-80"
                            />
                          </Link>
                        ) : (
                          <img
                            src={FALLBACK_IMG}
                            alt=""
                            className="h-10 w-10 flex-shrink-0 rounded border bg-secondary object-cover opacity-60"
                          />
                        )}
                        <div className="min-w-0">
                          {href ? (
                            <Link
                              to={href}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex items-center gap-1 font-medium hover:text-n11 hover:underline"
                            >
                              <span className="truncate">
                                {it.productName}
                              </span>
                              <ExternalLink className="h-3 w-3 flex-shrink-0 opacity-60" />
                            </Link>
                          ) : (
                            <span
                              className="font-medium text-muted-foreground"
                              title="Bu ürün artık mevcut değil"
                            >
                              {it.productName}
                            </span>
                          )}
                          <code className="block text-[10px] text-muted-foreground">
                            {it.productId}
                          </code>
                        </div>
                      </div>
                    </td>
                    <td className="px-3 py-2 text-right tabular-nums">
                      {it.quantity}
                    </td>
                    <td className="px-3 py-2 text-right tabular-nums text-muted-foreground">
                      {formatTRY(it.unitPrice, it.currency)}
                    </td>
                    <td className="px-3 py-2 text-right font-semibold tabular-nums">
                      {formatTRY(it.subtotal, it.currency)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
            <tfoot className="border-t bg-secondary/20">
              <tr>
                <td
                  colSpan={3}
                  className="px-3 py-2 text-right text-xs font-semibold uppercase text-muted-foreground"
                >
                  Toplam
                </td>
                <td className="px-3 py-2 text-right font-bold text-foreground tabular-nums">
                  {formatTRY(o.totalAmount, o.currency)}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  );

  const pageInfo = ordersQuery.data?.page;

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Siparişler</h1>
        <p className="text-sm text-muted-foreground">
          {pageInfo
            ? `Toplam ${pageInfo.totalElements} sipariş`
            : "Yükleniyor..."}
        </p>
      </div>

      <form onSubmit={onSearch} className="mb-4 flex gap-2">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchLocal}
            onChange={(e) => setSearchLocal(e.target.value)}
            placeholder="Müşteri adı, e-posta veya sipariş ID ara..."
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

      <div className="mb-4 flex flex-wrap gap-2">
        {STATUS_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            onClick={() => setStatusFilter(opt.value)}
            className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
              status === opt.value
                ? "border-n11 bg-n11 text-white"
                : "bg-white text-muted-foreground hover:border-n11 hover:text-n11"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <DataTable
        columns={columns}
        rows={ordersQuery.data?.items}
        rowKey={(o) => o.id}
        isLoading={ordersQuery.isLoading}
        isError={ordersQuery.isError}
        expandedRowKey={expandedId}
        renderExpandedRow={renderOrderDetails}
        emptyMessage="Bu kriterlere uygun sipariş yok."
        errorMessage={`Siparişler yüklenemedi. Admin endpoint'i (GET ${API_BASE}/admin/orders) henüz aktif değil olabilir.`}
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

      <OrderStatusEditDialog
        order={editing}
        onClose={() => setEditing(null)}
        onSubmit={(body) =>
          editing && updateStatusMutation.mutate({ id: editing.id, body })
        }
        isPending={updateStatusMutation.isPending}
      />
    </div>
  );
}

function OrderStatusEditDialog({
  order,
  onClose,
  onSubmit,
  isPending,
}: {
  order: Order | null;
  onClose: () => void;
  onSubmit: (body: AdminOrderStatusUpdate) => void;
  isPending: boolean;
}) {
  const targets = order ? VALID_TRANSITIONS[order.status] : [];
  const defaultStatus: OrderStatus = targets[0] ?? "CANCELLED";

  const {
    register,
    handleSubmit,
    reset,
  } = useForm<AdminOrderStatusUpdate>({
    defaultValues: { status: defaultStatus },
  });

  return (
    <Dialog
      open={Boolean(order)}
      onOpenChange={(o) => {
        if (o && order) reset({ status: defaultStatus });
        if (!o) onClose();
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Sipariş Durumu Güncelle</DialogTitle>
        </DialogHeader>
        {order && (
          <div className="mb-2 rounded-md bg-secondary/40 p-3 text-xs">
            Sipariş <code className="font-mono">{order.id}</code>{" "}
            — şu an{" "}
            <OrderStatusBadge
              status={order.status}
              cancelReason={order.cancelReason}
              viewer="admin"
            />
          </div>
        )}
        {targets.length === 0 ? (
          <div className="py-4 text-center text-sm text-muted-foreground">
            Bu sipariş için geçerli bir durum geçişi yok.
          </div>
        ) : (
          <form
            onSubmit={handleSubmit((data) => onSubmit({ status: data.status }))}
            className="space-y-3"
          >
            <div>
              <label className="mb-1 block text-xs font-medium uppercase text-muted-foreground">
                Yeni Durum
              </label>
              <select
                {...register("status")}
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
              >
                {targets.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_LABELS[s]}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={onClose}>
                İptal
              </Button>
              <Button type="submit" disabled={isPending}>
                Güncelle
              </Button>
            </div>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
