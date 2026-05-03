import { Check, CircleDashed, X } from "lucide-react";
import { cn } from "@/lib/utils";
import type { CancelReason, OrderStatus } from "@/types/api";

type Step = {
  key: OrderStatus;
  label: string;
  description: string;
};

const STEPS: Step[] = [
  {
    key: "PENDING",
    label: "Sipariş Alındı",
    description: "Siparişin sisteme düştü",
  },
  {
    key: "STOCK_RESERVED",
    label: "Stok Ayrıldı",
    description: "Ürünler senin için ayrıldı",
  },
  {
    key: "CONFIRMED",
    label: "Ödeme Tamamlandı",
    description: "Ödemen başarıyla alındı",
  },
  {
    key: "SHIPPED",
    label: "Kargoya Verildi",
    description: "Paketin yola çıktı",
  },
  {
    key: "DELIVERED",
    label: "Teslim Edildi",
    description: "Paketin teslim edildi",
  },
];

const STATUS_ORDER: OrderStatus[] = [
  "PENDING",
  "STOCK_RESERVED",
  "PAYMENT_PROCESSING",
  "CONFIRMED",
  "SHIPPED",
  "DELIVERED",
];

const REASON_LABEL: Record<CancelReason, string> = {
  STOCK_UNAVAILABLE: "Stok yetersizliği nedeniyle iptal edildi",
  PAYMENT_FAILED: "Ödeme başarısız olduğu için iptal edildi",
  USER_CANCELLED: "Senin tarafından iptal edildi",
  ADMIN_CANCELLED: "Mağaza yöneticisi tarafından iptal edildi",
  TIMEOUT: "Zaman aşımı nedeniyle iptal edildi",
};

export function OrderTimeline({
  status,
  cancelReason,
}: {
  status: OrderStatus;
  cancelReason?: CancelReason;
}) {
  if (status === "CANCELLED") {
    return (
      <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-5">
        <div className="flex items-start gap-3">
          <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-destructive text-white">
            <X className="h-5 w-5" />
          </div>
          <div>
            <div className="font-semibold text-destructive">
              Sipariş İptal Edildi
            </div>
            {cancelReason && (
              <p className="mt-1 text-sm text-muted-foreground">
                {REASON_LABEL[cancelReason]}
              </p>
            )}
          </div>
        </div>
      </div>
    );
  }

  const currentIndex = STATUS_ORDER.indexOf(status);

  return (
    <div className="rounded-lg border bg-white p-5">
      <ol className="relative space-y-6">
        {STEPS.map((step, idx) => {
          const stepIdx = STATUS_ORDER.indexOf(step.key);
          const isCompleted = currentIndex >= stepIdx && stepIdx >= 0;
          const isCurrent =
            (status === step.key) ||
            (status === "PAYMENT_PROCESSING" && step.key === "CONFIRMED" && false) ||
            (status === "PAYMENT_PROCESSING" && step.key === "STOCK_RESERVED" && currentIndex === stepIdx);
          const showLine = idx < STEPS.length - 1;

          return (
            <li key={step.key} className="flex gap-4">
              <div className="relative flex flex-col items-center">
                <div
                  className={cn(
                    "z-10 flex h-9 w-9 items-center justify-center rounded-full border-2",
                    isCompleted
                      ? "border-emerald-500 bg-emerald-500 text-white"
                      : "border-border bg-background text-muted-foreground"
                  )}
                >
                  {isCompleted ? (
                    <Check className="h-5 w-5" />
                  ) : (
                    <CircleDashed
                      className={cn(
                        "h-5 w-5",
                        isCurrent && "animate-spin text-n11"
                      )}
                    />
                  )}
                </div>
                {showLine && (
                  <div
                    className={cn(
                      "absolute top-9 h-[calc(100%+0.5rem)] w-0.5",
                      isCompleted && currentIndex >= stepIdx + 1
                        ? "bg-emerald-500"
                        : "bg-border"
                    )}
                  />
                )}
              </div>
              <div className="pb-1 pt-1">
                <div
                  className={cn(
                    "font-medium",
                    isCompleted ? "text-foreground" : "text-muted-foreground"
                  )}
                >
                  {step.label}
                </div>
                <p className="text-xs text-muted-foreground">
                  {step.description}
                </p>
              </div>
            </li>
          );
        })}
      </ol>
      {status === "PAYMENT_PROCESSING" && (
        <p className="mt-4 rounded-md bg-amber-50 p-2 text-xs text-amber-800">
          Ödeme şu an işleniyor — durum birkaç saniye içinde güncellenecek.
        </p>
      )}
    </div>
  );
}
