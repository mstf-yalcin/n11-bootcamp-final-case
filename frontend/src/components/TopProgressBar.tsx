import { useEffect, useState } from "react";
import { useIsFetching, useIsMutating } from "@tanstack/react-query";

export function TopProgressBar() {
  const fetching = useIsFetching({
    predicate: (q) => {
      const k = q.queryKey[0];
      return k === "cart" || k === "products" || k === "addresses";
    },
  });
  const mutating = useIsMutating();
  const active = fetching + mutating > 0;

  const [progress, setProgress] = useState(0);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    let raf: number | undefined;
    let timer: ReturnType<typeof setTimeout> | undefined;

    if (active) {
      setVisible(true);
      setProgress(20);
      const tick = () => {
        setProgress((p) => (p < 90 ? p + (90 - p) * 0.15 : p));
        raf = window.requestAnimationFrame(tick);
      };
      raf = window.requestAnimationFrame(tick);
    } else if (visible) {
      setProgress(100);
      timer = setTimeout(() => {
        setVisible(false);
        setProgress(0);
      }, 170);
    }

    return () => {
      if (raf) cancelAnimationFrame(raf);
      if (timer) clearTimeout(timer);
    };
  }, [active, visible]);

  if (!visible) return null;

  return (
    <div className="pointer-events-none fixed inset-x-0 top-0 z-[60] h-[3px] bg-transparent">
      <div
        className="h-full bg-n11 shadow-[0_0_8px_rgba(255,37,245,0.7)] transition-[width] duration-150 ease-out"
        style={{ width: `${progress}%` }}
      />
    </div>
  );
}
