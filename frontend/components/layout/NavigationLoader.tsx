"use client";

import LoadingScreen from "@/components/layout/LoadingScreen";
import { useNavigation } from "@/hooks/use-navigation";

/**
 * 画面遷移時のローディング表示コンポーネント
 *
 * 画面遷移を検知して、全画面ローディングを表示します。
 * クライアントコンポーネントとして実装されており、
 * サーバーコンポーネントのレイアウトから使用できます。
 */
export default function NavigationLoader() {
  const isNavigating = useNavigation();

  if (!isNavigating) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm">
      <LoadingScreen message="読み込み中..." fullScreen={true} />
    </div>
  );
}
