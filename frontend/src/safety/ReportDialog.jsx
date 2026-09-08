import { useEffect, useState } from "react";
import { safetyApi } from "../api/safety";

/**
 * 신고 창.
 *
 * 신고한 상대를 계속 보고 싶지 않은 것이 보통이라 "함께 차단"을 기본으로 켜 둔다.
 * 다만 신고와 차단은 별개 동작이라 끌 수 있게 남겨 둔다.
 */
export default function ReportDialog({ targetUserId, targetNickname, onClose, onDone }) {
  const [reasons, setReasons] = useState([]);
  const [reason, setReason] = useState(null);
  const [detail, setDetail] = useState("");
  const [alsoBlock, setAlsoBlock] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    safetyApi
      .reportReasons()
      .then((list) => {
        setReasons(list);
        setReason(list[0]?.code ?? null);
      })
      .catch((err) => setError(err.message));
  }, []);

  const submit = async () => {
    if (!reason) return;
    setError(null);
    setSubmitting(true);
    try {
      await safetyApi.report({ targetUserId, reason, detail: detail.trim() || null, alsoBlock });
      onDone(alsoBlock);
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  };

  return (
    <div className="sheet-backdrop" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <div className="sheet__handle" />
        <h2 className="sheet__title">{targetNickname}님 신고</h2>
        <p className="hint">
          접수된 내용은 운영자가 확인합니다. 신고 사실은 상대에게 알려지지 않습니다.
        </p>

        <div className="chip-grid">
          {reasons.map((option) => (
            <button
              key={option.code}
              type="button"
              className={reason === option.code ? "chip chip--on" : "chip"}
              onClick={() => setReason(option.code)}
            >
              {option.label}
            </button>
          ))}
        </div>

        <label className="field">
          <span>설명 (선택)</span>
          <textarea
            className="search-input"
            rows={3}
            maxLength={500}
            value={detail}
            onChange={(e) => setDetail(e.target.value)}
            placeholder="어떤 일이 있었는지 적어주시면 확인에 도움이 됩니다."
          />
        </label>

        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={alsoBlock}
            onChange={(e) => setAlsoBlock(e.target.checked)}
          />
          <span>이 사용자를 함께 차단합니다</span>
        </label>

        {error && <p className="error">{error}</p>}

        <div className="sheet__actions">
          <button className="secondary" type="button" onClick={onClose}>
            취소
          </button>
          <button className="danger" type="button" onClick={submit} disabled={submitting || !reason}>
            {submitting ? "접수 중..." : "신고하기"}
          </button>
        </div>
      </div>
    </div>
  );
}
