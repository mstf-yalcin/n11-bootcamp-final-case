import { Outlet } from "react-router-dom";
import { Header } from "./Header";
import { CategoryBar } from "./CategoryBar";
import { Footer } from "./Footer";
import { TopProgressBar } from "@/components/TopProgressBar";
import { ScrollToTop } from "@/components/ScrollToTop";

export function RootLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <ScrollToTop />
      <TopProgressBar />
      <Header />
      <CategoryBar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
