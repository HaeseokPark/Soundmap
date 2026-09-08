import { useEffect, useState } from "react";
import { notificationApi } from "../api/discovery";
import {
  currentSubscription,
  enablePush,
  disablePush,
  supportsPush,
} from "./push";
export default function PushSettings() {
  const [enabled, setEnabled] = useState(false),
    [busy, setBusy] = useState(false),
    [message, setMessage] = useState(""),
    [configured, setConfigured] = useState(null);
  useEffect(() => {
    let active = true;
    Promise.all([currentSubscription(), notificationApi.config()])
      .then(([sub, config]) => {
        if (active) {
          setEnabled(!!sub);
          setConfigured(config.enabled);
        }
      })
      .catch((e) => {
        if (active) setMessage(e.message);
      });
    return () => {
      active = false;
    };
  }, []);
  const toggle = async () => {
    setBusy(true);
    setMessage("");
    try {
      if (enabled) await disablePush();
      else await enablePush();
      setEnabled(!enabled);
      setMessage(
        enabled
          ? "이 기기의 푸시 알림을 껐습니다."
          : "대화 요청·수락·새 메시지 알림을 받습니다.",
      );
    } catch (e) {
      setMessage(e.message);
    } finally {
      setBusy(false);
    }
  };
  return (
    <section className="chat-section">
      <h2>푸시 알림</h2>
      <p className="hint">
        이 기기에서 대화 요청·수락·새 메시지를 받습니다. 메시지 내용과 위치는
        알림에 표시하지 않습니다.
      </p>
      {!supportsPush() && (
        <p className="hint">
          이 환경은 푸시를 지원하지 않습니다. iPhone/iPad에서는 홈 화면에 추가한
          웹 앱에서 확인해 주세요.
        </p>
      )}
      {configured === false && (
        <p className="hint">
          서버 푸시 키 설정이 필요합니다. 알림함은 사용할 수 있습니다.
        </p>
      )}
      <button
        type="button"
        className="secondary"
        disabled={busy || !supportsPush() || (!enabled && configured !== true)}
        onClick={toggle}
      >
        {busy
          ? "변경 중…"
          : enabled
            ? "이 기기 푸시 끄기"
            : "이 기기 푸시 켜기"}
      </button>
      {message && (
        <p role="status" className="hint">
          {message}
        </p>
      )}
    </section>
  );
}
