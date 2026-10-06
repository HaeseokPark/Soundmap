# 잔향 (Janhyang)

현재 듣고 있는 음악과 음악 취향을 지도에 공유하고, 같은 음악을 좋아하는
주변 사람을 발견해 대화할 수 있는 위치 기반 음악 소셜 플랫폼.

**배포 주소: https://soundmap.duckdns.org**

휴대폰 브라우저에서 열고 홈 화면에 추가하면 앱처럼 전체화면으로 실행됩니다.
위치 기능은 HTTPS 에서만 동작하므로 배포 주소로 접속해야 합니다.

## 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Security 7, Spring Data JPA |
| DB | MySQL 8.0 (utf8mb4) |
| 인증 | JWT (JJWT 0.13, HS256) |
| 실시간 | WebSocket + STOMP (Spring 내장 SimpleBroker) |
| 지도 | 카카오맵 Maps JavaScript SDK |
| 음악 메타 | iTunes Search API (US 스토어 조회 + KR 표기로 현지화) |
| 알림 | Web Push (VAPID) + Service Worker |
| Frontend | React 19, Vite 8, React Router 7, Axios, @stomp/stompjs |
| 배포 | AWS Lightsail (Ubuntu), nginx, systemd, Let's Encrypt |

## 처음 실행하기

### 1. 데이터베이스 준비 (최초 1회)

```bash
mysql -u root -p < backend/db/init.sql
```

`music` 데이터베이스와 로컬 개발용 `soundmap` 계정이 만들어집니다.
여러 번 실행해도 안전합니다.
테이블은 애플리케이션이 시작될 때 JPA가 자동 생성합니다 (`ddl-auto: update`).

### 2. 백엔드 실행

```bash
cd backend && ./gradlew bootRun
```

http://localhost:8080 에서 뜹니다. 확인:

```bash
curl http://localhost:8080/api/health
```

### 3. 프론트엔드 실행

```bash
cd frontend && npm install && npm run dev
```

http://localhost:5173 으로 접속합니다.

