// 잔향 시연 영상 촬영기.
//
// 무대 페이지(__demo-stage.html)를 1920x1080 으로 띄우고, 그 안의 iframe 에 있는
// 실제 앱을 조작하면서 프레임을 모은다. 실시간 녹화가 아니라 "상태 하나당 한 장"을
// 찍고 ffmpeg 에 표시 시간을 따로 알려주는 방식이라, 캡처가 느려도 영상은 매끄럽다.

import fs from 'node:fs';
import path from 'node:path';

const OUT = process.argv[2];
const TOKEN = process.argv[3];      // dummy01 (민수)
const TOKEN_NEW = process.argv[4];  // 취향 미등록 계정 (온보딩용)
const STAGE = 'http://localhost:5173/__demo-stage.html';
const FRAMES = path.join(OUT, 'frames');
fs.rmSync(FRAMES, { recursive: true, force: true });
fs.mkdirSync(FRAMES, { recursive: true });

/* ---------------- CDP ---------------- */
const info = await (await fetch('http://127.0.0.1:9333/json/new?about:blank', { method: 'PUT' })).json();
const ws = new WebSocket(info.webSocketDebuggerUrl);
await new Promise(r => ws.addEventListener('open', r, { once: true }));
let id = 0; const pending = new Map(); const waiters = [];
ws.addEventListener('message', e => {
  const m = JSON.parse(e.data);
  if (m.id && pending.has(m.id)) {
    const { res, rej } = pending.get(m.id); pending.delete(m.id);
    m.error ? rej(new Error(JSON.stringify(m.error))) : res(m.result);
  } else if (m.method) {
    for (let i = waiters.length - 1; i >= 0; i--)
      if (waiters[i].method === m.method) waiters.splice(i, 1)[0].res(m.params);
  }
});
const send = (method, params = {}) => new Promise((res, rej) => {
  const n = ++id; pending.set(n, { res, rej });
  ws.send(JSON.stringify({ id: n, method, params }));
});
const once = (method, ms = 20000) => new Promise(res => {
  const o = { method, res }; waiters.push(o);
  setTimeout(() => { const i = waiters.indexOf(o); if (i >= 0) waiters.splice(i, 1); res(null); }, ms);
});
const sleep = ms => new Promise(r => setTimeout(r, ms));
const ev = async expr => {
  const r = await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.text + ' :: ' + (r.exceptionDetails.exception?.description || ''));
  return r.result?.value;
};
const safe = async (expr, label) => { try { return await ev(expr); } catch (e) { console.log('   ! ' + (label || '') + ': ' + e.message.slice(0, 120)); return null; } };

/* ---------------- 프레임 ---------------- */
let n = 0;
const manifest = [];
async function snap(seconds) {
  const { data } = await send('Page.captureScreenshot', { format: 'jpeg', quality: 96, captureBeyondViewport: false });
  const file = `f${String(++n).padStart(5, '0')}.jpg`;
  fs.writeFileSync(path.join(FRAMES, file), Buffer.from(data, 'base64'));
  manifest.push({ file, dur: seconds });
}
/** 정지 화면을 seconds 만큼 보여준다 (한 장만 찍는다). */
const hold = s => snap(s);
/** 0→1 로 진행하는 움직임을 frames 장으로 나눠 찍는다. */
async function anim(frames, seconds, step) {
  const per = seconds / frames;
  for (let i = 0; i < frames; i++) {
    await step(frames === 1 ? 1 : i / (frames - 1));
    await snap(per);
  }
}

/* ---------------- 무대 조작 ---------------- */
const S = '__stage';
const cap = (num, label, html) =>
  ev(`${S}.cap(${JSON.stringify(num)}, ${JSON.stringify(label)}, ${JSON.stringify(html)})`);

/** 자막을 부드럽게 띄운다. */
async function capIn(num, label, html, holdSec) {
  await ev(`(()=>{const c=document.querySelector('#cap'),h=document.querySelector('#chapter');
    c.style.transition='none';h.style.transition='none';c.style.opacity=0;h.style.opacity=0;})()`);
  await cap(num, label, html);
  await anim(7, 0.26, async p => {
    await ev(`(()=>{const c=document.querySelector('#cap'),h=document.querySelector('#chapter');
      c.style.opacity=${p};h.style.opacity=${p};c.style.transform='translateY(${(1 - p) * 9}px)';})()`);
  });
  if (holdSec) await hold(holdSec);
}
async function capOut() {
  await anim(5, 0.18, async p => {
    await ev(`(()=>{const c=document.querySelector('#cap');c.style.opacity=${1 - p};})()`);
  });
}

