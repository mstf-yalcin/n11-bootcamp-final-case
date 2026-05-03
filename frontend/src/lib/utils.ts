import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatTRY(amount: number | string, currency = "TRY"): string {
  const value = typeof amount === "string" ? Number(amount) : amount;
  if (Number.isNaN(value)) return "—";
  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  }).format(value);
}

export function formatDate(iso: string | undefined): string {
  if (!iso) return "—";
  try {
    return new Intl.DateTimeFormat("tr-TR", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}

/**
 * Backend telefonu farklı biçimlerde dönebilir ("905...", "+905...", "05...",
 * "5..."). 10 haneli yerel parçayı çıkarır.
 */
export function normalizeTrPhone(value: string | undefined): string {
  if (!value) return "";
  let digits = value.replace(/\D/g, "");
  if (digits.length > 10 && digits.startsWith("90")) digits = digits.slice(2);
  if (digits.length > 10 && digits.startsWith("0")) digits = digits.slice(1);
  return digits.slice(0, 10);
}

/**
 * "5551112233" → "+90 555 111 22 33". Geçerli olmayan değer dönerse `—`.
 */
export function formatTrPhone(value: string | undefined): string {
  const d = normalizeTrPhone(value);
  if (d.length !== 10) return value ?? "—";
  return `+90 ${d.slice(0, 3)} ${d.slice(3, 6)} ${d.slice(6, 8)} ${d.slice(8, 10)}`;
}
