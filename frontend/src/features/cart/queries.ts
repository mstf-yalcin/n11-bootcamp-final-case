import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { cartApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import { useAnonymousCartStore } from "@/store/anonymousCartStore";
import { useFavoritesStore } from "@/store/favoritesStore";
import { MAX_QUANTITY_PER_CART_ITEM } from "@/lib/cart-constants";

export const cartQueryKey = ["cart"] as const;

export function useCart() {
  const accessToken = useAuthStore((s) => s.accessToken);
  return useQuery({
    queryKey: cartQueryKey,
    queryFn: cartApi.get,
    enabled: Boolean(accessToken),
  });
}

type AddVars = {
  productId: string;
  quantity: number;
  availableQuantity?: number | null;
  currentInCart?: number;
};

// Trendyol/n11 hissi: butonun loading state'i en az bu kadar görünsün ki
// spam önlensin ve "sepete eklendi" feedback'i fark edilsin.
const MIN_ADD_TO_CART_FEEDBACK_MS = 450;
// Sepetteki adet güncelleme — spinner kullanıcının fark edebileceği kadar görünsün.
// Add'e göre daha kısa, çünkü kullanıcı zaten sepette ne yaptığını biliyor.
const MIN_CART_UPDATE_FEEDBACK_MS = 250;

async function withMinDuration<T>(promise: Promise<T>, minMs: number): Promise<T> {
  const start = Date.now();
  const result = await promise;
  const elapsed = Date.now() - start;
  if (elapsed < minMs) {
    await new Promise((r) => setTimeout(r, minMs - elapsed));
  }
  return result;
}

export function useAddToCart() {
  const queryClient = useQueryClient();
  const accessToken = useAuthStore((s) => s.accessToken);
  const addAnon = useAnonymousCartStore((s) => s.addItem);
  const removeFavorite = useFavoritesStore((s) => s.remove);

  return useMutation({
    mutationFn: async (vars: AddVars) => {
      const cap =
        vars.availableQuantity != null && vars.availableQuantity >= 0
          ? Math.min(MAX_QUANTITY_PER_CART_ITEM, vars.availableQuantity)
          : MAX_QUANTITY_PER_CART_ITEM;
      const current = vars.currentInCart ?? 0;
      const remaining = Math.max(0, cap - current);
      const askedFor = vars.quantity;
      const toAdd = Math.min(askedFor, remaining);

      if (toAdd <= 0) {
        return { capped: true, added: 0, finalQuantity: current };
      }

      const work = (async () => {
        if (accessToken) {
          await cartApi.addItem(vars.productId, toAdd);
        } else {
          addAnon(vars.productId, toAdd, vars.availableQuantity ?? null);
        }
      })();

      await withMinDuration(work, MIN_ADD_TO_CART_FEEDBACK_MS);

      return {
        capped: toAdd < askedFor,
        added: toAdd,
        finalQuantity: current + toAdd,
      };
    },
    onSuccess: (result, vars) => {
      queryClient.invalidateQueries({ queryKey: cartQueryKey });
      if (!result) return;
      if (result.added > 0) {
        // Sepete giren ürünü favorilerden çıkar — "sonra al" listesi olduğu için
        // sepete girdiğinde duplikasyon olmamalı.
        removeFavorite(vars.productId);
      }
      if (result.added > 0 && !result.capped) {
        toast.success("Ürün sepete eklendi");
      } else if (result.added > 0 && result.capped) {
        toast.success(
          `${result.added} adet sepete eklendi (mevcut stok kadar).`
        );
      } else {
        toast.info("Bu üründen sepetinde maksimum adet zaten mevcut.");
      }
    },
    onError: (err) => notifyApiError(err, "Sepete eklenirken hata oluştu"),
  });
}

export function useUpdateCartItem() {
  const queryClient = useQueryClient();
  const accessToken = useAuthStore((s) => s.accessToken);
  const updateAnon = useAnonymousCartStore((s) => s.updateItem);

  return useMutation({
    mutationFn: async (vars: { productId: string; quantity: number }) => {
      const work = (async () => {
        if (accessToken) {
          return cartApi.updateItem(vars.productId, vars.quantity);
        }
        updateAnon(vars.productId, vars.quantity);
        return null;
      })();
      return withMinDuration(work, MIN_CART_UPDATE_FEEDBACK_MS);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartQueryKey });
    },
    onError: (err) => notifyApiError(err, "Güncellenemedi"),
  });
}

export function useAddManyToCart() {
  const queryClient = useQueryClient();
  const accessToken = useAuthStore((s) => s.accessToken);
  const addAnon = useAnonymousCartStore((s) => s.addItem);
  const removeFavorite = useFavoritesStore((s) => s.remove);

  return useMutation({
    mutationFn: async (items: { productId: string; quantity: number }[]) => {
      if (items.length === 0) {
        return { added: 0, skipped: [] as string[] };
      }
      const work = (async () => {
        if (accessToken) {
          const result = await cartApi.merge(items);
          return {
            added: items.length - result.skippedProductIds.length,
            skipped: result.skippedProductIds,
          };
        }
        for (const it of items) {
          addAnon(it.productId, it.quantity, null);
        }
        return { added: items.length, skipped: [] as string[] };
      })();
      return withMinDuration(work, MIN_ADD_TO_CART_FEEDBACK_MS);
    },
    onSuccess: (result, vars) => {
      queryClient.invalidateQueries({ queryKey: cartQueryKey });
      // Sepete giren her ürünü favorilerden çıkar — skipped (artık var olmayan)
      // ürünler favoride bırakılır, kullanıcı manuel temizleyebilir.
      const skippedSet = new Set(result.skipped);
      for (const it of vars) {
        if (!skippedSet.has(it.productId)) removeFavorite(it.productId);
      }
      if (result.added > 0) {
        toast.success(
          result.skipped.length > 0
            ? `${result.added} ürün sepete eklendi (${result.skipped.length} ürün artık mevcut değil).`
            : `${result.added} ürün sepete eklendi.`
        );
      }
    },
    onError: (err) => notifyApiError(err, "Sepete eklenirken hata oluştu"),
  });
}

export function useRemoveCartItem() {
  const queryClient = useQueryClient();
  const accessToken = useAuthStore((s) => s.accessToken);
  const removeAnon = useAnonymousCartStore((s) => s.removeItem);

  return useMutation({
    mutationFn: async (productId: string) => {
      if (accessToken) {
        return cartApi.removeItem(productId);
      }
      removeAnon(productId);
      return null;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartQueryKey });
      toast.success("Ürün sepetten çıkarıldı");
    },
    onError: (err) => notifyApiError(err, "Silinemedi"),
  });
}
