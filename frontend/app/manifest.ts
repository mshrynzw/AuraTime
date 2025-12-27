import { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "AuraTime",
    short_name: "AuraTime",
    description: "勤怠・給与管理システム",
    start_url: "/",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#0066cc",
    icons: [
      {
        src: "/icons/icon-192x192.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "any maskable",
      },
      {
        src: "/icons/icon-512x512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "any maskable",
      },
    ],
    orientation: "portrait-primary",
    scope: "/",
  };
}




