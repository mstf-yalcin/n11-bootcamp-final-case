import { useQuery } from "@tanstack/react-query";
import { productApi } from "@/api/endpoints";

export type ProductLookup = {
  slug: string;
  imageUrl?: string;
  name: string;
};

export function useProductLookups(productIds: string[]) {
  const sortedKey = [...productIds].sort().join(",");
  const query = useQuery({
    queryKey: ["products", "lookups", sortedKey],
    queryFn: () => productApi.byIds(productIds),
    enabled: productIds.length > 0,
    staleTime: 60_000,
  });

  const map = new Map<string, ProductLookup>(
    (query.data ?? []).map((p) => [
      p.id,
      { slug: p.slug, imageUrl: p.imageUrl, name: p.name },
    ])
  );

  return { lookups: map, isLoading: query.isLoading };
}
