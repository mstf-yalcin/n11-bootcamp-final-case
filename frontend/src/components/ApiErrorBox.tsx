import { AlertTriangle, RefreshCcw, WifiOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { extractApiError } from "@/api/client";

type Props = {
  error: unknown;
  onRetry?: () => void;
  title?: string;
};

export function ApiErrorBox({ error, onRetry, title }: Props) {
  const { message, status } = extractApiError(error);
  const isNetwork = !status;

  return (
    <div className="flex flex-col items-center justify-center gap-4 rounded-lg border border-destructive/30 bg-destructive/5 p-10 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10">
        {isNetwork ? (
          <WifiOff className="h-6 w-6 text-destructive" />
        ) : (
          <AlertTriangle className="h-6 w-6 text-destructive" />
        )}
      </div>
      <div>
        <h3 className="font-semibold">
          {title ?? (isNetwork ? "Bağlantı kurulamadı" : "Bir hata oluştu")}
        </h3>
        <p className="mt-1 text-sm text-muted-foreground">
          {isNetwork
            ? "Sunucuya ulaşılamıyor. İnternet bağlantını kontrol et veya birazdan tekrar dene."
            : message}
        </p>
      </div>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCcw className="mr-2 h-4 w-4" />
          Tekrar Dene
        </Button>
      )}
    </div>
  );
}
