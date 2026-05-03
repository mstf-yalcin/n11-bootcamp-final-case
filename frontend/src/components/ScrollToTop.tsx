import { useEffect, useLayoutEffect, useRef } from "react";
import { useLocation, useNavigationType } from "react-router-dom";

export function ScrollToTop() {
  const { pathname, search } = useLocation();
  const navType = useNavigationType();
  const rafRef = useRef<number | null>(null);

  // Browser'ın history-based scroll restoration'ını kapat — yoksa
  // bizim scrollTo'muzu eski pozisyonla geri alıyor.
  useEffect(() => {
    if ("scrollRestoration" in window.history) {
      const prev = window.history.scrollRestoration;
      window.history.scrollRestoration = "manual";
      return () => {
        window.history.scrollRestoration = prev;
      };
    }
  }, []);

  // Paint öncesi scroll. POP'da (geri/ileri) browser native pozisyonuna
  // bırakmıyoruz çünkü manual mode'dayız; her zaman tepeye gidiyoruz.
  useLayoutEffect(() => {
    const scrollNow = () => {
      window.scrollTo(0, 0);
      if (document.scrollingElement) {
        document.scrollingElement.scrollTop = 0;
      }
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
    };

    scrollNow();
    // Suspense fallback yeni sayfa render olduğunda eski scroll'u geri
    // koyabiliyor; iki frame sonra bir kez daha sıfırla.
    rafRef.current = requestAnimationFrame(() => {
      scrollNow();
      rafRef.current = requestAnimationFrame(scrollNow);
    });
    return () => {
      if (rafRef.current != null) {
        cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
    };
  }, [pathname, search, navType]);

  return null;
}
