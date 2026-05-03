import { useState } from "react";
import { Link } from "react-router-dom";
import {
  ChevronRight,
  CreditCard,
  Facebook,
  Instagram,
  Mail,
  RotateCcw,
  Star,
  TicketPercent,
  Twitter,
  Youtube,
} from "lucide-react";
import { toast } from "sonner";

const BENEFITS = [
  {
    icon: TicketPercent,
    title: "Her Alışverişte",
    subtitle: "Kupon Fırsatları",
  },
  {
    icon: Star,
    title: "Her gün Yeni",
    subtitle: "Ürünler ve Fırsatlar",
  },
  {
    icon: CreditCard,
    title: "Herkese Uygun",
    subtitle: "Ödeme Yöntemleri",
  },
  {
    icon: RotateCcw,
    title: "Kolay İade",
    subtitle: "ve İptal",
  },
];

const SECTIONS = [
  {
    title: "n11 Hakkında",
    links: [
      "Biz Kimiz",
      "Kariyer",
      "İletişim",
      "Yatırımcı İlişkileri",
      "Basın Odası",
    ],
  },
  {
    title: "Yardım & Destek",
    links: [
      "Sıkça Sorulan Sorular",
      "Canlı Yardım",
      "Nasıl Alırım",
      "İade ve Değişim",
      "Kargo Bilgileri",
    ],
  },
  {
    title: "Kategoriler",
    links: [
      "Elektronik",
      "Moda",
      "Kozmetik",
      "Ev & Yaşam",
      "Spor & Outdoor",
    ],
  },
  {
    title: "Satıcı Olmak",
    links: [
      "n11'de Satış Yap",
      "Pro Mağaza",
      "Pro Hesap",
      "Satıcı Akademisi",
      "Reklam Ver",
    ],
  },
];

export function Footer() {
  const [email, setEmail] = useState("");

  const handleSubscribe = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = email.trim();
    if (!trimmed || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      toast.error("Geçerli bir e-posta adresi gir");
      return;
    }
    toast.success("Bültenimize abone oldun, hoş geldin!");
    setEmail("");
  };

  return (
    <footer className="mt-16 border-t bg-white">
      <div className="container py-10">
        <div className="grid gap-6 border-b pb-8 sm:grid-cols-2 lg:grid-cols-4">
          {BENEFITS.map((b) => {
            const Icon = b.icon;
            return (
              <div key={b.title} className="flex items-center gap-4">
                <div className="flex h-16 w-16 flex-shrink-0 items-center justify-center rounded-full border-2 border-n11">
                  <Icon className="h-7 w-7 text-n11" />
                </div>
                <div className="text-base leading-tight">
                  <div className="font-medium">{b.title}</div>
                  <div className="font-medium">{b.subtitle}</div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="grid items-center gap-4 border-b py-8 md:grid-cols-[1fr_auto]">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-n11/10">
              <Mail className="h-5 w-5 text-n11" />
            </div>
            <div>
              <div className="text-sm font-semibold">
                İndirim ve fırsatlardan ilk sen haberdar ol
              </div>
              <div className="text-xs text-muted-foreground">
                Bültenimize abone ol, sana özel kampanyalardan e-posta yoluyla
                haberdar olalım.
              </div>
            </div>
          </div>
          <form
            onSubmit={handleSubscribe}
            className="flex w-full max-w-md items-center gap-2"
          >
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="ornek@eposta.com"
              className="h-10 flex-1 rounded-md border border-input bg-background px-3 text-sm focus:border-n11 focus:outline-none"
            />
            <button
              type="submit"
              className="h-10 rounded-md bg-neutral-900 px-4 text-sm font-medium text-white hover:bg-neutral-800"
            >
              Abone Ol
            </button>
          </form>
        </div>

        <div className="grid gap-8 py-10 sm:grid-cols-2 md:grid-cols-4">
          {SECTIONS.map((sec) => (
            <div key={sec.title}>
              <div className="mb-4">
                <h4 className="text-sm font-bold uppercase tracking-wide">
                  {sec.title}
                </h4>
                <div className="mt-1.5 h-0.5 w-8 rounded bg-n11" />
              </div>
              <ul className="space-y-2.5 text-sm">
                {sec.links.map((l) => (
                  <li key={l}>
                    <Link
                      to="#"
                      className="group inline-flex items-center gap-1 text-muted-foreground transition-colors hover:text-n11"
                    >
                      <ChevronRight className="h-3 w-3 -translate-x-1 opacity-0 transition-all group-hover:translate-x-0 group-hover:opacity-100" />
                      <span className="-ml-3 transition-all group-hover:ml-0">
                        {l}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="flex flex-col items-start gap-4 border-t pt-6 md:flex-row md:items-center md:justify-between">
          <div className="text-xs text-muted-foreground">
            © {new Date().getFullYear()} n11 — Bootcamp demo. Tüm hakları
            saklıdır.
          </div>

          <div className="flex items-center gap-2">
            <span className="mr-1 text-xs text-muted-foreground">
              Bizi takip et:
            </span>
            {[
              { Icon: Facebook, label: "Facebook" },
              { Icon: Twitter, label: "Twitter" },
              { Icon: Instagram, label: "Instagram" },
              { Icon: Youtube, label: "Youtube" },
            ].map(({ Icon, label }) => (
              <Link
                key={label}
                to="#"
                aria-label={label}
                className="flex h-9 w-9 items-center justify-center rounded-full border border-input text-muted-foreground transition-colors hover:border-n11 hover:bg-n11/5 hover:text-n11"
              >
                <Icon className="h-4 w-4" />
              </Link>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
