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
      <div className="flex flex-col items-center gap-4">
        <Spinner className="size-8 text-primary" />
        {message && <p className="text-sm text-muted-foreground">{message}</p>}
      </div>
    </div>
  );
}
