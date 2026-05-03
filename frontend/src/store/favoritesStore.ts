import { create } from "zustand";
import { persist } from "zustand/middleware";

const MAX_ITEMS = 50;

export type FavoriteItem = {
  productId: string;
  name: string;
  slug: string;
  imageUrl?: string;
  price: number;
  currency: string;
  addedAt: number;
};

type State = {
  items: FavoriteItem[];
  add: (item: Omit<FavoriteItem, "addedAt">) => void;
  remove: (productId: string) => void;
  clear: () => void;
  has: (productId: string) => boolean;
};

export const useFavoritesStore = create<State>()(
  persist(
    (set, get) => ({
      items: [],
      add: (item) =>
        set((state) => {
          const without = state.items.filter(
            (i) => i.productId !== item.productId
          );
          return {
            items: [{ ...item, addedAt: Date.now() }, ...without].slice(
              0,
              MAX_ITEMS
            ),
          };
        }),
      remove: (productId) =>
        set((state) => ({
          items: state.items.filter((i) => i.productId !== productId),
        })),
      clear: () => set({ items: [] }),
      has: (productId) => get().items.some((i) => i.productId === productId),
    }),
    { name: "n11-favorites" }
  )
);
