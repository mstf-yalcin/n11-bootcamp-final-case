import { create } from "zustand";
import { persist } from "zustand/middleware";

const MAX_ITEMS = 10;

type State = {
  ids: string[];
  push: (productId: string) => void;
  remove: (productId: string) => void;
  clear: () => void;
};

export const useRecentlyViewedStore = create<State>()(
  persist(
    (set) => ({
      ids: [],
      push: (productId) =>
        set((state) => {
          const without = state.ids.filter((id) => id !== productId);
          return { ids: [productId, ...without].slice(0, MAX_ITEMS) };
        }),
      remove: (productId) =>
        set((state) => ({ ids: state.ids.filter((id) => id !== productId) })),
      clear: () => set({ ids: [] }),
    }),
    { name: "n11-recently-viewed" }
  )
);
