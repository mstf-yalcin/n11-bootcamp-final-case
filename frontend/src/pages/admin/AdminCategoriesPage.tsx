import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { adminCategoryApi, categoryApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import axios from "axios";
import { Button } from "@/components/ui/button";
import { FloatingInput } from "@/components/ui/floating-input";
import { DataTable, type Column } from "@/components/DataTable";
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
  const [editing, setEditing] = useState<Category | null>(null);
  const [creating, setCreating] = useState(false);
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(
    null
  );
  const [deleteNeedsTarget, setDeleteNeedsTarget] = useState(false);
  const [deleteTargetId, setDeleteTargetId] = useState<string>("");

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
  });

  const closeDelete = () => {
    setDeletingCategory(null);
    setDeleteNeedsTarget(false);
    setDeleteTargetId("");
  };

  const removeMutation = useMutation({
    mutationFn: (vars: { id: string; targetCategoryId?: string }) =>
      adminCategoryApi.remove(vars.id, vars.targetCategoryId),
    onSuccess: () => {
      toast.success("Kategori silindi");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      closeDelete();
    },
    onError: (err) => {
      const errorCode = axios.isAxiosError(err)
        ? (err.response?.data as { errorCode?: string } | undefined)?.errorCode
        : undefined;
      if (errorCode === "TARGET_CATEGORY_REQUIRED") {
        setDeleteNeedsTarget(true);
      } else {
        notifyApiError(err, "Silme başarısız");
      }
    },
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
            onClick={() => setDeletingCategory(c)}
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
      <DeleteCategoryDialog
        category={deletingCategory}
        needsTarget={deleteNeedsTarget}
        targetId={deleteTargetId}
        onTargetIdChange={setDeleteTargetId}
        allCategories={categoriesQuery.data ?? []}
        onCancel={closeDelete}
        onConfirm={() =>
          deletingCategory &&
          removeMutation.mutate({
            id: deletingCategory.id,
            targetCategoryId: deleteNeedsTarget
              ? deleteTargetId || undefined
              : undefined,
          })
        }
        isPending={removeMutation.isPending}
      />
    </div>
  );
}

function DeleteCategoryDialog({
  category,
  needsTarget,
  targetId,
  onTargetIdChange,
  allCategories,
  onCancel,
  onConfirm,
  isPending,
}: {
  category: Category | null;
  needsTarget: boolean;
  targetId: string;
  onTargetIdChange: (id: string) => void;
  allCategories: Category[];
  onCancel: () => void;
  onConfirm: () => void;
  isPending: boolean;
}) {
  const open = Boolean(category);
  const availableTargets = allCategories.filter((c) => c.id !== category?.id);
  const canConfirm = !needsTarget || Boolean(targetId);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onCancel()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {needsTarget
              ? `"${category?.name}" — ürünleri taşı ve sil`
              : `"${category?.name}" kategorisini sil`}
          </DialogTitle>
        </DialogHeader>

        {needsTarget ? (
          <div className="space-y-3">
            <p className="text-sm text-muted-foreground">
              Bu kategoride aktif ürünler var. Silmeden önce ürünlerin
              taşınacağı kategoriyi seç. Tüm aktif ürünler tek seferde hedefe
              taşınır.
            </p>
            <div>
              <label className="mb-1 block text-xs font-medium uppercase text-muted-foreground">
                Hedef kategori
              </label>
              <select
                value={targetId}
                onChange={(e) => onTargetIdChange(e.target.value)}
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
              >
                <option value="">Seç...</option>
                {availableTargets.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">
            Bu kategori soft delete edilecek. Altında aktif ürün varsa hedef
            kategori seçimi istenecek.
          </p>
        )}

        <div className="mt-2 flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onCancel}>
            İptal
          </Button>
          <Button
            type="button"
            variant="destructive"
            disabled={!canConfirm || isPending}
            onClick={onConfirm}
          >
            {needsTarget ? "Taşı ve Sil" : "Sil"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
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

  useEffect(() => {
    if (open) {
      reset({
        name: category?.name ?? "",
        description: category?.description ?? "",
        imageUrl: category?.imageUrl ?? "",
      });
    }
  }, [open, category, reset]);

  const createMutation = useMutation({
    mutationFn: adminCategoryApi.create,
    onSuccess: () => {
      toast.success("Kategori oluşturuldu");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      onOpenChange(false);
    },
    onError: (err) => notifyApiError(err, "Oluşturulamadı"),
  });
  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; body: CreateCategoryRequest }) =>
      adminCategoryApi.update(vars.id, vars.body),
    onSuccess: () => {
      toast.success("Kategori güncellendi");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      onOpenChange(false);
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
    <Dialog open={open} onOpenChange={onOpenChange}>
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
              onClick={() => onOpenChange(false)}
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
