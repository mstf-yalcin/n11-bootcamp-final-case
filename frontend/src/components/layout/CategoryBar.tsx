import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { categoryApi } from "@/api/endpoints";
import { Spinner } from "@/components/ui/spinner";

export function CategoryBar() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
    staleTime: 5 * 60 * 1000,
  });

  return (
    <div className="border-b bg-white">
      <div className="container flex items-center gap-1 overflow-x-auto py-2">
        {isLoading && <Spinner />}
        {isError && (
          <span className="text-xs text-muted-foreground">
            Kategoriler yüklenemedi
          </span>
        )}
        {data?.map((cat) => (
          <Link
            key={cat.id}
            to={`/products?category=${cat.slug}`}
            className="flex items-center gap-2 whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium hover:bg-accent hover:text-n11"
          >
            {cat.imageUrl ? (
              <img
                src={cat.imageUrl}
                alt=""
                loading="lazy"
                className="h-6 w-8 rounded-sm object-cover ring-1 ring-border"
              />
            ) : (
              <div className="h-6 w-8 rounded-sm bg-muted" />
            )}
            {cat.name}
          </Link>
        ))}
      </div>
    </div>
  );
}
