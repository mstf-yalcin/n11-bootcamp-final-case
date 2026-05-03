import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { addressApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AddressForm } from "@/features/checkout/AddressForm";
import { useConfirm } from "@/components/ConfirmDialog";
import { ApiErrorBox } from "@/components/ApiErrorBox";
import { AddressCardSkeleton } from "@/components/ListSkeleton";
import { usePageTitle } from "@/hooks/usePageTitle";
import { formatTrPhone } from "@/lib/utils";
import type { Address, CreateAddressRequest } from "@/types/api";

export default function AccountAddressesPage() {
  usePageTitle("Adreslerim");
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Address | null>(null);
  const [creating, setCreating] = useState(false);
  const { confirm, dialog: confirmDialog } = useConfirm();

  const addressesQuery = useQuery({
    queryKey: ["addresses"],
    queryFn: addressApi.list,
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["addresses"] });

  const createMutation = useMutation({
    mutationFn: (body: CreateAddressRequest) => addressApi.create(body),
    onSuccess: () => {
      toast.success("Adres eklendi");
      setCreating(false);
      invalidate();
    },
    onError: (err) => notifyApiError(err, "Adres eklenemedi"),
  });

  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; body: CreateAddressRequest }) =>
      addressApi.update(vars.id, vars.body),
    onSuccess: () => {
      toast.success("Adres güncellendi");
      setEditing(null);
      invalidate();
    },
    onError: (err) => notifyApiError(err, "Adres güncellenemedi"),
  });

  const removeMutation = useMutation({
    mutationFn: (id: string) => addressApi.remove(id),
    onSuccess: () => {
      toast.success("Adres silindi");
      invalidate();
    },
    onError: (err) => notifyApiError(err, "Adres silinemedi"),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Adreslerim</h1>
        <Button onClick={() => setCreating(true)}>
          <Plus className="mr-1 h-4 w-4" /> Yeni Adres
        </Button>
      </div>

      {addressesQuery.isLoading && (
        <div className="grid gap-4 sm:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <AddressCardSkeleton key={i} />
          ))}
        </div>
      )}

      {addressesQuery.isError && (
        <ApiErrorBox
          error={addressesQuery.error}
          onRetry={addressesQuery.refetch}
          title="Adresler yüklenemedi"
        />
      )}

      {addressesQuery.data && addressesQuery.data.length === 0 && (
        <div className="rounded-lg border bg-white p-10 text-center">
          <p className="mb-4 text-muted-foreground">
            Henüz kayıtlı adresin yok.
          </p>
          <Button onClick={() => setCreating(true)}>İlk adresi ekle</Button>
        </div>
      )}

      {addressesQuery.data && addressesQuery.data.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2">
          {addressesQuery.data.map((addr) => (
            <div
              key={addr.id}
              className="rounded-lg border bg-white p-4 text-sm"
            >
              <div className="mb-2 flex items-start justify-between">
                <div className="flex items-center gap-2 font-semibold">
                  {addr.title}
                  {addr.isDefault && (
                    <span className="rounded-full bg-n11/10 px-2 py-0.5 text-[10px] font-semibold text-n11">
                      Varsayılan
                    </span>
                  )}
                </div>
                <div className="flex gap-1">
                  <button
                    onClick={() => setEditing(addr)}
                    className="rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-n11"
                    aria-label="Düzenle"
                  >
                    <Pencil className="h-4 w-4" />
                  </button>
                  <button
                    onClick={async () => {
                      const ok = await confirm({
                        title: "Adresi sil",
                        description: `"${addr.title}" adresini silmek istediğine emin misin? Bu işlem geri alınamaz.`,
                        confirmLabel: "Sil",
                        cancelLabel: "Vazgeç",
                        destructive: true,
                      });
                      if (ok) removeMutation.mutate(addr.id);
                    }}
                    className="rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-destructive"
                    aria-label="Sil"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
              <div className="text-xs text-muted-foreground">
                {addr.contactName}
              </div>
              <p className="mt-2 leading-relaxed">
                {addr.fullAddress}
                {addr.district ? `, ${addr.district}` : ""}, {addr.city}
                {addr.zipCode ? ` ${addr.zipCode}` : ""}
              </p>
              {addr.phone && (
                <p className="mt-1 text-xs text-muted-foreground">
                  Tel: {formatTrPhone(addr.phone)}
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      <Dialog open={creating} onOpenChange={setCreating}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Yeni Adres Ekle</DialogTitle>
          </DialogHeader>
          <AddressForm
            onSubmit={(data) => createMutation.mutate(data)}
            onCancel={() => setCreating(false)}
            isPending={createMutation.isPending}
          />
        </DialogContent>
      </Dialog>

      <Dialog
        open={Boolean(editing)}
        onOpenChange={(open) => !open && setEditing(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Adresi Düzenle</DialogTitle>
          </DialogHeader>
          {editing && (
            <AddressForm
              initial={editing}
              submitLabel="Güncelle"
              onSubmit={(data) =>
                updateMutation.mutate({ id: editing.id, body: data })
              }
              onCancel={() => setEditing(null)}
              isPending={updateMutation.isPending}
            />
          )}
        </DialogContent>
      </Dialog>
      {confirmDialog}
    </div>
  );
}
