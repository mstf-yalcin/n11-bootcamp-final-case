import { Link } from "react-router-dom";
import { cn } from "@/lib/utils";

const ACTIVE = "bg-foreground text-white shadow-sm";
const INACTIVE =
  "text-muted-foreground hover:text-foreground hover:bg-white/60";

export function AuthTabs({ active }: { active: "login" | "register" }) {
  return (
    <div className="grid grid-cols-2 rounded-md border bg-secondary/40 p-1">
      <Link
        to="/login"
        className={cn(
          "rounded-md py-2 text-center text-sm font-semibold transition-colors",
          active === "login" ? ACTIVE : INACTIVE
        )}
      >
        Giriş Yap
      </Link>
      <Link
        to="/register"
        className={cn(
          "rounded-md py-2 text-center text-sm font-semibold transition-colors",
          active === "register" ? ACTIVE : INACTIVE
        )}
      >
        Üye Ol
      </Link>
    </div>
  );
}
