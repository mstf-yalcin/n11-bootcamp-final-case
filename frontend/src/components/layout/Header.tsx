import { Link } from "react-router-dom";
import { MapPin, ShoppingCart, User } from "lucide-react";
import { useAuthStore } from "@/store/authStore";
import { useAnonymousCartStore } from "@/store/anonymousCartStore";
import { useQuery } from "@tanstack/react-query";
import { addressApi, cartApi } from "@/api/endpoints";
import { useLogout } from "@/features/auth/queries";
import { Logo } from "@/components/Logo";
import { SearchAutocomplete } from "@/features/search/SearchAutocomplete";
import { Roles } from "@/types/api";

export function Header() {
  const user = useAuthStore((s) => s.user);
  const accessToken = useAuthStore((s) => s.accessToken);
  const anonCount = useAnonymousCartStore((s) => s.totalCount());
  const logout = useLogout();

  const cartQuery = useQuery({
    queryKey: ["cart"],
    queryFn: cartApi.get,
    enabled: Boolean(accessToken),
    staleTime: 30_000,
  });

  const addressesQuery = useQuery({
    queryKey: ["addresses"],
    queryFn: addressApi.list,
    enabled: Boolean(accessToken),
    staleTime: 60_000,
  });

  const cartCount = accessToken
    ? cartQuery.data?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0
    : anonCount;

  const selectedAddress =
    addressesQuery.data?.find((a) => a.isDefault) ?? addressesQuery.data?.[0];
  const addressLine = selectedAddress
    ? `${selectedAddress.title} • ${selectedAddress.city}`
    : "Adres Ekle";

  return (
    <header className="sticky top-0 z-40 border-b bg-white">
      <div className="container flex h-16 items-center gap-4">
        <Link to="/" className="flex flex-shrink-0 items-center gap-2">
          <Logo size={40} />
          <span className="hidden text-xl font-semibold tracking-tight sm:inline">
            n<span className="text-n11">11</span>
          </span>
        </Link>

        <div className="min-w-0 flex-1">
          <SearchAutocomplete />
        </div>

        <div className="flex flex-shrink-0 items-center gap-1">
          <Link
            to={accessToken ? "/account/addresses" : "/login"}
            className="hidden h-12 items-center gap-2 rounded-md px-2 text-sm hover:bg-accent md:flex"
            title={
              selectedAddress
                ? `${selectedAddress.title} — ${selectedAddress.fullAddress}, ${selectedAddress.city}`
                : "Teslimat adresi ekle"
            }
          >
            <MapPin className="h-5 w-5 flex-shrink-0" />
            <div className="leading-tight">
              <div className="text-[10px] uppercase text-muted-foreground">
                Teslimat
              </div>
              <div className="max-w-[160px] truncate text-xs font-medium">
                {addressLine}
              </div>
            </div>
          </Link>

          <Link
            to="/cart"
            className="relative flex h-12 w-12 items-center justify-center rounded-md hover:bg-accent"
            aria-label="Sepet"
          >
            <ShoppingCart className="h-5 w-5" />
            {cartCount > 0 && (
              <span className="absolute right-1 top-1 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-n11 px-1 text-[10px] font-bold text-white ring-2 ring-white">
                {cartCount > 99 ? "99+" : cartCount}
              </span>
            )}
          </Link>

          {accessToken ? (
            <div className="group relative">
              <button className="flex h-12 items-center gap-2 rounded-md px-2 hover:bg-accent">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-n11/10 text-sm font-semibold text-n11">
                  {user?.firstName?.[0]?.toUpperCase() ?? "?"}
                </div>
                <div className="hidden leading-tight md:block">
                  <div className="text-[10px] uppercase text-muted-foreground">
                    Hesabım
                  </div>
                  <div className="max-w-[100px] truncate text-xs font-medium">
                    {user?.firstName ?? "Hesap"}
                  </div>
                </div>
              </button>
              <div className="invisible absolute right-0 top-full z-50 w-48 rounded-md border bg-white p-1 opacity-0 shadow-lg transition-all group-hover:visible group-hover:opacity-100">
                <Link
                  to="/account/profile"
                  className="block rounded px-3 py-2 text-sm hover:bg-accent"
                >
                  Profilim
                </Link>
                <Link
                  to="/orders"
                  className="block rounded px-3 py-2 text-sm hover:bg-accent"
                >
                  Siparişlerim
                </Link>
                <Link
                  to="/account/addresses"
                  className="block rounded px-3 py-2 text-sm hover:bg-accent"
                >
                  Adreslerim
                </Link>
                <Link
                  to="/account/payments"
                  className="block rounded px-3 py-2 text-sm hover:bg-accent"
                >
                  Ödemelerim
                </Link>
                {user?.roles?.includes(Roles.ADMIN) && (
                  <>
                    <div className="my-1 border-t" />
                    <Link
                      to="/admin"
                      className="block rounded px-3 py-2 text-sm font-semibold text-n11 hover:bg-n11/10"
                    >
                      Admin Paneli
                    </Link>
                  </>
                )}
                <div className="my-1 border-t" />
                <button
                  onClick={() => logout.mutate()}
                  className="block w-full rounded px-3 py-2 text-left text-sm text-destructive hover:bg-destructive/10"
                >
                  Çıkış Yap
                </button>
              </div>
            </div>
          ) : (
            <Link
              to="/login"
              className="flex h-12 items-center gap-2 rounded-md px-2 text-sm hover:bg-accent"
            >
              <User className="h-5 w-5 flex-shrink-0" />
              <div className="hidden leading-tight md:block">
                <div className="text-[10px] uppercase text-muted-foreground">
                  Hesabım
                </div>
                <div className="text-xs font-medium">Üye Ol / Giriş Yap</div>
              </div>
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
