// Service Worker for AuraTime PWA
const CACHE_NAME = "auratime-v1";
const urlsToCache = ["/", "/login", "/dashboard", "/manifest.webmanifest"];

// Install event: キャッシュを作成
self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(urlsToCache);
    })
  );
});

// Activate event: 古いキャッシュを削除
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});

// Fetch event: Network First戦略
self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);

  // HTTP/HTTPSスキームのリクエストのみを処理
  // chrome-extension:// などのスキームはスキップ
  if (url.protocol !== "http:" && url.protocol !== "https:") {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then((response) => {
        // GETリクエストで成功した場合のみキャッシュに保存
        // Cache APIはGETリクエストのみをサポート
        if (
          event.request.method === "GET" &&
          response &&
          response.status === 200
        ) {
          const responseToCache = response.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseToCache);
          });
        }
        return response;
      })
      .catch(() => {
        // ネットワークから取得できない場合は、キャッシュから取得
        // GETリクエストのみキャッシュから取得可能
        if (event.request.method === "GET") {
          return caches.match(event.request);
        }
        // POST/PUT/DELETEなどの書き込み系リクエストは
        // キャッシュから取得できないため、そのままエラーを返す
        throw new Error("Network request failed");
      })
  );
});

// Push通知の受信
self.addEventListener("push", (event) => {
  const data = event.data ? event.data.json() : {};
  const title = data.title || "AuraTime";
  const options = {
    body: data.body || "新しい通知があります",
    icon: "/icons/icon-192x192.png",
    badge: "/icons/icon-192x192.png",
    data: data.url || "/",
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

// 通知クリック時の処理
self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  event.waitUntil(clients.openWindow(event.notification.data || "/"));
});