/** 요소를 탭하는 모습(파문)을 보여주고 실제로 누른다. */
async function tap(selectorExpr, { settle = 500, label = '' } = {}) {
  const found = await safe(`(()=>{const el=${selectorExpr}; return !!el;})()`, 'tap찾기 ' + label);
  if (!found) { console.log('   ! 탭 대상 없음: ' + label); return false; }
  await anim(6, 0.26, async p => {
    await ev(`${S}.tapAt(${selectorExpr}, ${p})`);
    if (p > 0.3 && p < 0.5) await ev(`(()=>{const el=${selectorExpr}; if(el) el.click();})()`);
  });
  await ev(`${S}.tapOff()`);
  await sleep(settle);
  return true;
}

/** 앱 안에서 스크롤을 흘린다. */
async function scrollApp(selectorExpr, toPx, frames = 14, seconds = 0.7) {
  await anim(frames, seconds, async p => {
    await ev(`(()=>{const el=${selectorExpr}; if(el) el.scrollTop = ${toPx} * ${p};})()`);
  });
}

/** 한 글자씩 입력하는 모습. */
async function typeInto(selectorExpr, text, perChar = 0.085) {
  for (let i = 1; i <= text.length; i++) {
    await ev(`(()=>{const el=${selectorExpr}; if(el) ${S}.setInput(el, ${JSON.stringify(text.slice(0, i))});})()`);
    await snap(perChar);
  }
}

const goApp = async (p, wait = 1700) => { await ev(`${S}.go(${JSON.stringify(p)})`); await sleep(wait); };

/* ---------------- 시작 ---------------- */
await send('Page.enable'); await send('Runtime.enable');
await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false });
const loaded = once('Page.loadEventFired');
await send('Page.navigate', { url: STAGE });
await loaded;
await sleep(3500); // 웹폰트 + 앱 첫 로딩

console.log('무대 준비됨. 촬영 시작.');
export {};

/* =======================================================================
   장면
   ======================================================================= */
const D = sel => `${S}.doc().querySelector(${JSON.stringify(sel)})`;
const T = (sel, txt) => `${S}.byText(${JSON.stringify(sel)}, ${JSON.stringify(txt)})`;

async function titleCard(big, sub, u, sec) {
  await ev(`${S}.title(${JSON.stringify(big)}, ${JSON.stringify(sub)}, ${JSON.stringify(u || '')})`);
  await sleep(420);
  await hold(sec);
}

// ---------- 00 오프닝 ----------
console.log('> 오프닝');
await ev(`${S}.cap(null)`);
await titleCard('잔향',
  '같은 건물에서 매일 마주치는 수백 명.<br>그중 누군가는 지금 나와 같은 노래를 듣고 있습니다.', '', 5.0);
await titleCard('잔향',
  '그런데 우리는 서로를 모릅니다.', '', 3.2);
await titleCard('잔향',
  '지금 듣는 음악으로 주변 사람과 연결되는<br>위치 기반 음악 소셜 플랫폼', 'soundmap.duckdns.org', 4.2);

// ---------- 01 가입과 취향 ----------
console.log('> 01 취향 온보딩');
await ev(`${S}.demo()`);
await ev(`${S}.token(${JSON.stringify(TOKEN_NEW)})`);
await goApp('/onboarding', 2600);
await capIn('01', '시작', '가입하면 가장 먼저 <b>음악 취향</b>을 받습니다.', 3.4);
await capIn('01', '시작', '장르는 1개 이상, 가수는 3명 이상.', 2.6);

const genre = g => `[...${S}.doc().querySelectorAll('.onboarding__section .chip-grid button.chip')].find(b=>b.textContent.trim()===${JSON.stringify(g)})`;
for (const g of ['K-Pop', '인디', '재즈']) {
  await tap(genre(g), { settle: 260, label: '장르 ' + g });
  await hold(0.25);
}
await hold(0.9);
await scrollApp(`${S}.doc().querySelector('.onboarding')`, 540, 14, 0.75);
await hold(0.5);
const artistSel = i => `${S}.doc().querySelectorAll('.artist-grid button')[${i}]`;
for (let i = 0; i < 4; i++) {
  await tap(artistSel(i), { settle: 230, label: '가수 ' + i });
}
await hold(1.1);
await capIn('01', '시작',
  '가수를 3명 이상 받는 데는 이유가 있습니다.<span class="gap"></span>' +
  '<span class="dim">한두 명만 고르면 취향 일치도가 0% 아니면 100%로 튀어서 숫자가 의미를 잃습니다.</span>', 6.4);
