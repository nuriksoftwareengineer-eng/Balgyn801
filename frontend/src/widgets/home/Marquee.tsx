import { motion, useReducedMotion } from "framer-motion";

interface MarqueeProps {
  items: string[];
  className?: string;
  itemClassName?: string;
  reverse?: boolean;
  speed?: number;
}

/** Бесконечная бегущая строка (дизайн BALGYN). */
export function Marquee({
  items,
  className = "",
  itemClassName = "text-[44px] md:text-[88px] font-extrabold tracking-[-3.52px] uppercase leading-none",
  reverse = false,
  speed = 40,
}: MarqueeProps) {
  const reduce = useReducedMotion();
  const loop = [...items, ...items, ...items, ...items];

  return (
    <div className={`overflow-hidden whitespace-nowrap ${className}`}>
      <motion.div
        className="inline-flex gap-[48px] will-change-transform"
        animate={reduce ? undefined : { x: reverse ? ["-50%", "0%"] : ["0%", "-50%"] }}
        transition={{ duration: speed, ease: "linear", repeat: Infinity }}
      >
        {loop.map((item, i) => (
          <span key={i} className={`inline-flex items-center ${itemClassName}`}>
            {item}
          </span>
        ))}
      </motion.div>
    </div>
  );
}
