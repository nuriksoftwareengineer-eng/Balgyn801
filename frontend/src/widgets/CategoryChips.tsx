import { CATEGORY_LABELS, STORE_VIEW_ALL_CATEGORY } from "@/shared/constants/store-content";
import { cn } from "@/shared/lib/cn";

type Props = {
  /** `null` — все категории (как «Смотреть всё»). */
  selectedCategory: string | null;
  onSelect: (category: string | null) => void;
};

export function CategoryChips({ selectedCategory, onSelect }: Props) {
  return (
    <div
      className="flex gap-3 overflow-x-auto pb-2"
      role="list"
    >
      {CATEGORY_LABELS.map((label) => {
        const isAll = label === STORE_VIEW_ALL_CATEGORY;
        const active = isAll
          ? selectedCategory === null
          : selectedCategory === label;
        return (
          <button
            key={label}
            type="button"
            role="listitem"
            aria-pressed={active}
            onClick={() => onSelect(isAll ? null : label)}
            className={cn(
              "flex-shrink-0 rounded-full border px-[22px] py-3 text-sm font-semibold transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-violet-500",
              active
                ? "border-violet-500 bg-violet-500/20 text-zinc-100 shadow-[0_0_24px_rgba(139,92,246,0.25)]"
                : "border-white/10 bg-zinc-900 text-zinc-400 hover:border-violet-500 hover:bg-violet-500/15 hover:text-zinc-100",
            )}
          >
            {label}
          </button>
        );
      })}
    </div>
  );
}
