import { Outlet } from "react-router-dom";
import { Header } from "./Header";
import { CategoryBar } from "./CategoryBar";
import { Footer } from "./Footer";
import { useEffect } from "react";
import { useAuthStore } from "@/store/authStore";
import { useQuery } from "@tanstack/react-query";
import { authApi } from "@/api/endpoints";
import { TopProgressBar } from "@/components/TopProgressBar";
import { ScrollToTop } from "@/components/ScrollToTop";

function HydrateUser() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const setUser = useAuthStore((s) => s.setUser);
  const { data } = useQuery({
    queryKey: ["auth", "me"],
    queryFn: authApi.me,
    enabled: Boolean(accessToken),
    staleTime: 5 * 60 * 1000,
  });
  useEffect(() => {
    if (data) setUser(data);
  }, [data, setUser]);
  return null;
}

export function RootLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <ScrollToTop />
      <TopProgressBar />
      <HydrateUser />
      <Header />
      <CategoryBar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
