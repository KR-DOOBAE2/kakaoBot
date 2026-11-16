const $ = selector => document.querySelector(selector);
const messages = $('#messages');
const menus = [];
let phase = 'collect', winner = null, rolling = false;
function message(text, kind = 'bot', action = false) {
  const row = document.createElement('div'); row.className = `message ${kind}`;
  if (kind !== 'mine') { const avatar = document.createElement('span'); avatar.className = 'avatar'; avatar.textContent = kind === 'anonymous' ? '🎭' : '🥚'; row.append(avatar); }
  const body = document.createElement('div'); body.className = 'message-body';
  if (kind !== 'mine') { const sender = document.createElement('div'); sender.className = 'sender'; sender.textContent = kind === 'anonymous' ? '익명' : '점메추리알'; body.append(sender); }
  const bubble = document.createElement('div'); bubble.className = 'bubble'; bubble.textContent = text;
  if (action) { const button = document.createElement('button'); button.textContent = '#룰렛돌리기 🎲'; button.onclick = spin; bubble.append(button); }
  body.append(bubble); row.append(body); messages.append(row); messages.scrollTop = messages.scrollHeight;
  return bubble;
}
function reset() {
  menus.splice(0); phase = 'collect'; winner = null; rolling = false; messages.replaceChildren();
  $('#clockLabel').textContent = '오전 10:30 · 메뉴 모집 중'; $('#advanceBtn').disabled = false;
  $('#chatInput').placeholder = '먹고 싶은 메뉴를 입력하세요';
  const day = document.createElement('div'); day.className = 'day'; day.textContent = '오늘 · 점심 메뉴 모집'; messages.append(day);
  message('안녕하세요! 우리 팀 점심을 정할게요 🐣\n회사 위치와 점심시간 설정 완료: 12:00~13:00\n\n먹고 싶은 메뉴를 오전 10시 44분까지 입력해주세요. 이름은 공개되지 않아요!');
  message('예: 김치찌개, 돈까스, 초밥\n같은 메뉴는 한 번만 룰렛에 넣어요.');
}
function submit(text) {
  const input = text.trim(); if (!input) return; $('#chatInput').value = '';
  if (input === '#룰렛돌리기') { spin(); return; }
  if (phase !== 'collect') { message('메뉴 입력은 10시 44분에 마감됐어요. #룰렛돌리기로 결과를 확인해주세요!'); return; }
  if (input.length > 80) { message('메뉴 이름은 80자 이하로 입력해주세요.'); return; }
  menus.push(input); message(input, 'anonymous');
  const unique = [...new Map(menus.map(m => [m.toLocaleLowerCase('ko'), m])).values()];
  message(`익명 메뉴가 등록됐어요! 현재 ${menus.length}명 참여 · ${unique.length}개 메뉴 후보 🍽️`);
}
function advance() {
  if (phase !== 'collect') return;
  phase = 'drawn'; $('#clockLabel').textContent = '오전 10:45 · 결과 공개'; $('#advanceBtn').disabled = true;
  $('#chatInput').placeholder = '#룰렛돌리기';
  const unique = [...new Map(menus.map(m => [m.toLocaleLowerCase('ko'), m])).values()];
  if (!unique.length) { message('입력된 메뉴가 없어 오늘 룰렛은 쉬어가요 🥚'); return; }
  winner = unique[Math.floor(Math.random() * unique.length)];
  message(`입력이 마감됐어요! ${unique.length}개 메뉴로 룰렛 결과가 정해졌어요.\n결과가 궁금하면 아래 버튼을 눌러주세요 👀`, 'bot', true);
}
function spin() {
  if (rolling) return;
  if (phase === 'collect') { message('두구두구… 결과는 오전 10시 45분에 공개돼요!'); return; }
  if (!winner) { message('오늘은 등록된 메뉴가 없어요.'); return; }
  rolling = true; message('#룰렛돌리기', 'mine');
  const bubble = message('🥚 두구두구…', 'result'); bubble.classList.add('roulette');
  const unique = [...new Map(menus.map(m => [m.toLocaleLowerCase('ko'), m])).values()];
  let step = 0;
  const timer = setInterval(() => { bubble.textContent = `🎲 ${unique[step++ % unique.length]}`; messages.scrollTop = messages.scrollHeight; }, 90);
  setTimeout(() => {
    clearInterval(timer); bubble.textContent = `🎉 오늘의 메뉴는 ${winner}!\n\n모두 같은 결과를 봐요. 맛있게 드세요 🐣`;
    rolling = false; message('식사 후 오후 1시부터 방문한 식당에 좋아요/싫어요 평가를 남길 수 있어요.');
  }, 2800);
}
$('#sendBtn').onclick = () => submit($('#chatInput').value);
$('#chatInput').onkeydown = event => { if (event.key === 'Enter') submit($('#chatInput').value); };
$('#advanceBtn').onclick = advance; $('#spinQuick').onclick = spin;
$('#resetBtn').onclick = reset; $('#backBtn').onclick = reset;
$('#plusBtn').onclick = () => $('#chatInput').focus();
document.querySelectorAll('[data-menu]').forEach(button => button.onclick = () => submit(button.dataset.menu));
reset();
