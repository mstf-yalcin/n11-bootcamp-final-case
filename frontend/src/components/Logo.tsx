import { cn } from "@/lib/utils";

const LOGO_SRC =
  "data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48' fill='none' xmlns:v='https://vecta.io/nano'%3E%3Cg clip-path='url(%23A)'%3E%3Crect x='.057' width='48.114' height='48.114' rx='24.057' fill='%23f4e'/%3E%3Cg fill='%232c2b33'%3E%3Cpath d='M32.171 53.371c8.837 0 16-7.163 16-16s-7.163-16-16-16-16 7.163-16 16 7.163 16 16 16zM2.855 29.333a13.32 13.32 0 0 0 13.316-13.314A13.32 13.32 0 0 0 2.855 2.705c-7.354 0-13.312 5.961-13.312 13.314s5.961 13.314 13.312 13.314zm29.316-18.592c4.416 0 8-3.582 8-8a8 8 0 0 0-8-7.998c-4.416 0-8 3.582-8 8s3.584 8 8 8v-.002z'/%3E%3C/g%3E%3C/g%3E%3Cdefs%3E%3CclipPath id='A'%3E%3Crect x='.057' width='48.114' height='48.114' rx='24.057' fill='%23fff'/%3E%3C/clipPath%3E%3C/defs%3E%3C/svg%3E";

export function Logo({
  className,
  withText = false,
  size = 36,
}: {
  className?: string;
  withText?: boolean;
  size?: number;
}) {
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <img
        src={LOGO_SRC}
        alt="n11"
        width={size}
        height={size}
        className="flex-shrink-0"
        style={{ width: size, height: size }}
      />
      {withText && (
        <span className="text-xl font-semibold tracking-tight">
          n<span className="text-n11">11</span>
        </span>
      )}
    </span>
  );
}
