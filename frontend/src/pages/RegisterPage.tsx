import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FloatingInput } from "@/components/ui/floating-input";
import { useRegister } from "@/features/auth/queries";
import { AuthTabs } from "@/features/auth/AuthTabs";
import { Logo } from "@/components/Logo";
import { usePageTitle } from "@/hooks/usePageTitle";
import { cn } from "@/lib/utils";
import { extractApiError } from "@/api/client";
import type { RegisterRequest } from "@/types/api";

type FormValues = RegisterRequest & {
  agreeTerms: boolean;
  agreeMarketing: boolean;
};

function formatPhone(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 10);
  const parts: string[] = [];
  if (digits.length > 0) parts.push(digits.slice(0, Math.min(3, digits.length)));
  if (digits.length > 3) parts.push(digits.slice(3, Math.min(6, digits.length)));
  if (digits.length > 6) parts.push(digits.slice(6, Math.min(8, digits.length)));
  if (digits.length > 8) parts.push(digits.slice(8, 10));
  return parts.join(" ");
}

const PASSWORD_RULES = [
  {
    key: "len",
    label: "8-50 karakter",
    test: (v: string) => v.length >= 8 && v.length <= 50,
  },
  {
    key: "digit",
    label: "En az 1 rakam (0-9)",
    test: (v: string) => /\d/.test(v),
  },
  {
    key: "case",
    label: "En az 1 büyük, 1 küçük harf",
    test: (v: string) => /[a-z]/.test(v) && /[A-Z]/.test(v),
  },
];

export default function RegisterPage() {
  usePageTitle("Üye Ol");
  const [showPassword, setShowPassword] = useState(false);
  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      email: "",
      password: "",
      firstName: "",
      lastName: "",
      phone: "",
      agreeTerms: false,
      agreeMarketing: false,
    },
  });
  const mutation = useRegister();
  const password = watch("password");

  const onSubmit = (data: FormValues) => {
    const phoneDigits = data.phone?.replace(/\D/g, "") ?? "";
    const payload: RegisterRequest = {
      email: data.email.trim(),
      password: data.password,
      firstName: data.firstName.trim(),
      lastName: data.lastName.trim(),
      phone: phoneDigits || undefined,
    };
    mutation.mutate(payload, {
      onError: (err) => {
        const apiErr = extractApiError(err);
        if (apiErr.errorCode === "EMAIL_ALREADY_EXISTS") {
          setError("email", {
            type: "server",
            message: "Bu e-posta zaten kayıtlı. Giriş yapmayı dene.",
          });
        } else if (apiErr.errorCode === "PHONE_ALREADY_EXISTS") {
          setError("phone", {
            type: "server",
            message: "Bu telefon numarası kullanılamaz.",
          });
        }
      },
    });
  };

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
            <h1 className="mt-3 text-xl font-semibold">Merhaba!</h1>
            <p className="text-sm text-muted-foreground">
              Üyelere özel kupon ve fırsatlar seni bekliyor 🤩
            </p>
          </div>

          <AuthTabs active="register" />

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <FloatingInput
                id="firstName"
                label="Ad"
                error={errors.firstName?.message}
                {...register("firstName", {
                  required: "Ad zorunludur",
                  minLength: { value: 2, message: "En az 2 karakter" },
                  maxLength: { value: 50, message: "En fazla 50 karakter" },
                })}
              />
              <FloatingInput
                id="lastName"
                label="Soyad"
                error={errors.lastName?.message}
                {...register("lastName", {
                  required: "Soyad zorunludur",
                  minLength: { value: 2, message: "En az 2 karakter" },
                  maxLength: { value: 50, message: "En fazla 50 karakter" },
                })}
              />
            </div>

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

            <div className="grid grid-cols-[110px_1fr] gap-2">
              <div className="relative h-14 rounded-md border bg-secondary/30">
                <div className="absolute left-3 top-2 text-[11px] font-medium uppercase text-muted-foreground">
                  Ülke Kodu
                </div>
                <div className="absolute bottom-1 left-3 text-sm font-medium">
                  TR (+90)
                </div>
              </div>
              <FloatingInput
                id="phone"
                type="tel"
                inputMode="numeric"
                label="Telefon Numarası"
                autoComplete="tel"
                placeholder="555 555 55 55"
                maxLength={13}
                error={errors.phone?.message}
                {...register("phone", {
                  required: "Telefon numarası zorunludur",
                  validate: (v) => {
                    const digits = v?.replace(/\D/g, "") ?? "";
                    if (digits.length !== 10) {
                      return "10 haneli telefon numarası gir";
                    }
                    if (!digits.startsWith("5")) {
                      return "Telefon numarası 5 ile başlamalı (örn. 555...)";
                    }
                    return true;
                  },
                  onChange: (e) => {
                    e.target.value = formatPhone(e.target.value);
                  },
                })}
              />
            </div>

            <div className="space-y-1.5">
              <FloatingInput
                id="password"
                type={showPassword ? "text" : "password"}
                label="Şifre"
                autoComplete="new-password"
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
                  minLength: { value: 8, message: "En az 8 karakter" },
                  maxLength: { value: 50, message: "En fazla 50 karakter" },
                  validate: {
                    digit: (v) => /\d/.test(v) || "En az 1 rakam içermeli",
                    case: (v) =>
                      (/[a-z]/.test(v) && /[A-Z]/.test(v)) ||
                      "En az 1 büyük ve 1 küçük harf içermeli",
                  },
                })}
              />
              <ul className="space-y-0.5 px-1 text-[11px]">
                {PASSWORD_RULES.map((rule) => {
                  const ok = password ? rule.test(password) : false;
                  return (
                    <li
                      key={rule.key}
                      className={cn(
                        "flex items-center gap-1.5",
                        ok ? "text-emerald-600" : "text-muted-foreground"
                      )}
                    >
                      <span
                        className={cn(
                          "inline-block h-1.5 w-1.5 rounded-full",
                          ok ? "bg-emerald-500" : "bg-muted-foreground/40"
                        )}
                      />
                      {rule.label}
                    </li>
                  );
                })}
              </ul>
            </div>

            <div className="space-y-2 pt-1">
              <label className="flex items-start gap-2 text-xs leading-snug">
                <input
                  type="checkbox"
                  className="mt-0.5"
                  {...register("agreeTerms", {
                    required:
                      "Devam etmek için üyelik sözleşmesini kabul etmelisin",
                  })}
                />
                <span>
                  <span className="text-n11 underline">Üyelik Sözleşmesi</span>{" "}
                  şartlarını okudum ve kabul ediyorum.
                </span>
              </label>
              {errors.agreeTerms && (
                <p className="text-xs text-destructive">
                  {errors.agreeTerms.message}
                </p>
              )}
              <label className="flex items-start gap-2 text-xs leading-snug">
                <input
                  type="checkbox"
                  className="mt-0.5"
                  {...register("agreeMarketing")}
                />
                <span>
                  n11'in bana özel sunduğu kampanya ve fırsatlardan haberdar
                  olmak istiyorum.
                </span>
              </label>
            </div>

            <p className="text-[11px] leading-snug text-muted-foreground">
              KVKK kapsamının detaylarına,{" "}
              <span className="text-n11 underline">
                n11 Kişisel Verilerin Korunması ve İşlenmesi
              </span>{" "}
              şartlarının yer aldığı sayfamızdan ulaşabilirsin.
            </p>

            <Button
              type="submit"
              size="lg"
              className="w-full bg-foreground text-white hover:bg-foreground/90"
              disabled={mutation.isPending}
            >
              {mutation.isPending ? "Kayıt yapılıyor..." : "Üye Ol"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