## API

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/health` | - | 헬스 체크 |
| POST | `/api/auth/signup` | - | 회원가입 (가입 즉시 토큰 발급) |
| POST | `/api/auth/login` | - | 로그인 |
| GET | `/api/auth/me` | 필요 | 내 정보 조회 (토큰 유효성 확인용) |
| GET | `/api/auth/check-email?email=` | - | 이메일 중복 확인 |
| GET | `/api/auth/check-nickname?nickname=` | - | 닉네임 중복 확인 |
| GET | `/api/tracks/search?q=` | 필요 | 곡 검색 (iTunes Search API) |
| PUT | `/api/me/now-playing` | 필요 | 지금 듣는 곡 등록 |
| GET | `/api/me/now-playing` | 필요 | 내가 올린 곡 조회 (없으면 204) |
| DELETE | `/api/me/now-playing` | 필요 | 곡 내리기 |
| PUT | `/api/me/location` | 필요 | 위치 등록 (격자 스냅 후 저장) |
| GET | `/api/me/location` | 필요 | 내 위치 조회 (없으면 204) |
| DELETE | `/api/me/location` | 필요 | 위치 공유 끄기 |
| GET | `/api/map/pins?radiusMeters=&artistId=&genreId=` | 필요 | 지도·목록 데이터 (반경 + 취향 필터) |
| GET | `/api/genres` | 필요 | 장르 목록 |
| GET | `/api/artists?q=` | 필요 | 가수 검색 (q 생략 시 기본 목록) |
| GET | `/api/me/taste` | 필요 | 내 음악 취향 조회 |
| PUT | `/api/me/taste` | 필요 | 음악 취향 저장 (통째로 교체) |
| GET | `/api/users/{userId}/profile` | 필요 | 프로필 + 공통 취향 |
| POST | `/api/chats/requests` | 필요 | 대화 요청 (이미 있으면 그 방) |
| GET | `/api/chats` | 필요 | 내 채팅방 목록 |
| GET | `/api/chats/unread-count` | 필요 | 안 읽은 메시지 총 개수 |
| GET | `/api/chats/{roomId}` | 필요 | 채팅방 상세 + 공통 취향 배너 |
| POST | `/api/chats/{roomId}/accept` | 필요 | 대화 수락 |
| POST | `/api/chats/{roomId}/reject` | 필요 | 대화 거절 |
| GET | `/api/chats/{roomId}/messages` | 필요 | 메시지 목록 (id 커서) |
| POST | `/api/chats/{roomId}/messages` | 필요 | 메시지 전송 (REST 대체 경로) |
| POST | `/api/chats/{roomId}/read` | 필요 | 읽음 처리 |
| POST | `/api/blocks` | 필요 | 사용자 차단 |
| DELETE | `/api/blocks/{userId}` | 필요 | 차단 해제 |
| GET | `/api/blocks` | 필요 | 내가 차단한 사용자 목록 |
| GET | `/api/reports/reasons` | 필요 | 신고 사유 선택지 |
| POST | `/api/reports` | 필요 | 신고 (함께 차단 선택 가능) |
| GET | `/api/me/settings` | 필요 | 공개 범위 설정 조회 |
| PUT | `/api/me/settings` | 필요 | 공개 범위 설정 저장 |
| POST | `/api/chats/{roomId}/tracks` | 필요 | 대화에 곡 카드 보내기 |
| GET | `/api/me/saved-tracks` | 필요 | 내 보관함 목록 |
| GET | `/api/me/saved-tracks/ids` | 필요 | 저장한 곡 id 목록 (버튼 상태용) |
| PUT | `/api/me/saved-tracks/{trackId}` | 필요 | 곡 저장 |
| DELETE | `/api/me/saved-tracks/{trackId}` | 필요 | 저장 해제 |
| GET | `/api/me/music-history?month=&page=` | 필요 | 내가 올린 곡 기록 + 월별 가수 요약 |
| DELETE | `/api/me/music-history/{id}` | 필요 | 기록 한 건 삭제 |
| GET | `/api/music/local-chart?radiusMeters=` | 필요 | 오늘 이 근처에서 많이 공유된 곡 |
| GET | `/api/me/notifications?page=` | 필요 | 알림함 (안 읽은 수 포함) |
| POST | `/api/me/notifications/{id}/read` | 필요 | 알림 읽음 |
| POST | `/api/me/notifications/read-all` | 필요 | 전체 읽음 |
| GET | `/api/me/notifications/push-config` | 필요 | 푸시 사용 가능 여부 + 공개키 |
| PUT | `/api/me/notifications/push` | 필요 | 이 기기 푸시 구독 |
| DELETE | `/api/me/notifications/push` | 필요 | 이 기기 구독 해제 |

WebSocket (STOMP): `ws://localhost:8080/ws`

| 방향 | 주소 | 설명 |
|---|---|---|
| 발행 | `/app/chats/{roomId}/send` | 메시지 전송 |
| 구독 | `/topic/chats/{roomId}` | 그 방의 새 메시지 |
| 구독 | `/user/queue/chats` | 방 상태 변경 알림 (요청·수락·새 메시지) |
| 구독 | `/user/queue/errors` | 내 전송이 실패했을 때 |

토큰은 STOMP CONNECT 프레임의 `Authorization` 헤더로 보냅니다.
브라우저 WebSocket 은 핸드셰이크에 임의 헤더를 붙일 수 없기 때문입니다.

인증이 필요한 요청은 `Authorization: Bearer <accessToken>` 헤더를 붙입니다.

실패 응답은 모두 같은 형태입니다:

```json
{ "code": "DUPLICATE_EMAIL", "message": "이미 사용 중인 이메일입니다.", "fieldErrors": null }
```

## 패키지 구조

