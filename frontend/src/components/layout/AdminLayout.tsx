import { NavLink, Outlet, Link } from "react-router-dom";
import {
  ArrowLeft,
  Boxes,
  CreditCard,
  LayoutDashboard,
  LogOut,
  Package,
  ShoppingBag,
  Tag,
  Users,
  Warehouse,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Logo } from "@/components/Logo";
import { useLogout } from "@/features/auth/queries";
import { useAuthStore } from "@/store/authStore";

const NAV = [
  { to: "/admin/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/admin/products", label: "Ürünler", icon: Package },
  { to: "/admin/categories", label: "Kategoriler", icon: Boxes },
  { to: "/admin/tags", label: "Etiketler", icon: Tag },
  { to: "/admin/stocks", label: "Stoklar", icon: Warehouse },
  { to: "/admin/orders", label: "Siparişler", icon: ShoppingBag },
  { to: "/admin/payments", label: "Ödemeler", icon: CreditCard },
  { to: "/admin/users", label: "Kullanıcılar", icon: Users },
];

export function AdminLayout() {
  const user = useAuthStore((s) => s.user);
  const logout = useLogout();

  return (
    <div className="flex min-h-screen bg-secondary/30">
      <aside className="sticky top-0 flex h-screen w-64 flex-shrink-0 flex-col border-r bg-white">
        <Link
          to="/admin/dashboard"
          className="flex items-center gap-2 border-b px-4 py-4"
        >
          <Logo size={32} />
          <div className="leading-tight">
            <div className="text-base font-bold">
              n<span className="text-n11">11</span>
            </div>
            <div className="text-[10px] uppercase text-muted-foreground">
              Admin
            </div>
          </div>
        </Link>

        <nav className="flex-1 space-y-1 p-2">
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
                      : "text-foreground/70 hover:bg-accent hover:text-foreground"
                  )
                }
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            );
          })}
        </nav>

        <div className="space-y-1 border-t p-3">
          {user && (
            <div className="px-3 py-2 text-xs">
              <div className="font-medium">
                {user.firstName} {user.lastName}
              </div>
              <div className="truncate text-muted-foreground">{user.email}</div>
            </div>
          )}
          <Link
            to="/"
            className="flex items-center gap-2 rounded-md px-3 py-2 text-xs text-muted-foreground hover:bg-accent hover:text-foreground"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            Mağazaya dön
          </Link>
          <button
            type="button"
            onClick={() => logout.mutate()}
            disabled={logout.isPending}
            className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-xs text-destructive hover:bg-destructive/10 disabled:opacity-60"
          >
            <LogOut className="h-3.5 w-3.5" />
            {logout.isPending ? "Çıkış yapılıyor..." : "Çıkış Yap"}
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-x-auto">
        <Outlet />
      </main>
    </div>
  );
}
