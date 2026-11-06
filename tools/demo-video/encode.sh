#!/usr/bin/env bash
# 캡처한 프레임을 1080p MP4 로 묶는다.
#   - concat 데머서로 장면마다 다른 표시 시간을 준다
#   - 최종 출력은 30fps 고정 (유튜브가 가장 잘 다룬다)
#   - 무음 오디오 트랙을 넣는다. 트랙이 아예 없는 파일을 거부하는 편집·업로드 도구가 있다
set -e
FRAMES="$1"; OUT="$2"
cd "$FRAMES"
ffmpeg -y -hide_banner -loglevel warning -stats \
  -f concat -safe 0 -i concat.txt \
  -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=48000 \
  -fps_mode cfr -r 30 \
  -c:v libx264 -preset slow -crf 19 -profile:v high -level 4.1 -pix_fmt yuv420p \
  -x264-params "keyint=60:min-keyint=30" \
  -c:a aac -b:a 128k -shortest \
  -movflags +faststart \
  "$OUT"
