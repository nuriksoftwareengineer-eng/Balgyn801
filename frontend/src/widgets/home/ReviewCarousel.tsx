import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getPublishedReviews } from "@/shared/api/backend-api";
import { ReviewCard } from "@/shared/ui/ReviewCard";
import { Container } from "@/shared/ui/container";

export function ReviewCarousel() {
  const { data: reviews, isLoading } = useQuery({
    queryKey: ["shop-reviews-home"],
    queryFn: () => getPublishedReviews(8),
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading || !reviews?.length) return null;

  return (
    <section className="py-16 md:py-20 bg-[--color-surface]">
      <Container>
        <div className="mb-8 flex items-end justify-between gap-4">
          <div>
            <p className="text-[0.6rem] font-semibold uppercase tracking-[0.2em] text-[--color-muted]">
              Клиенты о нас
            </p>
            <h2 className="mt-1 text-2xl font-semibold uppercase tracking-[0.04em] text-black md:text-3xl">
              Отзывы
            </h2>
          </div>
          <Link
            to="/reviews"
            className="shrink-0 text-xs font-semibold uppercase tracking-[0.12em] text-black underline underline-offset-4 transition hover:text-zinc-500"
          >
            Все отзывы →
          </Link>
        </div>

        {/* Horizontal scroll carousel */}
        <div className="relative -mx-4 md:-mx-0">
          <div className="flex gap-4 overflow-x-auto px-4 pb-2 md:px-0 snap-x snap-mandatory scrollbar-none">
            {reviews.map((review) => (
              <div
                key={review.id}
                className="w-[min(320px,80vw)] shrink-0 snap-start"
              >
                <ReviewCard review={review} />
              </div>
            ))}
          </div>
        </div>
      </Container>
    </section>
  );
}
