import { useEffect } from "react";

const SUFFIX = "n11";
const DEFAULT_TITLE = "n11 — Alışverişin uğurlu adresi";

export function usePageTitle(title: string | undefined | null) {
  useEffect(() => {
    document.title = title ? `${title} | ${SUFFIX}` : DEFAULT_TITLE;
  }, [title]);
}
