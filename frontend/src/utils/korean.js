const HANGUL_START = 0xac00;
const HANGUL_END = 0xd7a3;
const JONGSEONG_COUNT = 28;

/**
 * 목적격 조사(을/를)를 고른다.
 *
 * 받침이 있으면 "을", 없으면 "를". 가수 이름을 그대로 문장에 넣으면
 * "폴킴를 좋아합니다" 같은 문장이 나온다.
 *
 * 한글이 아닌 이름(영문 아티스트)은 "를"로 둔다. 실제 표기 관행도 대체로 그렇다.
 */
export function objectParticle(word) {
  if (!word) return "를";

  const code = word.charCodeAt(word.length - 1);
  if (code < HANGUL_START || code > HANGUL_END) {
    return "를";
  }
  return (code - HANGUL_START) % JONGSEONG_COUNT === 0 ? "를" : "을";
}

/** "폴킴, 아이유" 처럼 이어 붙인 목록 뒤에 올 조사. 마지막 단어가 기준이다. */
export function objectParticleForList(names) {
  if (!names || names.length === 0) return "를";
  return objectParticle(names[names.length - 1]);
}
