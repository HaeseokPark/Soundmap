/**
 * 카카오맵 SDK 로더.
 *
 * <p>SDK 는 npm 패키지가 없고 스크립트 태그로만 받는다. 지도 화면이 여러 번
 * 마운트돼도 스크립트는 한 번만 붙어야 하므로 진행 중인 약속을 재사용한다.
 *
 * <p>키는 도메인에 묶여 있다. 카카오 개발자 콘솔의 [플랫폼 > Web]에 등록하지 않은
 * 주소에서 부르면 스크립트는 내려오지만 지도가 그려지지 않는다. 개발용
 * http://localhost:5173 과 배포 도메인을 모두 넣어야 한다.
 */

const SDK_URL = "https://dapi.kakao.com/v2/maps/sdk.js";

let pending = null;

export function loadKakaoMaps() {
  if (window.kakao?.maps?.Map) {
    return Promise.resolve(window.kakao.maps);
  }
  if (pending) {
    return pending;
  }

  const appKey = import.meta.env.VITE_KAKAO_MAP_KEY;
  if (!appKey) {
    return Promise.reject(new Error("지도 키(VITE_KAKAO_MAP_KEY)가 비어 있습니다."));
  }

  pending = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    // autoload=false 로 받아야 스크립트가 내려온 뒤 kakao.maps.load 로 준비 시점을 잡을 수 있다.
    // 기본값(true)이면 언제 준비됐는지 알 방법이 없어 첫 렌더에서 지도 생성이 실패한다.
    script.src = `${SDK_URL}?appkey=${appKey}&autoload=false`;
    script.async = true;
    script.onload = () => window.kakao.maps.load(() => resolve(window.kakao.maps));
    script.onerror = () => {
      // 다음 시도에서 다시 붙일 수 있도록 실패한 약속은 버린다.
      pending = null;
      script.remove();
      reject(new Error("지도를 불러오지 못했습니다."));
    };
    document.head.appendChild(script);
  });

  return pending;
}
