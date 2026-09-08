#!/usr/bin/env bash
#
# public/logo.svg 하나에서 PNG 아이콘들을 만든다.
#
#   bash scripts/build-icons.sh
#
# 왜 PNG 가 따로 필요한가:
#   - iOS 홈 화면 아이콘(apple-touch-icon)은 SVG 를 무시한다.
#   - 카카오톡·슬랙 같은 링크 미리보기(og:image)도 PNG/JPG 만 읽는다.
#   - SVG 파비콘을 못 읽는 구형 브라우저용 대체본도 있어야 한다.
#
# 로고를 고칠 때는 public/logo.svg 만 고치고 이 스크립트를 다시 돌리면 된다.
# 합성본이 logo.svg 를 data URI 로 품기 때문에 모양이 두 곳에 복사되지 않는다.
#
# 필요: rsvg-convert (librsvg). msys2 라면 `pacman -S mingw-w64-ucrt-x86_64-librsvg`
set -euo pipefail

cd "$(dirname "$0")/.."

command -v rsvg-convert >/dev/null || { echo "rsvg-convert 가 필요합니다"; exit 1; }

OUT=public
# 원본 로고 이미지와 같은 짙은 회색. 아이콘 배경은 불투명해야 한다.
# 투명하게 두면 iOS 홈 화면에서 검게 칠해진다.
BG="#20242b"
LOGO_URI="data:image/svg+xml;base64,$(base64 -w0 "$OUT/logo.svg")"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# ---------- iOS 홈 화면 아이콘 ----------
# iOS 가 모서리를 둥글게 깎으므로 마크를 62% 크기로 넣어 여백을 둔다.
cat > "$TMP/apple.svg" <<SVG
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="180" height="180" viewBox="0 0 180 180">
  <rect width="180" height="180" fill="$BG"/>
  <image xlink:href="$LOGO_URI" x="34" y="34" width="112" height="112"/>
</svg>
SVG
rsvg-convert -w 180 -h 180 "$TMP/apple.svg" -o "$OUT/apple-touch-icon.png"

# ---------- 링크 미리보기 ----------
cat > "$TMP/og.svg" <<SVG
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1200" height="630" viewBox="0 0 1200 630">
  <rect width="1200" height="630" fill="$BG"/>
  <image xlink:href="$LOGO_URI" x="500" y="118" width="200" height="200"/>
  <text x="600" y="412" text-anchor="middle" fill="#ffffff"
        font-family="Malgun Gothic, Segoe UI, sans-serif" font-size="76" font-weight="bold"
        letter-spacing="-2">SoundMap</text>
  <text x="600" y="474" text-anchor="middle" fill="#98a1ad"
        font-family="Malgun Gothic, Segoe UI, sans-serif" font-size="30">같은 음악을 듣는 사람이 근처에 있습니다.</text>
</svg>
SVG
rsvg-convert -w 1200 -h 630 "$TMP/og.svg" -o "$OUT/og-image.png"

# ---------- 홈 화면 추가(PWA)용 ----------
# 안드로이드는 아이콘을 원·사각형 등으로 잘라 쓴다(maskable). 잘려 나가도 되는
# 가장자리를 남기려면 마크를 60% 안쪽에 두어야 한다.
for size in 192 512; do
  cat > "$TMP/icon-$size.svg" <<SVG
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="$size" height="$size" viewBox="0 0 100 100">
  <rect width="100" height="100" fill="$BG"/>
  <image xlink:href="$LOGO_URI" x="20" y="20" width="60" height="60"/>
</svg>
SVG
  rsvg-convert -w "$size" -h "$size" "$TMP/icon-$size.svg" -o "$OUT/icon-$size.png"
done

# ---------- SVG 파비콘을 못 읽는 브라우저용 ----------
rsvg-convert -w 32 -h 32 "$OUT/logo.svg" -o "$OUT/favicon-32.png"

ls -l "$OUT"/apple-touch-icon.png "$OUT"/og-image.png "$OUT"/favicon-32.png "$OUT"/icon-192.png "$OUT"/icon-512.png
