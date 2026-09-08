import PreviewButton from "./PreviewButton";
import SaveTrackButton from "./SaveTrackButton";
export default function TrackCard({ track, children }) {
  return (
    <div className="music-card">
      {track.albumArtUrl ? (
        <img src={track.albumArtUrl} alt="" loading="lazy" />
      ) : (
        <span className="music-card__placeholder">♪</span>
      )}
      <div className="music-card__body">
        <strong>{track.title}</strong>
        <span>{track.artistName}</span>
        {children}
      </div>
      <div className="music-card__actions">
        <PreviewButton url={track.previewUrl} />
        <SaveTrackButton track={track} />
      </div>
    </div>
  );
}
