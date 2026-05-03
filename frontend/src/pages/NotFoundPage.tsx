import { Link } from "react-router-dom";
import { Frown, Home, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { usePageTitle } from "@/hooks/usePageTitle";

export default function NotFoundPage() {
  usePageTitle("Sayfa bulunamadı");
  return (
    <div className="container flex min-h-[60vh] items-center justify-center">
      <div className="w-full max-w-md rounded-lg border bg-white p-10 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-n11/10">
          <Frown className="h-9 w-9 text-n11" />
        </div>
        <div className="mb-1 text-5xl font-bold text-n11">404</div>
        <h1 className="mb-2 text-xl font-semibold">Sayfa bulunamadı</h1>
        <p className="mb-6 text-sm text-muted-foreground">
          Aradığın sayfa taşınmış veya hiç var olmamış olabilir. Anasayfaya
          dön ya da arama yap.
        </p>
        <div className="flex flex-col gap-2 sm:flex-row sm:justify-center">
          <Button asChild>
            <Link to="/">
              <Home className="mr-2 h-4 w-4" />
              Anasayfa
            </Link>
          </Button>
          <Button asChild variant="outline">
            <Link to="/products">
              <Search className="mr-2 h-4 w-4" />
              Ürünleri Keşfet
            </Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
