import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, CreditCard, MapPin, Package } from "lucide-react";
import { toast } from "sonner";
import { addressApi, orderApi, paymentApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { Separator } from "@/components/ui/separator";
import { ApiErrorBox } from "@/components/ApiErrorBox";
import { useConfirm } from "@/components/ConfirmDialog";
import { OrderStatusBadge } from "@/features/orders/OrderStatusBadge";
import { OrderTimeline } from "@/features/orders/OrderTimeline";
import { useProductLookups } from "@/features/products/useProductLookups";
import { usePageTitle } from "@/hooks/usePageTitle";
import { formatDate, formatTRY, formatTrPhone } from "@/lib/utils";
import type { Order, OrderStatus } from "@/types/api";

const FALLBACK_IMG = "https://placehold.co/96x96/fff3eb/ff6000?text=n11";

const POLLING_STATUSES: OrderStatus[] = [
  "PENDING",
  "STOCK_RESERVED",
  "PAYMENT_PROCESSING",
];

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { confirm, dialog: confirmDialog } = useConfirm();

  const orderQuery = useQuery({
    queryKey: ["order", id],
    queryFn: () => orderApi.byId(id!),
    enabled: Boolean(id),
    refetchInterval: (q) => {
      const order = q.state.data as Order | undefined;
      if (order && POLLING_STATUSES.includes(order.status)) {
        return 2000;
      }
      return false;
    },
  });

  const order = orderQuery.data;
  const isInProgress = order && POLLING_STATUSES.includes(order.status);

  usePageTitle(
    order ? `Sipariş #${order.id.slice(0, 8).toUpperCase()}` : "Sipariş"
  );

  const paymentQuery = useQuery({
    queryKey: ["payment", id],
    queryFn: () => paymentApi.byOrderId(id!),
    enabled: Boolean(id) && !isInProgress,
    retry: false,
  });

  const addressQuery = useQuery({
    queryKey: ["addresses"],
    queryFn: addressApi.list,
    enabled: Boolean(order),
  });

  const cancelMutation = useMutation({
    mutationFn: () => orderApi.cancel(id!),
    onSuccess: (cancelled) => {
      queryClient.invalidateQueries({ queryKey: ["order", id] });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      queryClient.invalidateQueries({ queryKey: ["payment", id] });
      const wasPaid = order?.status === "CONFIRMED";
      if (wasPaid) {
        toast.success(
          "Sipariş iptal edildi, ödemen iade için işleme alındı.",
          { duration: 5000 }
        );
      } else {
        toast.success("Sipariş iptal edildi");
      }
      // Suppress unused var warning when toast logic doesn't use response
      void cancelled;
    },
    onError: (err) => notifyApiError(err, "Sipariş iptal edilemedi"),
  });

  if (orderQuery.isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner size={28} />
      </div>
    );
  }

  if (orderQuery.isError || !order) {
    return (
      <div className="container py-12">
        <ApiErrorBox
          error={orderQuery.error}
          onRetry={orderQuery.refetch}
          title="Sipariş yüklenemedi"
        />
        <div className="mt-4 text-center">
          <Button onClick={() => navigate("/orders")} variant="outline">
            Siparişlere dön
          </Button>
        </div>
      </div>
    );
  }

  const address = addressQuery.data?.find((a) => a.id === order.addressId);
  const canCancel =
    order.status === "PENDING" ||
    order.status === "STOCK_RESERVED" ||
    order.status === "CONFIRMED";
  const isPaidCancel = order.status === "CONFIRMED";

  return (
    <div className="container py-6">
      <div className="mb-6 flex items-center justify-between">
        <Link
          to="/orders"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-n11"
        >
          <ArrowLeft className="h-4 w-4" /> Tüm siparişler
        </Link>
        {canCancel && (
          <Button
            variant="outline"
            size="sm"
            onClick={async () => {
              const ok = await confirm({
                title: "Siparişi iptal et",
                description: isPaidCancel
                  ? "Siparişini iptal etmek istediğine emin misin? Ödemen otomatik olarak iade edilecek (kart hesabına yansıması birkaç iş günü sürebilir)."
                  : "Siparişini iptal etmek istediğine emin misin? Bu işlem geri alınamaz.",
                confirmLabel: isPaidCancel ? "İptal Et ve İade Al" : "İptal Et",
                cancelLabel: "Vazgeç",
                destructive: true,
              });
              if (ok) cancelMutation.mutate();
            }}
            disabled={cancelMutation.isPending}
          >
            {cancelMutation.isPending ? "İptal ediliyor..." : "Siparişi İptal Et"}
          </Button>
        )}
      </div>

      <div className="mb-6 rounded-lg border bg-white p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="text-xs text-muted-foreground">Sipariş No</div>
            <code className="font-mono text-base">
              {order.id.toUpperCase()}
            </code>
            <div className="mt-1 text-xs text-muted-foreground">
              Oluşturma: {formatDate(order.createdAt)}
            </div>
            <div className="text-xs text-muted-foreground">
              Son Güncelleme: {formatDate(order.updatedAt)}
            </div>
          </div>
          <OrderStatusBadge
            status={order.status}
            cancelReason={order.cancelReason}
          />
        </div>

        {isInProgress && (
          <div className="mt-4 flex items-center gap-3 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm">
            <Spinner size={16} />
            <span>
              Saga akışı çalışıyor — stok rezervasyonu ve ödeme işleniyor.
              Durum otomatik güncelleniyor.
            </span>
          </div>
        )}
      </div>

      <div className="mb-6">
        <OrderTimeline status={order.status} cancelReason={order.cancelReason} />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <section className="rounded-lg border bg-white p-5">
            <h2 className="mb-3 flex items-center gap-2 text-base font-semibold">
              <Package className="h-4 w-4 text-n11" /> Ürünler
            </h2>
            <OrderItemsList items={order.items} />
          </section>

          {address && (
            <section className="rounded-lg border bg-white p-5">
              <h2 className="mb-3 flex items-center gap-2 text-base font-semibold">
                <MapPin className="h-4 w-4 text-n11" /> Teslimat Adresi
              </h2>
              <div className="text-sm">
                <div className="font-semibold">{address.title}</div>
                <div className="text-muted-foreground">
                  {address.contactName}
                </div>
                <p className="mt-1 leading-relaxed">
                  {address.fullAddress}
                  {address.district ? `, ${address.district}` : ""},{" "}
                  {address.city}
                  {address.zipCode ? ` ${address.zipCode}` : ""}
                </p>
                {address.phone && (
                  <p className="mt-1 text-xs text-muted-foreground">
                    Tel: {formatTrPhone(address.phone)}
                  </p>
                )}
              </div>
            </section>
          )}

          {paymentQuery.data && (
            <section className="rounded-lg border bg-white p-5">
              <h2 className="mb-3 flex items-center gap-2 text-base font-semibold">
                <CreditCard className="h-4 w-4 text-n11" /> Ödeme
              </h2>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Sağlayıcı</span>
                  <span>{paymentQuery.data.provider}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Durum</span>
                  <span className="font-medium">
                    {paymentQuery.data.status}
                  </span>
                </div>
                {paymentQuery.data.errorMessage && (
                  <div className="rounded bg-destructive/10 p-2 text-xs text-destructive">
                    {paymentQuery.data.errorMessage}
                  </div>
                )}
              </div>
            </section>
          )}
        </div>

        <aside>
          <div className="space-y-4 rounded-lg border bg-white p-5 lg:sticky lg:top-20">
            <h3 className="text-base font-semibold">Tutar Özeti</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Ara Toplam</span>
                <span>{formatTRY(order.totalAmount, order.currency)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Kargo</span>
                <span className="text-emerald-600">Ücretsiz</span>
              </div>
            </div>
            <Separator />
            <div className="flex justify-between text-base">
              <span className="font-semibold">Toplam</span>
              <span className="font-bold text-foreground">
                {formatTRY(order.totalAmount, order.currency)}
              </span>
            </div>
          </div>
        </aside>
      </div>
      {confirmDialog}
    </div>
  );
}

