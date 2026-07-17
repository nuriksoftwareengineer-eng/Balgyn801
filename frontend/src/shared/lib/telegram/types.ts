export type TelegramUser = {
  id: number;
  username?: string;
  firstName?: string;
  lastName?: string;
  photoUrl?: string;
};

/** Impact/notification styles Telegram's native haptic engine understands, plus "selection". */
export type HapticFeedback =
  | "light"
  | "medium"
  | "heavy"
  | "rigid"
  | "soft"
  | "success"
  | "warning"
  | "error"
  | "selection";

export type MainButtonOptions = {
  text: string;
  onClick: () => void;
  isEnabled?: boolean;
  isLoaderVisible?: boolean;
};
