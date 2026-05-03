import { Badge } from "@/components/ui/badge";
import type { CancelReason, OrderStatus } from "@/types/api";

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "Onay Bekliyor",
  STOCK_RESERVED: "Stok Ayrıldı",
  PAYMENT_PROCESSING: "Ödeme İşleniyor",
  CONFIRMED: "Onaylandı",
  SHIPPED: "Kargoda",
  DELIVERED: "Teslim Edildi",
  CANCELLED: "İptal Edildi",
};

const REASON_LABEL_USER: Record<CancelReason, string> = {
  STOCK_UNAVAILABLE: "Stok bulunamadı",
  PAYMENT_FAILED: "Ödeme alınamadı",
  USER_CANCELLED: "Tarafınızca iptal edildi",
  ADMIN_CANCELLED: "Firma tarafından iptal edildi",
  TIMEOUT: "Zaman aşımı",
};

const REASON_LABEL_ADMIN: Record<CancelReason, string> = {
  STOCK_UNAVAILABLE: "Stok bulunamadı",
  PAYMENT_FAILED: "Ödeme alınamadı",
  USER_CANCELLED: "Müşteri iptal etti",
  ADMIN_CANCELLED: "Yönetici iptal etti",
  TIMEOUT: "Zaman aşımı",
};

export function OrderStatusBadge({
  status,
  cancelReason,
  viewer = "user",
}: {
  status: OrderStatus;
  cancelReason?: CancelReason;
  viewer?: "user" | "admin";
}) {
  let variant: "default" | "success" | "warning" | "info" | "destructive" =
    "info";
  switch (status) {
    case "PENDING":
    case "STOCK_RESERVED":
    case "PAYMENT_PROCESSING":
      variant = "warning";
      break;
    case "CONFIRMED":
    case "SHIPPED":
      variant = "info";
      break;
    case "DELIVERED":
      variant = "success";
      break;
    case "CANCELLED":
      variant = "destructive";
      break;
  }
  return (
    <div className="flex items-center gap-2">
      <Badge variant={variant}>{STATUS_LABEL[status]}</Badge>
      {status === "CANCELLED" && cancelReason && (
        <span className="text-xs text-muted-foreground">
          ({(viewer === "admin" ? REASON_LABEL_ADMIN : REASON_LABEL_USER)[cancelReason]})
        </span>
      )}
    </div>
  );
}
