import { useState } from "react";
import { useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus } from "lucide-react";
import { tagApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { FloatingInput } from "@/components/ui/floating-input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ApiErrorBox } from "@/components/ApiErrorBox";
import { Spinner } from "@/components/ui/spinner";
import { usePageTitle } from "@/hooks/usePageTitle";

export default function AdminTagsPage() {
  usePageTitle("Admin · Etiketler");
  const [creating, setCreating] = useState(false);
  const queryClient = useQueryClient();

  const tagsQuery = useQuery({
    queryKey: ["tags"],
    queryFn: tagApi.list,
  });

  const createMutation = useMutation({
    mutationFn: (name: string) => tagApi.create({ name }),
    onSuccess: () => {
      toast.success("Etiket oluşturuldu");
      queryClient.invalidateQueries({ queryKey: ["tags"] });
      setCreating(false);
    },
    onError: (err) => notifyApiError(err, "Oluşturulamadı"),
  });

  return (
    <div className="p-8">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Etiketler</h1>
          <p className="text-sm text-muted-foreground">
            {tagsQuery.data
              ? `Toplam ${tagsQuery.data.length} etiket`
              : "Yükleniyor..."}
          </p>
        </div>
        <Button onClick={() => setCreating(true)}>
          <Plus className="mr-1 h-4 w-4" />
          Yeni Etiket
        </Button>
      </div>

      {tagsQuery.isLoading && (
        <div className="flex justify-center py-10">
          <Spinner size={28} />
        </div>
      )}

      {tagsQuery.isError && (
        <ApiErrorBox
          error={tagsQuery.error}
          onRetry={tagsQuery.refetch}
          title="Etiketler yüklenemedi"
        />
      )}

      {tagsQuery.data && tagsQuery.data.length === 0 && (
        <div className="rounded-lg border bg-white p-10 text-center text-sm text-muted-foreground">
          Henüz etiket eklenmemiş.
        </div>
      )}

      {tagsQuery.data && tagsQuery.data.length > 0 && (
        <div className="rounded-lg border bg-white p-4">
          <div className="flex flex-wrap gap-2">
            {tagsQuery.data.map((t) => (
              <span
                key={t.id}
                className="rounded-full border bg-secondary/40 px-3 py-1 text-sm"
              >
                {t.name}
                <span className="ml-2 font-mono text-[10px] text-muted-foreground">
                  {t.slug}
                </span>
              </span>
            ))}
          </div>
        </div>
      )}

      <NewTagDialog
        open={creating}
        onOpenChange={setCreating}
        onSubmit={(name) => createMutation.mutate(name)}
        isPending={createMutation.isPending}
      />
    </div>
  );
}

function NewTagDialog({
  open,
  onOpenChange,
  onSubmit,
  isPending,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (name: string) => void;
  isPending: boolean;
}) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<{ name: string }>({ defaultValues: { name: "" } });

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        if (!o) reset();
        onOpenChange(o);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Yeni Etiket</DialogTitle>
        </DialogHeader>
        <form
          onSubmit={handleSubmit((data) => onSubmit(data.name.trim()))}
          className="space-y-3"
        >
          <FloatingInput
            id="tag-name"
            label="Etiket Adı"
            error={errors.name?.message}
            {...register("name", {
              required: "İsim zorunludur",
              maxLength: { value: 100, message: "En fazla 100 karakter" },
            })}
          />
          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              İptal
            </Button>
            <Button type="submit" disabled={isPending}>
              Oluştur
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
