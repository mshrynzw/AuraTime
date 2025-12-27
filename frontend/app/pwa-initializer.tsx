"use client";

import { registerServiceWorker } from "@/lib/pwa/register-sw";
import { useEffect } from "react";

export function PWAInitializer() {
  useEffect(() => {
    registerServiceWorker();
  }, []);

  return null;
}




