// Usage: node deploy/generate-push-keys.mjs mailto:admin@example.com
// Writes an exclusive local file; never prints the private key or overwrites existing keys.
import { createECDH } from "node:crypto";
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
const subject = process.argv[2];
if (!subject || !/^mailto:[^\s@]+@[^\s@]+$/.test(subject))
  throw new Error(
    "운영자 연락 이메일을 mailto:admin@example.com 형식으로 지정하세요.",
  );
const keys = createECDH("prime256v1");
keys.generateKeys();
const path = fileURLToPath(new URL("./push-keys.env", import.meta.url));
writeFileSync(
  path,
  `PUSH_VAPID_PUBLIC_KEY=${keys.getPublicKey().toString("base64url")}\nPUSH_VAPID_PRIVATE_KEY=${keys.getPrivateKey().toString("base64url")}\nPUSH_VAPID_SUBJECT=${subject}\n`,
  { flag: "wx", mode: 0o600 },
);
console.log(
  "deploy/push-keys.env 생성 완료. 서버 환경변수에 안전하게 등록하세요.",
);
