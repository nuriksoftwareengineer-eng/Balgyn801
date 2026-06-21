import { Link } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import img0 from "@/assets/figma/image0.jpeg";
import img1 from "@/assets/figma/image1.jpeg";
import img2 from "@/assets/figma/image2.jpeg";
import img3 from "@/assets/figma/image3.jpeg";

const COVERS = [img0, img1, img2, img3];

/** Карточка группы/коллекции каталога в стиле дизайна (обложка + hover-стрелка). */
export function CatalogCard({
  to,
  title,
  index = 0,
  cover,
  hint = "Смотреть →",
}: {
  to: string;
  title: string;
  index?: number;
  cover?: string | null;
  hint?: string;
}) {
  const src = cover ?? COVERS[index % COVERS.length];
  return (
    <Link to={to} className="group block cursor-pointer">
      <div className="relative mb-4 aspect-[4/5] overflow-hidden border border-[#E6E6E6] bg-white">
        <img
          src={src}
          alt={title}
          className="h-full w-full object-cover transition-transform duration-[1200ms] ease-out group-hover:scale-110"
          loading="lazy"
        />
        <div className="absolute inset-0 bg-black/0 transition-colors duration-500 group-hover:bg-black/15" />
        <div className="absolute bottom-4 right-4 flex h-10 w-10 translate-y-2 items-center justify-center rounded-full bg-white opacity-0 transition-all duration-500 group-hover:translate-y-0 group-hover:opacity-100">
          <ArrowUpRight className="h-5 w-5" />
        </div>
      </div>
      <div className="flex flex-col items-start gap-1">
        <h3 className="text-[22px] font-bold uppercase tracking-[-0.02em] md:text-[28px]">
          {title}
        </h3>
        <span className="text-[12px] uppercase tracking-[0.2em] text-[#7A7A7A]">
          {hint}
        </span>
      </div>
    </Link>
  );
}
