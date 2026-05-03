import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { useSearchParams } from "react-router-dom";
import { ChevronRight, Package } from "lucide-react";
import { orderApi } from "@/api/endpoints";
import { Button } from "@/components/ui/button";
import { OrderRowSkeleton } from "@/components/ListSkeleton";
import { ApiErrorBox } from "@/components/ApiErrorBox";
import { OrderStatusBadge } from "@/features/orders/OrderStatusBadge";
import { usePageTitle } from "@/hooks/usePageTitle";
import { formatDate, formatTRY } from "@/lib/utils";

export default function OrderListPage() {
  usePageTitle("Siparişlerim");
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") ?? "0");

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["orders", page],
    queryFn: () => orderApi.list(page, 20),
  });

  return (
    <div className="container py-6">
      <h1 className="mb-6 text-2xl font-semibold">Siparişlerim</h1>

      {isLoading && (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <OrderRowSkeleton key={i} />
          ))}
        </div>
      )}
      {isError && (
        <ApiErrorBox
          error={error}
          onRetry={refetch}
          title="Siparişler yüklenemedi"
        />
      )}

      {data && data.items.length === 0 && (
        <div className="rounded-lg border bg-white p-10 text-center">
          <Package className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
          <p className="mb-4 text-muted-foreground">
            Henüz hiç siparişin yok.
          </p>
          <Button asChild>
            <Link to="/products">Alışverişe başla</Link>
          </Button>
        </div>
      )}

      {data && data.items.length > 0 && (
        <div className="space-y-3">
          {data.items.map((order) => (
            <Link
              key={order.id}
              to={`/orders/${order.id}`}
              className="block rounded-lg border bg-white p-4 transition-shadow hover:shadow-sm"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 space-y-2">
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-muted-foreground">
                      Sipariş No
                    </span>
                    <code className="text-xs font-mono">
                      {order.id.slice(0, 8).toUpperCase()}
                    </code>
                    <OrderStatusBadge
                      status={order.status}
                      cancelReason={order.cancelReason}
                    />
                  </div>
                  <div className="flex items-center gap-4 text-sm text-muted-foreground">
                    <span>{formatDate(order.createdAt)}</span>
                    <span>•</span>
                    <span>{order.items.length} ürün</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {order.items.slice(0, 3).map((it) => (
                      <span
                        key={it.productId}
                        className="rounded bg-secondary px-2 py-0.5 text-xs"
                      >
                        {it.productName} × {it.quantity}
                      </span>
                    ))}
                    {order.items.length > 3 && (
                      <span className="rounded bg-secondary px-2 py-0.5 text-xs">
                        +{order.items.length - 3} ürün
                      </span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <div className="text-right">
                    <div className="font-bold text-foreground">
                      {formatTRY(order.totalAmount, order.currency)}
                    </div>
                  </div>
                  <ChevronRight className="h-5 w-5 text-muted-foreground" />
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      {data?.page && data.page.totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-2">
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
