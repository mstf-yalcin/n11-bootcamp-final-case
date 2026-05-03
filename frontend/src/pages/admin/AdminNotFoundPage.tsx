import { Link } from "react-router-dom";
import { Frown, LayoutDashboard } from "lucide-react";
import { Button } from "@/components/ui/button";
import { usePageTitle } from "@/hooks/usePageTitle";

export default function AdminNotFoundPage() {
  usePageTitle("Admin · Sayfa bulunamadı");
  return (
    <div className="flex min-h-[60vh] items-center justify-center p-8">
      <div className="w-full max-w-md rounded-lg border bg-white p-10 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-n11/10">
          <Frown className="h-9 w-9 text-n11" />
        </div>
        <div className="mb-1 text-5xl font-bold text-n11">404</div>
        <h1 className="mb-2 text-xl font-semibold">Yönetim sayfası bulunamadı</h1>
        <p className="mb-6 text-sm text-muted-foreground">
          Aradığın yönetim sayfası mevcut değil. Soldaki menüden geçerli bir
          bölüme gidebilirsin.
        </p>
        <Button asChild>
          <Link to="/admin/dashboard">
            <LayoutDashboard className="mr-2 h-4 w-4" />
            Dashboard'a dön
          </Link>
        </Button>
      </div>
    </div>
  );
}
