import { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";

interface SizeChartModalProps {
  imageUrl: string;
  title: string | null;
  onClose: () => void;
}

export function SizeChartModal({ imageUrl, title, onClose }: SizeChartModalProps) {
  const { t } = useTranslation();
  const overlayRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [onClose]);

  function handleOverlayClick(e: React.MouseEvent) {
    if (e.target === overlayRef.current) onClose();
  }

  const label = title ?? t("design.sizeChart");

  return (
    <div
      ref={overlayRef}
      onClick={handleOverlayClick}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label={label}
    >
      <div className="relative max-h-[90dvh] w-full max-w-2xl overflow-auto bg-white shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[--color-border] px-5 py-4">
          <p className="text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-black">
            {label}
          </p>
          <button
            type="button"
            onClick={onClose}
            aria-label={t("cart.close")}
            className="text-[--color-muted] transition hover:text-black"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Image */}
        <div className="p-4">
          <img
            src={imageUrl}
            alt={title ?? "Размерная сетка"}
            className="h-auto w-full object-contain"
            draggable={false}
          />
        </div>
      </div>
    </div>
  );
}