```
backend/src/main/java/com/soundmap/
├── global/
│   ├── config/   SecurityConfig (필터체인, CORS, BCrypt)
│   ├── error/    ErrorCode, ApiException, GlobalExceptionHandler
│   └── jwt/      JwtTokenProvider, JwtAuthenticationFilter, AuthPrincipal
├── auth/         AuthController, AuthService, dto/
├── user/         User 엔티티, UserRepository
├── music/        곡 검색(iTunes) · 체크인, Track/NowPlaying 엔티티
├── location/     GeoGrid(격자 스냅), GeoDistance(거리·바운딩박스), UserLocation
├── taste/        가수·장르 기준 데이터, 즐겨찾기, 프로필, 취향 일치도(TasteMatch)
├── chat/         채팅방·메시지, STOMP 컨트롤러, 브로드캐스터
├── safety/       차단·신고
├── settings/     공개 범위 설정
├── map/          주변 사용자 조회 + 마커 조합
├── notification/ 알림함, 웹푸시 구독·발송(VAPID)
├── dev/          더미 데이터 시더 (배포 시 SEED_ENABLED=false)
└── health/       HealthController
```

앞으로 추가될 도메인(`chat` 등)도 같은 층위에 둡니다.

```
frontend/src/
├── api/          axios 인스턴스, 토큰 인터셉터, auth API
├── auth/         AuthContext (로그인 상태 전역 관리)
├── components/   ProtectedRoute
├── map/          useGeolocation, albumMarker, TrackSearchPanel, PinSheet,
│                 TasteFilterBar, PinList
├── chat/         ChatSocketContext (앱 전역 WebSocket 연결)
├── music/        보관함 상태, 곡 카드, 저장 버튼, 미리듣기
├── notifications/ 웹푸시 구독 처리, 알림 설정 화면
├── safety/       ReportDialog
├── utils/        korean.js (조사 을/를 처리)
└── pages/        LoginPage, SignupPage, MapPage, OnboardingPage, ProfilePage,
                  ChatListPage, ChatRoomPage, SettingsPage
```

## 설정값

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `DB_USERNAME` | `soundmap` | MySQL 계정 (DB 이름은 `music`) |
| `DB_PASSWORD` | `soundmap_local_dev` | MySQL 비밀번호 (로컬 전용) |
| `JWT_SECRET` | 개발용 문자열 | HS256 키. 최소 32바이트 |
| `JWT_EXPIRATION_MINUTES` | `1440` | 액세스 토큰 유효시간 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 허용 오리진 (쉼표 구분) |
| `LOCATION_GRID_SIZE` | `0.001` | 격자 크기(도). 클수록 익명성이 높아짐 |
| `SEED_ENABLED` | `true` | 더미 사용자 시딩. **배포 시 false** |
| `ITUNES_COUNTRY` | `US` | 곡을 찾는 스토어프론트. KR 은 곡을 주지 않음 |
| `ITUNES_LOCALIZE_COUNTRY` | `KR` | 곡·가수 이름을 이 나라 표기로 바꿔 끼움 (비우면 끔) |
| `PUSH_VAPID_PUBLIC_KEY` | 없음 | 웹푸시 공개키. 비우면 푸시만 꺼지고 알림함은 동작 |
| `PUSH_VAPID_PRIVATE_KEY` | 없음 | 웹푸시 개인키. **서버 밖으로 내보내지 말 것** |
| `PUSH_VAPID_SUBJECT` | 없음 | 푸시 게이트웨이에 알리는 연락처 (`mailto:` 또는 사이트 주소) |

프론트엔드는 `frontend/.env.*` 를 씁니다.

| 환경변수 | 설명 |
|---|---|
| `VITE_API_BASE_URL` | 개발은 `http://localhost:8080`, 배포는 비움(같은 도메인) |
| `VITE_KAKAO_MAP_KEY` | 카카오맵 JavaScript 키 |

배포 시 `DB_PASSWORD`와 `JWT_SECRET`은 **반드시** 환경변수로 덮어쓰세요.
푸시 키는 `node deploy/generate-push-keys.mjs` 로 만듭니다.

## 개발 로드맵

### 추가 구현: 음악 발견과 알림

