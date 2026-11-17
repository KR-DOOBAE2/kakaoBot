const $ = selector => document.querySelector(selector);
let roundId, joinToken, shareUrl, roundData, ownerToken, diningMode = '매장', spinning = false, revealed = false, lastDrawAt;
let restaurantLoading = false, restaurantLoaded = false;
const emojis = ['🍜', '🍚', '🥟', '🍛', '🍣', '🍲', '🥗', '🍔'];
function toast(message) { const el = $('#toast'); el.textContent = message; el.classList.add('on'); setTimeout(() => el.classList.remove('on'), 2600); }
function participantId() { const key = `jm-p-${roundId}`; let id = localStorage.getItem(key); if (!id) { id = crypto.randomUUID(); localStorage.setItem(key, id); } return id; }
function escapeHtml(value) { return String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
async function api(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { 'Content-Type': 'application/json', ...(options.headers || {}) } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw Error(data.error || '요청을 처리하지 못했어요.');
  return data;
}
function drawTime(data) { return data.drawAt ? new Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul', month: 'long', day: 'numeric', weekday: 'short', hour: 'numeric', minute: '2-digit' }).format(new Date(data.drawAt)) : null; }
function updateCountdown() {
  if (!roundData) return;
  if (!roundData.drawAt) { $('#countdown').textContent = '이전 방식의 방이에요. 홈에서 새 방을 만들어주세요.'; $('#spinBtn').disabled = true; return; }
  const ms = new Date(roundData.drawAt).getTime() - Date.now();
  if (roundData.status === 'CLOSED') $('#countdown').textContent = '등록된 메뉴가 없어 룰렛이 마감됐어요.';
  else if (ms > 0) { const seconds = Math.ceil(ms / 1000), hours = Math.floor(seconds / 3600), minutes = Math.floor(seconds % 3600 / 60); $('#countdown').textContent = `결과 공개까지 ${hours ? `${hours}시간 ` : ''}${minutes}분 ${seconds % 60}초 남았어요 🐣`; }
  else $('#countdown').textContent = '두구두구… 룰렛 결과가 준비됐어요!';
  $('#spinBtn').disabled = roundData.status === 'CLOSED' || ms > 0 || spinning || roundData.candidates.length === 0;
}
function showRound(data) {
  if (lastDrawAt && lastDrawAt !== data.drawAt) {
    revealed = false; restaurantLoaded = false;
    $('#winner').classList.add('hidden');
    $('#restaurantList').replaceChildren();
  }
  lastDrawAt = data.drawAt;
  roundData = data;
  $('#create').classList.add('hidden'); $('#roomView').classList.remove('hidden');
  $('#roundTitle').textContent = data.title; $('#roomName').textContent = `💬 ${data.roomName}`;
  $('#officeDetails').textContent = data.officeAddress ? `📍 ${data.officeAddress} · 🍽️ 점심 ${String(data.lunchStartTime).slice(0,5)}~${String(data.lunchEndTime).slice(0,5)}` : '회사 주소와 식사 시간이 설정되지 않은 이전 방이에요.';
  $('#settingsForm').classList.toggle('hidden', !ownerToken);
  $('#settingsReadOnly').classList.toggle('hidden', !!ownerToken);
  $('#status').textContent = { OPEN: '모집 중', DRAWN: '추첨 완료', CLOSED: '모집 마감' }[data.status];
  $('#schedule').textContent = data.drawAt ? `${drawTime(data)} 공개 · 공개 직전까지 익명 메뉴 입력` : '이전 방식의 방 · 새 점심방을 만들어주세요';
  $('#count').textContent = `${data.candidates.length}개 메뉴 후보`;
  const accepting = data.status === 'OPEN' && data.drawAt && Date.now() < new Date(data.drawAt).getTime();
  $('#menuForm').classList.toggle('hidden', !accepting); $('#cancelBtn').classList.toggle('hidden', !accepting);
  updateCountdown();
  if (data.winner && !revealed && !spinning) animate(data);
  $('#restaurantCard').classList.toggle('hidden', !data.winner);
  if (data.winner && !restaurantLoaded && !restaurantLoading) loadRestaurants();
}
async function loadRestaurants() {
  if (restaurantLoading || !roundId || !joinToken) return;
  restaurantLoading = true;
  $('#restaurantStatus').textContent = '회사 주변 식당을 찾고 있어요…';
  $('#restaurantRetry').classList.add('hidden');
  try {
    const result = await api(`/api/rounds/${roundId}/restaurants?token=${encodeURIComponent(joinToken)}`);
    $('#restaurantStatus').textContent = (result.menuMatched
      ? `🥚 ${result.winningMenu} 메뉴와 관련된 식당 ${result.restaurants.length}곳`
      : `🥚 ${result.winningMenu} 메뉴와 맞는 식당을 찾지 못해 주변 식당 ${result.restaurants.length}곳을 보여드려요.`)
      + (result.winningMenu.startsWith('배달 ·') ? ' 배달 가능 여부는 매장에 확인해주세요.' : '');
    $('#restaurantList').innerHTML = result.restaurants.map((p, i) => {
      const safeUrl = /^https:\/\//i.test(p.placeUrl || '') ? escapeHtml(p.placeUrl) : '';
      return `<div class="restaurant"><div class="restaurant-rank">${i === 0 ? '👑' : i + 1}</div><div class="restaurant-main"><strong>${escapeHtml(p.name)}</strong>${p.recentlyFound ? '<span class="recent">최근 발견</span>' : ''}<p>${escapeHtml(p.address || '주소 정보 없음')}</p><small>${p.distanceMeters}m · 메뉴 언급 ${p.menuMentions}회 · 평가 ${p.ratingScore > 0 ? '+' : ''}${p.ratingScore}</small>${safeUrl ? `<a href="${safeUrl}" target="_blank" rel="noopener noreferrer">지도에서 보기 ↗</a>` : ''}</div></div>`;
    }).join('');
    restaurantLoaded = true;
  } catch (error) {
    $('#restaurantStatus').textContent = error.message;
    $('#restaurantRetry').classList.remove('hidden');
  } finally { restaurantLoading = false; }
}
$('#restaurantRetry').onclick = loadRestaurants;
function animate(data) {
  if (spinning || !data.winner) return;
  spinning = true; $('#winner').classList.remove('hidden'); $('#winner').classList.add('spinning');
  const choices = data.candidates.length ? data.candidates : [data.winner];
  let step = 0;
  const timer = setInterval(() => { $('#winner').innerHTML = `<small>🥚 두구두구…</small>${escapeHtml(choices[step++ % choices.length])}`; }, 85);
  updateCountdown();
  setTimeout(() => { clearInterval(timer); $('#winner').classList.remove('spinning'); $('#winner').innerHTML = `<small>오늘의 점메추리알 당첨!</small>🎉 ${escapeHtml(data.winner)}`; spinning = false; revealed = true; updateCountdown(); }, 2700);
}
async function refresh() { showRound(await api(`/api/rounds/${roundId}?token=${encodeURIComponent(joinToken)}`, { headers: { 'X-Participant-Id': participantId() } })); }
$('#createBtn').onclick = async () => {
  try {
    const officeAddress = $('#officeAddress').value.trim(), lunchStartTime = $('#lunchStart').value, lunchEndTime = $('#lunchEnd').value;
    if (!officeAddress) throw Error('회사 주소를 입력해주세요.');
    if (!lunchStartTime || !lunchEndTime || lunchEndTime <= lunchStartTime) throw Error('점심 종료 시간은 시작 시간보다 늦어야 해요.');
    const data = await api('/api/rounds', { method: 'POST', body: JSON.stringify({ roomName: $('#room').value || '우리 점심팟', title: $('#title').value || '오늘 점심 메뉴 모집', officeAddress, lunchStartTime, lunchEndTime }) });
    roundId = data.id; joinToken = data.joinToken; shareUrl = data.joinUrl; ownerToken = data.ownerToken;
    sessionStorage.setItem(`jm-join-${roundId}`, joinToken);
    localStorage.setItem(`jm-owner-${roundId}`, ownerToken);
    history.replaceState({}, '', `/r/${roundId}`); await refresh();
    toast('방이 만들어졌어요! 링크를 단톡방에 공유해보세요 🥚');
  } catch (error) { toast(error.message); }
};
$('#menuForm').onsubmit = async event => {
  event.preventDefault();
  try {
    const category = $('#menu').value.trim();
    if (!category) throw Error('먹고 싶은 음식 카테고리를 입력해주세요.');
    const data = await api(`/api/rounds/${roundId}/entries?token=${encodeURIComponent(joinToken)}`, { method: 'PUT', headers: { 'X-Participant-Id': participantId() }, body: JSON.stringify({ menu: `${diningMode} · ${category}` }) });
    showRound(data); $('#menu').value = ''; toast(`${diningMode} · ${category} 선택을 익명으로 저장했어요!`);
  } catch (error) { toast(error.message); }
};
$('#cancelBtn').onclick = async () => { try { showRound(await api(`/api/rounds/${roundId}/entries/me?token=${encodeURIComponent(joinToken)}`, { method: 'DELETE', headers: { 'X-Participant-Id': participantId() } })); toast('내 메뉴를 취소했어요.'); } catch (error) { toast(error.message); } };
$('#spinBtn').onclick = async () => { try { await refresh(); if (roundData.winner) { revealed = false; animate(roundData); } else toast('결과를 준비 중이에요. 잠시 후 다시 눌러주세요.'); } catch (error) { toast(error.message); } };
$('#shareBtn').onclick = async () => { const url = shareUrl || location.href; const text = `🥚 점메추리알에서 익명으로 오늘 점심 메뉴를 골라요!\n10시 44분까지 메뉴 입력 · 10시 45분 룰렛 결과\n${url}`; if (navigator.share) { try { await navigator.share({ title: '점메추리알 🥚', text, url }); } catch {} } else { try { await navigator.clipboard.writeText(text); toast('초대 링크를 복사했어요. 카카오톡 방에 붙여넣어주세요!'); } catch { toast(url); } } };
document.querySelectorAll('[data-mode]').forEach(button => button.onclick = () => {
  diningMode = button.dataset.mode;
  document.querySelectorAll('[data-mode]').forEach(item => {
    const active = item.dataset.mode === diningMode;
    item.classList.toggle('active', active);
    item.setAttribute('aria-pressed', String(active));
  });
});
$('#settingsBtn').onclick = () => {
  $('#settingsCard').classList.toggle('hidden');
  if (!$('#settingsCard').classList.contains('hidden') && roundData && ownerToken) {
    $('#settingsAddress').value = roundData.officeAddress || '';
    $('#settingsStart').value = String(roundData.lunchStartTime || '').slice(0,5);
    $('#settingsEnd').value = String(roundData.lunchEndTime || '').slice(0,5);
  }
};
$('#settingsClose').onclick = () => $('#settingsCard').classList.add('hidden');
$('#settingsForm').onsubmit = async event => {
  event.preventDefault();
  try {
    if (!ownerToken) throw Error('방장만 설정을 변경할 수 있어요.');
    const officeAddress = $('#settingsAddress').value.trim(), lunchStartTime = $('#settingsStart').value, lunchEndTime = $('#settingsEnd').value;
    if (!officeAddress) throw Error('회사 주소를 입력해주세요.');
    if (!lunchStartTime || !lunchEndTime || lunchEndTime <= lunchStartTime) throw Error('점심 종료 시간은 시작 시간보다 늦어야 해요.');
    restaurantLoaded = false;
    showRound(await api(`/api/rounds/${roundId}/settings`, { method: 'PUT', headers: { 'X-Owner-Token': ownerToken }, body: JSON.stringify({ officeAddress, lunchStartTime, lunchEndTime }) }));
    toast('팀 설정이 저장됐어요. 새 주소로 식당을 찾아요.');
  } catch (error) { toast(error.message); }
};
setInterval(updateCountdown, 1000);
setInterval(() => { if (roundId && joinToken) refresh().catch(() => {}); }, 15000);
(async () => {
  const match = location.pathname.match(/^\/r\/([0-9a-f-]{36})$/i); if (!match) return;
  roundId = match[1]; const params = new URLSearchParams(location.search); joinToken = params.get('token') || sessionStorage.getItem(`jm-join-${roundId}`);
  if (!joinToken) { toast('초대 링크의 token 값이 없어요. 초대 링크를 다시 열어주세요.'); return; }
  sessionStorage.setItem(`jm-join-${roundId}`, joinToken);
  ownerToken = localStorage.getItem(`jm-owner-${roundId}`);
  shareUrl = `${location.origin}/r/${roundId}?token=${encodeURIComponent(joinToken)}`;
  history.replaceState({}, '', location.pathname);
  try { await refresh(); } catch (error) { toast(error.message); }
})();