await capOut();

// ---------- 02 지도 ----------
console.log('> 02 지도');
await ev(`${S}.token(${JSON.stringify(TOKEN)})`);
await goApp('/', 3600);
await capIn('02', '지도', '지도 위의 동그라미는 사람 사진이 아닙니다.', 3.0);
await capIn('02', '지도', '<b>그 사람이 지금 듣고 있는 앨범 커버</b>입니다.', 3.8);
await capIn('02', '지도',
  '화면을 훑는 것만으로<br>이 동네가 무슨 음악을 듣고 있는지 보입니다.', 4.4);

// 확대 / 축소
await capIn('02', '지도', '가까이 보고 멀리 보고.', 1.2);
const zoomIn = `${S}.doc().querySelector('.zoom-control button[aria-label="확대"]')`;
const zoomOut = `${S}.doc().querySelector('.zoom-control button[aria-label="축소"]')`;
await tap(zoomIn, { settle: 800, label: '확대' });
await hold(1.0);
await tap(zoomIn, { settle: 800, label: '확대2' });
await hold(1.2);
await tap(zoomOut, { settle: 800, label: '축소' });
await hold(1.4);

// 반경
await capIn('02', '지도', '반경은 500m부터 전국까지 다섯 단계.', 2.4);
const chip = r => `[...${S}.doc().querySelectorAll('.radius-picker button')].find(b=>b.textContent.trim()===${JSON.stringify(r)})`;
for (const r of ['500m', '1km', '5km']) {
  await tap(chip(r), { settle: 950, label: '반경 ' + r });
  await hold(1.0);
}
await tap(chip('2km'), { settle: 950, label: '반경 복귀' });
await hold(0.8);
await capOut();

// ---------- 03 지금 듣는 곡 ----------
console.log('> 03 곡 올리기');
await capIn('03', '지금 듣는 곡', '내가 듣는 곡도 지도에 올립니다.', 2.6);
await tap(`[...${S}.doc().querySelectorAll('.map-dock__actions button')].find(b=>/듣는 곡 올리기|곡 바꾸기/.test(b.textContent))`,
  { settle: 1100, label: '곡 올리기' });
await hold(0.7);
const searchBox = `[...${S}.doc().querySelectorAll('input')].find(e=>/곡 제목|가수/.test(e.placeholder||''))`;
await typeInto(searchBox, '아이유', 0.1);
await sleep(2600);
await hold(1.2);
await capIn('03', '지금 듣는 곡', '<b>한글로 검색하면 한국어 제목이 그대로 나옵니다.</b>', 4.0);
await capIn('03', '지금 듣는 곡',
  '간단해 보이지만 그렇지 않았습니다.<span class="gap"></span>' +
  '<span class="dim">iTunes 한국 스토어는 곡 검색 결과를 아예 주지 않습니다. ' +
  '미국 스토어로 찾은 뒤 받은 ID 를 한국 스토어에 다시 조회해 제목만 바꿔 끼웁니다.</span>', 7.2);
await tap(`${S}.doc().querySelector('.track-list button.track-row')`,
  { settle: 2200, label: '곡 선택' });
await hold(1.0);
await capIn('03', '지금 듣는 곡', '고른 순간 내 마커의 앨범 아트가 바뀝니다.', 3.6);
await capOut();

// ---------- 04 취향으로 찾기 ----------
console.log('> 04 취향으로 찾기');
await tap(`[...${S}.doc().querySelectorAll('.view-toggle button')].find(b=>b.textContent.trim()==='목록')`,
  { settle: 1000, label: '목록 보기' });
await capIn('04', '취향으로 찾기', '거리만으로는 부족합니다.', 2.6);
await capIn('04', '취향으로 찾기', '취향이 얼마나 겹치는지도 함께 봅니다.', 3.0);
await scrollApp(`${S}.doc().querySelector('.pin-list')`, 420, 16, 0.9);
await hold(1.6);
await capIn('04', '취향으로 찾기',
  '일치도는 자카드 유사도로 계산합니다.<span class="gap"></span>' +
  '<span class="dim">교집합을 합집합으로 나누기 때문에, 가수를 30명 고른 사람이 ' +
  '무조건 유리해지는 일이 없습니다.</span>', 6.8);
