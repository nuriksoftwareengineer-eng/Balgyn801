interface AdminPaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPage: (page: number) => void;
}

export function AdminPagination({ page, totalPages, totalElements, size, onPage }: AdminPaginationProps) {
  if (totalPages <= 1) return null;

  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-xs text-zinc-500">
      <span>{from}–{to} из {totalElements}</span>
      <div className="flex items-center gap-1">
        <button
          type="button"
          disabled={page === 0}
          onClick={() => onPage(0)}
          className="rounded px-2 py-1 hover:bg-white/10 disabled:opacity-30"
          aria-label="Первая страница"
        >
          «
        </button>
        <button
          type="button"
          disabled={page === 0}
          onClick={() => onPage(page - 1)}
          className="rounded px-2 py-1 hover:bg-white/10 disabled:opacity-30"
          aria-label="Предыдущая"
        >
          ‹
        </button>
        <span className="px-2 tabular-nums text-zinc-300">
          {page + 1} / {totalPages}
        </span>
        <button
          type="button"
          disabled={page >= totalPages - 1}
          onClick={() => onPage(page + 1)}
          className="rounded px-2 py-1 hover:bg-white/10 disabled:opacity-30"
          aria-label="Следующая"
        >
          ›
        </button>
        <button
          type="button"
          disabled={page >= totalPages - 1}
          onClick={() => onPage(totalPages - 1)}
          className="rounded px-2 py-1 hover:bg-white/10 disabled:opacity-30"
          aria-label="Последняя страница"
        >
          »
        </button>
      </div>
    </div>
  );
}

interface AdminSearchBarProps {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
}

export function AdminSearchBar({ value, onChange, placeholder = "Поиск…" }: AdminSearchBarProps) {
  return (
    <div className="relative">
      <svg
        className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
        width="14" height="14" viewBox="0 0 20 20" fill="none" aria-hidden="true"
      >
        <circle cx="8.5" cy="8.5" r="6" stroke="currentColor" strokeWidth="1.75" />
        <path d="M13 13l4 4" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
      </svg>
      <input
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="rounded-[8px] border border-white/15 bg-zinc-800 py-2 pl-9 pr-3 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-white/40 w-64"
      />
    </div>
  );
}
