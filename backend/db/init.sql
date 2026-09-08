-- SoundMap 로컬 개발용 DB 초기화
-- 실행: mysql -u root -p < backend/db/init.sql
--
-- 여러 번 실행해도 안전합니다 (IF NOT EXISTS).
-- 여기서 만드는 soundmap 계정은 "로컬 개발 전용"입니다.
-- 배포할 때는 반드시 다른 비밀번호를 쓰고 환경변수로 주입하세요.

-- 1) 데이터베이스. 이미 있으면 건너뛰고, 문자셋만 utf8mb4 로 맞춘다.
--    utf8(3바이트)이면 곡 제목의 이모지에서 INSERT 가 실패한다.
CREATE DATABASE IF NOT EXISTS music
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

ALTER DATABASE music
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- 2) 애플리케이션 전용 계정. 앱이 root 로 붙지 않도록 분리한다.
CREATE USER IF NOT EXISTS 'soundmap'@'localhost' IDENTIFIED BY 'soundmap_local_dev';
GRANT ALL PRIVILEGES ON music.* TO 'soundmap'@'localhost';
FLUSH PRIVILEGES;

SELECT '완료: music 데이터베이스와 soundmap 계정이 준비되었습니다.' AS result;
