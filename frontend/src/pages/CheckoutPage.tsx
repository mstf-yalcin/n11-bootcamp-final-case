import { useState, useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Plus } from "lucide-react";
import { addressApi, orderApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/ui/spinner";
import { Separator } from "@/components/ui/separator";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AddressForm } from "@/features/checkout/AddressForm";
import { useCart } from "@/features/cart/queries";
import { usePageTitle } from "@/hooks/usePageTitle";
import { formatTRY, formatTrPhone } from "@/lib/utils";
import type { Address, CreateAddressRequest } from "@/types/api";

export default function CheckoutPage() {
  usePageTitle("Ödeme");
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const cartQuery = useCart();
  const addressesQuery = useQuery({
    queryKey: ["addresses"],
    queryFn: addressApi.list,
  });

  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(
    null
  );
  const [identityNumber, setIdentityNumber] = useState("");
  const [identityError, setIdentityError] = useState<string | null>(null);
  const [openNewAddress, setOpenNewAddress] = useState(false);

  useEffect(() => {
    const list = addressesQuery.data;
    if (!list || selectedAddressId) return;
    const def = list.find((a) => a.isDefault);
    if (def) setSelectedAddressId(def.id);
    else if (list.length > 0) setSelectedAddressId(list[0]!.id);
  }, [addressesQuery.data, selectedAddressId]);

  const createAddressMutation = useMutation({
    mutationFn: (body: CreateAddressRequest) => addressApi.create(body),
    onSuccess: (created: Address) => {
      queryClient.invalidateQueries({ queryKey: ["addresses"] });
      setSelectedAddressId(created.id);
      setOpenNewAddress(false);
      toast.success("Adres eklendi");
    },
    onError: (err) => notifyApiError(err, "Adres eklenemedi"),
  });

  const createOrderMutation = useMutation({
    mutationFn: () => {
      if (!selectedAddressId) throw new Error("Adres seçilmedi");
      return orderApi.create({
        addressId: selectedAddressId,
        identityNumber,
      });
    },
    onSuccess: (order) => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      toast.success("Sipariş alındı, ödeme işleniyor...");
      navigate(`/orders/${order.id}`, { replace: true });
    },
    onError: (err) => notifyApiError(err, "Sipariş oluşturulamadı"),
  });

  const validateIdentity = (value: string): string | null => {
    if (!value) return "TC kimlik numarası zorunludur";
    if (!/^\d{11}$/.test(value)) return "TC kimlik numarası 11 rakamdan oluşmalı";
    if (value.startsWith("0")) return "TC kimlik numarası 0 ile başlayamaz";
    return null;
  };

  const validateAndPlace = () => {
    const idErr = validateIdentity(identityNumber);
    if (!selectedAddressId) {
      toast.error("Lütfen bir teslimat adresi seç");
      if (idErr) setIdentityError(idErr);
      return;
    }
    if (idErr) {
      setIdentityError(idErr);
      return;
    }
    setIdentityError(null);
    createOrderMutation.mutate();
  };

  if (cartQuery.isLoading || addressesQuery.isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner size={28} />
      </div>
    );
  }

  if (!cartQuery.data || cartQuery.data.items.length === 0) {
    return (
      <div className="container py-20 text-center">
        <p className="mb-4 text-muted-foreground">
          Sepetinde ürün yok. Önce alışverişe başla.
        </p>
        <Button onClick={() => navigate("/products")}>Ürünlere git</Button>
      </div>
    );
  }

  const cart = cartQuery.data;

  return (
    <div className="container py-6">
      <h1 className="mb-6 text-2xl font-semibold">Ödeme</h1>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <section className="rounded-lg border bg-white p-5">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-semibold">Teslimat Adresi</h2>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setOpenNewAddress(true)}
              >
                <Plus className="mr-1 h-4 w-4" /> Yeni Adres
              </Button>
            </div>
            {addressesQuery.data && addressesQuery.data.length === 0 ? (
              <div className="rounded-md border border-dashed p-6 text-center text-sm text-muted-foreground">
                Kayıtlı adresin yok. Devam etmek için yeni bir adres ekle.
              </div>
            ) : (
              <div className="grid gap-3 sm:grid-cols-2">
                {addressesQuery.data?.map((addr) => (
                  <label
                    key={addr.id}
                    className={`relative cursor-pointer rounded-md border p-3 text-sm transition-colors ${
                      selectedAddressId === addr.id
                        ? "border-n11 ring-2 ring-n11/30"
                        : "hover:border-n11/50"
                    }`}
                  >
                    <input
                      type="radio"
                      name="address"
                      checked={selectedAddressId === addr.id}
                      onChange={() => setSelectedAddressId(addr.id)}
                      className="absolute right-3 top-3"
                    />
                    <div className="flex items-center gap-2 font-semibold">
                      {addr.title}
                      {addr.isDefault && (
                        <span className="rounded-full bg-n11/10 px-2 py-0.5 text-[10px] font-semibold text-n11">
                          Varsayılan
                        </span>
                      )}
                    </div>
                    <div className="mt-1 text-xs text-muted-foreground">
                      {addr.contactName}
                    </div>
                    <p className="mt-2 leading-snug">
                      {addr.fullAddress}
                      {addr.district ? `, ${addr.district}` : ""},{" "}
                      {addr.city}
                      {addr.zipCode ? ` ${addr.zipCode}` : ""}
                    </p>
                    {addr.phone && (
                      <p className="mt-1 text-xs text-muted-foreground">
                        Tel: {formatTrPhone(addr.phone)}
                      </p>
                    )}
                  </label>
                ))}
              </div>
            )}
          </section>

          <section className="rounded-lg border bg-white p-5">
            <h2 className="mb-3 text-base font-semibold">Fatura Bilgileri</h2>
            <div className="space-y-1.5">
              <Label htmlFor="tc">T.C. Kimlik Numarası</Label>
              <Input
                id="tc"
                inputMode="numeric"
                maxLength={11}
                value={identityNumber}
                onChange={(e) => {
                  const onlyDigits = e.target.value.replace(/\D/g, "");
                  setIdentityNumber(onlyDigits);
                  if (identityError) setIdentityError(null);
                }}
                onBlur={() => {
                  setIdentityError(validateIdentity(identityNumber));
                }}
                placeholder="11 rakam"
                aria-invalid={Boolean(identityError)}
                className={
                  identityError
                    ? "border-destructive focus-visible:ring-destructive"
                    : undefined
                }
              />
              {identityError && (
                <p className="text-xs text-destructive">{identityError}</p>
              )}
              <p className="text-xs text-muted-foreground">
                Faturalandırma için zorunludur. Bilgileriniz güvenle saklanır.
              </p>
            </div>
          </section>

          <section className="rounded-lg border bg-white p-5">
            <h2 className="mb-3 text-base font-semibold">Sepetindeki Ürünler</h2>
            <div className="space-y-3">
              {cart.items.map((item) => (
                <div
                  key={item.productId}
                  className="flex items-center gap-3 text-sm"
                >
                  <img
                    src={
                      item.imageUrl ||
                      "https://placehold.co/64x64/fff3eb/ff6000?text=n11"
                    }
                    alt={item.productName}
                    onError={(e) => {
                      (e.target as HTMLImageElement).src =
                        "https://placehold.co/64x64/fff3eb/ff6000?text=n11";
                    }}
                    className="h-12 w-12 flex-shrink-0 rounded-md border object-cover"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-medium">
                      {item.productName}
                    </div>
                    <div className="text-xs text-muted-foreground">
                      {item.quantity} ×{" "}
                      {formatTRY(item.unitPrice, item.currency)}
                    </div>
                  </div>
                  <div className="font-semibold">
                    {formatTRY(item.subtotal, item.currency)}
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        <aside className="lg:sticky lg:top-20 lg:h-fit">
          <div className="space-y-4 rounded-lg border bg-white p-5">
            <h3 className="text-base font-semibold">Sipariş Özeti</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Ara Toplam</span>
                <span>{formatTRY(cart.totalAmount, cart.currency)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Kargo</span>
                <span className="text-emerald-600">Ücretsiz</span>
              </div>
            </div>
            <Separator />
            <div className="flex justify-between text-base">
              <span className="font-semibold">Toplam</span>
              <span className="font-bold text-foreground">
                {formatTRY(cart.totalAmount, cart.currency)}
              </span>
            </div>
            <Button
              size="lg"
              className="w-full"
              onClick={validateAndPlace}
              disabled={createOrderMutation.isPending}
            >
              {createOrderMutation.isPending
                ? "Sipariş oluşturuluyor..."
                : "Siparişi Onayla"}
            </Button>
            <p className="text-center text-xs text-muted-foreground">
              Onayladığında ödeme işlemi otomatik başlatılır.
            </p>
          </div>
        </aside>
      </div>

      <Dialog open={openNewAddress} onOpenChange={setOpenNewAddress}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Yeni Adres Ekle</DialogTitle>
          </DialogHeader>
          <AddressForm
            onSubmit={(data) => createAddressMutation.mutate(data)}
            onCancel={() => setOpenNewAddress(false)}
            isPending={createAddressMutation.isPending}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