function OrderItemsList({ items }: { items: Order["items"] }) {
  const { lookups } = useProductLookups(items.map((i) => i.productId));

  return (
    <div className="space-y-3">
      {items.map((item) => {
        const lookup = lookups.get(item.productId);
        const detailHref = lookup ? `/products/${lookup.slug}` : null;
        const thumbSrc = lookup?.imageUrl || FALLBACK_IMG;

        const content = (
          <>
            <img
              src={thumbSrc}
              alt={item.productName}
              onError={(e) => {
                (e.target as HTMLImageElement).src = FALLBACK_IMG;
              }}
              className="h-14 w-14 flex-shrink-0 rounded-md border object-cover"
            />
            <div className="min-w-0 flex-1">
              <div className="truncate font-medium group-hover:text-n11">
                {item.productName}
              </div>
              <div className="text-xs text-muted-foreground">
                {item.quantity} × {formatTRY(item.unitPrice, item.currency)}
              </div>
            </div>
            <div className="font-semibold">
              {formatTRY(item.subtotal, item.currency)}
            </div>
          </>
        );

        return detailHref ? (
          <Link
            key={item.productId}
            to={detailHref}
            className="group flex items-center gap-3 border-b pb-3 last:border-b-0 last:pb-0"
          >
            {content}
          </Link>
        ) : (
          <div
            key={item.productId}
            className="flex items-center gap-3 border-b pb-3 last:border-b-0 last:pb-0"
          >
            {content}
          </div>
        );
      })}
    </div>
  );
}
