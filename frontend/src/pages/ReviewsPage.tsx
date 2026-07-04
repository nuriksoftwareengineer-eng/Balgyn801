import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { getPublishedReviews } from "@/shared/api/backend-api";
import { ReviewCard } from "@/shared/ui/ReviewCard";
import { Container } from "@/shared/ui/container";

const PAGE_SIZE = 12;
const STAR_FILTERS = [0, 5, 4, 3, 2, 1] as const;

function StarIcon({ filled }: { filled: boolean }) {
  return (
    <svg
      viewBox="0 0 12 12"
      className={`h-3.5 w-3.5 ${filled ? "text-black" : "text-zinc-300"}`}
      fill="currentColor"
    >
      <path d="M6 0l1.545 3.13L11 3.635l-2.5 2.435.59 3.44L6 7.87l-3.09 1.64.59-3.44L1 3.635l3.455-.505z" />
    </svg>
  );
}

function StarsSummary({ reviews }: { reviews: { rating: number }[] }) {
  if (!reviews.length) return null;
  const avg = reviews.reduce((s, r) => s + r.rating, 0) / reviews.length;
  return (
    <div className="flex items-center gap-3">
      <span className="text-3xl font-semibold text-black">{avg.toFixed(1)}</span>
      <div>
        <div className="flex gap-0.5">
          {Array.from({ length: 5 }, (_, i) => (
            <StarIcon key={i} filled={i < Math.round(avg)} />
          ))}
        </div>
        <p className="text-xs text-[--color-muted]">{reviews.length}</p>
      </div>
    </div>
  );
}

export function ReviewsPage() {
  const { t } = useTranslation();
  const [starFilter, setStarFilter] = useState<0 | 1 | 2 | 3 | 4 | 5>(0);
  const [page, setPage] = useState(1);

  const { data: reviews = [], isLoading, isError } = useQuery({
    queryKey: ["shop-reviews-all"],
    queryFn: () => getPublishedReviews(200),
    staleTime: 5 * 60 * 1000,
  });

  const filtered = starFilter === 0
    ? reviews
    : reviews.filter((r) => r.rating === starFilter);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const pageItems = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  function handleFilter(star: typeof starFilter) {
    setStarFilter(star);
    setPage(1);
  }

  return (
    <div className="py-12 md:py-16">
      <Container>
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.14em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">
            {t("aboutPage.breadcrumbHome")}
          </Link>
          <span aria-hidden>›</span>
          <span className="text-black">{t("reviews.breadcrumb", "Отзывы")}</span>
        </nav>

        <div className="mb-8 flex flex-wrap items-end justify-between gap-6">
          <div>
            <h1 className="text-3xl font-semibold uppercase tracking-[0.04em] text-black md:text-4xl">
              {t("reviews.title", "Отзывы")}
            </h1>
            <p className="mt-2 text-sm text-[--color-muted]">
              {t("reviews.subtitle", "Реальные отзывы наших покупателей")}
            </p>
          </div>
          {reviews.length > 0 && <StarsSummary reviews={reviews} />}
        </div>

        {/* Star filter */}
        {reviews.length > 0 && (
          <div className="mb-8 flex flex-wrap gap-2">
            {STAR_FILTERS.map((star) => (
              <button
                key={star}
                type="button"
                onClick={() => handleFilter(star)}
                className={`flex items-center gap-1.5 border px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.08em] transition-colors ${
                  starFilter === star
                    ? "border-black bg-black text-white"
                    : "border-[--color-border] bg-white text-black hover:border-black"
                }`}
              >
                {star === 0 ? (
                  t("reviews.filterAll", "Все")
                ) : (
                  <>
                    {star}
                    <StarIcon filled />
                  </>
                )}
              </button>
            ))}
          </div>
        )}

        {isLoading && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }, (_, i) => (
              <div
                key={i}
                className="h-48 animate-pulse border border-[--color-border] bg-[--color-surface]"
              />
            ))}
          </div>
        )}

        {isError && (
          <p className="text-sm text-red-600">
            {t("reviews.loadError", "Не удалось загрузить отзывы.")}
          </p>
        )}

        {!isLoading && !isError && filtered.length === 0 && (
          <p className="text-sm text-[--color-muted]">
            {t("reviews.noReviews", "Отзывов пока нет.")}
          </p>
        )}

        {pageItems.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {pageItems.map((r) => (
              <ReviewCard key={r.id} review={r} />
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="mt-10 flex items-center justify-center gap-4">
            <button
              type="button"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={safePage === 1}
              className="border border-[--color-border] px-4 py-2 text-[12px] font-semibold uppercase tracking-[0.1em] transition-colors hover:border-black disabled:opacity-30"
            >
              {t("reviews.prev", "← Назад")}
            </button>
            <span className="text-[12px] text-[--color-muted]">
              {safePage} / {totalPages}
            </span>
            <button
              type="button"
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={safePage === totalPages}
              className="border border-[--color-border] px-4 py-2 text-[12px] font-semibold uppercase tracking-[0.1em] transition-colors hover:border-black disabled:opacity-30"
            >
              {t("reviews.next", "Далее →")}
            </button>
          </div>
        )}
      </Container>
    </div>
  );
}
