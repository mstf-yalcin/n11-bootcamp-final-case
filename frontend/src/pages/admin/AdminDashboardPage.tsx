import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  Package,
  ShoppingBag,
  Tag,
  Users,
  Warehouse,
} from "lucide-react";
import {
  adminOrderApi,
  adminUserApi,
  categoryApi,
  productApi,
  tagApi,
} from "@/api/endpoints";
import { Spinner } from "@/components/ui/spinner";
import { usePageTitle } from "@/hooks/usePageTitle";

type StatCardProps = {
  label: string;
  value: number | string;
  loading?: boolean;
  icon: React.ComponentType<{ className?: string }>;
  to: string;
  hint?: string;
};

function StatCard({ label, value, loading, icon: Icon, to, hint }: StatCardProps) {
  return (
    <Link
      to={to}
      className="group flex flex-col gap-2 rounded-lg border bg-white p-5 transition-shadow hover:shadow-sm"
    >
      <div className="flex items-center justify-between">
        <div className="flex h-9 w-9 items-center justify-center rounded-md bg-n11/10 text-n11">
          <Icon className="h-5 w-5" />
        </div>
        <ArrowRight className="h-4 w-4 text-muted-foreground transition-colors group-hover:text-n11" />
      </div>
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="text-2xl font-bold">
        {loading ? <Spinner size={16} /> : value}
      </div>
      {hint && <div className="text-xs text-muted-foreground">{hint}</div>}
    </Link>
  );
}

export default function AdminDashboardPage() {
  usePageTitle("Admin · Dashboard");

  const productsQuery = useQuery({
    queryKey: ["admin", "stat", "products"],
    queryFn: () => productApi.list({ size: 1 }),
  });
  const categoriesQuery = useQuery({
    queryKey: ["admin", "stat", "categories"],
    queryFn: categoryApi.list,
  });
  const tagsQuery = useQuery({
    queryKey: ["admin", "stat", "tags"],
    queryFn: tagApi.list,
  });
  const ordersQuery = useQuery({
    queryKey: ["admin", "stat", "orders"],
    queryFn: () => adminOrderApi.list({ size: 1 }),
    retry: false,
  });
  const usersQuery = useQuery({
    queryKey: ["admin", "stat", "users"],
    queryFn: () => adminUserApi.list({ size: 1 }),
    retry: false,
  });

  return (
    <div className="p-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          n11 admin panelinde genel görünüm ve hızlı erişim.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Toplam Ürün"
          value={productsQuery.data?.page?.totalElements ?? 0}
          loading={productsQuery.isLoading}
          icon={Package}
          to="/admin/products"
        />
        <StatCard
          label="Kategori"
          value={categoriesQuery.data?.length ?? 0}
          loading={categoriesQuery.isLoading}
          icon={Warehouse}
          to="/admin/categories"
        />
        <StatCard
          label="Etiket"
          value={tagsQuery.data?.length ?? 0}
          loading={tagsQuery.isLoading}
          icon={Tag}
          to="/admin/tags"
        />
        <StatCard
          label="Toplam Sipariş"
          value={
            ordersQuery.isError
              ? "—"
              : ordersQuery.data?.page?.totalElements ?? 0
          }
          loading={ordersQuery.isLoading}
          icon={ShoppingBag}
          to="/admin/orders"
          hint={ordersQuery.isError ? "Endpoint henüz aktif değil" : undefined}
        />
        <StatCard
          label="Toplam Kullanıcı"
          value={
            usersQuery.isError
              ? "—"
              : usersQuery.data?.page?.totalElements ?? 0
          }
          loading={usersQuery.isLoading}
          icon={Users}
          to="/admin/users"
          hint={usersQuery.isError ? "Endpoint henüz aktif değil" : undefined}
        />
      </div>

      <div className="mt-10 rounded-lg border border-dashed bg-white p-6">
        <h2 className="mb-2 text-lg font-semibold">Hızlı İşlemler</h2>
        <div className="grid gap-2 sm:grid-cols-3">
          <Link
            to="/admin/products"
            className="rounded-md border bg-secondary/30 px-4 py-3 text-sm hover:bg-accent"
          >
            + Yeni ürün ekle
          </Link>
          <Link
            to="/admin/categories"
            className="rounded-md border bg-secondary/30 px-4 py-3 text-sm hover:bg-accent"
          >
            + Yeni kategori
          </Link>
          <Link
            to="/admin/tags"
            className="rounded-md border bg-secondary/30 px-4 py-3 text-sm hover:bg-accent"
          >
            + Yeni etiket
          </Link>
        </div>
      </div>
    </div>
  );
}
