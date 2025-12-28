"use client";

import { Spinner } from "@/components/ui/spinner";

interface LoadingScreenProps {
  /**
   * ローディングメッセージ
   * @default "読み込み中..."
   */
  message?: string;
  /**
   * 全画面表示にするかどうか
   * @default true
   */
  fullScreen?: boolean;
  /**
   * 追加のクラス名
   */
  className?: string;
}

/**
 * ローディング画面コンポーネント
 *
 * 認証状態の読み込み中など、全画面またはインラインでローディング表示を行います。
 * Spinnerコンポーネントを使用してアニメーション付きのローディング表示を提供します。
 */
export default function LoadingScreen({
  message = "読み込み中...",
  fullScreen = true,
  className = "",
}: LoadingScreenProps) {
  const containerClasses = fullScreen
    ? "min-h-screen flex items-center justify-center"
    : "flex items-center justify-center p-8";

  return (
    <div className={`${containerClasses} ${className}`}>
      <div className="flex flex-col items-center gap-6 opacity-0 animate-[fadeIn_0.3s_ease-in-out_0s_forwards]">
        <div className="relative">
          {/* 背景のパルスリング */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="absolute size-16 rounded-full bg-primary/20 animate-ping" />
            <div className="absolute size-12 rounded-full bg-primary/10 animate-pulse" />
          </div>
          {/* メインのSpinner */}
          <div className="relative">
            <Spinner
              className={`${
                fullScreen ? "size-12" : "size-8"
              } text-primary drop-shadow-lg`}
            />
          </div>
        </div>
        {message && (
          <div className="flex flex-col items-center gap-2 opacity-0 animate-[fadeIn_0.5s_ease-in-out_0.15s_forwards]">
            <p className="text-sm font-medium text-foreground/90 tracking-wide">
              {message}
            </p>
            {/* ドットアニメーション */}
            <div className="flex gap-1.5 mt-1">
              <span className="size-1.5 rounded-full bg-primary/60 animate-pulse [animation-delay:0ms]" />
              <span className="size-1.5 rounded-full bg-primary/60 animate-pulse [animation-delay:150ms]" />
              <span className="size-1.5 rounded-full bg-primary/60 animate-pulse [animation-delay:300ms]" />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
