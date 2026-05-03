import { Heart } from "lucide-react";
import { toast } from "sonner";
import { useFavoritesStore } from "@/store/favoritesStore";
import type { Product } from "@/types/api";
import { cn } from "@/lib/utils";

type Props = {
  product: Pick<Product, "id" | "slug" | "name" | "price" | "currency" | "imageUrl">;
  size?: "sm" | "md" | "lg";
  variant?: "icon" | "button";
  className?: string;
};

export function FavoriteButton({
  product,
  size = "md",
  variant = "icon",
  className,
}: Props) {
  const has = useFavoritesStore((s) => s.has(product.id));
  const add = useFavoritesStore((s) => s.add);
  const remove = useFavoritesStore((s) => s.remove);

  const toggle = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (has) {
      remove(product.id);
      toast.success("Favorilerden çıkarıldı");
    } else {
      add({
        productId: product.id,
        name: product.name,
        slug: product.slug,
        price: Number(product.price),
        currency: product.currency,
        imageUrl: product.imageUrl,
      });
      toast.success("Favorilere eklendi");
    }
  };

  const iconSize = size === "sm" ? "h-3.5 w-3.5" : size === "lg" ? "h-5 w-5" : "h-4 w-4";

  if (variant === "button") {
    return (
      <button
        onClick={toggle}
        aria-label={has ? "Favorilerden çıkar" : "Favorilere ekle"}
        aria-pressed={has}
        className={cn(
          "inline-flex items-center gap-2 rounded-md border border-input px-4 py-2 text-sm font-medium transition-colors",
          has
            ? "border-n11/40 bg-n11/10 text-n11 hover:bg-n11/15"
            : "bg-white hover:bg-accent",
          className
        )}
      >
        <Heart
          className={cn(iconSize, has && "fill-current")}
        />
        {has ? "Favorilerimde" : "Favorilere Ekle"}
      </button>
    );
  }

  const buttonSize =
    size === "sm" ? "h-7 w-7" : size === "lg" ? "h-10 w-10" : "h-8 w-8";

  return (
    <button
      onClick={toggle}
      aria-label={has ? "Favorilerden çıkar" : "Favorilere ekle"}
      aria-pressed={has}
      className={cn(
        "flex flex-shrink-0 items-center justify-center rounded-full border bg-white shadow-sm transition-colors",
        has
          ? "border-n11 text-n11"
          : "border-input text-muted-foreground hover:border-n11 hover:text-n11",
        buttonSize,
        className
      )}
    >
      <Heart
        className={cn(iconSize, has && "fill-current")}
      />
    </button>
  );
}
