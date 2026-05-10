import { DEMO_REVIEWS } from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";
import { SectionHead } from "@/shared/ui/section-head";

export function ReviewsSection() {
  return (
    <section className="py-8 md:py-16" id="reviews">
      <Container>
        <SectionHead title="Отзывы покупателей" />
        <div className="grid gap-[18px] md:grid-cols-3">
          {DEMO_REVIEWS.map((r) => (
            <blockquote
              key={r.author}
              className="rounded-[14px] border border-white/10 bg-zinc-900 p-[22px]"
            >
              <div className="mb-3 text-sm tracking-[2px] text-violet-400" aria-hidden>
                ★★★★★
              </div>
              <p className="mb-4 text-[0.9375rem] text-zinc-400">{r.text}</p>
              <footer className="text-sm font-semibold text-zinc-100">
                {r.author}
              </footer>
            </blockquote>
          ))}
        </div>
      </Container>
    </section>
  );
}
