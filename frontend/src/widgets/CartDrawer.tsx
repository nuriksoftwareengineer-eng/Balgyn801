import { useNavigate } from "react-router-dom";
import { useCart } from "@/app/use-cart";
import { isDesignLine } from "@/app/cart-context";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Sheet,
  SheetBody,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { formatMoney } from "@/shared/lib/format-money";

// ─── Types ────────────────────────────────────────────────────────────────

interface CartDrawerProps {
  open: boolean;
  onClose: () => void;
}

// ─── Icons ────────────────────────────────────────────────────────────────

function CloseIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
      <path d="M18 6L6 18M6 6l12 12" />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
      <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" />
    </svg>
  );
}

// ─── Empty state ──────────────────────────────────────────────────────────

function EmptyCart({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate();
  return (
    <div className="flex flex-col items-center justify-center gap-5 py-16 text-center">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.25" className="text-zinc-300" aria-hidden="true">
        <path d="M6 6h15l-1.5 9h-12z" />
        <circle cx="9" cy="20" r="1" />
        <circle cx="18" cy="20" r="1" />
      </svg>
      <div>
        <p className="text-sm font-medium text-black">Корзина пуста</p>
        <p className="mt-1 text-xs text-[--color-muted]">Добавьте что-нибудь из каталога</p>
      </div>
      <Button
        size="sm"
        onClick={() => { navigate("/catalog"); onClose(); }}
      >
        В каталог
      </Button>
    </div>
  );
}

// ─── Cart line row ────────────────────────────────────────────────────────

function CartLineRow({ lineKey, title, price, imageUrl, qty, size, color, increment, decrement, removeLine }: {
  lineKey: string;
  title: string;
  price: number;
  imageUrl?: string | null;
  qty: number;
  size: string | null;
  color: string | null;
  increment: (k: string) => void;
  decrement: (k: string) => void;
  removeLine: (k: string) => void;
}) {
  const meta = [size, color].filter(Boolean).join(" · ");

  return (
    <div className="flex gap-3.5 py-4">
      {/* Thumbnail */}
      <div className="h-[72px] w-[72px] shrink-0 overflow-hidden bg-[--color-surface]">
        {imageUrl ? (
          <img src={imageUrl} alt={title} className="h-full w-full object-cover" />
        ) : (
          <div className="h-full w-full" />
        )}
      </div>

      {/* Info */}
      <div className="flex flex-1 flex-col gap-1.5">
        <div className="flex items-start justify-between gap-2">
          <p className="text-xs font-medium leading-snug text-black">{title}</p>
          <button
            type="button"
            onClick={() => removeLine(lineKey)}
            className="shrink-0 text-zinc-400 transition hover:text-black"
            aria-label="Удалить"
          >
            <TrashIcon />
          </button>
        </div>

        {meta && (
          <p className="text-[0.65rem] uppercase tracking-wider text-[--color-muted]">{meta}</p>
        )}

        <div className="flex items-center justify-between">
          {/* Qty controls */}
          <div className="flex items-center gap-px border border-[--color-border]">
            <button
              type="button"
              onClick={() => decrement(lineKey)}
              className="flex h-7 w-7 items-center justify-center text-sm transition hover:bg-[--color-surface]"
              aria-label="Убрать 1"
            >
              −
            </button>
            <span className="flex h-7 w-7 items-center justify-center text-xs font-medium">
              {qty}
            </span>
            <button
              type="button"
              onClick={() => increment(lineKey)}
              className="flex h-7 w-7 items-center justify-center text-sm transition hover:bg-[--color-surface]"
              aria-label="Добавить 1"
            >
              +
            </button>
          </div>

          <p className="text-xs font-semibold text-black">
            {formatMoney(price * qty)} ₸
          </p>
        </div>
      </div>
    </div>
  );
}

// ─── Main component ───────────────────────────────────────────────────────

export function CartDrawer({ open, onClose }: CartDrawerProps) {
  const { lines, totalQty, subtotal, increment, decrement, removeLine } = useCart();
  const navigate = useNavigate();

  const handleCheckout = () => {
    navigate("/cart");
    onClose();
  };

  return (
    <Sheet
      open={open}
      onClose={onClose}
      side="right"
      sizeClass="w-full max-w-sm"
      topOffset="top-[92px] md:top-[104px]"
    >
      <SheetHeader>
        <SheetTitle>
          Корзина{totalQty > 0 ? ` (${totalQty})` : ""}
        </SheetTitle>
        <button
          type="button"
          onClick={onClose}
          className="text-zinc-400 transition hover:text-black"
          aria-label="Закрыть"
        >
          <CloseIcon />
        </button>
      </SheetHeader>

      <SheetBody className="px-5 py-0">
        {lines.length === 0 ? (
          <EmptyCart onClose={onClose} />
        ) : (
          <div className="divide-y divide-[--color-border]">
            {lines.map((line) => (
              <CartLineRow
                key={line.lineKey}
                lineKey={line.lineKey}
                title={isDesignLine(line) ? `${line.title} (${line.garmentLabel})` : line.title}
                price={line.price}
                imageUrl={line.imageUrl}
                qty={line.qty}
                size={isDesignLine(line) ? line.sizeLabel : line.size}
                color={isDesignLine(line) ? line.colorName : line.color}
                increment={increment}
                decrement={decrement}
                removeLine={removeLine}
              />
            ))}
          </div>
        )}
      </SheetBody>

      {lines.length > 0 && (
        <SheetFooter className="flex flex-col gap-4 px-5 pb-6 pt-4">
          <Separator />
          <div className="flex items-end justify-between">
            <div>
              <p className="text-[0.6rem] uppercase tracking-[0.16em] text-[--color-muted]">
                Итого
              </p>
              <p className="mt-0.5 text-[0.6rem] text-[--color-muted]">
                {totalQty}&thinsp;{totalQty === 1 ? "позиция" : totalQty < 5 ? "позиции" : "позиций"}
              </p>
            </div>
            <span className="text-xl font-semibold text-black">
              {formatMoney(subtotal)} ₸
            </span>
          </div>
          <Button size="lg" className="w-full" onClick={handleCheckout}>
            Оформить заказ
          </Button>
        </SheetFooter>
      )}
    </Sheet>
  );
}