곡 보관함, 채팅 음악 카드, 취향순 정렬, 월별 공유 기록, 오늘의 지역 음악 차트,
알림함 및 선택형 Web Push를 추가했습니다. 화면 동작, 새 DB 스키마, 푸시 키 생성과
배포·검증 절차는 [음악 기능 운영 가이드](deploy/MUSIC_FEATURES.md)를 참고하세요.

기록과 차트는 이 버전 이후 새로 공유한 곡부터 누적됩니다.
푸시는 서버 VAPID 환경변수와 사용자 브라우저 알림 허용이 필요합니다.

- [x] **1주차** 프로젝트 세팅 + 로그인/회원가입
- [x] **2~3주차** 곡 체크인 + 지도에 내 앨범아트 마커
- [x] **4주차** 더미 유저 시딩 + 주변 사용자 조회
- [x] **5주차** 음악 취향 등록 + 프로필
- [x] **6주차** 가수/장르 검색 + 취향 일치도
- [ ] **7주차** 버퍼
- [x] **8~9주차** 1:1 채팅 (STOMP)
- [x] **10주차** 차단/신고/설정
- [ ] **11주차** 다듬기
- [ ] **12주차** 문서 + 발표

## 더미 데이터

앱을 처음 띄우면 한동대 반경 1.5km 안에 더미 사용자 40명이 자동 생성됩니다.
각자 위치와 재생곡을 가지고 있어서, 개발 중에도 지도가 비어 보이지 않습니다.

- 이미 있으면 다시 만들지 않습니다. 새로 만들려면 `users` 테이블을 비우세요.
- 계정: `dummy01@dummy.soundmap.dev` ~ `dummy40@dummy.soundmap.dev`,
  비밀번호는 모두 `dummy_password`
  (8주차 채팅 테스트 때 이 계정으로 로그인해 상대역을 만들 수 있습니다)
- 곡 정보는 `resources/seed/dummy-tracks.json`에 굳혀 두었습니다.
  시딩할 때마다 외부 API를 호출하지 않기 위해서입니다.
- **배포할 때는 `SEED_ENABLED=false`로 끄세요.**

## 음악 취향

가수 58명과 장르 14개는 `ReferenceDataSeeder`가 넣습니다. 더미 사용자와 달리
운영에서도 필요한 데이터라 `SEED_ENABLED`와 무관하게 항상 실행되며, 이미 있는 항목은 건너뜁니다.
가수 이미지는 iTunes에서 대표곡 아트워크를 받아 `resources/seed/artists.json`에 굳혀 두었습니다.

즐겨찾기 테이블은 `(user_id, artist_id)` 복합 기본키를 씁니다. 중복 저장을 구조적으로 막고,
PK 인덱스가 "이 사용자의 가수 목록" 조회에 그대로 쓰입니다. 반대 방향("이 가수를 좋아하는 사람")은
6주차 검색에서 필요하므로 `artist_id`에 별도 인덱스를 두었습니다.

취향 저장은 부분 수정이 아니라 **통째로 교체**입니다. delete 후 insert 하므로 사이에 `flush()`가
필요합니다 — 없으면 같은 키로 insert가 먼저 나가 충돌합니다.

가수를 최소 3명 받는 것은 6주차 취향 일치도 때문입니다. 한두 명만 골라두면 교집합이
0 아니면 100으로 튀어서 숫자가 의미를 잃습니다.

취향을 등록하지 않은 사용자는 `ProtectedRoute`가 `/onboarding`으로 보냅니다.
온보딩 화면 자체는 `requireTaste={false}`로 두어야 무한 리다이렉트가 나지 않습니다.

## 주변 사용자 조회 방식

MySQL에는 PostGIS의 `ST_DWithin`처럼 반경 검색이 인덱스를 타는 함수가 없습니다.
`ST_Distance_Sphere(...) < 2000` 으로 쓰면 전체 스캔이 됩니다. 그래서 2단계로 나눕니다.

