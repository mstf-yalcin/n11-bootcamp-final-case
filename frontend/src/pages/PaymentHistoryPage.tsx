import { useQuery } from "@tanstack/react-query";
import { useSearchParams, Link } from "react-router-dom";
import { CreditCard, ExternalLink } from "lucide-react";
import { paymentApi } from "@/api/endpoints";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ApiErrorBox } from "@/components/ApiErrorBox";
import { PaymentRowSkeleton } from "@/components/ListSkeleton";
import { usePageTitle } from "@/hooks/usePageTitle";
import { formatDate, formatTRY } from "@/lib/utils";
import type { PaymentStatus } from "@/types/api";

const STATUS_LABEL: Record<PaymentStatus, string> = {
  PENDING: "Bekliyor",
  COMPLETED: "Tamamlandı",
  FAILED: "Başarısız",
  CANCELLED: "İptal",
  REFUNDED: "İade Edildi",
};

function statusVariant(
  status: PaymentStatus
): "default" | "success" | "warning" | "info" | "destructive" {
  switch (status) {
    case "COMPLETED":
      return "success";
    case "PENDING":
      return "warning";
    case "FAILED":
      return "destructive";
    case "CANCELLED":
      return "destructive";
    case "REFUNDED":
      return "info";
  }
}

export default function PaymentHistoryPage() {
  usePageTitle("Ödemelerim");
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") ?? "0");

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["payments", "me", page],
    queryFn: () => paymentApi.myPayments(page, 20),
  });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Ödemelerim</h1>

      {isLoading && (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <PaymentRowSkeleton key={i} />
          ))}
        </div>
      )}

      {isError && (
        <ApiErrorBox
          error={error}
          onRetry={refetch}
          title="Ödemeler yüklenemedi"
        />
      )}

      {data && data.items.length === 0 && (
        <div className="rounded-lg border bg-white p-10 text-center">
          <CreditCard className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
          <p className="text-muted-foreground">
            Henüz tamamlanmış bir ödemen yok.
          </p>
        </div>
      )}

      {data && data.items.length > 0 && (
        <div className="space-y-2">
          {data.items.map((p) => (
            <div
              key={p.id}
              className="flex items-center justify-between gap-4 rounded-lg border bg-white p-4"
            >
              <div className="flex-1 space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-muted-foreground">
                    Sipariş No
                  </span>
                  <code className="text-xs font-mono">
                    {p.orderId.slice(0, 8).toUpperCase()}
                  </code>
                  <Badge variant={statusVariant(p.status)}>
                    {STATUS_LABEL[p.status]}
                  </Badge>
                </div>
                <div className="text-xs text-muted-foreground">
                  {formatDate(p.createdAt)} • {p.provider}
                </div>
                {p.errorMessage && (
                  <div className="text-xs text-destructive">
                    {p.errorMessage}
                  </div>
                )}
              </div>
              <div className="text-right">
                <div className="font-bold text-foreground">
                  {formatTRY(p.amount, p.currency)}
                </div>
              </div>
              <Link
                to={`/orders/${p.orderId}`}
                className="rounded-md p-2 text-muted-foreground hover:bg-accent hover:text-n11"
                aria-label="Sipariş detayına git"
              >
                <ExternalLink className="h-4 w-4" />
              </Link>
            </div>
          ))}
        </div>
      )}

      {data?.page && data.page.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
          <Button
            variant="outline"
            size="sm"
            disabled={!data.page.hasPrevious}
            onClick={() => {
              const next = new URLSearchParams(params);
              next.set("page", String(page - 1));
              setParams(next);
            }}
          >
            Önceki
          </Button>
          <span className="text-sm text-muted-foreground">
            Sayfa {data.page.pageNumber + 1} / {data.page.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={!data.page.hasNext}
            onClick={() => {
              const next = new URLSearchParams(params);
              next.set("page", String(page + 1));
              setParams(next);
            }}
          >
            Sonraki
          </Button>
        </div>
      )}
    </div>
  );
}
