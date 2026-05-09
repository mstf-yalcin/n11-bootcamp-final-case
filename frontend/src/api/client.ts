import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { toast } from "sonner";
import { useAuthStore } from "@/store/authStore";
import type { ApiResponse, AuthTokens } from "@/types/api";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

/** Backend API path prefix. v2'ye geçilirse tek yerden değişir. */
export const API_BASE = "/api/v1";

export const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

const NO_AUTH_PATHS = [
  `${API_BASE}/auth/login`,
  `${API_BASE}/auth/register`,
  `${API_BASE}/auth/refresh`,
];

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const url = config.url ?? "";
  const isAuthBootstrap = NO_AUTH_PATHS.some((p) => url.includes(p));
  if (isAuthBootstrap) return config;

  const token = useAuthStore.getState().accessToken;
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise: Promise<AuthTokens> | null = null;

async function refreshAccessToken(): Promise<AuthTokens> {
  const refreshToken = useAuthStore.getState().refreshToken;
  if (!refreshToken) {
    throw new Error("No refresh token");
  }
  const { data } = await axios.post<ApiResponse<AuthTokens>>(
    `${baseURL}${API_BASE}/auth/refresh`,
    { refreshToken }
  );
  if (!data.success || !data.data) throw new Error("Refresh failed");
  useAuthStore.getState().setTokens(data.data);
  return data.data;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };
    const status = error.response?.status;
    const url = original?.url ?? "";

    const isAuthEndpoint = NO_AUTH_PATHS.some((p) => url.includes(p));

    if (status === 401 && !original?._retry && !isAuthEndpoint) {
      original._retry = true;
      try {
        if (!refreshPromise) {
          refreshPromise = refreshAccessToken().finally(() => {
            refreshPromise = null;
          });
        }
        const tokens = await refreshPromise;
        if (original.headers) {
          original.headers.Authorization = `Bearer ${tokens.accessToken}`;
        }
        return api(original);
      } catch {
        useAuthStore.getState().clear();
        if (typeof window !== "undefined") {
          const next = encodeURIComponent(
            window.location.pathname + window.location.search
          );
          window.location.href = `/login?next=${next}`;
        }
      }
    }

    return Promise.reject(error);
  }
);

export type ApiErrorShape = {
  message: string;
  errorCode?: string;
  status?: number;
  fieldErrors?: { field?: string; message: string }[];
};

