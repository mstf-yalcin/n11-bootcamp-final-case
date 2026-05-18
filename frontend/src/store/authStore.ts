import { create } from "zustand";
import { persist } from "zustand/middleware";

type AuthState = {
  accessToken: string | null;
  setAccessToken: (token: string | null) => void;
  clear: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      setAccessToken: (accessToken) => set({ accessToken }),
      clear: () => set({ accessToken: null }),
    }),
    {
      name: "n11-auth",
      partialize: (state) => ({
        accessToken: state.accessToken,
      }),
    }
  )
);

export const isAuthenticated = () =>
  Boolean(useAuthStore.getState().accessToken);
