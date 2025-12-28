"use client";

import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";

/**
 * 画面遷移状態を管理するカスタムフック
 *
 * Next.js App Routerでの画面遷移を検知し、
 * 遷移中であることを示す状態を返します。
 * パス名の変更を検知して、短い間ローディング状態を維持します。
 *
 * @returns {boolean} 遷移中かどうか
 */
export function useNavigation() {
  const pathname = usePathname();
  const [isNavigating, setIsNavigating] = useState(false);
  const prevPathnameRef = useRef<string | null>(null);

  useEffect(() => {
    // 初回レンダリング時は遷移状態にしない
    if (prevPathnameRef.current === null) {
      prevPathnameRef.current = pathname;
      return;
    }

    // パスが変更された場合
    if (prevPathnameRef.current !== pathname) {
      setIsNavigating(true);
      prevPathnameRef.current = pathname;

      // 短い遅延後に遷移状態を解除
      // これにより、遷移アニメーションが表示される
      const timer = setTimeout(() => {
        setIsNavigating(false);
      }, 150); // 150ms後に解除

      return () => clearTimeout(timer);
    }
  }, [pathname]);

  return isNavigating;
}
