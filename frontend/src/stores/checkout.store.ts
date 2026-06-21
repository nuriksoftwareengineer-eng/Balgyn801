import { create } from "zustand";
import type { DeliveryAddressRequest, DeliveryType, PaymentProvider } from "@/shared/api/types";

// ─── Re-export for consumers ──────────────────────────────────────────────
export type { DeliveryType, PaymentProvider };

// ─── Types ────────────────────────────────────────────────────────────────

export interface CheckoutState {
  // Step 1 — Delivery
  deliveryType: DeliveryType | null;
  address: DeliveryAddressRequest | null;
  deliveryPrice: number | null;

  // Step 2 — Payment
  provider: PaymentProvider | null;

  // Step 3 — Created order / payment
  orderId: number | null;
  paymentId: number | null;
  paymentUrl: string | null;

  // Actions
  setDelivery: (
    type: DeliveryType,
    address: DeliveryAddressRequest | null,
    price: number,
  ) => void;
  setProvider: (provider: PaymentProvider) => void;
  setOrder: (orderId: number) => void;
  setPayment: (paymentId: number, paymentUrl: string | null) => void;
  reset: () => void;
}

// ─── Store ────────────────────────────────────────────────────────────────

export const useCheckoutStore = create<CheckoutState>()((set) => ({
  deliveryType: null,
  address: null,
  deliveryPrice: null,
  provider: null,
  orderId: null,
  paymentId: null,
  paymentUrl: null,

  setDelivery: (deliveryType, address, deliveryPrice) =>
    set({ deliveryType, address, deliveryPrice }),

  setProvider: (provider) => set({ provider }),

  setOrder: (orderId) => set({ orderId }),

  setPayment: (paymentId, paymentUrl) => set({ paymentId, paymentUrl }),

  reset: () =>
    set({
      deliveryType: null,
      address: null,
      deliveryPrice: null,
      provider: null,
      orderId: null,
      paymentId: null,
      paymentUrl: null,
    }),
}));

// ─── Selectors ────────────────────────────────────────────────────────────

export const selectDeliveryType  = (s: CheckoutState) => s.deliveryType;
export const selectAddress       = (s: CheckoutState) => s.address;
export const selectDeliveryPrice = (s: CheckoutState) => s.deliveryPrice;
export const selectProvider      = (s: CheckoutState) => s.provider;
export const selectOrderId       = (s: CheckoutState) => s.orderId;
export const selectPaymentId     = (s: CheckoutState) => s.paymentId;
export const selectPaymentUrl    = (s: CheckoutState) => s.paymentUrl;
