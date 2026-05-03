import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { authApi, cartApi } from "@/api/endpoints";
import { extractApiError, notifyApiError } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import { useAnonymousCartStore } from "@/store/anonymousCartStore";
import type { LoginRequest, RegisterRequest } from "@/types/api";

export const meQueryKey = ["auth", "me"] as const;

export function useMe() {
  const accessToken = useAuthStore((s) => s.accessToken);
  return useQuery({
    queryKey: meQueryKey,
    queryFn: authApi.me,
    enabled: Boolean(accessToken),
    staleTime: 5 * 60 * 1000,
  });
}

export function useLogin() {
  const setTokens = useAuthStore((s) => s.setTokens);
  const setUser = useAuthStore((s) => s.setUser);
  const queryClient = useQueryClient();
  const anonItems = useAnonymousCartStore((s) => s.items);
  const clearAnon = useAnonymousCartStore((s) => s.clear);
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (body: LoginRequest) => authApi.login(body),
    onSuccess: async (tokens) => {
      setTokens(tokens);
      try {
        const me = await authApi.me();
        setUser(me);
      } catch {
        /* ignore — token good but profile fetch failed */
      }

      if (anonItems.length > 0) {
        try {
          const result = await cartApi.merge(anonItems);
          clearAnon();
          if (result.skippedProductIds.length > 0) {
            toast.warning(
              `${result.skippedProductIds.length} ürün artık mevcut olmadığı için sepete eklenemedi.`
            );
          }
        } catch {
          /* swallow — cart merge non-fatal */
        }
      }

      queryClient.invalidateQueries({ queryKey: ["cart"] });
      toast.success("Giriş başarılı, hoş geldin!");
      const next = new URLSearchParams(window.location.search).get("next");
      navigate(next ?? "/", { replace: true });
    },
    onError: (err) => notifyApiError(err, "Giriş yapılamadı"),
  });
}

const REGISTER_FIELD_ERROR_CODES = new Set([
  "EMAIL_ALREADY_EXISTS",
  "PHONE_ALREADY_EXISTS",
]);

export function useRegister() {
  const setTokens = useAuthStore((s) => s.setTokens);
  const setUser = useAuthStore((s) => s.setUser);
  const queryClient = useQueryClient();
  const anonItems = useAnonymousCartStore((s) => s.items);
  const clearAnon = useAnonymousCartStore((s) => s.clear);
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (body: RegisterRequest) => authApi.register(body),
    onSuccess: async (tokens) => {
      setTokens(tokens);
      try {
        const me = await authApi.me();
        setUser(me);
      } catch {
        /* ignore */
      }

      if (anonItems.length > 0) {
        try {
          const result = await cartApi.merge(anonItems);
          clearAnon();
          if (result.skippedProductIds.length > 0) {
            toast.warning(
              `${result.skippedProductIds.length} ürün artık mevcut olmadığı için sepete eklenemedi.`
            );
          }
        } catch {
          /* swallow — cart merge non-fatal */
        }
      }

      queryClient.invalidateQueries({ queryKey: ["cart"] });
      toast.success("Kayıt başarılı, hoş geldin!");
      const next = new URLSearchParams(window.location.search).get("next");
      navigate(next ?? "/", { replace: true });
    },
    onError: (err) => {
      const { errorCode } = extractApiError(err);
      if (errorCode && REGISTER_FIELD_ERROR_CODES.has(errorCode)) {
        return;
      }
      notifyApiError(err, "Kayıt başarısız");
    },
  });
}

export function useLogout() {
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const clear = useAuthStore((s) => s.clear);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: async () => {
      if (refreshToken) {
        try {
          await authApi.logout(refreshToken);
        } catch {
          /* swallow — local logout still proceeds */
        }
      }
    },
    onSettled: () => {
      clear();
      queryClient.clear();
      toast.success("Çıkış yapıldı");
      navigate("/", { replace: true });
    },
  });
}
