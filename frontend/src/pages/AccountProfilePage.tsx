import { useCurrentUser } from "@/features/auth/queries";
import { Spinner } from "@/components/ui/spinner";
import { Badge } from "@/components/ui/badge";
import { usePageTitle } from "@/hooks/usePageTitle";

const ROLE_LABELS: Record<string, string> = {
  USER: "Üye",
  ADMIN: "Yönetici",
};

function roleLabel(role: string): string {
  if (ROLE_LABELS[role]) return ROLE_LABELS[role];
  return role.toLowerCase().replace(/(^|\s)\S/g, (m) => m.toUpperCase());
}

export default function AccountProfilePage() {
  usePageTitle("Profilim");
  const meQuery = useCurrentUser();

  if (meQuery.isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner size={28} />
      </div>
    );
  }

  const user = meQuery.data;
  if (!user) {
    return (
      <div className="rounded-lg border bg-white p-6 text-sm text-muted-foreground">
        Profil bilgileri alınamadı.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Profilim</h1>
      <div className="rounded-lg border bg-white">
        <dl className="divide-y">
          <Row label="Ad" value={user.firstName} />
          <Row label="Soyad" value={user.lastName} />
          <Row label="E-posta" value={user.email} />
          <Row label="Telefon" value={user.phone || "—"} />
          <div className="grid grid-cols-[160px_1fr] gap-4 px-5 py-3">
            <dt className="text-sm text-muted-foreground">Üyelik Tipi</dt>
            <dd className="flex flex-wrap gap-1.5">
              {user.roles?.length ? (
                user.roles.map((r) => (
                  <Badge key={r} variant="secondary">
                    {roleLabel(r)}
                  </Badge>
                ))
              ) : (
                <Badge variant="secondary">Üye</Badge>
              )}
            </dd>
          </div>
        </dl>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="grid grid-cols-[160px_1fr] gap-4 px-5 py-3">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium">{value}</dd>
    </div>
  );
}
