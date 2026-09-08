/**
 * 30초 미리듣기 재생기.
 *
 * 오디오 엘리먼트를 모듈 하나에 두고 공유한다. 검색창과 지도 핀 시트가 각각
 * 자기 오디오를 만들면 두 곡이 동시에 흘러나온다. 여기서 하나만 유지하고
 * 새로 재생할 때 앞의 것을 멈추므로, 어느 화면에서 눌러도 한 곡만 들린다.
 *
 * 재생 위치는 기억하지 않는다. 30초짜리라 매번 처음부터 트는 편이 자연스럽다.
 */

let audio = null;
let playingUrl = null;
const listeners = new Set();

function notify() {
  listeners.forEach((listener) => listener());
}

function getAudio() {
  if (audio === null) {
    audio = new Audio();
    // 끝까지 듣거나 파일을 못 받으면 버튼을 재생 상태로 두지 않는다.
    audio.addEventListener("ended", stop);
    audio.addEventListener("error", stop);
  }
  return audio;
}

export function stop() {
  if (audio !== null) {
    audio.pause();
  }
  if (playingUrl !== null) {
    playingUrl = null;
    notify();
  }
}

/** 같은 곡을 다시 누르면 정지, 다른 곡이면 그 곡으로 바꿔서 재생한다. */
export function toggle(url) {
  if (!url) return;

  if (playingUrl === url) {
    stop();
    return;
  }

  const el = getAudio();
  el.src = url;
  el.currentTime = 0;
  playingUrl = url;
  notify();

  // play() 는 프로미스를 준다. 재생이 시작되기 전에 다른 곡을 누르면
  // 거부되는데, 그건 정상적인 흐름이라 조용히 넘긴다. 실제로 못 트는
  // 경우(네트워크 실패 등)는 여기서 버튼을 되돌린다.
  el.play().catch(() => {
    if (playingUrl === url) {
      stop();
    }
  });
}

export function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getPlayingUrl() {
  return playingUrl;
}
