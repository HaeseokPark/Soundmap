import { notificationApi } from "../api/discovery";
export const supportsPush = () =>
  window.isSecureContext &&
  "serviceWorker" in navigator &&
  "PushManager" in window &&
  "Notification" in window;
export async function currentSubscription() {
  if (!supportsPush()) return null;
  const registration = await navigator.serviceWorker.getRegistration("/");
  return registration ? registration.pushManager.getSubscription() : null;
}
export async function enablePush() {
  if (!supportsPush())
    throw new Error(
      "이 브라우저에서는 푸시를 지원하지 않습니다. HTTPS 접속과 브라우저 지원을 확인해 주세요.",
    );
  // Permission is requested directly from the user's click, before network waits.
  const permission = await Notification.requestPermission();
  if (permission !== "granted")
    throw new Error(
      "알림 권한이 허용되지 않았습니다. 브라우저 사이트 설정에서 변경할 수 있습니다.",
    );
  const config = await notificationApi.config();
  if (!config.enabled)
    throw new Error(
      "서버의 푸시 키 설정이 필요합니다. 알림함은 계속 사용할 수 있습니다.",
    );
  await navigator.serviceWorker.register("/sw.js");
  const registration = await navigator.serviceWorker.ready;
  const base64 = config.publicKey.replace(/-/g, "+").replace(/_/g, "/");
  const key = Uint8Array.from(
    atob(base64 + "=".repeat((4 - (base64.length % 4)) % 4)),
    (c) => c.charCodeAt(0),
  );
  const existing = await registration.pushManager.getSubscription();
  const subscription =
    existing ??
    (await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: key,
    }));
  try {
    await notificationApi.subscribe(subscription.toJSON());
  } catch (e) {
    if (!existing) await subscription.unsubscribe();
    throw e;
  }
  return subscription;
}
export async function disablePush() {
  const sub = await currentSubscription();
  if (sub) {
    await notificationApi.unsubscribe(sub.endpoint);
    await sub.unsubscribe();
  }
}
