self.addEventListener("push", (event) => {
  let data = {};
  try {
    data = event.data?.json() ?? {};
  } catch {
    /* Show a generic notification for malformed payloads. */
  }
  const url =
    typeof data.url === "string" && /^\/chats\/\d+$/.test(data.url)
      ? data.url
      : "/notifications";
  event.waitUntil(
    self.registration.showNotification(data.title || "SoundMap", {
      body: data.body || "새 알림이 도착했습니다.",
      icon: "/icon-192.png",
      tag: data.tag || "soundmap",
      data: { url },
    }),
  );
});
self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = new URL(
    event.notification.data?.url || "/notifications",
    self.location.origin,
  );
  if (url.origin !== self.location.origin) return;
  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({
        type: "window",
        includeUncontrolled: true,
      });
      for (const client of windows) {
        if (new URL(client.url).origin === url.origin) {
          await client.navigate(url.href);
          return client.focus();
        }
      }
      return self.clients.openWindow(url.href);
    })(),
  );
});
