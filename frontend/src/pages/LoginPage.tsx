import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FloatingInput } from "@/components/ui/floating-input";
import { useLogin } from "@/features/auth/queries";
import { AuthTabs } from "@/features/auth/AuthTabs";
import { Logo } from "@/components/Logo";
import { usePageTitle } from "@/hooks/usePageTitle";
import type { LoginRequest } from "@/types/api";

export default function LoginPage() {
  usePageTitle("Giriş Yap");
  const [showPassword, setShowPassword] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginRequest>({
    defaultValues: { email: "", password: "" },
  });
  const login = useLogin();

  const onSubmit = (data: LoginRequest) => login.mutate(data);

  return (
    <div className="container flex justify-center py-10">
      <Card className="w-full max-w-md">
        <CardContent className="space-y-5 pt-6">
          <div className="text-center">
            <Link to="/" className="inline-flex items-center gap-2">
              <Logo size={32} />
              <span className="text-3xl font-extrabold tracking-tight">
                n<span className="text-n11">11</span>
              </span>
              <span className="ml-1 inline-block h-3 w-3 rounded-full bg-n11" />
            </Link>
            <h1 className="mt-3 text-xl font-semibold">Tekrar Hoş Geldin!</h1>
            <p className="text-sm text-muted-foreground">
              Hesabına giriş yap, alışverişe kaldığın yerden devam et.
            </p>
          </div>

          <AuthTabs active="login" />

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
            <FloatingInput
              id="email"
              type="email"
              label="E-posta Adresi"
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
              {...register("password", {
                required: "Şifre zorunludur",
                minLength: { value: 6, message: "En az 6 karakter" },
              })}
            />

            <div className="flex items-center justify-end">
              <button
                type="button"
                className="text-xs text-n11 hover:underline"
                onClick={() =>
                  alert("Şifre sıfırlama bu sürümde mevcut değil")
                }
              >
                Şifremi Unuttum
              </button>
            </div>

            <Button
              type="submit"
              size="lg"
              className="w-full bg-foreground text-white hover:bg-foreground/90"
              disabled={login.isPending}
            >
              {login.isPending ? "Giriş yapılıyor..." : "Giriş Yap"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
