# AWS Lightsail 배포

한 대의 Lightsail 인스턴스에 nginx · Spring Boot · MySQL 을 모두 올리는 구성입니다.
캡스톤 규모에서는 이게 가장 싸고 단순합니다.

```
        인터넷
          │  https (443)
      ┌───▼────────────────────────────┐
      │  nginx                          │
      │   /        → /var/www/soundmap  │  React 빌드 결과
      │   /api/    → 127.0.0.1:8080     │  Spring Boot
      │   /ws      → 127.0.0.1:8080     │  WebSocket
      └───┬─────────────────────────────┘
          │
      ┌───▼──────────┐   ┌──────────────┐
      │ soundmap.jar │──▶│  MySQL 8.0   │
      └──────────────┘   └──────────────┘
```

프론트와 API 가 **같은 도메인**을 쓰므로 프론트는 `/api/...` 상대 경로로 호출하고,
WebSocket 주소도 브라우저가 접속한 주소에서 자동으로 유도됩니다.

다만 같은 도메인이어도 **CORS 설정은 여전히 필요합니다.** 백엔드가 프록시 뒤에 있어서
브라우저가 보낸 Origin 을 교차 출처로 인식하기 때문입니다. 아래 "CORS 를 조심하세요" 참고.

---

## 시작 전에 알아둘 것

**HTTPS 는 선택이 아니라 필수입니다.** 브라우저의 Geolocation API 는 보안 컨텍스트
(HTTPS 또는 localhost)에서만 동작합니다. HTTP 로 띄우면 PC 에서는 되는 것처럼 보여도
**폰에서 위치 기능이 아예 동작하지 않습니다.** 이 서비스의 핵심 기능이 죽는 것이라
도메인과 인증서 준비가 배포의 출발점입니다.

**도메인이 필요합니다.** Let's Encrypt 는 IP 주소에 인증서를 발급하지 않습니다.

- 도메인 구입: 가비아·Cloudflare 등에서 연 1~2만 원대
- 무료: [DuckDNS](https://www.duckdns.org) 같은 서비스로 `이름.duckdns.org` 를 받아도
  Let's Encrypt 인증서가 발급됩니다. 발표용으로 충분합니다.

**인스턴스 크기.** JVM + MySQL + nginx 를 한 장비에 올리므로 메모리 1GB 는 빠듯합니다.
**2GB 플랜을 권합니다.** 1GB 로 가려면 스왑을 반드시 잡으세요(아래 3단계 참고).
요금은 바뀌므로 콘솔에서 확인하세요.

---

## 1. 인스턴스 만들기

Lightsail 콘솔의 **인스턴스 생성** 화면에서 아래대로 고릅니다.

| 항목 | 선택 |
|---|---|
| 리전 | 서울 `ap-northeast-2` (가용 영역은 아무거나) |
| 플랫폼 | Linux/Unix |
| 블루프린트 | **[OS 전용] 탭 → Ubuntu 24.04 LTS** |
| SSH 키페어 | 새로 만들고 `.pem` 다운로드 |
| 자동 스냅샷 | 켜기 (권장) |
| 플랜 | **2GB RAM / 2 vCPU** |
| 인스턴스 이름 | `soundmap` |

### 블루프린트는 반드시 "OS 전용"

`[앱 + OS]` 탭의 Node.js·LAMP 같은 항목은 Bitnami 스택이라 nginx 와 서비스 구동 방식이
자체 규칙으로 미리 잡혀 있습니다. 이 저장소의 `nginx-soundmap.conf` 와
`soundmap.service` 가 그 구성과 충돌합니다. 빈 Ubuntu 에서 시작하세요.

### 플랜은 2GB

이 앱이 한 장비에서 실제로 쓰는 메모리입니다.

```
Spring Boot (JVM, -Xmx640m)   ~500MB
MySQL 8                       ~400MB
nginx                          ~10MB
Ubuntu 자체                   ~200MB
                              ─────────
                              ~1.1GB
```

1GB 플랜은 이 상태로 이미 넘칩니다. 스왑을 잡으면 죽지는 않지만 눈에 띄게 느려집니다.
1GB 로 가야 한다면 스왑(3단계)과 함께 MySQL 버퍼 풀을 줄이세요.

```bash
sudo tee /etc/mysql/mysql.conf.d/lowmem.cnf > /dev/null <<EOF
[mysqld]
innodb_buffer_pool_size = 128M
max_connections = 30
performance_schema = OFF
EOF
sudo systemctl restart mysql
```

### 생성 직후에 할 일

**1) 고정 IP 연결** — `네트워킹` 탭 → 고정 IP 생성 → 인스턴스에 연결

