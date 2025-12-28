import DashboardContent from "./DashboardContent";

/**
 * ダッシュボードページ
 *
 * サーバーコンポーネントとして実装されています。
 * 認証チェックとローディング表示はAuthGuardコンポーネントが担当します。
 */
export default function DashboardPage() {
  return (
    <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div className="px-4 py-6 sm:px-0">
        <div className="bg-white border-4 border-dashed border-gray-200 rounded-lg p-8">
          <h2 className="text-2xl font-bold mb-4">ダッシュボード</h2>
          <DashboardContent />
        </div>
      </div>
    </main>
  );
}
