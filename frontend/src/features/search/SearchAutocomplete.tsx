import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent,
} from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Clock, History, Search, Tag, X } from "lucide-react";
import { categoryApi, productApi } from "@/api/endpoints";
import { useDebounce } from "@/hooks/useDebounce";
import { useRecentSearchStore } from "@/store/recentSearchStore";
import { Spinner } from "@/components/ui/spinner";
import { formatTRY, cn } from "@/lib/utils";

const FALLBACK_IMG = "https://placehold.co/64x64/fff0fe/ff25f5?text=n11";
const MIN_CHARS = 2;
const MAX_PRODUCTS = 5;
const MAX_CATEGORIES = 3;

type Suggestion =
  | { kind: "category"; id: string; slug: string; label: string }
  | {
      kind: "product";
      id: string;
      slug: string;
      label: string;
      imageUrl?: string;
      price: number;
      currency: string;
      categoryName: string;
    }
  | { kind: "recent"; term: string }
  | { kind: "viewAll"; query: string };

export function SearchAutocomplete() {
  const navigate = useNavigate();
  const location = useLocation();
  const urlQ =
    new URLSearchParams(location.search).get("q") ?? "";
  const [query, setQuery] = useState(urlQ);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setQuery(urlQ);
  }, [urlQ]);

  const debouncedQuery = useDebounce(query.trim(), 300);
  const isSearching = debouncedQuery.length >= MIN_CHARS;

  const recentItems = useRecentSearchStore((s) => s.items);
  const pushRecent = useRecentSearchStore((s) => s.push);
  const removeRecent = useRecentSearchStore((s) => s.remove);
  const clearRecents = useRecentSearchStore((s) => s.clear);

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
    staleTime: 5 * 60 * 1000,
  });

  const productsQuery = useQuery({
    queryKey: ["search-suggest", debouncedQuery],
    queryFn: () =>
      productApi.list({ search: debouncedQuery, size: MAX_PRODUCTS }),
    enabled: isSearching,
    staleTime: 30_000,
  });

  // Build suggestion list
  const suggestions = useMemo<Suggestion[]>(() => {
    if (!isSearching) {
      return recentItems.map((term) => ({ kind: "recent", term }) as const);
    }
    const list: Suggestion[] = [];
    const lower = debouncedQuery.toLowerCase();
    const matchedCats = (categoriesQuery.data ?? [])
      .filter((c) => c.name.toLowerCase().includes(lower))
      .slice(0, MAX_CATEGORIES)
      .map(
        (c) =>
          ({ kind: "category", id: c.id, slug: c.slug, label: c.name }) as const
      );
    list.push(...matchedCats);

    const products = productsQuery.data?.items ?? [];
    list.push(
      ...products.slice(0, MAX_PRODUCTS).map(
        (p) =>
          ({
            kind: "product",
            id: p.id,
            slug: p.slug,
            label: p.name,
            imageUrl: p.imageUrl,
            price: Number(p.price),
            currency: p.currency,
            categoryName: p.categoryName,
          }) as const
      )
    );
    list.push({ kind: "viewAll", query: debouncedQuery });
    return list;
  }, [
    isSearching,
    debouncedQuery,
    categoriesQuery.data,
    productsQuery.data,
    recentItems,
  ]);

  // Reset active index when suggestions change
  useEffect(() => {
    setActiveIndex(-1);
  }, [suggestions.length]);

  // Close on click outside
  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const submitSearch = (term: string) => {
    const t = term.trim();
    if (!t) {
      navigate("/products");
    } else {
      pushRecent(t);
      navigate(`/products?q=${encodeURIComponent(t)}`);
    }
    setOpen(false);
    setQuery(t);
    inputRef.current?.blur();
  };

  const selectSuggestion = (s: Suggestion) => {
    switch (s.kind) {
      case "recent":
        submitSearch(s.term);
        return;
      case "viewAll":
        submitSearch(s.query);
        return;
      case "category":
        navigate(`/products?category=${s.slug}`);
        setOpen(false);
        setQuery("");
        inputRef.current?.blur();
        return;
      case "product":
        navigate(`/products/${s.slug}`);
        setOpen(false);
        setQuery("");
        inputRef.current?.blur();
        return;
    }
  };

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (!open) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((i) => (i + 1) % Math.max(1, suggestions.length));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((i) =>
        i <= 0 ? suggestions.length - 1 : i - 1
      );
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (activeIndex >= 0 && suggestions[activeIndex]) {
        selectSuggestion(suggestions[activeIndex]);
      } else {
        submitSearch(query);
      }
    } else if (e.key === "Escape") {
      setOpen(false);
      inputRef.current?.blur();
    }
  };

  const showRecents = !isSearching && recentItems.length > 0;
  const showEmpty =
    isSearching &&
    !productsQuery.isLoading &&
    suggestions.filter((s) => s.kind !== "viewAll").length === 0;

  return (
    <div ref={containerRef} className="relative w-full">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitSearch(query);
        }}
      >
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              if (!open) setOpen(true);
            }}
            onFocus={() => setOpen(true)}
            onKeyDown={onKeyDown}
            placeholder="Ürün, kategori, marka ara"
            autoComplete="off"
            className="h-11 w-full rounded-full border border-input bg-secondary/40 pl-10 pr-10 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-n11"
          />
          {query && (
            <button
              type="button"
              onClick={() => {
                setQuery("");
                inputRef.current?.focus();
              }}
              aria-label="Aramayı temizle"
              className="absolute right-3 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-full text-muted-foreground hover:bg-accent hover:text-foreground"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      </form>

      {open && (showRecents || isSearching) && (
        <div className="absolute left-0 right-0 top-[calc(100%+6px)] z-50 max-h-[60vh] overflow-y-auto rounded-lg border bg-white shadow-xl">
          {showRecents && (
            <div>
              <div className="flex items-center justify-between px-3 pt-3 pb-1">
                <span className="text-[10px] font-semibold uppercase text-muted-foreground">
                  Son aramaların
                </span>
                <button
                  type="button"
                  onClick={clearRecents}
                  className="text-[11px] text-n11 hover:underline"
                >
                  Temizle
                </button>
              </div>
              {suggestions.map((s, i) =>
                s.kind === "recent" ? (
                  <SuggestionRow
                    key={`r-${s.term}`}
                    active={activeIndex === i}
                    onMouseEnter={() => setActiveIndex(i)}
                    onClick={() => selectSuggestion(s)}
                    leading={<History className="h-4 w-4 text-muted-foreground" />}
                    trailing={
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          removeRecent(s.term);
                        }}
                        className="rounded p-1 text-muted-foreground hover:text-foreground"
                        aria-label="Geçmişten kaldır"
                      >
                        <X className="h-3.5 w-3.5" />
                      </button>
                    }
                  >
                    {s.term}
                  </SuggestionRow>
                ) : null
              )}
            </div>
          )}

          {isSearching && (
            <>
              {productsQuery.isLoading && (
                <div className="flex items-center justify-center gap-2 px-3 py-6 text-sm text-muted-foreground">
                  <Spinner size={14} /> Aranıyor...
                </div>
              )}

              {showEmpty && (
                <div className="px-3 py-6 text-center text-sm text-muted-foreground">
                  "{debouncedQuery}" için sonuç bulunamadı
                </div>
              )}

              {/* Categories */}
              {suggestions.some((s) => s.kind === "category") && (
                <div className="px-3 pt-3 pb-1">
                  <span className="text-[10px] font-semibold uppercase text-muted-foreground">
                    Kategoriler
                  </span>
                </div>
              )}
              {suggestions.map((s, i) =>
                s.kind === "category" ? (
                  <SuggestionRow
                    key={`c-${s.id}`}
                    active={activeIndex === i}
                    onMouseEnter={() => setActiveIndex(i)}
                    onClick={() => selectSuggestion(s)}
                    leading={<Tag className="h-4 w-4 text-n11" />}
                  >
                    {s.label}
                    <span className="ml-1 text-xs text-muted-foreground">
                      kategorisi
                    </span>
                  </SuggestionRow>
                ) : null
              )}

              {/* Products */}
              {suggestions.some((s) => s.kind === "product") && (
                <div className="px-3 pt-3 pb-1">
                  <span className="text-[10px] font-semibold uppercase text-muted-foreground">
                    Ürünler
                  </span>
                </div>
              )}
              {suggestions.map((s, i) =>
                s.kind === "product" ? (
                  <SuggestionRow
                    key={`p-${s.id}`}
                    active={activeIndex === i}
                    onMouseEnter={() => setActiveIndex(i)}
                    onClick={() => selectSuggestion(s)}
                    leading={
                      <img
                        src={s.imageUrl || FALLBACK_IMG}
                        alt=""
                        onError={(e) => {
                          (e.target as HTMLImageElement).src = FALLBACK_IMG;
                        }}
                        className="h-9 w-9 flex-shrink-0 rounded object-cover"
                      />
                    }
                    trailing={
                      <span className="text-sm font-bold text-foreground">
                        {formatTRY(s.price, s.currency)}
                      </span>
                    }
                  >
                    <div className="flex flex-col">
                      <span className="line-clamp-1">{s.label}</span>
                      <span className="text-[11px] text-muted-foreground">
                        {s.categoryName}
                      </span>
                    </div>
                  </SuggestionRow>
                ) : null
              )}

              {/* View all */}
              {suggestions.some((s) => s.kind === "viewAll") &&
                !productsQuery.isLoading && (
                  <>
                    <div className="my-1 border-t" />
                    {suggestions.map((s, i) =>
                      s.kind === "viewAll" ? (
                        <SuggestionRow
                          key="va"
                          active={activeIndex === i}
                          onMouseEnter={() => setActiveIndex(i)}
                          onClick={() => selectSuggestion(s)}
                          leading={<Search className="h-4 w-4 text-n11" />}
                        >
                          <span className="font-medium text-n11">
                            "{s.query}" için tüm sonuçlar
                          </span>
                        </SuggestionRow>
                      ) : null
                    )}
                  </>
                )}
            </>
          )}

          {!isSearching && !showRecents && (
            <div className="px-3 py-6 text-center text-xs text-muted-foreground">
              <Clock className="mx-auto mb-1 h-4 w-4" />
              Aramaya başlamak için yazın...
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function SuggestionRow({
  children,
  leading,
  trailing,
  active,
  onClick,
  onMouseEnter,
}: {
  children: React.ReactNode;
  leading?: React.ReactNode;
  trailing?: React.ReactNode;
  active?: boolean;
  onClick?: () => void;
  onMouseEnter?: () => void;
}) {
  return (
    <button
      type="button"
      onMouseDown={(e) => e.preventDefault()} // input'tan blur'u önle
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      className={cn(
        "flex w-full items-center gap-3 px-3 py-2 text-left text-sm transition-colors",
        active && "bg-n11/10"
      )}
    >
      {leading}
      <div className="min-w-0 flex-1">{children}</div>
      {trailing}
    </button>
  );
}
