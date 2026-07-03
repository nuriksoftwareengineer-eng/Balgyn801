import type { ShopReviewResponse } from "@/shared/types/reviews";

function Stars({ rating }: { rating: number }) {
  return (
    <span className="flex gap-0.5" aria-label={`${rating} из 5`}>
      {Array.from({ length: 5 }, (_, i) => (
        <svg
          key={i}
          viewBox="0 0 12 12"
          className={`h-3 w-3 ${i < rating ? "text-black" : "text-zinc-300"}`}
          fill="currentColor"
        >
          <path d="M6 0l1.545 3.13L11 3.635l-2.5 2.435.59 3.44L6 7.87l-3.09 1.64.59-3.44L1 3.635l3.455-.505z" />
        </svg>
      ))}
    </span>
  );
}

function Avatar({ name, avatarUrl }: { name: string; avatarUrl: string | null }) {
  if (avatarUrl) {
    return (
      <img
        src={avatarUrl}
        alt={name}
        className="h-10 w-10 rounded-full object-cover object-center ring-1 ring-black/10"
      />
    );
  }
  return (
    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-zinc-900 text-sm font-semibold uppercase text-white">
      {name.charAt(0)}
    </div>
  );
}

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString("ru-RU", {
      day: "numeric",
      month: "long",
      year: "numeric",
    });
  } catch {
    return iso;
  }
}

interface ReviewCardProps {
  review: ShopReviewResponse;
  className?: string;
}

export function ReviewCard({ review, className = "" }: ReviewCardProps) {
  return (
    <article
      className={`flex flex-col gap-4 border border-[--color-border] bg-white p-5 ${className}`}
    >
      {/* Header */}
      <div className="flex items-start gap-3">
        <Avatar name={review.name} avatarUrl={review.avatarUrl} />
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
            <span className="text-sm font-semibold text-black truncate">{review.name}</span>
            {review.city && (
              <span className="text-xs text-[--color-muted]">{review.city}</span>
            )}
          </div>
          <div className="mt-1 flex items-center gap-2">
            <Stars rating={review.rating} />
            <span className="text-[0.65rem] text-[--color-muted]">
              {formatDate(review.createdAt)}
            </span>
          </div>
        </div>
      </div>

      {/* Body */}
      <p className="text-sm leading-relaxed text-zinc-700 whitespace-pre-line">
        {review.body}
      </p>

      {/* Photos */}
      {review.photoUrls.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {review.photoUrls.slice(0, 4).map((url, i) => (
            <a key={i} href={url} target="_blank" rel="noopener noreferrer">
              <img
                src={url}
                alt=""
                className="h-16 w-16 object-cover object-center border border-[--color-border] transition-opacity hover:opacity-80"
              />
            </a>
          ))}
          {review.photoUrls.length > 4 && (
            <div className="flex h-16 w-16 items-center justify-center border border-[--color-border] bg-[--color-surface] text-xs font-medium text-[--color-muted]">
              +{review.photoUrls.length - 4}
            </div>
          )}
        </div>
      )}
    </article>
  );
}
