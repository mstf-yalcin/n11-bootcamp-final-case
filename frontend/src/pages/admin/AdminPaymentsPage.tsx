import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { Search } from "lucide-react";
import { adminPaymentApi } from "@/api/endpoints";
import { API_BASE } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { DataTable, type Column, type SortState } from "@/components/DataTable";
import { formatDate, formatTRY } from "@/lib/utils";
import { usePageTitle } from "@/hooks/usePageTitle";
import type { Payment, PaymentStatus } from "@/types/api";

const STATUS_OPTIONS: { value: PaymentStatus | ""; label: string }[] = [
  { value: "", label: "Tümü" },
  { value: "PENDING", label: "Bekliyor" },
  { value: "COMPLETED", label: "Tamamlandı" },
  { value: "FAILED", label: "Başarısız" },
  { value: "CANCELLED", label: "İptal" },
  { value: "REFUNDED", label: "İade" },
];

function parseSort(raw: string | null): SortState | null {
  if (!raw) return null;
  const [key, direction] = raw.split(",");
  if (!key) return null;
  return { key, direction: direction === "desc" ? "desc" : "asc" };
}

function statusVariant(
  s: PaymentStatus
): "default" | "success" | "warning" | "info" | "destructive" {
  switch (s) {
    case "COMPLETED":
      return "success";
    case "PENDING":
      return "warning";
    case "FAILED":
    case "CANCELLED":
      return "destructive";
    case "REFUNDED":
      return "info";
  }
}

export default function AdminPaymentsPage() {
  usePageTitle("Admin · Ödemeler");
  const [params, setParams] = useSearchParams();
  const status = (params.get("status") ?? "") as PaymentStatus | "";
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

  const paymentsQuery = useQuery({
    queryKey: ["admin", "payments", { status, search, page, sort: sortParam }],
    queryFn: () =>
      adminPaymentApi.list({
        status: status || undefined,
        search: search || undefined,
        page,
        size: 20,
        sort: sortParam,
      }),
  });

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const next = new URLSearchParams(params);
    if (searchLocal.trim()) next.set("search", searchLocal.trim());
    else next.delete("search");
    next.delete("page");
    setParams(next, { replace: true });
  };

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

  const columns: Column<Payment>[] = [
    {
      key: "orderId",
      header: "Sipariş No",
      cell: (p) => (
        <code className="text-xs font-mono">
          {p.orderId.slice(0, 8).toUpperCase()}
        </code>
      ),
    },
    {
      key: "amount",
      header: "Tutar",
      sortKey: "amount",
      cell: (p) => (
        <span className="font-semibold">{formatTRY(p.amount, p.currency)}</span>
      ),
    },
    {
      key: "provider",
      header: "Sağlayıcı",
      sortKey: "provider",
      cell: (p) => (
        <span className="text-muted-foreground">{p.provider}</span>
      ),
    },
    {
      key: "status",
      header: "Durum",
      sortKey: "status",
      cell: (p) => <Badge variant={statusVariant(p.status)}>{p.status}</Badge>,
    },
    {
      key: "createdAt",
      header: "Tarih",
      sortKey: "createdAt",
      cell: (p) => (
        <span className="text-xs text-muted-foreground">
          {formatDate(p.createdAt)}
        </span>
      ),
    },
  ];

  const pageInfo = paymentsQuery.data?.page;

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Ödemeler</h1>
        <p className="text-sm text-muted-foreground">
          {pageInfo
            ? `Toplam ${pageInfo.totalElements} ödeme`
            : "Yükleniyor..."}
        </p>
      </div>

      <form onSubmit={onSearch} className="mb-4 flex gap-2">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchLocal}
            onChange={(e) => setSearchLocal(e.target.value)}
            placeholder="Müşteri adı, e-posta veya ödeme/sipariş ID ara..."
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
        rows={paymentsQuery.data?.items}
        rowKey={(p) => p.id}
        isLoading={paymentsQuery.isLoading}
        isError={paymentsQuery.isError}
        emptyMessage="Bu kriterlere uygun ödeme yok."
        errorMessage={`Ödemeler yüklenemedi. Admin endpoint'i (GET ${API_BASE}/admin/payments) henüz aktif değil olabilir.`}
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
    </div>
  );
}
