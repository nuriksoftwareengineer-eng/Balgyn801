import { useEffect, useRef, useState, type ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

export interface DropdownOption {
  value: string;
  label: string;
}

interface Props {
  trigger: ReactNode;
  options: DropdownOption[];
  value: string;
  onChange: (value: string) => void;
  className?: string;
  triggerClassName?: string;
  align?: "left" | "right";
}

export function CompactDropdown({ trigger, options, value, onChange, className, triggerClassName, align = "right" }: Props) {
  const [open, setOpen] = useState(false);
  const [focusIdx, setFocusIdx] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<(HTMLButtonElement | null)[]>([]);

  useEffect(() => {
    if (!open) return;
    function handleOutside(e: MouseEvent) {
      if (!containerRef.current?.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", handleOutside);
    return () => document.removeEventListener("mousedown", handleOutside);
  }, [open]);

  useEffect(() => {
    if (open && focusIdx >= 0) {
      itemRefs.current[focusIdx]?.focus();
    }
  }, [open, focusIdx]);

  function handleTriggerKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" || e.key === " " || e.key === "ArrowDown") {
      e.preventDefault();
      setOpen(true);
      setFocusIdx(0);
    }
  }

  function handleListKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Escape") {
      e.preventDefault();
      setOpen(false);
      setFocusIdx(-1);
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      setFocusIdx((i) => Math.min(i + 1, options.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setFocusIdx((i) => Math.max(i - 1, 0));
    } else if (e.key === "Tab") {
      setOpen(false);
    }
  }

  return (
    <div ref={containerRef} className={cn("relative", className)}>
      <button
        type="button"
        onClick={() => { setOpen((o) => !o); setFocusIdx(-1); }}
        onKeyDown={handleTriggerKeyDown}
        aria-haspopup="listbox"
        aria-expanded={open}
        className={cn("flex h-7 items-center gap-[3px] px-1.5 text-[0.6rem] font-semibold uppercase tracking-[0.08em] text-[--color-muted] transition-colors hover:text-black", triggerClassName)}
      >
        {trigger}
        <svg
          width="7"
          height="5"
          viewBox="0 0 7 5"
          fill="none"
          aria-hidden="true"
          className={cn("mt-px transition-transform duration-150", open && "rotate-180")}
        >
          <path d="M1 1l2.5 2.5L6 1" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {open && (
        <div
          role="listbox"
          onKeyDown={handleListKeyDown}
          className={cn(
            "absolute top-full z-50 mt-0.5 min-w-[76px] border border-[--color-border] bg-white text-black py-0.5 shadow-sm",
            align === "right" ? "right-0" : "left-0",
          )}
        >
          {options.map((opt, i) => (
            <button
              key={opt.value}
              ref={(el) => { itemRefs.current[i] = el; }}
              type="button"
              role="option"
              aria-selected={opt.value === value}
              onClick={() => { onChange(opt.value); setOpen(false); setFocusIdx(-1); }}
              className={cn(
                "w-full px-3 py-2 text-left text-[0.65rem] font-semibold uppercase tracking-[0.08em] transition-colors hover:bg-gray-100",
                opt.value === value ? "text-black" : "text-[--color-muted]",
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
