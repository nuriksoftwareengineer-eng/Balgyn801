/// <reference types="vite/client" />

// Видео-ассеты (импортируются как URL), которых нет в типах vite/client.
declare module "*.MOV" {
  const src: string;
  export default src;
}
declare module "*.mov" {
  const src: string;
  export default src;
}
declare module "*.mp4" {
  const src: string;
  export default src;
}