const ERROR_CODE_MESSAGES: Record<string, string> = {
  // Auth & user
  EMAIL_ALREADY_EXISTS: "Bu e-posta zaten kayıtlı. Giriş yapmayı dene.",
  PHONE_ALREADY_EXISTS: "Bu telefon numarası kullanılamaz.",
  INVALID_CREDENTIALS: "E-posta veya şifre hatalı.",
  INVALID_TOKEN: "Oturumun süresi doldu, tekrar giriş yap.",
  ACCESS_DENIED: "Bu işlem için yetkin yok.",
  ACCOUNT_DISABLED: "Hesabınız askıya alındı. Lütfen yöneticiyle iletişime geçin.",
  ACCOUNT_LOCKED: "Hesabınız kilitlenmiş. Lütfen yöneticiyle iletişime geçin.",
  USER_NOT_FOUND: "Kullanıcı bulunamadı.",
  ADDRESS_NOT_FOUND: "Adres bulunamadı.",

  // Cart & order
  CART_ITEM_NOT_FOUND: "Bu ürün sepetinde değil.",
  CART_ITEM_QUANTITY_LIMIT_EXCEEDED:
    "Bir üründen sepete en fazla 10 adet ekleyebilirsin.",
  EMPTY_CART: "Sepetin boş. Önce ürün ekle.",
  ORDER_NOT_FOUND: "Sipariş bulunamadı.",
  INVALID_ORDER_STATE: "Sipariş bu aşamada işlem yapmaya uygun değil.",
  PRODUCT_SNAPSHOT_MISMATCH:
    "Sepetindeki bir ürün artık sistemde yok. Sepetini gözden geçir.",
  PAYMENT_NOT_FOUND: "Ödeme kaydı bulunamadı.",

  // Catalog
  PRODUCT_NOT_FOUND: "Ürün bulunamadı veya kaldırılmış.",
  CATEGORY_NOT_FOUND: "Kategori bulunamadı.",
  TAG_NOT_FOUND: "Etiket bulunamadı.",
  SLUG_ALREADY_EXISTS: "Bu slug zaten kullanılıyor.",
  SLUG_GENERATION_FAILED: "Slug oluşturulamadı, başlığı değiştirip tekrar dene.",
  TARGET_CATEGORY_REQUIRED:
    "Bu kategoride aktif ürünler var. Önce taşınacak hedef kategoriyi seç.",
  INVALID_TARGET_CATEGORY:
    "Hedef kategori geçersiz (bulunamadı, pasif veya kaynakla aynı).",

  // Stock
  INSUFFICIENT_STOCK: "Stok yetersiz, ürünü daha az adetle tekrar dene.",
  STOCK_NOT_FOUND: "Stok kaydı bulunamadı.",
  STOCK_ALREADY_EXISTS: "Bu ürün için stok kaydı zaten mevcut.",
  INVALID_STOCK_QUANTITY: "Geçersiz stok miktarı.",

  // Generic / cross-cutting
  VALIDATION_ERROR: "Form bilgileri geçerli değil.",
  DUPLICATE_ENTRY: "Bu kayıt zaten mevcut.",
  NOT_FOUND: "Aradığın şey bulunamadı.",
  MALFORMED_REQUEST: "Geçersiz istek formatı.",
  INTERNAL_ERROR: "Sunucu hatası, lütfen tekrar dene.",
  GATEWAY_ERROR: "Servis hatası, az sonra tekrar dene.",

  // Service availability (gateway circuit breaker fallback)
  CART_SERVICE_UNAVAILABLE:
    "Sepet servisine ulaşılamıyor, az sonra tekrar dene.",
  PRODUCT_SERVICE_UNAVAILABLE:
    "Ürün servisine ulaşılamıyor, az sonra tekrar dene.",
  STOCK_SERVICE_UNAVAILABLE:
    "Stok bilgisi alınamıyor, lütfen tekrar dene.",
  USER_SERVICE_UNAVAILABLE:
    "Kullanıcı servisine ulaşılamıyor, az sonra tekrar dene.",
  SERVICE_UNAVAILABLE: "Servis şu an meşgul. Lütfen biraz sonra tekrar dene.",
};

export function extractApiError(error: unknown): ApiErrorShape {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiResponse<unknown> | undefined;
    const code = data?.errorCode;
    const friendly = code ? ERROR_CODE_MESSAGES[code] : undefined;
    const fieldErrors =
      data?.errors && data.errors.length > 0 ? data.errors : undefined;

    // VALIDATION_ERROR: generic mesaj yerine alan-spesifik mesajı göster
    let message: string;
    if (code === "VALIDATION_ERROR" && fieldErrors && fieldErrors.length > 0) {
      message = fieldErrors.map((e) => e.message).join(" • ");
    } else {
      message =
        friendly ??
        data?.message ??
        error.message ??
        "Beklenmeyen bir hata oluştu";
    }

    return {
      message,
      errorCode: code,
      status: error.response?.status,
      fieldErrors,
    };
  }
  if (error instanceof Error) {
    return { message: error.message };
  }
  return { message: "Beklenmeyen bir hata oluştu" };
}

export function notifyApiError(error: unknown, fallback = "Bir hata oluştu") {
  const { message } = extractApiError(error);
  toast.error(message || fallback);
}

export function unwrap<T>(res: { data: ApiResponse<T> }): T {
  return res.data.data;
}
