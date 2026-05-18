import { Navigate, useLocation } from "react-router-dom";
import { ShieldX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/authStore";
import { useCurrentUser } from "@/features/auth/queries";

export function RequireRole({
  role,
  children,
}: {
  role: string;
  children: React.ReactNode;
}) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const { data: user, isLoading } = useCurrentUser();
  const location = useLocation();

  const isAdminArea = location.pathname.startsWith("/admin");
  const loginPath = isAdminArea ? "/admin/login" : "/login";

  if (!accessToken) {
    const next = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`${loginPath}?next=${next}`} replace />;
  }

  if (isLoading || !user) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <span className="text-sm text-muted-foreground">
          Yetki kontrol ediliyor...
        </span>
      </div>
    );
  }

  if (!user.roles?.includes(role)) {
    return (
      <div className="container flex min-h-[60vh] items-center justify-center">
        <div className="w-full max-w-md rounded-lg border bg-white p-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-destructive/10">
            <ShieldX className="h-7 w-7 text-destructive" />
          </div>
          <h1 className="mb-2 text-xl font-semibold">Yetkin yok</h1>
          <p className="mb-6 text-sm text-muted-foreground">
            Bu sayfayı görüntülemek için <code>{role}</code> rolüne sahip
            olmalısın.
          </p>
          <div className="flex justify-center gap-2">
            <Button asChild variant="outline">
              <a href="/">Anasayfaya dön</a>
            </Button>
            {isAdminArea && (
              <Button asChild>
                <a href="/admin/login">Yönetici Olarak Giriş Yap</a>
              </Button>
            )}
          </div>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