안 하면 재시작할 때마다 IP 가 바뀌어 도메인 연결과 SSL 인증서가 깨집니다.
인스턴스에 붙어 있는 동안은 무료입니다. 다만 인스턴스를 지울 때 고정 IP 를 남겨두면
그때부터 과금되니 같이 지우세요.

**2) 방화벽** — 인스턴스 → `네트워킹` 탭

기본으로 22 와 80 만 열려 있습니다. **443 을 직접 추가해야 합니다.**

| 규칙 | 포트 |
|---|---|
| SSH | 22 |
| HTTP | 80 |
| HTTPS | **443 ← 추가** |

**8080 과 3306 은 열지 마세요.** nginx 가 앞에서 받고 MySQL 은 localhost 로만
접속합니다. 열어두면 백엔드가 HTTPS 없이 노출되고 DB 가 인터넷에 드러납니다.

Lightsail 방화벽은 IPv4 와 IPv6 규칙이 따로입니다. IPv6 를 쓸 일이 없으니
`네트워킹` 탭에서 꺼두면 규칙을 두 벌 관리할 일이 없어집니다.

### SSH 접속

`.pem` 키는 **인스턴스를 만들 때 한 번만** 받을 수 있습니다. 잃어버리면 콘솔의
브라우저 SSH 로는 들어갈 수 있어도 `scp` 업로드가 안 됩니다.

Ubuntu 인스턴스의 접속 계정명은 `ubuntu` 입니다.

```bash
ssh -i ~/.ssh/lightsail.pem ubuntu@고정IP
```

Windows 에서 권한 문제로 거부되면:

```powershell
icacls "$HOME.sshlightsail.pem" /inheritance:r /grant:r "$($env:USERNAME):(R)"
```

---

## 2. 도메인 연결

도메인 관리 화면에서 A 레코드를 고정 IP 로 지정합니다.

```
soundmap.example.com.   A   43.201.xx.xx
```

DuckDNS 를 쓴다면 사이트에서 IP 만 입력하면 됩니다.

전파 확인:

```bash
dig +short soundmap.example.com
```

고정 IP 가 나오면 다음으로 갑니다. 안 나오면 몇 분 기다리세요.

---

## 3. 서버 기본 설정

SSH 로 접속합니다 (Lightsail 콘솔의 브라우저 SSH 또는 발급받은 키).

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-21-jre-headless mysql-server nginx unzip
```

**메모리 1GB 플랜이라면 스왑을 잡으세요.** 없으면 빌드나 피크 시점에
OOM Killer 가 Java 프로세스를 죽입니다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

시간대를 맞춰 둡니다. 채팅 시각과 "3분 전" 표시가 틀어지지 않게 합니다.

```bash
sudo timedatectl set-timezone Asia/Seoul
```

---

## 4. 데이터베이스

```bash
sudo mysql_secure_installation
```

익명 사용자 제거·원격 root 로그인 금지에는 모두 `y` 로 답하세요.

이제 DB 와 앱 계정을 만듭니다. **비밀번호는 직접 정해서 넣으세요.**

```bash
sudo mysql
```

```sql
CREATE DATABASE IF NOT EXISTS music
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'soundmap'@'localhost' IDENTIFIED BY '여기에_긴_임의_문자열';
GRANT ALL PRIVILEGES ON music.* TO 'soundmap'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

`'localhost'` 로 만드는 것이 중요합니다. `'%'` 로 만들면 외부에서 접속 가능한
계정이 됩니다.

---

## 5. 앱 계정과 디렉터리

서비스를 root 로 돌리지 않기 위해 전용 계정을 만듭니다.

```bash
sudo useradd --system --no-create-home --shell /usr/sbin/nologin soundmap
sudo mkdir -p /opt/soundmap /etc/soundmap /var/www/soundmap
sudo chown -R soundmap:soundmap /opt/soundmap /etc/soundmap
```

---

## 6. 빌드 (내 PC 에서)

서버에서 빌드하지 말고 로컬에서 만들어 올리는 편이 낫습니다.
Gradle 빌드는 메모리를 꽤 먹어서 작은 인스턴스에서는 느리거나 실패합니다.

```powershell
cd C:\Users\danie\postCpastone_music\backend
.\gradlew.bat clean bootJar
```

```powershell
cd C:\Users\danie\postCpastone_music\frontend
npm run build
```

결과물:

- `backend/build/libs/soundmap-0.0.1-SNAPSHOT.jar`
- `frontend/dist/`

