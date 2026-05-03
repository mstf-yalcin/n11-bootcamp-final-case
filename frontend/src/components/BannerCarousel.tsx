import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Link } from "react-router-dom";
import { cn } from "@/lib/utils";

type Slide = {
  eyebrow: string;
  title: string;
  description: string;
  ctaLabel: string;
  ctaTo: string;
  imageUrl: string;
  overlayClass: string;
};

const SLIDES: Slide[] = [
  {
    eyebrow: "Yeni Sezon",
    title: "Moda'da Yeni Koleksiyon",
    description:
      "Kadın, erkek ve çocuk giyimde yüzlerce yeni ürün — stiline yakışanı keşfet.",
    ctaLabel: "Modayı Keşfet",
    ctaTo: "/products?category=moda",
    imageUrl:
      "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1600&q=80",
    overlayClass: "bg-gradient-to-r from-n11/95 via-n11/80 to-n11/30",
  },
  {
    eyebrow: "Süper Fırsatlar",
    title: "Elektronik'te Büyük İndirim",
    description:
      "Telefon, laptop, kulaklık ve daha fazlasında %50'ye varan fırsatlar.",
    ctaLabel: "Keşfet",
    ctaTo: "/products?category=elektronik",
    imageUrl:
      "https://images.unsplash.com/photo-1498049794561-7780e7231661?auto=format&fit=crop&w=1600&q=80",
    overlayClass:
      "bg-gradient-to-r from-violet-700/95 via-fuchsia-600/70 to-n11/20",
  },
  {
    eyebrow: "Ücretsiz Kargo",
    title: "Tüm Siparişlerde Kargo Bedava",
    description:
      "Tüm Türkiye'ye 1-3 iş günü içinde teslim, ek ücret yok.",
    ctaLabel: "Alışverişe Başla",
    ctaTo: "/products",
    imageUrl:
      "https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?auto=format&fit=crop&w=1600&q=80",
    overlayClass: "bg-gradient-to-r from-zinc-900/95 via-zinc-800/80 to-zinc-700/40",
  },
];

const ROTATE_MS = 6000;

export function BannerCarousel() {
  const [active, setActive] = useState(0);

  useEffect(() => {
    const t = setInterval(
      () => setActive((i) => (i + 1) % SLIDES.length),
      ROTATE_MS
    );
    return () => clearInterval(t);
  }, []);

  const go = (delta: number) => {
    setActive((i) => (i + delta + SLIDES.length) % SLIDES.length);
  };

  return (
    <div className="relative overflow-hidden rounded-2xl">
      <div
        className="flex transition-transform duration-500 ease-in-out"
        style={{ transform: `translateX(-${active * 100}%)` }}
      >
        {SLIDES.map((slide, i) => (
          <div
            key={i}
            className="relative flex min-h-[280px] min-w-full items-center overflow-hidden text-white md:min-h-[420px]"
          >
            <img
              src={slide.imageUrl}
              alt=""
              className="absolute inset-0 h-full w-full object-cover"
              loading={i === 0 ? "eager" : "lazy"}
            />
            <div className={cn("absolute inset-0", slide.overlayClass)} />
            <div className="relative px-8 py-12 md:px-20 md:py-16">
              <div className="max-w-xl">
                <div className="mb-2 inline-block rounded-full bg-white/20 px-3 py-1 text-xs font-semibold backdrop-blur">
                  {slide.eyebrow}
                </div>
                <h2 className="mb-3 text-2xl font-bold drop-shadow md:text-4xl">
                  {slide.title}
                </h2>
                <p className="mb-5 text-sm text-white/95 md:text-base">
                  {slide.description}
                </p>
                <Link
                  to={slide.ctaTo}
                  className="inline-block rounded-md bg-white px-5 py-2.5 text-sm font-semibold text-foreground shadow hover:bg-white/95"
                >
                  {slide.ctaLabel} →
                </Link>
              </div>
            </div>
          </div>
        ))}
      </div>

      <button
        onClick={() => go(-1)}
        className="absolute left-2 top-1/2 hidden h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white/30 text-white backdrop-blur transition-colors hover:bg-white/50 md:flex"
        aria-label="Önceki"
      >
        <ChevronLeft className="h-5 w-5" />
      </button>
      <button
        onClick={() => go(1)}
        className="absolute right-2 top-1/2 hidden h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white/30 text-white backdrop-blur transition-colors hover:bg-white/50 md:flex"
        aria-label="Sonraki"
      >
        <ChevronRight className="h-5 w-5" />
      </button>

      <div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-2">
        {SLIDES.map((_, i) => (
          <button
            key={i}
            onClick={() => setActive(i)}
            aria-label={`Slayt ${i + 1}`}
            className={cn(
              "h-1.5 rounded-full transition-all",
              i === active
                ? "w-6 bg-white"
                : "w-1.5 bg-white/50 hover:bg-white/70"
            )}
          />
        ))}
      </div>
    </div>
  );
}
