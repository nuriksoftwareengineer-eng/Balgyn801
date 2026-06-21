import { Component, type ReactNode } from "react";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  message: string;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, message: "" };

  static getDerivedStateFromError(error: unknown): State {
    const message = error instanceof Error ? error.message : String(error);
    return { hasError: true, message };
  }

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback ?? (
          <div className="flex min-h-[40vh] flex-col items-center justify-center gap-4 px-4 text-center">
            <p className="text-[15px] font-semibold text-[--color-danger]">
              Произошла ошибка
            </p>
            {this.state.message && (
              <p className="max-w-sm text-[13px] text-[--color-muted]">
                {this.state.message}
              </p>
            )}
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="mt-2 bg-black px-5 py-2 text-[12px] font-bold uppercase tracking-[0.12em] text-white hover:bg-zinc-800"
            >
              Обновить страницу
            </button>
          </div>
        )
      );
    }
    return this.props.children;
  }
}
