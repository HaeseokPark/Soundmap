# 음악 발견 기능 추가

프런트엔드와 백엔드를 함께 배포해야 합니다. 기존 서버의 nginx/HTTPS 설정이나 운영 환경변수는 이 작업에서 변경하지 않았습니다.

## 화면

- 지도: `정렬`에서 가까운 순 / 취향이 비슷한 순. 취향순을 고르면 목록으로 전환합니다. 공통 가수 수와 이름을 추천 이유로 표시합니다.
- 곡 보관함 `/music/library`: 지도 상세, 프로필, 내 현재 곡, 음악 카드, 기록, 차트의 `곡 저장`. 사용자별 저장·해제, 페이지당 50곡.
- 음악 카드 채팅: 수락된 채팅에서 `♪ 곡 추천` → 검색 → 곡 선택. 앨범·가수·미리듣기·저장 버튼이 있는 카드로 전송하며 REST 저장 후 WebSocket으로 전달합니다. 미리듣기 URL이 없는 곡은 기존 미리듣기 정책을 따릅니다.
- 개인 음악 기록 `/music/history`: 새로 공유한 곡부터 기록합니다. 월별 목록, 가수별 공유 횟수, 개별 삭제. 과거 현재 곡 데이터를 청취 기록으로 소급 생성하지 않습니다.
- 지역 차트 `/music/chart`: 한국 시간 오늘, 반경 500m/1km/2km/5km, 최대 20곡. **현재 공개 위치** 기준입니다. 과거 좌표는 저장하지 않습니다. 같은 곡을 여러 번 올려도 사람당 1회입니다. 공개 시점에 비공개였던 곡, 현재 비공개 설정, 차단 관계, 만료 위치는 집계에서 제외합니다. 본인의 공개 체크인도 포함합니다.
- 알림함 `/notifications`: 대화 요청·수락·새 메시지 저장, 개별/전체 읽음, 50건씩 더 보기, 안 읽은 배지. 동일 요청/수락 재호출로 알림을 중복 생성하지 않습니다.

## 데이터베이스

추가 테이블은 `saved_tracks`, `music_check_ins`, `user_notifications`, `push_subscriptions`이며 `chat_messages`에 nullable `track_id`가 추가됩니다.

프로젝트 기본 `JPA_DDL_AUTO=update`에서는 시작할 때 추가됩니다. `validate`로 운영하는 경우 백업 후 `backend/db/music-features.sql`을 **기능 최초 배포 전에 한 번** 적용하세요. 이미 자동 생성된 DB에는 중복 적용하지 마세요. 기존 채팅은 `track_id=NULL`인 텍스트로 그대로 표시됩니다.

## 선택형 Web Push

1. 운영자 연락 이메일로 키를 한 번 생성합니다:

   ```sh
   node deploy/generate-push-keys.mjs mailto:admin@example.com
   ```

2. 생성된 `deploy/push-keys.env`의 세 환경변수를 서버 서비스 환경에 등록합니다. 파일은 git에서 제외되며 기존 파일을 덮어쓰지 않습니다. 공개키만 프런트로 전달됩니다. 개인키는 서버에서만 보관합니다.
3. 백엔드를 재시작하고 새 프런트를 배포합니다. `dist/sw.js`도 웹 루트에 있어야 합니다. HTTPS가 필요합니다(localhost 테스트 제외). `/sw.js`는 SPA HTML로 대체되거나 장기 캐시되지 않도록 설정합니다.
4. 사용자가 알림함 또는 설정에서 `이 기기 푸시 켜기`를 누르고 브라우저 권한을 허용합니다. iOS/iPadOS에서는 홈 화면에 설치한 웹 앱과 지원 버전이 필요합니다.

키가 없으면 버튼을 비활성화하고 설정 필요 안내를 표시하며, 알림함은 정상 작동합니다. 로그아웃 시 현재 기기 구독을 해제합니다. 계정을 새로 로그인하면 이전 브라우저 구독을 폐기하고 새 계정에서 다시 켜도록 합니다.

알림에는 메시지 본문, 사용자 위치, 음악 취향을 넣지 않습니다. 푸시 전송은 DB 커밋 이후 별도 제한된 스레드 풀에서 처리하며 실패해도 저장된 알림과 채팅은 유지됩니다. 만료된 구독(404/410)은 제거합니다. 외부 푸시 장애와 큐 포화 시 별도 재시도는 없으며 알림함에서 확인할 수 있습니다. 푸시 엔드포인트는 Chrome/Firefox/Safari/Windows의 허용된 HTTPS 게이트웨이로 제한합니다.

실제 브라우저 푸시 도착은 운영 키와 브라우저 권한을 설정한 환경에서 최종 확인해야 합니다. 라이브러리 사용은 [webpush-java 공식 문서](https://github.com/web-push-libs/webpush-java)를 따릅니다.

## 검증

```powershell
.\backend\gradlew.bat -p backend test --console=plain
npm.cmd --prefix frontend run build
npm.cmd --prefix frontend run lint
```

H2 통합 테스트는 소유권, 중복 저장, 월별 기록/삭제/페이지, 차트의 중복·거리·위치 만료·공개 범위·차단, 채팅 수락 권한과 음악 카드 보존, 알림 읽음/중복을 확인합니다. VAPID 암호화 요청 생성은 외부 전송 없이 테스트합니다.

실제 API 점검은 운영 MySQL과 분리된 임시 서버에서 실행합니다:

```powershell
.\backend\gradlew.bat -p backend -I ../tools/smoke-server.gradle smokeServer --console=plain
# 별도 터미널: 새 임시 서버에서 한 번 실행
python tools/smoke-api.py
# 프런트 확인용
$env:VITE_API_BASE_URL='http://localhost:18080'
npm.cmd --prefix frontend run dev -- --host localhost --port 15173
```

임시 DB의 테스트 로그인은 `qa_alice@test.local` / `SmokeTest123!`입니다. 서버를 종료하면 데이터가 사라집니다. 별도 포트에서는 카카오 앱의 허용 도메인 설정에 따라 지도가 로드되지 않을 수 있습니다.
