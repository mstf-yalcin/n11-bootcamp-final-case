import { NavLink, Outlet } from "react-router-dom";
import { CreditCard, MapPin, Package, User } from "lucide-react";
import { useAuthStore } from "@/store/authStore";
import { cn } from "@/lib/utils";

const NAV = [
  { to: "/account/profile", label: "Profilim", icon: User },
  { to: "/orders", label: "Siparişlerim", icon: Package },
  { to: "/account/addresses", label: "Adreslerim", icon: MapPin },
  { to: "/account/payments", label: "Ödemelerim", icon: CreditCard },
];

export function AccountLayout() {
  const user = useAuthStore((s) => s.user);
  return (
    <div className="container py-6">
      <div className="grid gap-6 lg:grid-cols-[260px_1fr]">
        <aside className="lg:sticky lg:top-20 lg:h-fit">
          <div className="rounded-lg border bg-white p-4">
            <div className="flex items-center gap-3 border-b pb-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-n11/10 font-semibold text-n11">
                {user?.firstName?.[0]?.toUpperCase() ?? "?"}
              </div>
              <div className="min-w-0 flex-1">
                <div className="truncate font-semibold">
                  {user?.firstName} {user?.lastName}
                </div>
                <div className="truncate text-xs text-muted-foreground">
                  {user?.email}
                </div>
              </div>
            </div>
            <nav className="mt-3 flex flex-col gap-1">
              {NAV.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      cn(
                        "flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors",
                        isActive
                          ? "bg-n11/10 font-semibold text-n11"
                          : "hover:bg-accent"
                      )
                    }
                  >
                    <Icon className="h-4 w-4" />
                    {item.label}
                  </NavLink>
                );
              })}
            </nav>
          </div>
        </aside>
        <div>
          <Outlet />
        </div>
      </div>
    </div>
  );
}