await scrollApp(`${S}.doc().querySelector('.pin-list')`, 0, 10, 0.5);
await capIn('04', '취향으로 찾기', '취향이 비슷한 순으로 다시 줄을 세웁니다.', 2.4);
await safe(`(()=>{const el=${S}.doc().querySelector('.sort-picker select'); if(!el) return 0; ${S}.setSelect(el,'taste'); return 1;})()`, '정렬 변경');
await sleep(900); await hold(2.6);

await tap(`${S}.doc().querySelector('.filter-bar__open')`,
  { settle: 1400, label: '가수·장르로 찾기' });
await capIn('04', '취향으로 찾기', '가수나 장르로 좁혀 찾을 수도 있습니다.', 3.0);
await tap(`[...${S}.doc().querySelectorAll('.sheet .chip')].find(b=>b.textContent.trim()==='인디')`,
  { settle: 1500, label: '장르 필터' });
await hold(2.4);
await capOut();

// ---------- 05 연결 ----------
console.log('> 05 연결');
await goApp('/', 3400);
await capIn('05', '연결', '마커를 누르면 거리, 듣는 곡,<br>그리고 취향 일치도가 보입니다.', 1.4);
await tap(`(()=>{const o=[...${S}.doc().querySelectorAll('.album-marker')]
  .filter(e=>e.style.getPropertyValue('--marker-size')==='48px');
  const v=o.filter(e=>{const b=e.getBoundingClientRect();return b.top>300&&b.bottom<800&&b.left>10&&b.right<380});
  return v[0]||o[0];})()`, { settle: 1600, label: '핀 열기' });
await hold(3.4);
await capIn('05', '연결',
  '<b>위치는 약 100m 격자로만 표시됩니다.</b><span class="gap"></span>' +
  '<span class="dim">원본 GPS 좌표는 서버에 저장조차 하지 않습니다. ' +
  '같은 자리에서 몇 번을 갱신해도 늘 같은 격자점이라, 여러 번 모아 평균을 내도 역산되지 않습니다.</span>', 7.6);
await goApp('/users/2', 2600);
await capIn('05', '연결',
  '프로필에서 가장 먼저 보이는 건 나이도 사진도 아니라<br>' +
  '<b>"두 분 모두 폴킴을 좋아합니다"</b> 한 줄입니다.', 5.6);
await scrollApp(`${S}.doc().querySelector('.profile-body') || ${S}.doc().scrollingElement`, 420, 16, 0.9);
await hold(1.4);
await capIn('05', '연결',
  '<span class="dim">낯선 사람에게 말 거는 일의 어려움은 대부분 첫 문장에 있습니다. ' +
  '그 문장을 서비스가 대신 만들어 줍니다.</span>', 5.4);
await capOut();

// ---------- 06 대화 ----------
console.log('> 06 대화');
await goApp('/chats', 2400);
await capIn('06', '대화', '대화는 <b>수락해야만</b> 열립니다.', 3.4);
await capIn('06', '대화',
  '<span class="dim">수락 전에는 메시지를 한 글자도 보낼 수 없습니다. ' +
  '낯선 사람이 일방적으로 말을 걸 수 있으면, 위치 기반 서비스에서 그건 ' +
  '곧 괴롭힘 통로가 되기 때문입니다.</span>', 7.0);
await goApp('/chats/1', 2600);
await ev(`(()=>{const l=${S}.doc().querySelector('.message-list, .chat-messages, [class*=message]');
  if(l&&l.scrollHeight>l.clientHeight) l.scrollTop=l.scrollHeight;})()`);
await hold(0.6);
await capIn('06', '대화', 'WebSocket 으로 실시간 전송됩니다.', 3.0);
await capIn('06', '대화',
  '상단에는 공통 취향과 상대가 지금 듣는 곡이 고정되어,<br>' +
  '무슨 이야기를 할지가 계속 눈앞에 있습니다.', 5.0);

// 메시지 한 통 직접 보내기
const msgBox = `[...${S}.doc().querySelectorAll('input, textarea')].find(e=>/메시지/.test(e.placeholder||''))`;
await typeInto(msgBox, '주말에 그 공연 같이 가요', 0.075);
await hold(0.8);
await tap(`[...${S}.doc().querySelectorAll('button')].find(b=>b.textContent.trim()==='보내기')`,
  { settle: 1500, label: '메시지 전송' });
await ev(`(()=>{const l=${S}.doc().querySelector('.message-list, .chat-messages, [class*=message]');
  if(l&&l.scrollHeight>l.clientHeight) l.scrollTop=l.scrollHeight;})()`);
