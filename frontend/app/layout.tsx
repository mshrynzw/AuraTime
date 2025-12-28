import AuroraBackground from "@/components/aurora/AuroraBackground";
import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";
import { PWAInitializer } from "./pwa-initializer";
const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "AuraTime - グループウェア",
  description: "マルチテナント型グループウェア",
  manifest: "/manifest.ts",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "AuraTime",
  },
  icons: {
    icon: [{ url: "/icons/favicon.ico", sizes: "any", type: "image/x-icon" }],
    other: [
      {
        url: "/icons/android-chrome-192x192.png",
        sizes: "192x192",
        type: "image/png",
      },
      {
        url: "/icons/android-chrome-512x512.png",
        sizes: "512x512",
        type: "image/png",
      },
    ],
    apple: [
      {
        url: "/icons/apple-touch-icon.png",
        sizes: "180x180",
        type: "image/png",
      },
    ],
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
        <link rel="apple-touch-icon" href="/icons/apple-touch-icon.png" />
      </head>
      <body className={inter.className} suppressHydrationWarning>
        <Providers>
          <PWAInitializer />
          <AuroraBackground />
          {children}
        </Providers>
      </body>
    </html>
  );
}
