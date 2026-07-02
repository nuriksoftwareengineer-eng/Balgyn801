export type ShopReviewStatus = "PUBLISHED" | "HIDDEN";

export interface ShopReviewResponse {
  id: number;
  name: string;
  avatarUrl: string | null;
  city: string | null;
  rating: number;
  body: string;
  photoUrls: string[];
  status: ShopReviewStatus;
  createdAt: string;
}

export interface ShopReviewRequest {
  name: string;
  avatarUrl: string | null;
  city: string | null;
  rating: number;
  body: string;
  photoUrls: string[];
  status: ShopReviewStatus;
}
