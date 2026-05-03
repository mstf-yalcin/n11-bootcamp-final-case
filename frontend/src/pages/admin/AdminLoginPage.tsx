import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { useMutation } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft, Eye, EyeOff, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FloatingInput } from "@/components/ui/floating-input";
import { Logo } from "@/components/Logo";
import { authApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import { usePageTitle } from "@/hooks/usePageTitle";
import { Roles, type LoginRequest } from "@/types/api";

export default function AdminLoginPage() {
  usePageTitle("Yönetici Girişi");
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();
  const setTokens = useAuthStore((s) => s.setTokens);
  const setUser = useAuthStore((s) => s.setUser);
  const clear = useAuthStore((s) => s.clear);
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);

  // Zaten admin olarak login'liyse direkt panele yönlendir
  useEffect(() => {
    if (accessToken && user?.roles?.includes(Roles.ADMIN)) {
      navigate("/admin", { replace: true });
    }
  }, [accessToken, user, navigate]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginRequest>({ defaultValues: { email: "", password: "" } });

  const login = useMutation({
    mutationFn: (body: LoginRequest) => authApi.login(body),
    onSuccess: async (tokens) => {
      setTokens(tokens);
      try {
        const me = await authApi.me();
        if (!me.roles?.includes(Roles.ADMIN)) {
          clear();
          toast.error("Bu hesap admin yetkisine sahip değil.", {
            description: "Yönetici hesabıyla tekrar dene.",
          });
          return;
        }
        setUser(me);
        toast.success("Yönetici girişi başarılı");
        navigate("/admin", { replace: true });
      } catch (err) {
        clear();
        notifyApiError(err, "Profil alınamadı");
      }
    },
    onError: (err) => notifyApiError(err, "Giriş başarısız"),
  });

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-zinc-900 via-zinc-800 to-zinc-900 px-4 py-12">
      <div className="w-full max-w-sm">
        <Card className="overflow-hidden border-zinc-200 shadow-2xl">
          <div className="h-1 bg-gradient-to-r from-n11 via-n11-dark to-n11" />
          <CardContent className="space-y-5 pt-6">
            <div className="text-center">
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-n11/10 ring-4 ring-n11/20">
                <ShieldCheck className="h-6 w-6 text-n11" />
              </div>
              <Link to="/" className="inline-flex items-center gap-1.5">
                <Logo size={24} />
                <span className="text-lg font-bold tracking-tight">
                  n<span className="text-n11">11</span>
                </span>
                <span className="ml-2 rounded bg-foreground px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-white">
                  Admin
                </span>
              </Link>
              <h1 className="mt-3 text-lg font-semibold">Yönetici Girişi</h1>
              <p className="text-xs text-muted-foreground">
                Bu alan sadece yetkili personel içindir.
              </p>
            </div>

            <form
              onSubmit={handleSubmit((data) => login.mutate(data))}
              className="space-y-3"
            >
              <FloatingInput
                id="email"
                type="email"
                label="E-posta"
                autoComplete="email"
                error={errors.email?.message}
                {...register("email", {
                  required: "E-posta zorunludur",
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    message: "Geçerli bir e-posta gir",
                  },
                })}
              />

              <FloatingInput
                id="password"
                type={showPassword ? "text" : "password"}
                label="Şifre"
                autoComplete="current-password"
                error={errors.password?.message}
                suffix={
                  <button
                    type="button"
                    onClick={() => setShowPassword((s) => !s)}
                    className="rounded p-1 text-muted-foreground hover:text-foreground"
                    aria-label={
                      showPassword ? "Şifreyi gizle" : "Şifreyi göster"
                    }
                    tabIndex={-1}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                }
                {...register("password", { required: "Şifre zorunludur" })}
              />

              <Button
                type="submit"
                size="lg"
                className="w-full bg-foreground text-white hover:bg-foreground/90"
                disabled={login.isPending}
              >
                {login.isPending ? "Giriş yapılıyor..." : "Yönetici Olarak Giriş Yap"}
              </Button>
            </form>

            <div className="border-t pt-3 text-center">
              <Link
                to="/"
                className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
              >
                <ArrowLeft className="h-3 w-3" />
                Mağazaya dön
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