await hold(2.6);
await capIn('06', '대화',
  '대화 중에 곡을 카드로 보낼 수도 있습니다.<span class="gap"></span>' +
  '<span class="dim">음악으로 만난 사이의 대화가 다시 음악으로 돌아옵니다.</span>', 5.2);
await capOut();

// ---------- 07 음악 발견 ----------
console.log('> 07 음악 발견');
await goApp('/music/library', 2400);
await capIn('07', '음악 발견', '지도와 대화에서 만난 음악을 모아 둡니다.', 3.6);
await scrollApp(`${S}.doc().scrollingElement`, 380, 14, 0.8);
await hold(1.2);

await goApp('/music/history', 2400);
await capIn('07', '음악 발견', '내가 어떤 음악을 공유해 왔는지<br>월별로 돌아봅니다.', 4.4);
await scrollApp(`${S}.doc().scrollingElement`, 340, 14, 0.8);
await hold(1.2);

await goApp('/music/chart', 2600);
await capIn('07', '음악 발견', '<b>오늘의 동네 차트.</b>', 2.6);
await capIn('07', '음악 발견',
  '<span class="dim">알고리즘이 고른 추천이 아니라, 실제로 내 주변에 있는 사람들이 ' +
  '오늘 공유한 곡입니다. 같은 사람의 같은 곡은 하루에 한 번만 셉니다.</span>', 6.6);
await scrollApp(`${S}.doc().scrollingElement`, 460, 16, 0.9);
await hold(1.4);

await goApp('/notifications', 2400);
await capIn('07', '음악 발견',
  '대화 요청과 새 메시지는 알림함에 쌓이고,<br>원하면 웹 푸시로 바로 받습니다.', 4.6);
await capIn('07', '음악 발견',
  '<span class="dim">알림에는 메시지 내용과 위치를 담지 않습니다. ' +
  '잠금 화면은 남이 볼 수 있는 공간이니까요.</span>', 5.4);
await capOut();

// ---------- 08 내가 통제한다 ----------
console.log('> 08 프라이버시');
await goApp('/settings', 2600);
await capIn('08', '내가 통제한다',
  '위치 기반 서비스가 무서운 이유는<br>내가 통제할 수 없기 때문입니다.', 4.6);
await scrollApp(`${S}.doc().querySelector('.chat-list-body') || ${S}.doc().scrollingElement`, 260, 12, 0.7);
await hold(0.8);
await tap(`${S}.doc().querySelectorAll('.toggle, .switch, input[type=checkbox], [role=switch]')[0]`,
  { settle: 1200, label: '위치 공유 끄기' });
await hold(2.2);
await capIn('08', '내가 통제한다',
  '끄면 지도에서 사라지고,<br><b>서버에 저장된 좌표까지 삭제됩니다.</b>', 5.0);
await tap(`${S}.doc().querySelectorAll('.toggle, .switch, input[type=checkbox], [role=switch]')[0]`,
  { settle: 1200, label: '위치 공유 켜기' });
await hold(1.2);
await capIn('08', '내가 통제한다',
  '<span class="dim">듣는 곡 공개와 프로필 공개도 따로 끌 수 있습니다. ' +
  '차단은 한쪽만 눌러도 양방향으로 적용되고, 상대에게는 드러나지 않습니다.</span>', 6.2);
await capOut();

// ---------- 엔딩 ----------
console.log('> 엔딩');
await ev(`${S}.cap(null)`);
await titleCard('잔향', '지금 이 노래를 듣는 사람이<br>바로 옆에 있다는 사실.', '', 4.2);
await titleCard('잔향', '지금 바로 열어 볼 수 있습니다.', 'soundmap.duckdns.org', 6.0);

/* ---------------- 마무리 ---------------- */
const total = manifest.reduce((a, b) => a + b.dur, 0);
const lines = [];
for (const m of manifest) {
  lines.push(`file '${m.file}'`);
  lines.push(`duration ${m.dur.toFixed(4)}`);
}
lines.push(`file '${manifest[manifest.length - 1].file}'`); // concat 데머서는 마지막 장을 한 번 더 요구한다
fs.writeFileSync(path.join(FRAMES, 'concat.txt'), lines.join('\n') + '\n');
fs.writeFileSync(path.join(OUT, 'manifest.json'), JSON.stringify({ frames: manifest.length, seconds: total }, null, 2));
console.log(`\n프레임 ${manifest.length}장, 총 ${Math.floor(total / 60)}분 ${(total % 60).toFixed(1)}초`);
ws.close();
process.exit(0);