1. **사각형으로 후보 압축** — `grid_lat`, `grid_lon`에 `BETWEEN`. 복합 인덱스를 탑니다.
2. **원형 거리로 재검증** — 애플리케이션에서 haversine으로 계산한 뒤 가까운 순으로 정렬.

사각형은 원보다 넓기 때문에 2단계가 반드시 필요합니다. 이 성질을 테스트로 고정해 두었습니다
(`GeoDistanceTest.boundingBoxIsWiderThanCircle`).

재생곡은 사용자 수만큼 조회하지 않고 `IN` 절로 한 번에 가져옵니다(N+1 방지).
마커가 40개든 400개든 쿼리 수는 고정입니다.

> 참고: 행이 적을 때는 MySQL 옵티마이저가 `idx_user_locations_geo` 대신 PK 스캔을
> 고르기도 합니다. 인덱스가 잘못된 게 아니라, 그 규모에서는 그게 더 싸기 때문입니다.

## 취향 일치도

자카드 유사도를 씁니다. 교집합 크기를 합집합 크기로 나눈 값입니다.

교집합만 세면 가수를 30명 고른 사람이 무조건 유리해집니다. 합집합으로 나누면
"고른 것 대비 얼마나 겹치는가"가 되어 취향의 폭과 무관해집니다.
이 성질을 테스트로 고정해 두었습니다 (`TasteMatchTest.breadthDoesNotInflateScore`).

최종 점수는 가수 70% + 장르 30% 입니다. 장르는 14개뿐이라 우연히 겹치기 쉬운 반면,
같은 가수를 좋아한다는 건 훨씬 구체적인 신호이기 때문입니다.

## 검색과 목록

지도와 목록은 **같은 응답**을 씁니다. `/api/map/pins` 한 번으로 받아 화면에서만 갈라 씁니다.
검색 결과를 두 방식으로 보여주는 것이 이 서비스의 탐색 방식이라, 응답을 나누면
두 화면이 어긋나기 쉽습니다.

가수/장르 필터는 SQL 이 아니라 애플리케이션에서 겁니다. 일치도 계산을 위해 후보들의
취향을 어차피 한 번에 불러오므로, 필터링이 집합 조회로 끝나 추가 쿼리가 들지 않습니다.
마커가 4개든 42개든 **쿼리는 9개로 고정**입니다.

## 채팅

**수락 전에는 메시지를 보낼 수 없습니다.** 낯선 사람이 일방적으로 말을 걸 수 있으면
위치 기반 서비스에서 곧바로 괴롭힘 통로가 됩니다. 요청자가 스스로 수락하는 것도 막았고,
거절당한 뒤 다시 요청하는 것도 막았습니다.

방은 두 사용자 id 를 작은 쪽/큰 쪽으로 정렬해 저장합니다. A→B 요청과 B→A 요청이
같은 행으로 모여서, 유니크 제약 하나로 중복 방을 막을 수 있습니다.

안 읽은 개수와 마지막 메시지는 `chat_rooms` 에 함께 둡니다. 방 목록을 그릴 때마다
방마다 메시지 테이블을 뒤지면 방 수만큼 쿼리가 나가기 때문입니다.

**전송할 때는 방 행에 쓰기 잠금을 겁니다** (`findByIdForUpdate`). 잠금이 없으면 메시지가
거의 동시에 들어왔을 때 두 트랜잭션이 같은 값을 읽고 같은 값을 써서 증가가 사라집니다
(lost update). 실제로 3건을 연달아 보냈더니 카운터가 1로 남는 것을 확인했고,
잠금을 건 뒤 20건 동시 전송에서 정확히 20이 나오는 것을 확인했습니다.

WebSocket 이 끊겨도 REST 로 보낼 수 있게 두 경로 모두 열어 두었습니다. 어느 쪽으로
들어와도 검증과 저장은 같은 `ChatService.sendMessage` 를 거칩니다.

## 차단과 신고

**차단은 한쪽만 눌러도 양방향으로 동작합니다.** A가 B를 차단하면 B의 지도에서도 A가
사라집니다. 차단당한 쪽에게 그 사실이 드러나지 않아야 하고, 계정을 새로 만들지 않는 한
다시 접근할 수 없어야 하기 때문입니다.

