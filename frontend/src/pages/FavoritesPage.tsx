import { Link } from "react-router-dom";
import { Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FavoritesList } from "@/features/cart/FavoritesList";
import { useFavoritesStore } from "@/store/favoritesStore";
import { usePageTitle } from "@/hooks/usePageTitle";

export default function FavoritesPage() {
  usePageTitle("Favorilerim");
  const items = useFavoritesStore((s) => s.items);

  if (items.length === 0) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-md rounded-lg border bg-white p-10 text-center">
          <Heart className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
          <h2 className="mb-2 text-xl font-semibold">Favorilerin boş</h2>
          <p className="mb-6 text-sm text-muted-foreground">
            Beğendiğin ürünleri favorilere ekleyerek sonra kolayca bulabilirsin.
          </p>
          <Button asChild>
            <Link to="/products">Ürünleri Keşfet</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-6">
      <h1 className="mb-2 text-2xl font-semibold">
        Favorilerim ({items.length})
      </h1>
      <p className="mb-6 text-sm text-muted-foreground">
        Favoriler tarayıcında saklanır.
      </p>
      <FavoritesList showHeader={false} topMargin={false} />
    </div>
  );
}
