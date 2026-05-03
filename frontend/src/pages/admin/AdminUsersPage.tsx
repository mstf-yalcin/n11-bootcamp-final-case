import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { Search, Shield, ShieldOff, UserCheck, UserX } from "lucide-react";
import { adminUserApi } from "@/api/endpoints";
import { API_BASE } from "@/api/client";
import { notifyApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { DataTable, type Column } from "@/components/DataTable";
import { useConfirm } from "@/components/ConfirmDialog";
import { formatDate } from "@/lib/utils";
import { usePageTitle } from "@/hooks/usePageTitle";
import { Roles } from "@/types/api";
import type { AdminUserResponse } from "@/types/api";

export default function AdminUsersPage() {
  usePageTitle("Admin · Kullanıcılar");
  const [params, setParams] = useSearchParams();
  const search = params.get("search") ?? "";
  const role = params.get("role") ?? "";
  const page = Number(params.get("page") ?? "0");
  const [searchLocal, setSearchLocal] = useState(search);

  const queryClient = useQueryClient();
  const { confirm, dialog: confirmDialog } = useConfirm();

  const usersQuery = useQuery({
    queryKey: ["admin", "users", { search, role, page }],
    queryFn: () =>
      adminUserApi.list({
        search: search || undefined,
        role: role || undefined,
        page,
        size: 20,
      }),
  });

  const rolesMutation = useMutation({
    mutationFn: (vars: { id: string; roles: string[] }) =>
      adminUserApi.updateRoles(vars.id, vars.roles),
    onSuccess: () => {
      toast.success("Roller güncellendi");
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    },
    onError: (err) => notifyApiError(err, "Güncelleme başarısız"),
  });

  const statusMutation = useMutation({
    mutationFn: (vars: { id: string; isActive: boolean }) =>
      adminUserApi.updateStatus(vars.id, vars.isActive),
    onSuccess: () => {
      toast.success("Durum güncellendi");
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    },
    onError: (err) => notifyApiError(err, "Güncelleme başarısız"),
  });

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const next = new URLSearchParams(params);
    if (searchLocal.trim()) next.set("search", searchLocal.trim());
    else next.delete("search");
    next.delete("page");
    setParams(next, { replace: true });
  };

  const setRoleFilter = (value: string) => {
    const next = new URLSearchParams(params);
    if (value) next.set("role", value);
    else next.delete("role");
    next.delete("page");
    setParams(next, { replace: true });
  };

  const setPage = (p: number) => {
    const next = new URLSearchParams(params);
    next.set("page", String(p));
    setParams(next, { replace: true });
  };

  const toggleAdmin = async (user: AdminUserResponse) => {
    const isAdmin = user.roles.includes(Roles.ADMIN);
    const ok = await confirm({
      title: isAdmin ? "Admin yetkisini kaldır" : "Admin yetkisi ver",
      description: isAdmin
        ? `${user.email} kullanıcısının admin yetkisini kaldırmak istediğine emin misin?`
        : `${user.email} kullanıcısına admin yetkisi vermek istediğine emin misin?`,
      destructive: isAdmin,
      confirmLabel: isAdmin ? "Yetkiyi Kaldır" : "Yetki Ver",
    });
    if (!ok) return;
    const nextRoles = isAdmin
      ? user.roles.filter((r) => r !== Roles.ADMIN)
      : [...new Set([...user.roles, Roles.ADMIN])];
    rolesMutation.mutate({ id: user.id, roles: nextRoles });
  };

  const toggleActive = async (user: AdminUserResponse) => {
    const ok = await confirm({
      title: user.isActive ? "Kullanıcıyı pasifleştir" : "Kullanıcıyı aktive et",
      description: user.isActive
        ? `${user.email} pasifleştirilirse hesabına giriş yapamaz.`
        : `${user.email} tekrar aktive edilecek.`,
      destructive: user.isActive,
      confirmLabel: user.isActive ? "Pasifleştir" : "Aktive Et",
    });
    if (!ok) return;
    statusMutation.mutate({ id: user.id, isActive: !user.isActive });
  };

  const columns: Column<AdminUserResponse>[] = [
    {
      key: "name",
      header: "Kullanıcı",
      cell: (u) => (
        <div>
          <div className="font-medium">
            {u.firstName} {u.lastName}
          </div>
          <div className="text-xs text-muted-foreground">{u.email}</div>
        </div>
      ),
    },
    {
      key: "phone",
      header: "Telefon",
      cell: (u) => (
        <span className="text-muted-foreground">{u.phone ?? "—"}</span>
      ),
    },
    {
      key: "roles",
      header: "Roller",
      cell: (u) => (
        <div className="flex flex-wrap gap-1">
          {u.roles.map((r) => (
            <Badge
              key={r}
              variant={r === Roles.ADMIN ? "default" : "secondary"}
              className="text-[10px]"
            >
              {r}
            </Badge>
          ))}
        </div>
      ),
    },
    {
      key: "isActive",
      header: "Durum",
      cell: (u) =>
        u.isActive ? (
          <Badge variant="success">Aktif</Badge>
        ) : (
          <Badge variant="destructive">Pasif</Badge>
        ),
    },
    {
      key: "createdAt",
      header: "Kayıt",
      cell: (u) => (
        <span className="text-xs text-muted-foreground">
          {formatDate(u.createdAt)}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "120px",
      className: "text-right",
      cell: (u) => (
        <div className="flex justify-end gap-1">
          <button
            onClick={() => toggleAdmin(u)}
            className="rounded p-1.5 text-muted-foreground hover:bg-accent hover:text-n11"
            aria-label={
              u.roles.includes(Roles.ADMIN)
                ? "Admin yetkisini kaldır"
                : "Admin yap"
            }
          >
            {u.roles.includes(Roles.ADMIN) ? (
              <ShieldOff className="h-4 w-4" />
            ) : (
              <Shield className="h-4 w-4" />
            )}
          </button>
          <button
            onClick={() => toggleActive(u)}
            className="rounded p-1.5 text-muted-foreground hover:bg-accent hover:text-n11"
            aria-label={u.isActive ? "Pasifleştir" : "Aktive et"}
          >
            {u.isActive ? (
              <UserX className="h-4 w-4" />
            ) : (
              <UserCheck className="h-4 w-4" />
            )}
          </button>
        </div>
      ),
    },
  ];

  const pageInfo = usersQuery.data?.page;

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Kullanıcılar</h1>
        <p className="text-sm text-muted-foreground">
          {pageInfo
            ? `Toplam ${pageInfo.totalElements} kullanıcı`
            : "Yükleniyor..."}
        </p>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <form onSubmit={onSearch} className="flex flex-1 max-w-md gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={searchLocal}
              onChange={(e) => setSearchLocal(e.target.value)}
              placeholder="İsim veya e-posta ara..."
              className="pl-10"
            />
          </div>
          <Button type="submit" variant="outline">
            Ara
          </Button>
        </form>

        <select
          value={role}
          onChange={(e) => setRoleFilter(e.target.value)}
          className="h-10 rounded-md border border-input bg-background px-3 text-sm"
        >
          <option value="">Tüm Roller</option>
          <option value="ADMIN">Admin</option>
          <option value="USER">User</option>
        </select>
      </div>

      <DataTable
        columns={columns}
        rows={usersQuery.data?.items}
        rowKey={(u) => u.id}
        isLoading={usersQuery.isLoading}
        isError={usersQuery.isError}
        emptyMessage="Bu kriterlere uygun kullanıcı yok."
        errorMessage={`Kullanıcılar yüklenemedi. Admin endpoint'i (GET ${API_BASE}/admin/users) henüz aktif değil olabilir.`}
      />

      {pageInfo && pageInfo.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={!pageInfo.hasPrevious}
            onClick={() => setPage(page - 1)}
          >
            Önceki
          </Button>
          <span className="text-sm text-muted-foreground">
            Sayfa {pageInfo.pageNumber + 1} / {pageInfo.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={!pageInfo.hasNext}
            onClick={() => setPage(page + 1)}
          >
            Sonraki
          </Button>
        </div>
      )}

      {confirmDialog}
    </div>
  );
}
