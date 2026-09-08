/**
 * 서비스 로고(지도 핀 + 음표).
 *
 * <p>파일 하나를 public 에 두고 img 로 부른다. 인라인 SVG 로 넣으면 로고가 쓰이는
 * 화면마다 같은 마크업이 복사되고, 그라데이션 id 가 겹쳐 색이 어긋난다.
 */
export default function Logo({ size = 24, className = "" }) {
  return (
    <img
      className={`logo-mark ${className}`.trim()}
      src="/logo.svg"
      width={size}
      height={size}
      alt=""
    />
  );
}
