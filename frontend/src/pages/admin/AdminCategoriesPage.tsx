import { useState } from "react";
import { useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { adminCategoryApi, categoryApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { FloatingInput } from "@/components/ui/floating-input";
import { DataTable, type Column } from "@/components/DataTable";
import { useConfirm } from "@/components/ConfirmDialog";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { usePageTitle } from "@/hooks/usePageTitle";
import { formatDate } from "@/lib/utils";
import type { Category, CreateCategoryRequest } from "@/types/api";

export default function AdminCategoriesPage() {
  usePageTitle("Admin · Kategoriler");
  const queryClient = useQueryClient();
  const { confirm, dialog: confirmDialog } = useConfirm();
  const [editing, setEditing] = useState<Category | null>(null);
  const [creating, setCreating] = useState(false);

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
  });

  const removeMutation = useMutation({
    mutationFn: (id: string) => adminCategoryApi.remove(id),
    onSuccess: () => {
      toast.success("Kategori silindi");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (err) => notifyApiError(err, "Silme başarısız"),
  });

  const columns: Column<Category>[] = [
    {
      key: "image",
      header: "Görsel",
      width: "80px",
      cell: (c) =>
        c.imageUrl ? (
          <img
            src={c.imageUrl}
            alt={c.name}
            className="h-10 w-10 rounded object-cover"
            loading="lazy"
          />
        ) : (
          <div className="h-10 w-10 rounded bg-muted" />
        ),
    },
    {
      key: "name",
      header: "Kategori",
      cell: (c) => <span className="font-medium">{c.name}</span>,
    },
    {
      key: "description",
      header: "Açıklama",
      cell: (c) => (
        <span className="text-muted-foreground">{c.description ?? "—"}</span>
      ),
    },
    {
      key: "createdAt",
      header: "Oluşturma",
      cell: (c) => (
        <span className="text-xs text-muted-foreground">
          {formatDate(c.createdAt)}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "100px",
      className: "text-right",
      cell: (c) => (
        <div className="flex justify-end gap-1">
          <button
            onClick={() => setEditing(c)}
            className="rounded p-1.5 text-muted-foreground hover:bg-accent hover:text-n11"
            aria-label="Düzenle"
          >
            <Pencil className="h-4 w-4" />
          </button>
          <button
            onClick={async () => {
              const ok = await confirm({
                title: "Kategoriyi sil",
                description: `"${c.name}" kategorisini silmek istediğine emin misin?`,
                destructive: true,
                confirmLabel: "Sil",
              });
              if (ok) removeMutation.mutate(c.id);
            }}
            className="rounded p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
            aria-label="Sil"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="p-8">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Kategoriler</h1>
          <p className="text-sm text-muted-foreground">
            {categoriesQuery.data
              ? `Toplam ${categoriesQuery.data.length} kategori`
              : "Yükleniyor..."}
          </p>
        </div>
        <Button onClick={() => setCreating(true)}>
          <Plus className="mr-1 h-4 w-4" />
          Yeni Kategori
        </Button>
      </div>

      <DataTable
        columns={columns}
        rows={categoriesQuery.data}
        rowKey={(c) => c.id}
        isLoading={categoriesQuery.isLoading}
        isError={categoriesQuery.isError}
        emptyMessage="Henüz kategori yok."
      />

      <CategoryFormDialog
        open={creating}
        onOpenChange={setCreating}
        category={null}
      />
      <CategoryFormDialog
        open={Boolean(editing)}
        onOpenChange={(o) => !o && setEditing(null)}
        category={editing}
      />
      {confirmDialog}
    </div>
  );
}

function CategoryFormDialog({
  open,
  onOpenChange,
  category,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  category: Category | null;
}) {
  const queryClient = useQueryClient();
  const isEdit = Boolean(category);
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<CreateCategoryRequest>({
    defaultValues: { name: "", description: "", imageUrl: "" },
  });

  const previewUrl = watch("imageUrl");

  const onClose = (o: boolean) => {
    if (!o) reset({ name: "", description: "", imageUrl: "" });
    onOpenChange(o);
  };

  // Re-init form when dialog opens with different category
  if (open && category && category.id) {
    // simple sync
  }

  const createMutation = useMutation({
    mutationFn: adminCategoryApi.create,
    onSuccess: () => {
      toast.success("Kategori oluşturuldu");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      onClose(false);
    },
    onError: (err) => notifyApiError(err, "Oluşturulamadı"),
  });
  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; body: CreateCategoryRequest }) =>
      adminCategoryApi.update(vars.id, vars.body),
    onSuccess: () => {
      toast.success("Kategori güncellendi");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      onClose(false);
    },
    onError: (err) => notifyApiError(err, "Güncellenemedi"),
  });

  const onSubmit = (data: CreateCategoryRequest) => {
    const body: CreateCategoryRequest = {
      ...data,
      description: data.description?.trim() || undefined,
      imageUrl: data.imageUrl.trim(),
    };
    if (isEdit && category) {
      updateMutation.mutate({ id: category.id, body });
    } else {
      createMutation.mutate(body);
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        if (o && category) {
          reset({
            name: category.name,
            description: category.description ?? "",
            imageUrl: category.imageUrl ?? "",
          });
        } else if (o && !category) {
          reset({ name: "", description: "", imageUrl: "" });
        }
        onClose(o);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Kategoriyi Düzenle" : "Yeni Kategori"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
          <FloatingInput
            id="cat-name"
            label="Kategori Adı"
            error={errors.name?.message}
            {...register("name", {
              required: "İsim zorunludur",
              maxLength: { value: 100, message: "En fazla 100 karakter" },
            })}
          />
          <div>
            <label className="mb-1 block text-xs font-medium uppercase text-muted-foreground">
              Açıklama
            </label>
            <textarea
              rows={3}
              placeholder="Kategori açıklaması..."
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-n11"
              {...register("description", {
                required: "Açıklama zorunludur",
                minLength: { value: 5, message: "En az 5 karakter" },
                maxLength: { value: 500, message: "En fazla 500 karakter" },
              })}
            />
            {errors.description && (
              <p className="mt-1 text-xs text-destructive">
                {errors.description.message}
              </p>
            )}
          </div>
          <div>
            <FloatingInput
              id="cat-image"
              label="Görsel URL"
              placeholder="https://example.com/category.jpg"
              error={errors.imageUrl?.message}
              {...register("imageUrl", {
                required: "Görsel URL zorunludur",
                pattern: {
                  value: /^https?:\/\/.+/i,
                  message: "Geçerli bir URL gir (http/https)",
                },
                maxLength: { value: 1024, message: "En fazla 1024 karakter" },
              })}
            />
            {previewUrl && /^https?:\/\/.+/i.test(previewUrl) && (
              <img
                src={previewUrl}
                alt="Önizleme"
                className="mt-2 h-24 w-24 rounded-md border object-cover"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).style.display = "none";
                }}
              />
            )}
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onClose(false)}
            >
              İptal
            </Button>
            <Button
              type="submit"
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {isEdit ? "Güncelle" : "Oluştur"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