> 프론트 빌드는 `.env.production` 을 자동으로 읽습니다. 그 파일의
> `VITE_API_BASE_URL` 은 비어 있어야 합니다. 값이 들어 있으면 배포된 화면이
> 여전히 localhost:8080 을 호출합니다.

---

### 배포 전에 로컬에서 미리 확인하기

빌드한 결과를 서버와 **같은 조건**(같은 도메인 · 상대 경로 · WebSocket 업그레이드)에서
먼저 확인할 수 있습니다. 이걸 건너뛰면 "로컬에서는 됐는데 서버에서만 안 되는" 문제를
배포 후에야 발견하게 됩니다.

백엔드를 띄운 상태에서:

```powershell
cd frontend
npm run build
npm run preview
```

http://localhost:4173 으로 열어 아래를 확인하세요. nginx 뒤에 있는 것과 같은 상태입니다.

- [ ] 로그인 · 지도가 뜬다 (상대 경로 API 호출이 통한다)
- [ ] 주소창에 `/chats/1` 을 직접 입력해도 열린다 (SPA 라우팅)
- [ ] 채팅이 실시간으로 오간다 (WebSocket 업그레이드)

---

## 7. 업로드

`~/.ssh/lightsail.pem` 은 Lightsail 에서 받은 키 경로로 바꾸세요.

```powershell
$KEY = "$HOME\.ssh\lightsail.pem"
$HOST_ = "ubuntu@43.201.xx.xx"

scp -i $KEY backend\build\libs\soundmap-0.0.1-SNAPSHOT.jar ${HOST_}:/tmp/soundmap.jar
scp -i $KEY -r frontend\dist\* ${HOST_}:/tmp/dist/
scp -i $KEY -r deploy ${HOST_}:/tmp/deploy
```

서버에서 제자리로 옮깁니다.

```bash
sudo mv /tmp/soundmap.jar /opt/soundmap/soundmap.jar
sudo chown soundmap:soundmap /opt/soundmap/soundmap.jar

sudo rm -rf /var/www/soundmap/*
sudo cp -r /tmp/dist/* /var/www/soundmap/
sudo chown -R www-data:www-data /var/www/soundmap
```

---

## 8. 환경변수

```bash
sudo cp /tmp/deploy/soundmap.env.example /etc/soundmap/soundmap.env
sudo chown soundmap:soundmap /etc/soundmap/soundmap.env
sudo chmod 600 /etc/soundmap/soundmap.env
sudo nano /etc/soundmap/soundmap.env
```

JWT 비밀키는 직접 만들어 넣으세요.

```bash
openssl rand -base64 48
```

**꼭 확인할 값**

| 항목 | 값 |
|---|---|
| `DB_PASSWORD` | 4단계에서 정한 비밀번호 |
| `JWT_SECRET` | 위 명령으로 만든 값 |
| `SEED_ENABLED` | **`false`** — 안 그러면 가짜 사용자 40명이 생깁니다 |
| `LOG_SQL` | **`info`** — `debug` 로 두면 디스크가 찹니다 |
| `CORS_ALLOWED_ORIGINS` | `https://내도메인` |

---

## 9. 서비스 등록

```bash
sudo cp /tmp/deploy/soundmap.service /etc/systemd/system/soundmap.service
sudo systemctl daemon-reload
sudo systemctl enable --now soundmap
sudo systemctl status soundmap
```

로그로 기동을 확인합니다.

```bash
sudo journalctl -u soundmap -f
```

`Started SoundmapApplication` 이 보이면 성공입니다. 그 자리에서 확인:

```bash
curl http://127.0.0.1:8080/api/health
```

---

## 10. nginx

```bash
sudo cp /tmp/deploy/nginx-soundmap.conf /etc/nginx/sites-available/soundmap
sudo nano /etc/nginx/sites-available/soundmap    # server_name 을 내 도메인으로
sudo ln -s /etc/nginx/sites-available/soundmap /etc/nginx/sites-enabled/soundmap
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

이 시점에 `http://내도메인` 으로 화면이 떠야 합니다.
아직 HTTP 라 위치 기능은 동작하지 않습니다.

---

## 11. HTTPS (여기가 핵심)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d soundmap.example.com
```

- 이메일 입력 (만료 알림용)
- 약관 동의
- 리다이렉트 여부를 물으면 **Redirect(HTTP→HTTPS)** 선택

certbot 이 nginx 설정에 443 블록과 리다이렉트를 자동으로 추가합니다.
갱신은 자동으로 걸립니다. 확인:

```bash
sudo certbot renew --dry-run
```

이제 `https://내도메인` 으로 접속하면 폰에서도 위치 기능이 동작합니다.

---

## 12. 확인 목록

