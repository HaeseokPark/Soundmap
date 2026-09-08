/**
 * 지도 마커를 앨범 커버로 그린다.
 *
 * <p>카카오맵의 기본 마커는 이미지 한 장이라 원형으로 자르거나 테두리를 두를 수
 * 없다. CustomOverlay 에 넣을 DOM 을 직접 만들어 CSS 로 꾸민다.
 *
 * <p>문자열(innerHTML) 대신 요소를 돌려주는 이유는 호출한 쪽에서 클릭 이벤트를
 * 바로 걸기 위해서다.
 */
export function createAlbumMarkerElement({ albumArtUrl, mine }) {
  const size = mine ? 56 : 48;
  // 밝은 지도 위라 남의 마커는 흰 테두리로 배경과 떼어 놓고,
  // 내 마커만 강조색을 둘러 한눈에 찾게 한다.
  const ring = mine ? "var(--primary)" : "#fff";

  const marker = document.createElement("div");
  marker.className = "album-marker";
  marker.style.setProperty("--marker-size", `${size}px`);
  marker.style.setProperty("--marker-ring", ring);

  if (albumArtUrl) {
    const image = document.createElement("img");
    image.src = albumArtUrl;
    image.alt = "";
    marker.append(image);
  } else {
    const empty = document.createElement("span");
    empty.className = "album-marker__empty";
    empty.textContent = "♪";
    marker.append(empty);
  }

  return marker;
}
