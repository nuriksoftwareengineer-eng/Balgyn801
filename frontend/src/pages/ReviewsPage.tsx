import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getPublishedReviews } from "@/shared/api/backend-api";
import { ReviewCard } from "@/shared/ui/ReviewCard";
import { Container } from "@/shared/ui/container";

function StarsSummary({ reviews }: { reviews: { rating: number }[] }) {
  if (!reviews.length) return null;
  const avg = reviews.reduce((s, r) => s + r.rating, 0) / reviews.length;
  return (
    <div className="flex items-center gap-2">
      <span className="text-3xl font-semibold text-black">{avg.toFixed(1)}</span>
      <div>
        <div className="flex gap-0.5">
          {Array.from({ length: 5 }, (_, i) => (
            <svg
              key={i}
              viewBox="0 0 12 12"
              className={`h-4 w-4 ${i < Math.round(avg) ? "text-black" : "text-zinc-300"}`}
              fill="currentColor"
            >
              <path d="M6 0l1.545 3.13L11 3.635l-2.5 2.435.59 3.44L6 7.87l-3.09 1.64.59-3.44L1 3.635l3.455-.505z" />
            </svg>
          ))}
        </div>
        <p className="text-xs text-[--color-muted]">{reviews.length} отзывов</p>
      </div>
    </div>
  );
}

export function ReviewsPage() {
  const { data: reviews = [], isLoading, isError } = useQuery({
    queryKey: ["shop-reviews-all"],
    queryFn: () => getPublishedReviews(100),
    staleTime: 5 * 60 * 1000,
  });

  return (
    <div className="py-12 md:py-16">
      <Container>
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.14em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">Главная</Link>
          <span aria-hidden>›</span>
          <span className="text-black">Отзывы</span>
        </nav>

        <div className="mb-10 flex flex-wrap items-end justify-between gap-6">
          <div>
            <h1 className="text-3xl font-semibold uppercase tracking-[0.04em] text-black md:text-4xl">
              Отзывы
            </h1>
            <p className="mt-2 text-sm text-[--color-muted]">
              Реальные отзывы наших клиентов
            </p>
          </div>
          {reviews.length > 0 && <StarsSummary reviews={reviews} />}
        </div>

        {isLoading && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }, (_, i) => (
              <div key={i} className="h-48 animate-pulse border border-[--color-border] bg-[--color-surface]" />
            ))}
          </div>
        )}

        {isError && (
          <p className="text-sm text-red-600">Не удалось загрузить отзывы.</p>
        )}

        {!isLoading && !isError && reviews.length === 0 && (
          <p className="text-sm text-[--color-muted]">Отзывов пока нет.</p>
        )}

        {reviews.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {reviews.map((r) => (
              <ReviewCard key={r.id} review={r} />
            ))}
          </div>
        )}
      </Container>
    </div>
  );
}
