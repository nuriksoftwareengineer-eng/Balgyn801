import { CATEGORY_LABELS } from "@/shared/constants/store-content";

export function CategoryChips() {
  return (
    <div
      className="flex gap-3 overflow-x-auto pb-2"
      role="list"
    >
      {CATEGORY_LABELS.map((label) => (
        <button
          key={label}
          type="button"
          role="listitem"
          className="flex-shrink-0 rounded-full border border-white/10 bg-zinc-900 px-[22px] py-3 text-sm font-semibold text-zinc-400 transition hover:border-violet-500 hover:bg-violet-500/15 hover:text-zinc-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-violet-500"
        >
          {label}
        </button>
      ))}
    </div>
  );
}
