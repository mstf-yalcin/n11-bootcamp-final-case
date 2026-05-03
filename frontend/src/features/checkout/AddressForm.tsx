import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { normalizeTrPhone } from "@/lib/utils";
import type { CreateAddressRequest } from "@/types/api";

type Props = {
  initial?: Partial<CreateAddressRequest>;
  onSubmit: (data: CreateAddressRequest) => void;
  onCancel?: () => void;
  isPending?: boolean;
  submitLabel?: string;
};

function formatPhone(value: string): string {
  const digits = normalizeTrPhone(value);
  const parts: string[] = [];
  if (digits.length > 0) parts.push(digits.slice(0, Math.min(3, digits.length)));
  if (digits.length > 3) parts.push(digits.slice(3, Math.min(6, digits.length)));
  if (digits.length > 6) parts.push(digits.slice(6, Math.min(8, digits.length)));
  if (digits.length > 8) parts.push(digits.slice(8, 10));
  return parts.join(" ");
}

export function AddressForm({
  initial,
  onSubmit,
  onCancel,
  isPending,
  submitLabel = "Kaydet",
}: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateAddressRequest>({
    defaultValues: {
      title: initial?.title ?? "",
      contactName: initial?.contactName ?? "",
      fullAddress: initial?.fullAddress ?? "",
      city: initial?.city ?? "",
      district: initial?.district ?? "",
      country: initial?.country ?? "Türkiye",
      zipCode: initial?.zipCode ?? "",
      phone: initial?.phone ? formatPhone(initial.phone) : "",
      isDefault: initial?.isDefault ?? false,
    },
  });

  const submit = (data: CreateAddressRequest) => {
    const phoneDigits = data.phone?.replace(/\D/g, "") ?? "";
    onSubmit({
      ...data,
      phone: phoneDigits || undefined,
    });
  };

  return (
    <form onSubmit={handleSubmit(submit)} className="space-y-3" noValidate>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="title">Adres Başlığı</Label>
          <Input
            id="title"
            placeholder="Ev / İş"
            {...register("title", {
              required: "Başlık zorunludur",
              maxLength: { value: 50, message: "En fazla 50 karakter" },
            })}
          />
          {errors.title && (
            <p className="text-xs text-destructive">{errors.title.message}</p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="contactName">Alıcı Adı Soyadı</Label>
          <Input
            id="contactName"
            {...register("contactName", {
              required: "Alıcı adı zorunludur",
              maxLength: { value: 100, message: "En fazla 100 karakter" },
            })}
          />
          {errors.contactName && (
            <p className="text-xs text-destructive">
              {errors.contactName.message}
            </p>
          )}
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="fullAddress">Açık Adres</Label>
        <textarea
          id="fullAddress"
          rows={3}
          className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          {...register("fullAddress", {
            required: "Açık adres zorunludur",
            maxLength: { value: 500, message: "En fazla 500 karakter" },
          })}
        />
        {errors.fullAddress && (
          <p className="text-xs text-destructive">
            {errors.fullAddress.message}
          </p>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="space-y-1.5">
          <Label htmlFor="city">İl</Label>
          <Input
            id="city"
            {...register("city", {
              required: "İl zorunludur",
              maxLength: 100,
            })}
          />
          {errors.city && (
            <p className="text-xs text-destructive">{errors.city.message}</p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="district">İlçe</Label>
          <Input id="district" {...register("district", { maxLength: 100 })} />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="zipCode">Posta Kodu</Label>
          <Input
            id="zipCode"
            inputMode="numeric"
            maxLength={5}
            placeholder="34000"
            {...register("zipCode", {
              pattern: {
                value: /^\d{5}$/,
                message: "Posta kodu 5 rakamdan oluşmalı",
              },
              onChange: (e) => {
                e.target.value = e.target.value.replace(/\D/g, "").slice(0, 5);
              },
            })}
          />
          {errors.zipCode && (
            <p className="text-xs text-destructive">
              {errors.zipCode.message}
            </p>
          )}
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="country">Ülke</Label>
          <Input id="country" {...register("country", { maxLength: 100 })} />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="phone">Telefon</Label>
          <Input
            id="phone"
            type="tel"
            inputMode="numeric"
            placeholder="555 555 55 55"
            maxLength={13}
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
          {errors.phone && (
            <p className="text-xs text-destructive">{errors.phone.message}</p>
          )}
        </div>
      </div>

      <label className="flex cursor-pointer items-center gap-2 text-sm">
        <input type="checkbox" {...register("isDefault")} />
        Varsayılan adres yap
      </label>

      <div className="flex justify-end gap-2 pt-2">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            İptal
          </Button>
        )}
        <Button type="submit" disabled={isPending}>
          {isPending ? "Kaydediliyor..." : submitLabel}
        </Button>
      </div>
    </form>
  );
}
