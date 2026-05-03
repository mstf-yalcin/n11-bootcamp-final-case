import { create } from "zustand";
import { persist } from "zustand/middleware";

const MAX_ITEMS = 5;

type State = {
  items: string[];
  push: (term: string) => void;
  remove: (term: string) => void;
  clear: () => void;
};

export const useRecentSearchStore = create<State>()(
  persist(
    (set) => ({
      items: [],
      push: (term) =>
        set((state) => {
          const trimmed = term.trim();
          if (!trimmed) return state;
          const without = state.items.filter(
            (i) => i.toLowerCase() !== trimmed.toLowerCase()
          );
          return { items: [trimmed, ...without].slice(0, MAX_ITEMS) };
        }),
      remove: (term) =>
        set((state) => ({
          items: state.items.filter(
            (i) => i.toLowerCase() !== term.toLowerCase()
          ),
        })),
      clear: () => set({ items: [] }),
    }),
    { name: "n11-recent-searches" }
  )
);
