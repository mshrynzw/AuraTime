import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";
import { PWAInitializer } from "./pwa-initializer";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "AuraTime - 勤怠・給与管理システム",
  description: "マルチテナント型勤怠・給与管理システム",
  manifest: "/manifest.json",
  themeColor: "#0066cc",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "AuraTime",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ja">
      <head>
        <link rel="manifest" href="/manifest.json" />
      </head>
      <body className={inter.className}>
        <Providers>
          <PWAInitializer />
          {children}
        </Providers>
      </body>
    </html>
  );
}

