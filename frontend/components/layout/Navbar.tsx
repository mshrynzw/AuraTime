"use client";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuList,
} from "@/components/ui/navigation-menu";
import { useAuth } from "@/lib/auth/context";

interface NavbarProps {
  /**
   * アプリケーション名
   * @default "AuraTime"
   */
  appName?: string;
}

/**
 * ナビゲーションバーコンポーネント
 *
 * 認証済みユーザー向けのナビゲーションバーを表示します。
 * shadcn/uiのコンポーネントを使用して実装されています。
 * ユーザーアバターとドロップダウンメニューを含みます。
 */
export default function Navbar({ appName = "AuraTime" }: NavbarProps) {
  const { user, logout } = useAuth();

  if (!user) {
    return null;
  }

  // ユーザーのイニシャルを生成（姓と名の最初の文字）
  const initials = `${user.familyName?.[0] || ""}`;

  return (
    <nav className="bg-gradient-to-br from-blue-50/50 via-white/60 to-blue-50/50 backdrop-blur-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          <div className="flex items-center gap-8">
            <h1 className="text-3xl font-bold tracking-wider animate-dimlight box-reflect">
              {appName}
            </h1>
            <NavigationMenu>
              <NavigationMenuList>
                <NavigationMenuItem>
                  {/* ナビゲーションメニュー項目をここに追加可能 */}
                </NavigationMenuItem>
              </NavigationMenuList>
            </NavigationMenu>
          </div>

          <div className="flex items-center gap-4">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className="relative h-10 w-10 rounded-full"
                >
                  <Avatar className="h-10 w-10">
                    <AvatarFallback className="bg-primary text-primary-foreground">
                      {initials}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent className="w-56" align="end" forceMount>
                <DropdownMenuLabel className="font-normal">
                  <div className="flex flex-col space-y-1">
                    <p className="text-sm font-medium leading-none">
                      {user.familyName} {user.firstName}
                    </p>
                    <p className="text-xs leading-none text-muted-foreground">
                      {user.email}
                    </p>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={logout} className="cursor-pointer">
                  ログアウト
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </div>
    </nav>
  );
}
