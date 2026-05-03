import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { adminProductApi, categoryApi, tagApi } from "@/api/endpoints";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { FloatingInput } from "@/components/ui/floating-input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { CreateProductRequest, Product } from "@/types/api";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  product?: Product | null;
};

type FormValues = CreateProductRequest;

export function AdminProductFormDialog({ open, onOpenChange, product }: Props) {
  const queryClient = useQueryClient();
  const isEdit = Boolean(product);

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
    enabled: open,
  });
  const tagsQuery = useQuery({
    queryKey: ["tags"],
    queryFn: tagApi.list,
    enabled: open,
  });

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      name: "",
      description: "",
      price: 0,
      currency: "TRY",
      imageUrl: "",
      categoryId: "",
      tagIds: [],
    },
  });

  useEffect(() => {
    if (open) {
      reset({
        name: product?.name ?? "",
        description: product?.description ?? "",
        price: product ? Number(product.price) : 0,
        currency: product?.currency ?? "TRY",
        imageUrl: product?.imageUrl ?? "",
        categoryId: product?.categoryId ?? "",
        tagIds: product?.tags.map((t) => t.id) ?? [],
      });
    }
  }, [open, product, reset]);

  const createMutation = useMutation({
    mutationFn: adminProductApi.create,
    onSuccess: () => {
      toast.success("Ürün oluşturuldu");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      onOpenChange(false);
    },
    onError: (err) => notifyApiError(err, "Ürün oluşturulamadı"),
  });

  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; body: CreateProductRequest }) =>
      adminProductApi.update(vars.id, vars.body),
    onSuccess: () => {
      toast.success("Ürün güncellendi");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      onOpenChange(false);
    },
    onError: (err) => notifyApiError(err, "Ürün güncellenemedi"),
  });

  const onSubmit = (data: FormValues) => {
    const body: CreateProductRequest = {
      name: data.name,
      description: data.description?.trim() || undefined,
      price: Number(data.price),
      currency: data.currency,
      categoryId: data.categoryId,
      tagIds: data.tagIds?.length ? data.tagIds : undefined,
      imageUrl: data.imageUrl.trim(),
    };
    if (isEdit && product) {
      updateMutation.mutate({ id: product.id, body });
    } else {
      createMutation.mutate(body);
    }
  };

  const tagIds = watch("tagIds") ?? [];
  const imagePreview = watch("imageUrl");
  const toggleTag = (id: string) => {
    const next = tagIds.includes(id)
      ? tagIds.filter((t) => t !== id)
      : [...tagIds, id];
    setValue("tagIds", next, { shouldDirty: true });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Ürünü Düzenle" : "Yeni Ürün"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
          <FloatingInput
            id="name"
            label="Ürün Adı"
            error={errors.name?.message}
            {...register("name", {
              required: "Ürün adı zorunludur",
              maxLength: { value: 255, message: "En fazla 255 karakter" },
            })}
          />

          <div>
            <label
              htmlFor="description"
              className="mb-1 block text-xs font-medium uppercase text-muted-foreground"
            >
              Açıklama
            </label>
            <textarea
              id="description"
              rows={3}
              placeholder="Ürün açıklaması..."
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-n11"
              {...register("description", {
                required: "Açıklama zorunludur",
                minLength: { value: 10, message: "En az 10 karakter" },
                maxLength: { value: 2000, message: "En fazla 2000 karakter" },
              })}
            />
            {errors.description && (
              <p className="mt-1 text-xs text-destructive">
                {errors.description.message}
              </p>
            )}
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2">
              <FloatingInput
                id="price"
                type="number"
                step="0.01"
                label="Fiyat"
                error={errors.price?.message}
                {...register("price", {
                  required: "Fiyat zorunludur",
                  min: { value: 0.01, message: "0'dan büyük olmalı" },
                  valueAsNumber: true,
                })}
              />
            </div>
            <FloatingInput
              id="currency"
              label="Para Birimi"
              {...register("currency", { required: true, maxLength: 3 })}
            />
          </div>

          <div>
            <FloatingInput
              id="imageUrl"
              label="Görsel URL"
              placeholder="https://example.com/image.jpg"
              error={errors.imageUrl?.message}
              {...register("imageUrl", {
                required: "Görsel URL zorunludur",
                pattern: {
                  value: /^https?:\/\/.+/i,
                  message: "Geçerli bir URL gir (http/https)",
                },
              })}
            />
            {imagePreview && /^https?:\/\/.+/i.test(imagePreview) && (
              <img
                src={imagePreview}
                alt="Önizleme"
                className="mt-2 h-24 w-24 rounded-md border object-cover"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).style.display = "none";
                }}
              />
            )}
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium uppercase text-muted-foreground">
              Kategori
            </label>
            <select
              {...register("categoryId", { required: "Kategori seç" })}
              className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
            >
              <option value="">Seçin...</option>
              {categoriesQuery.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            {errors.categoryId && (
              <p className="mt-1 text-xs text-destructive">
                {errors.categoryId.message}
              </p>
            )}
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium uppercase text-muted-foreground">
              Etiketler
            </label>
            <div className="flex flex-wrap gap-1.5 rounded-md border bg-secondary/30 p-2">
              {tagsQuery.data?.length === 0 && (
                <span className="text-xs text-muted-foreground">
                  Henüz etiket yok.
                </span>
              )}
              {tagsQuery.data?.map((t) => {
                const selected = tagIds.includes(t.id);
                return (
                  <button
                    key={t.id}
                    type="button"
                    onClick={() => toggleTag(t.id)}
                    className={`rounded-full px-2.5 py-0.5 text-xs font-medium transition-colors ${
                      selected
                        ? "bg-n11 text-white"
                        : "bg-white text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    {t.name}
                  </button>
                );
              })}
            </div>
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
