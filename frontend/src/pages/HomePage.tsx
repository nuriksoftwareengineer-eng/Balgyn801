import { Hero } from "@/widgets/home/Hero";
import { BestSellers } from "@/widgets/home/BestSellers";
import { AboutBand } from "@/widgets/home/AboutBand";
import { NewArrivalsSection } from "@/widgets/home/NewArrivalsSection";
import { ValueStrip } from "@/widgets/home/ValueStrip";
import { PopularSection } from "@/widgets/home/PopularSection";
import { ReviewCarousel } from "@/widgets/home/ReviewCarousel";
import { InstagramSection } from "@/widgets/home/InstagramSection";

export function HomePage() {
  return (
    <>
      <Hero />
      <BestSellers />
      <AboutBand />
      <NewArrivalsSection />
      <ValueStrip />
      <PopularSection />
      <ReviewCarousel />
      <InstagramSection />
    </>
  );
}
