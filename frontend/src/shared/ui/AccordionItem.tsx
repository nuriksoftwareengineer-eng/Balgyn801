import { AnimatePresence, motion } from "framer-motion";

/** Single expand/collapse row — controlled from the parent (see FAQPage / DesignPage
 *  for the "only one open at a time" state pattern). `content` renders with
 *  `whitespace-pre-line` so `\n` in backend-supplied text becomes a real line break. */
export function AccordionItem({
  title,
  content,
  open,
  onToggle,
}: {
  title: string;
  content: string;
  open: boolean;
  onToggle: () => void;
}) {
  return (
    <div className="border-b border-[--color-border]">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={open}
        className="flex w-full items-center justify-between gap-6 py-6 text-left transition-opacity focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-black focus-visible:ring-offset-2 md:py-7"
      >
        <span className={`text-[16px] tracking-[-0.01em] transition-colors md:text-[18px] ${open ? "font-medium text-black" : "font-normal text-black"}`}>
          {title}
        </span>
        <span
          className={`relative h-4 w-4 shrink-0 text-[--color-muted] transition-transform duration-300 ${open ? "rotate-45" : ""}`}
          aria-hidden
        >
          <span className="absolute left-1/2 top-1/2 h-[1.5px] w-4 -translate-x-1/2 -translate-y-1/2 bg-current" />
          <span className="absolute left-1/2 top-1/2 h-4 w-[1.5px] -translate-x-1/2 -translate-y-1/2 bg-current" />
        </span>
      </button>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
            className="overflow-hidden"
          >
            <p className="max-w-[640px] whitespace-pre-line pb-7 text-[14px] leading-relaxed text-zinc-600 md:text-[15px]">
              {content}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
