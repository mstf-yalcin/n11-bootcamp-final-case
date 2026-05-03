import { create } from "zustand";
import { persist } from "zustand/middleware";
import { MAX_QUANTITY_PER_CART_ITEM } from "@/lib/cart-constants";
import type { AnonymousCartItem } from "@/types/api";

type AddResult = {
  added: number;
  newQuantity: number;
  capped: boolean;
};

type State = {
  items: AnonymousCartItem[];
  addItem: (
    productId: string,
    quantity?: number,
    availableQuantity?: number | null
  ) => AddResult;
  updateItem: (productId: string, quantity: number) => void;
  removeItem: (productId: string) => void;
  clear: () => void;
  totalCount: () => number;
};

function effectiveCap(availableQuantity?: number | null): number {
  if (availableQuantity == null || availableQuantity < 0) {
    return MAX_QUANTITY_PER_CART_ITEM;
  }
  return Math.min(MAX_QUANTITY_PER_CART_ITEM, availableQuantity);
}

export const useAnonymousCartStore = create<State>()(
  persist(
    (set, get) => ({
      items: [],
      addItem: (productId, quantity = 1, availableQuantity) => {
        const cap = effectiveCap(availableQuantity);
        const existing = get().items.find((i) => i.productId === productId);
        const current = existing?.quantity ?? 0;
        const requested = current + quantity;
        const newQuantity = Math.max(0, Math.min(requested, cap));
        const added = newQuantity - current;
        const capped = newQuantity < requested;

        set((state) => {
          if (existing) {
            return {
              items: state.items.map((i) =>
                i.productId === productId
                  ? { ...i, quantity: newQuantity }
                  : i
              ),
            };
          }
          if (newQuantity <= 0) return state;
          return {
            items: [...state.items, { productId, quantity: newQuantity }],
          };
        });

        return { added, newQuantity, capped };
      },
      updateItem: (productId, quantity) =>
        set((state) => ({
          items:
            quantity <= 0
              ? state.items.filter((i) => i.productId !== productId)
              : state.items.map((i) =>
                  i.productId === productId
                    ? {
                        ...i,
                        quantity: Math.min(quantity, MAX_QUANTITY_PER_CART_ITEM),
                      }
                    : i
                ),
        })),
      removeItem: (productId) =>
        set((state) => ({
          items: state.items.filter((i) => i.productId !== productId),
        })),
      clear: () => set({ items: [] }),
      totalCount: () =>
        get().items.reduce((sum, item) => sum + item.quantity, 0),
    }),
    { name: "n11-anon-cart" }
  )
);
