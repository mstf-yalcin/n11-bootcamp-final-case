import { forwardRef, type ReactNode } from "react";
import { cn } from "@/lib/utils";

type Props = React.InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
  suffix?: ReactNode;
};

export const FloatingInput = forwardRef<HTMLInputElement, Props>(
  ({ label, error, id, className, suffix, placeholder, ...props }, ref) => (
    <div>
      <div className="relative">
        <input
          id={id}
          ref={ref}
          placeholder={placeholder ?? " "}
          className={cn(
            "peer h-14 w-full rounded-md border border-input bg-background px-3 pb-1 pt-6 text-sm outline-none transition-colors placeholder:text-transparent focus:border-n11 focus:ring-2 focus:ring-n11/20 focus:placeholder:text-muted-foreground",
            error &&
              "border-destructive focus:border-destructive focus:ring-destructive/20",
            suffix && "pr-10",
            className
          )}
          {...props}
        />
        <label
          htmlFor={id}
          className={cn(
            "pointer-events-none absolute left-3 top-1/2 origin-[0_0] -translate-y-1/2 text-sm text-muted-foreground transition-all duration-150 ease-out",
            "peer-focus:top-2 peer-focus:translate-y-0 peer-focus:text-[11px] peer-focus:font-medium peer-focus:uppercase peer-focus:text-n11",
            "peer-[:not(:placeholder-shown)]:top-2 peer-[:not(:placeholder-shown)]:translate-y-0 peer-[:not(:placeholder-shown)]:text-[11px] peer-[:not(:placeholder-shown)]:font-medium peer-[:not(:placeholder-shown)]:uppercase",
            error &&
              "peer-[:not(:placeholder-shown)]:text-destructive peer-focus:text-destructive"
          )}
        >
          {label}
        </label>
        {suffix && (
          <div className="absolute right-2 top-1/2 -translate-y-1/2">
            {suffix}
          </div>
        )}
      </div>
      {error && <p className="mt-1 px-1 text-xs text-destructive">{error}</p>}
    </div>
  )
);
FloatingInput.displayName = "FloatingInput";