차단은 **지도 · 검색 · 프로필 · 채팅 목록 · 메시지 전송 · 대화 요청** 여섯 군데 모두에
걸립니다. 걸러내는 곳이 흩어지면 한 곳을 빠뜨렸을 때 그게 곧 구멍이 되므로,
`SafetyService.hiddenUserIdsFor` 한 곳에서 집합을 만들어 각 화면이 가져다 씁니다.
지도는 아예 SQL 단계(`findInBoundingBox`)에서 제외합니다. 화면에서만 감추면 응답에는
남아 있어서 개발자 도구로 그대로 보입니다.

차단된 상대의 프로필은 403 이 아니라 **404** 를 줍니다. 403 은 "차단당했구나" 를
알려주는 셈이 되기 때문입니다.

신고와 차단은 별개 동작으로 두되, 화면에서 "함께 차단"을 기본 체크로 제안합니다.
같은 상대를 24시간 안에 다시 신고하면 중복으로 봅니다.

## 공개 범위 설정

| 항목 | 끄면 |
|---|---|
| 위치 공유 | 저장된 좌표를 **삭제**하고 새 좌표도 받지 않습니다 |
| 현재 듣는 곡 | 남의 화면에서만 감춥니다. 내 화면에는 그대로 보입니다 |
| 프로필 공개 | `CHAT_ONLY` 면 대화가 수락된 상대에게만 취향을 보여줍니다 |

위치는 지우고 재생곡은 감추는 차이가 있습니다. 위치는 "어디 있는지"라서 서버가
보관하지 않는 편이 안전하고, 재생곡은 "무엇을 보여줄지"라서 내 화면에는 계속 필요합니다.

설정 행은 한 번이라도 건드린 사용자에게만 생깁니다. 없으면 기본값(전부 공개)으로 봅니다.

## 배포

AWS Lightsail 배포 절차는 `deploy/README.md` 에 있습니다.
nginx 설정과 systemd 유닛, 환경변수 템플릿도 `deploy/` 안에 함께 있습니다.

**HTTPS 가 필수입니다.** 브라우저 Geolocation API 는 보안 컨텍스트에서만 동작해서,
HTTP 로 띄우면 폰에서 위치 기능이 아예 되지 않습니다.

배포 전에 `npm run build && npm run preview` 로 서버와 같은 조건
(같은 도메인 · 상대 경로 · WebSocket 프록시)을 로컬에서 확인할 수 있습니다.

## 알아둘 것

