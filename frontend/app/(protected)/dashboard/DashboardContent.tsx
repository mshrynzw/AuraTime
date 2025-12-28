"use client";

import { useAuth } from "@/lib/auth/context";

/**
 * ダッシュボードコンテンツコンポーネント
 *
 * 認証済みユーザーのダッシュボードコンテンツを表示します。
 * クライアントコンポーネントとして、ユーザー情報の表示を担当します。
 */
export default function DashboardContent() {
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <>
      <p className="text-gray-600">
        ようこそ、{user.familyName} {user.firstName}さん
      </p>
      <p className="text-sm text-gray-500 mt-2">ロール: {user.role}</p>
    </>
  );
}
