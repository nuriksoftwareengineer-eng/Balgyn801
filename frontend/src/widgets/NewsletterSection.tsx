import { Container } from "@/shared/ui/container";

export function NewsletterSection() {
  return (
    <section className="py-12 md:py-20" id="subscribe">
      <Container>
        <div className="rounded-[14px] border border-white/10 bg-gradient-to-br from-zinc-900 to-zinc-950 px-7 py-10 text-center md:px-10">
          <h2 className="font-display mb-2.5 text-[2.25rem] tracking-[0.04em] text-zinc-100">
            Скидка на первый заказ
          </h2>
          <p className="mb-6 text-[0.9375rem] text-zinc-400">
            Подпишитесь на рассылку — расскажем о новых дропах и промокодах.
          </p>
          <form
            className="mx-auto flex max-w-[440px] flex-wrap justify-center gap-2.5"
            onSubmit={(e) => e.preventDefault()}
          >
            <label htmlFor="email-news" className="sr-only">
              Email
            </label>
            <input
              id="email-news"
              type="email"
              placeholder="your@email.com"
              className="min-w-[200px] flex-1 rounded-[10px] border border-white/10 bg-zinc-950 px-4 py-3 text-[0.9375rem] text-zinc-100 placeholder:text-zinc-600 focus:border-violet-500 focus:outline-none"
            />
            <button
              type="submit"
              className="rounded-[10px] bg-violet-500 px-[22px] py-3 font-semibold text-white hover:brightness-110"
            >
              Подписаться
            </button>
          </form>
        </div>
      </Container>
    </section>
  );
}
