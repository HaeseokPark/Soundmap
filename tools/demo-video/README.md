# 시연 영상 자동 촬영

출품용 시연 영상(`잔향-시연영상.mp4`)을 만든 도구다. 화면을 사람이 녹화하는 대신,
헤드리스 크롬으로 앱을 조작하면서 프레임을 모으고 ffmpeg 으로 묶는다.

실시간 녹화가 아니라 **"화면 상태 하나당 한 장"** 을 찍고 ffmpeg 에 표시 시간을
따로 알려주는 방식이다. 캡처가 느려도 결과물은 끊기지 않고, 장면 길이를 숫자로
정확히 맞출 수 있다.

## 구성

| 파일 | 역할 |
|---|---|
| `stage.html` | 1920×1080 무대. 왼쪽은 자막, 오른쪽 폰 목업 안의 iframe 에 실제 앱이 돈다 |
| `film.mjs` | 연출 대본. 장면 순서·자막 문구·조작·각 장면 길이가 전부 여기 있다 |
| `encode.sh` | 모은 프레임을 1080p 30fps MP4 로 묶는다 (무음 오디오 트랙 포함) |

## 다시 찍는 법

`stage.html` 은 앱과 **같은 출처**에서 서비스돼야 한다. iframe 안의 앱을
직접 조작해야 하기 때문이다. 그래서 촬영 중에만 `frontend/public/` 으로 복사한다
(그대로 두면 배포 번들에 섞여 나가므로 `.gitignore` 에 걸어 두었다).

```bash
# 1) 무대 페이지를 임시로 공개 디렉터리에 둔다
cp tools/demo-video/stage.html frontend/public/__demo-stage.html

# 2) 더미 40명이 있는 상태로 백엔드와 프런트를 띄운다
cd backend && SEED_ENABLED=true ./gradlew bootRun     # 다른 터미널
cd frontend && npm run dev                            # 다른 터미널

# 3) 촬영용 크롬 (디버깅 포트 9333)
chrome --headless=new --disable-gpu --remote-debugging-port=9333 \
  --user-data-dir=/tmp/chrome-video --hide-scrollbars \
  --window-size=1920,1080 --force-device-scale-factor=1 about:blank

# 4) 토큰 두 개를 받아서 촬영 — 각각 민수(더미01), 취향 미등록 신규 계정
node tools/demo-video/film.mjs <작업폴더> <TOKEN_민수> <TOKEN_신규>

# 5) 인코딩
bash tools/demo-video/encode.sh <작업폴더>/frames 잔향-시연영상.mp4

# 6) 무대 페이지 치우기
rm frontend/public/__demo-stage.html
```

## 고칠 때

자막 문구와 장면 길이는 `film.mjs` 아래쪽 "장면" 구역에 모여 있다.
`capIn(번호, 소제목, 문구, 머무는초)` 한 줄이 자막 하나다.
`hold(초)` 는 정지 화면, `tap(선택자)` 은 파문과 함께 누르기,
`scrollApp(선택자, 목표px)` 은 스크롤이다.

앱의 클래스 이름이 바뀌면 `tap`/`scrollApp` 의 선택자도 같이 고쳐야 한다.
실행 로그에 `! 탭 대상 없음` 이 찍히면 그 선택자가 더 이상 맞지 않는다는 뜻이다.
