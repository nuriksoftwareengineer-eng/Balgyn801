import { type HTMLAttributes, type ReactNode, useEffect } from "react";
import { createPortal } from "react-dom";
import { AnimatePresence, motion } from "framer-motion";
import { cn } from "@/shared/lib/cn";

// ─── Types ────────────────────────────────────────────────────────────────

type Side = "left" | "right" | "top" | "bottom";

export interface SheetProps {
  open: boolean;
  onClose: () => void;
  side?: Side;
  /** Optional max-width/height override (Tailwind class). */
  sizeClass?: string;
  className?: string;
  children?: ReactNode;
  /**
   * Tailwind top-* class(es) that offset the panel below a sticky header.
   * e.g. "top-[92px] md:top-[104px]"
   * When set: panel starts below this offset; backdrop covers the full screen
   * but sits at z-[95] so the header (z-[100]) remains visible and clickable.
   */
  topOffset?: string;
}

// ─── Position / size maps ─────────────────────────────────────────────────

const panelPosition: Record<Side, string> = {
  right:  "inset-y-0 right-0",
  left:   "inset-y-0 left-0",
  top:    "inset-x-0 top-0",
  bottom: "inset-x-0 bottom-0",
};

const defaultSize: Record<Side, string> = {
  right:  "w-full max-w-md",
  left:   "w-full max-w-md",
  top:    "h-auto max-h-[80vh]",
  bottom: "h-auto max-h-[80vh]",
};

const spring = { type: "spring", stiffness: 380, damping: 38, mass: 0.8 } as const;

// ─── Component ────────────────────────────────────────────────────────────

export function Sheet({
  open,
  onClose,
  side = "right",
  sizeClass,
  className,
  children,
  topOffset,
}: SheetProps) {
  // Prevent body scroll when open
  useEffect(() => {
    if (open) {
      const prev = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => { document.body.style.overflow = prev; };
    }
  }, [open]);

  // Close on Escape
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  // Compute slide direction inline to keep types concrete (no intermediate object)
  const isH    = side === "left" || side === "right";
  const offset = (side === "right" || side === "bottom") ? "100%" : "-100%";
  const slideFrom = isH ? { x: offset } : { y: offset };
  const slideTo   = isH ? { x: 0 }      : { y: 0 };

  // When topOffset is provided the panel starts below the sticky header.
  // Backdrop stays full-screen but at z-[95] so the header (z-[100]) stays
  // visible and clickable. Panel is at z-[96] — above the backdrop, below header.
  const backdropZ = topOffset ? "z-[95]" : "z-40";
  const panelZ    = topOffset ? "z-[96]" : "z-50";
  const panelPos  = topOffset && isH
    ? cn(topOffset, "bottom-0", side === "right" ? "right-0" : "left-0")
    : panelPosition[side];

  return createPortal(
    <AnimatePresence>
      {open && (
        <>
          {/* Backdrop */}
          <motion.div
            key="sheet-backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className={cn("fixed inset-0 bg-black/30 backdrop-blur-[2px]", backdropZ)}
            onClick={onClose}
            aria-hidden="true"
          />

          {/* Panel */}
          <motion.div
            key="sheet-panel"
            role="dialog"
            aria-modal="true"
            initial={slideFrom}
            animate={slideTo}
            exit={slideFrom}
            transition={spring}
            className={cn(
              "fixed bg-white shadow-2xl",
              "flex flex-col overflow-hidden",
              panelZ,
              panelPos,
              sizeClass ?? defaultSize[side],
              className,
            )}
          >
            {children}
          </motion.div>
        </>
      )}
    </AnimatePresence>,
    document.body,
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────

export function SheetHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "flex items-center justify-between border-b border-[--color-border] px-6 py-5",
        className,
      )}
      {...props}
    />
  );
}

export function SheetTitle({ className, ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h2
      className={cn("text-sm font-semibold uppercase tracking-widest text-black", className)}
      {...props}
    />
  );
}

export function SheetBody({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("flex-1 overflow-y-auto px-6 py-6", className)}
      {...props}
    />
  );
}

export function SheetFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "border-t border-[--color-border] px-6 py-5",
        className,
      )}
      {...props}
    />
  );
}