브라우저에서 순서대로 확인하세요.

- [ ] `https://내도메인` 이 자물쇠 아이콘과 함께 열린다
- [ ] 회원가입 → 온보딩(가수·장르 선택) → 지도 진입
- [ ] **폰에서** "내 위치 공유" 를 눌렀을 때 권한 창이 뜨고 마커가 찍힌다
- [ ] 곡 검색이 결과를 준다 (iTunes API 호출)
- [ ] `/chats/1` 같은 주소로 **새로고침**해도 404 가 나지 않는다 (SPA 라우팅)
- [ ] 두 계정으로 대화가 실시간으로 오간다 (**WebSocket**)
- [ ] 개발자 도구 Network 에 `/ws` 가 `101 Switching Protocols` 로 뜬다

마지막 항목이 실패하면 nginx 의 WebSocket 블록(`Upgrade`/`Connection` 헤더)을
확인하세요. 이게 빠지면 **다른 기능은 다 되는데 채팅만 안 되는** 상태가 됩니다.

---

## 다시 배포할 때

```powershell
# 로컬에서 빌드
cd backend; .\gradlew.bat clean bootJar; cd ..
cd frontend; npm run build; cd ..

# 업로드
scp -i $KEY backend\build\libs\soundmap-0.0.1-SNAPSHOT.jar ${HOST_}:/tmp/soundmap.jar
scp -i $KEY -r frontend\dist\* ${HOST_}:/tmp/dist/
```

```bash
# 서버에서 교체
sudo systemctl stop soundmap
sudo mv /tmp/soundmap.jar /opt/soundmap/soundmap.jar
sudo chown soundmap:soundmap /opt/soundmap/soundmap.jar
sudo rm -rf /var/www/soundmap/* && sudo cp -r /tmp/dist/* /var/www/soundmap/
sudo chown -R www-data:www-data /var/www/soundmap
sudo systemctl start soundmap
```

---

## 자주 막히는 곳

| 증상 | 원인 |
|---|---|
| 폰에서 위치 권한 창이 안 뜬다 | HTTP 로 접속 중. HTTPS 여야 합니다 |
| 채팅만 안 된다 | nginx `/ws` 블록의 `Upgrade`/`Connection` 헤더 누락 |
| 새로고침하면 404 | nginx `try_files ... /index.html` 누락 |
| 화면은 뜨는데 API 가 전부 실패 | 프론트를 `.env.production` 없이 빌드해서 localhost:8080 을 호출 중 |
| 모든 요청이 403 · `Invalid CORS request` | `CORS_ALLOWED_ORIGINS` 가 실제 접속 주소와 다름 (아래 참고) |
| 기동 직후 죽는다 | `journalctl -u soundmap -n 50` — 대개 DB 비밀번호나 `JWT_SECRET` 길이(32바이트 미만) |
| 가짜 사용자 40명이 보인다 | `SEED_ENABLED` 가 `false` 가 아님 |
| 며칠 뒤 디스크가 꽉 찬다 | `LOG_SQL` 이 `debug` |
| 갑자기 느려지고 죽는다 | 메모리 부족. 스왑을 잡거나 플랜을 올리세요 |

## CORS 를 조심하세요

nginx 가 프론트와 API 를 같은 도메인으로 서비스하더라도, **백엔드는 여전히 Origin 헤더를
검사합니다.** 브라우저는 POST 같은 요청에 같은 도메인이어도 Origin 을 붙이고, 백엔드는
프록시 뒤에 있어서 그것을 교차 출처로 인식하기 때문입니다.

그래서 `CORS_ALLOWED_ORIGINS` 가 **브라우저 주소창에 뜨는 주소와 정확히** 같아야 합니다.

| 접속 주소 | 설정해야 할 값 |
|---|---|
| `https://soundmap.example.com` | `https://soundmap.example.com` |
| `https://www.soundmap.example.com` | `https://www.soundmap.example.com` |

`http` 와 `https`, `www` 유무가 다르면 전부 막힙니다. 두 주소를 다 쓴다면 쉼표로 나열하세요.
증상은 로그인부터 실패하고 응답 본문에 `Invalid CORS request` 가 찍히는 형태입니다.

## 백업

캡스톤이라도 발표 전에 DB 는 받아 두세요.

```bash
mysqldump -u soundmap -p --default-character-set=utf8mb4 music > ~/music-$(date +%F).sql
```

Lightsail 콘솔의 **스냅샷**으로 인스턴스 전체를 떠 두는 것도 방법입니다.
발표 직전에 한 번 떠 두면 무슨 일이 있어도 되돌릴 수 있습니다.
