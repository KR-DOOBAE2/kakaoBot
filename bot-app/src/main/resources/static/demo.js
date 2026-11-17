const $ = selector => document.querySelector(selector);
const messages = $('#messages');
const menus = [];
let phase = 'setup', winner = null, rolling = false, settings = null;
const characters = ['chiikawa', 'usagi', 'hachiware', 'momonga', 'rakko', 'kurimanju'];
let foodCatalog = [];
function catalogFoodFor(menu) {
  const normalized = menu.replace(/\s+/g, '');
  const aliases = { 돈가스: '돈까스', 자장면: '짜장면', 스시: '초밥' };
  return foodCatalog.find(food => food.name.replace(/\s+/g, '') === (aliases[normalized] || normalized));
}
function applyCatalogArt(art, menu) {
  const food = catalogFoodFor(menu);
  if (!food) return false;
  const image = document.createElement('img');
  image.src = food.imagePath;
  image.alt = `${food.name}을 접시에 든 캐릭터`;
  art.replaceChildren(image);
  art.classList.add('catalog-art');
  return true;
}
fetch('/api/foods').then(response => response.ok ? response.json() : []).then(foods => {
  foodCatalog = foods;
  document.querySelectorAll('.demo-art[data-menu]').forEach(art => applyCatalogArt(art, art.dataset.menu));
}).catch(() => {});
const plateY = {
  chiikawa: [65, 58], usagi: [72, 62], hachiware: [68, 60],
  momonga: [64, 61], rakko: [56, 53], kurimanju: [67, 60]
};
function characterFor(menu) {
  let hash = 2166136261;
  for (const char of menu) hash = Math.imul(hash ^ char.charCodeAt(0), 16777619);
  return characters[(hash >>> 0) % characters.length];
}
function foodFor(menu) {
  if (/초밥|스시|일식/.test(menu)) return '🍣';
  if (/돈까스|양식/.test(menu)) return '🍽️';
  if (/김치|찌개|국밥|탕/.test(menu)) return '🍲';
  if (/치킨/.test(menu)) return '🍗';
  if (/피자/.test(menu)) return '🍕';
  if (/버거/.test(menu)) return '🍔';
  return '🍚';
}
function addArt(bubble, menu, happy = false) {
  const art = document.createElement('div'); art.className = `demo-art${happy ? ' happy' : ''}`;
  art.dataset.menu = menu;
  if (applyCatalogArt(art, menu)) { bubble.append(art); return; }
  const character = characterFor(menu);
  art.style.setProperty('--plate-y', `${plateY[character][happy ? 1 : 0]}%`);
  const image = document.createElement('img');
  image.src = `/characters/${character}${happy ? '-happy' : ''}.png`;
  image.alt = `${menu} 음식을 들고 있는 캐릭터${happy ? '가 기뻐하는 모습' : ''}`;
  const food = document.createElement('span'); food.className = 'demo-plate-food'; food.setAttribute('aria-hidden', 'true');
  const icon = document.createElement('span'); icon.textContent = foodFor(menu); food.append(icon);
  art.append(image, food); bubble.append(art);
}
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
function setControls() {
  const collecting = phase === 'collect';
  $('#chatInput').disabled = phase === 'setup';
  $('#sendBtn').disabled = phase === 'setup';
  $('#advanceBtn').disabled = !collecting;
  $('#sampleBtn').disabled = !collecting;
  document.querySelectorAll('[data-menu]').forEach(button => button.disabled = !collecting);
}
function settingsForm() {
  const bubble = message('방장님, 회사 주소와 점심 시작·종료 시간을 등록해주세요. 종료 시간은 식사를 마치는 시각이에요. 🏢');
  const form = document.createElement('form'); form.className = 'settings-form';
  form.innerHTML = '<label>회사 주소<input name="address" maxlength="160" placeholder="예: 서울 강남구 테헤란로 123" autocomplete="street-address" required></label><div class="time-row"><label>점심 시작<input name="start" type="time" value="12:00" required></label><label>점심 종료<input name="end" type="time" value="13:00" required></label></div><button type="submit">회사·식사 시간 저장</button><small class="form-error" aria-live="polite"></small>';
  if (settings) { form.elements.address.value = settings.address; form.elements.start.value = settings.start; form.elements.end.value = settings.end; }
  form.onsubmit = event => {
    event.preventDefault();
    const address = form.elements.address.value.trim(), start = form.elements.start.value, end = form.elements.end.value;
    if (!address) { form.querySelector('.form-error').textContent = '회사 주소를 입력해주세요.'; return; }
    if (!start || !end || end <= start) { form.querySelector('.form-error').textContent = '종료 시간을 시작 시간보다 늦게 설정해주세요.'; return; }
    settings = { address, start, end };
    form.remove(); phase = 'collect';
    $('#clockLabel').textContent = '오전 10:30 · 메뉴 모집 중';
    $('#chatInput').placeholder = '먹고 싶은 메뉴를 입력하세요';
    setControls();
    message(`✅ 회사 설정이 저장됐어요!\n📍 ${address}\n🍽️ 점심 ${start}~${end}\n\n이제 오전 10시 44분까지 메뉴를 익명으로 입력해주세요.`);
  };
  bubble.append(form); messages.scrollTop = messages.scrollHeight;
}
function reset() {
  menus.splice(0); phase = 'setup'; winner = null; rolling = false; settings = null; messages.replaceChildren();
  $('#clockLabel').textContent = '오전 10:30 · 회사 설정 중';
  $('#chatInput').value = ''; $('#chatInput').placeholder = '방장이 먼저 설정을 저장해주세요';
  const day = document.createElement('div'); day.className = 'day'; day.textContent = '오늘 · 점심 메뉴 모집'; messages.append(day);
  message('안녕하세요! 우리 팀 점심을 정할게요 🐣\n처음에는 방장님이 회사 주소와 식사 시간을 등록해 주세요.');
  settingsForm(); setControls();
}
function submit(text) {
  const input = text.trim(); if (!input) return; $('#chatInput').value = '';
  if (phase === 'setup') { message('먼저 회사 주소와 점심시간을 저장해주세요.'); return; }
  if (input === '#룰렛돌리기') { spin(); return; }
  if (phase !== 'collect') { message('메뉴 입력은 10시 44분에 마감됐어요. #룰렛돌리기로 결과를 확인해주세요!'); return; }
  if (input.length > 80) { message('메뉴 이름은 80자 이하로 입력해주세요.'); return; }
  menus.push(input); addArt(message(input, 'anonymous'), input);
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
  if (phase === 'setup') { message('먼저 회사 주소와 점심시간을 저장해주세요.'); return; }
  if (phase === 'collect') { message('두구두구… 결과는 오전 10시 45분에 공개돼요!'); return; }
  if (!winner) { message('오늘은 등록된 메뉴가 없어요.'); return; }
  rolling = true; message('#룰렛돌리기', 'mine');
  const bubble = message('🥚 두구두구…', 'result'); bubble.classList.add('roulette');
  const unique = [...new Map(menus.map(m => [m.toLocaleLowerCase('ko'), m])).values()];
  let step = 0;
  const timer = setInterval(() => { bubble.textContent = `🎲 ${unique[step++ % unique.length]}`; messages.scrollTop = messages.scrollHeight; }, 90);
  setTimeout(() => {
    clearInterval(timer); bubble.textContent = `🎉 오늘의 메뉴는 ${winner}!\n\n모두 같은 결과를 봐요. 맛있게 드세요 🐣`;
    addArt(bubble, winner, true);
    rolling = false; message('식사 후 오후 1시부터 방문한 식당에 좋아요/싫어요 평가를 남길 수 있어요.');
  }, 2800);
}
$('#sendBtn').onclick = () => submit($('#chatInput').value);
$('#chatInput').onkeydown = event => { if (event.key === 'Enter') submit($('#chatInput').value); };
$('#advanceBtn').onclick = advance; $('#spinQuick').onclick = spin;
$('#settingsQuick').onclick = () => { if (phase === 'drawn') { message('오늘의 결과가 정해져 설정을 바꿀 수 없어요.'); return; } if (phase === 'setup') return; phase = 'setup'; $('#clockLabel').textContent = '오전 10:30 · 회사 설정 중'; setControls(); settingsForm(); };
$('#resetBtn').onclick = reset; $('#backBtn').onclick = reset;
$('#plusBtn').onclick = () => $('#chatInput').focus();
$('#sampleBtn').onclick = () => { if (phase !== 'collect') return; ['김치찌개', '돈까스', '초밥'].forEach(submit); };
document.querySelectorAll('[data-menu]').forEach(button => button.onclick = () => submit(button.dataset.menu));
reset();