- **원본 GPS는 저장하지 않습니다.** 좌표는 수신 즉시 격자로 스냅하고 스냅된 값만 DB에 넣습니다.
- 조회 API를 만들 때 **차단 사용자 필터가 들어갈 자리를 미리 비워두세요.** 나중에 끼워 넣으면 쿼리를 전부 고쳐야 합니다. `MapService.findVisiblePins`에 자리를 표시해 두었습니다.
- 마커가 많아지면 겹칩니다. 더미를 40명으로 유지하면 견딜 만합니다. 늘릴 계획이면 카카오맵의 `MarkerClusterer` 를 붙이세요.
- 개발 중 `users` 테이블을 비우고 `AUTO_INCREMENT`를 되돌리면, 브라우저에 남아 있던 토큰이 **다른 사람의 id로 붙습니다.** DB를 리셋하면 브라우저 localStorage도 함께 비우세요.
- 토큰은 `localStorage`에 둡니다. XSS에 노출되는 방식이라 상용화 시에는 refresh 토큰 + HttpOnly 쿠키로 옮기는 것을 고려하세요.
- Spring Boot 4는 Jackson 3(`tools.jackson`)를 씁니다. Jackson 2 기반 라이브러리(`jjwt-jackson` 등)와 섞이지 않게 주의하세요. 그래서 JWT 직렬화는 `jjwt-gson`을 씁니다.
- Boot 4에는 `RestClient.Builder` 빈과 `ClientHttpRequestFactoryBuilder`가 없습니다. Boot 3 예제를 그대로 복사하면 기동에 실패합니다.
- 개발 모드에서는 React StrictMode 때문에 **모든 요청이 두 번 나갑니다.** 프로덕션 빌드에서는 한 번입니다. 네트워크 탭에서 중복이 보여도 버그가 아닙니다. 같은 이유로 콘솔에 `WebSocket connection ... failed` 가 두 번 찍히는데, 첫 연결이 이중 마운트로 중단된 흔적입니다.
- **같은 브라우저의 탭 두 개로 두 계정을 테스트할 수 없습니다.** `localStorage` 가 탭 간에 공유되어 토큰이 서로 덮어씁니다. 시크릿 창이나 다른 브라우저를 쓰세요.
- 가수 이름을 문장에 넣을 때는 `objectParticle` 로 조사를 고릅니다. 그냥 `를` 를 붙이면 "폴킴를 좋아합니다" 가 됩니다.
- iTunes 검색 결과는 30분 · 최대 300건으로 캐싱합니다(`SearchCache`). 분당 20회 수준의 쿼터가 있어, 캐시가 없으면 개발 중에도 쉽게 막힙니다. 빈 결과는 캐시하지 않습니다 — 레이트 리밋으로 빈 응답이 온 검색어가 30분간 막혀버리기 때문입니다.
- 예외 처리에서 `Exception` 을 통째로 잡아 500 을 주면 안 됩니다. Spring MVC 가 던지는 요청 오류들은 이미 올바른 상태 코드를 들고 있어서, 덮어쓰면 클라이언트 잘못이 서버 장애처럼 보입니다. `GlobalExceptionHandler` 가 `ErrorResponse` 구현 여부를 확인해 상태 코드를 살립니다.
- **카카오맵 키는 세 단계를 다 통과해야 뜹니다.** ①유효한 JavaScript 키 ②개발자 콘솔에 사이트 도메인 등록(`http://localhost:5173`, 배포 도메인) ③제품 설정에서 카카오맵 활성화. 하나라도 빠지면 스크립트가 401/403 으로 내려오지 않는데, 브라우저 콘솔만 봐서는 이유를 알 수 없습니다. 서버에서 `curl` 로 부르면 응답 본문에 이유가 담겨 옵니다.
- **`map.setLevel(level, { animate: true })` 를 쓰지 마세요.** 한 단계는 움직이지만 그 뒤로 축척이 바뀌지 않습니다. 옵션 없이 부르면 정상입니다.
- **iTunes 는 KR 스토어프론트로 곡을 주지 않습니다.** 한국은 iTunes 음악 판매 지역이 아니라 `entity=song` 검색이 항상 0건입니다. 그래서 곡은 US 로 찾고, 받은 trackId 를 KR `lookup` 으로 한 번 더 조회해 한국어 제목·가수명만 바꿔 끼웁니다. 미리듣기 주소는 US 에만 있습니다.
- **한국어 줄바꿈은 `word-break: keep-all` 이 필요합니다.** 기본값은 글자 단위로 끊어서 "다릅니다" 가 "다릅 / 니다" 로 갈라집니다. 다만 keep-all 만 두면 긴 주소가 화면 밖으로 나가므로 `overflow-wrap: break-word` 를 함께 겁니다. `body` 에 걸어 두었습니다.
- **서버의 nginx 설정을 `deploy/nginx-soundmap.conf` 로 덮어쓰지 마세요.** certbot 이 HTTPS 를 붙이면서 443 리스너와 인증서 경로를 서버 파일에만 추가해 두었습니다. 덮어쓰면 `nginx -t` 는 통과하지만 사이트가 HTTPS 로 접속되지 않습니다. 서버 파일을 직접 고치고 `ss -lntp | grep :443` 으로 확인하세요.
- 줄바꿈은 LF 로 통일합니다(`.editorconfig`). 셸 스크립트가 CRLF 로 저장되면 리눅스에서 실행되지 않습니다.
