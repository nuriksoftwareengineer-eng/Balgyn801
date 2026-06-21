import { forwardRef, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

// ─── Types ────────────────────────────────────────────────────────────────

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  /** Slot rendered inside the input on the right (e.g. password toggle). */
  suffix?: ReactNode;
}

// ─── Component ────────────────────────────────────────────────────────────

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, label, error, hint, suffix, id, ...props },
  ref,
) {
  const inputId = id ?? (label ? label.toLowerCase().replace(/\s+/g, "-") : undefined);

  return (
    <div className="flex flex-col gap-1">
      {label && (
        <label
          htmlFor={inputId}
          className="text-xs font-medium uppercase tracking-widest text-black"
        >
          {label}
        </label>
      )}

      <div className="relative">
        <input
          ref={ref}
          id={inputId}
          className={cn(
            // COS-style: no border-radius, bottom border only
            "w-full bg-transparent py-2.5 text-sm text-black placeholder:text-zinc-400",
            "border-0 border-b border-zinc-300 outline-none",
            "transition-colors duration-150",
            "focus:border-black",
            error && "border-[--color-danger] focus:border-[--color-danger]",
            suffix && "pr-10",
            className,
          )}
          {...props}
        />
        {suffix && (
          <div className="absolute inset-y-0 right-0 flex items-center">
            {suffix}
          </div>
        )}
      </div>

      {error && (
        <p className="text-xs text-[--color-danger]">{error}</p>
      )}
      {!error && hint && (
        <p className="text-xs text-[--color-muted]">{hint}</p>
      )}
    </div>
  );
});
