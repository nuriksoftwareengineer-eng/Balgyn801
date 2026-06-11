import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

// ─── Types ────────────────────────────────────────────────────────────────

type Variant = "default" | "outline" | "ghost";
type Size    = "sm" | "md" | "lg";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children?: ReactNode;
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  /** Render as a different HTML element via asChild-like override (string tag). */
  asChild?: false;
}

// ─── Spinner ──────────────────────────────────────────────────────────────

function Spinner() {
  return (
    <svg
      className="animate-spin h-4 w-4 shrink-0"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle
        className="opacity-25"
        cx="12" cy="12" r="10"
        stroke="currentColor"
        strokeWidth="4"
      />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>
  );
}

// ─── Component ────────────────────────────────────────────────────────────

export function Button({
  className,
  variant = "default",
  size = "md",
  loading = false,
  disabled,
  children,
  ...props
}: ButtonProps) {
  const isDisabled = disabled || loading;

  return (
    <button
      type="button"
      disabled={isDisabled}
      className={cn(
        // base
        "inline-flex items-center justify-center gap-2 font-medium tracking-wide",
        "transition-all duration-150 focus-visible:outline focus-visible:outline-2",
        "focus-visible:outline-offset-2 focus-visible:outline-black",
        "disabled:pointer-events-none disabled:opacity-40",

        // variants
        variant === "default" && [
          "bg-black text-white hover:bg-zinc-800 active:bg-zinc-900",
        ],
        variant === "outline" && [
          "border border-black bg-transparent text-black",
          "hover:bg-black hover:text-white active:bg-zinc-900",
        ],
        variant === "ghost" && [
          "bg-transparent text-black hover:bg-black/5 active:bg-black/10",
        ],

        // sizes
        size === "sm" && "h-8 px-4 text-xs",
        size === "md" && "h-10 px-6 text-sm",
        size === "lg" && "h-12 px-8 text-base",

        className,
      )}
      {...props}
    >
      {loading && <Spinner />}
      {children}
    </button>
  );
}
