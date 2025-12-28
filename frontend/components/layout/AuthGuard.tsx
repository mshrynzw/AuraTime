"use client";

import LoadingScreen from "@/components/layout/LoadingScreen";
import { useAuth } from "@/lib/auth/context";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

interface AuthGuardProps {
  children: React.ReactNode;
}

/**
 * 認証ガードコンポーネント
 *
 * 認証が必要なページを保護し、未認証ユーザーをログインページにリダイレクトします。
 * 認証状態の読み込み中はローディング画面を表示します。
 */
export default function AuthGuard({ children }: AuthGuardProps) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  if (loading) {
    return <LoadingScreen />;
  }

  if (!user) {
    return null;
  }

  return <>{children}</>;
}
