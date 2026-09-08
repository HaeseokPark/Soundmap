import { useState } from "react";
import { useLibrary } from "./libraryState";
export default function SaveTrackButton({ track }) {
  const { ids, toggle } = useLibrary();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  if (!track?.id) return null;
  const saved = ids.has(track.id);
  const click = async () => {
    setBusy(true);
    setError("");
    try {
      await toggle(track.id);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };
  return (
    <span className="save-track">
      <button
        type="button"
        className="secondary"
        disabled={busy}
        aria-pressed={saved}
        onClick={click}
      >
        {busy ? "저장 중…" : saved ? "♥ 저장됨" : "♡ 곡 저장"}
      </button>
      {error && (
        <small role="alert" className="error">
          {error}
        </small>
      )}
    </span>
  );
}
