const $ = selector => document.querySelector(selector);
let roundId, joinToken, shareUrl, roundData, spinning = false, revealed = false;
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
  roundData = data;
  $('#create').classList.add('hidden'); $('#roomView').classList.remove('hidden');
  $('#roundTitle').textContent = data.title; $('#roomName').textContent = `💬 ${data.roomName}`;
  $('#officeDetails').textContent = data.officeAddress ? `📍 ${data.officeAddress} · 🍽️ 점심 ${data.lunchStartTime}~${data.lunchEndTime}` : '회사 주소와 식사 시간이 설정되지 않은 이전 방이에요.';
  $('#status').textContent = { OPEN: '모집 중', DRAWN: '추첨 완료', CLOSED: '모집 마감' }[data.status];
  $('#schedule').textContent = data.drawAt ? `${drawTime(data)} 공개 · 공개 직전까지 익명 메뉴 입력` : '이전 방식의 방 · 새 점심방을 만들어주세요';
  $('#count').textContent = `${data.candidates.length}개 메뉴 후보`;
  $('#entries').innerHTML = data.entries.length ? data.entries.map((entry, i) => `<div class="entry"><div class="entry-icon">${emojis[i % emojis.length]}</div><div class="entry-info"><div class="entry-name">익명</div><div class="entry-menu">${escapeHtml(entry.menu)}</div></div></div>`).join('') : '<div class="empty">아직 메뉴가 없어요.<br>첫 메뉴를 등록해볼까요? 🐣</div>';
  const accepting = data.status === 'OPEN' && data.drawAt && Date.now() < new Date(data.drawAt).getTime();
  $('#menuForm').classList.toggle('hidden', !accepting); $('#cancelBtn').classList.toggle('hidden', !accepting);
  updateCountdown();
  if (data.winner && !revealed && !spinning) animate(data);
}
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
    roundId = data.id; joinToken = data.joinToken; shareUrl = data.joinUrl;
    sessionStorage.setItem(`jm-join-${roundId}`, joinToken);
    history.replaceState({}, '', `/r/${roundId}`); await refresh();
    toast('방이 만들어졌어요! 링크를 단톡방에 공유해보세요 🥚');
  } catch (error) { toast(error.message); }
};
$('#menuForm').onsubmit = async event => {
  event.preventDefault();
  try {
    const data = await api(`/api/rounds/${roundId}/entries?token=${encodeURIComponent(joinToken)}`, { method: 'PUT', headers: { 'X-Participant-Id': participantId() }, body: JSON.stringify({ menu: $('#menu').value }) });
    showRound(data); $('#menu').value = ''; toast('메뉴를 익명으로 담았어요! 🍽️');
  } catch (error) { toast(error.message); }
};
$('#cancelBtn').onclick = async () => { try { showRound(await api(`/api/rounds/${roundId}/entries/me?token=${encodeURIComponent(joinToken)}`, { method: 'DELETE', headers: { 'X-Participant-Id': participantId() } })); toast('내 메뉴를 취소했어요.'); } catch (error) { toast(error.message); } };
$('#spinBtn').onclick = async () => { try { await refresh(); if (roundData.winner) { revealed = false; animate(roundData); } else toast('결과를 준비 중이에요. 잠시 후 다시 눌러주세요.'); } catch (error) { toast(error.message); } };
$('#shareBtn').onclick = async () => { const url = shareUrl || location.href; const text = `🥚 점메추리알에서 익명으로 오늘 점심 메뉴를 골라요!\n10시 44분까지 메뉴 입력 · 10시 45분 룰렛 결과\n${url}`; if (navigator.share) { try { await navigator.share({ title: '점메추리알 🥚', text, url }); } catch {} } else { try { await navigator.clipboard.writeText(text); toast('초대 링크를 복사했어요. 카카오톡 방에 붙여넣어주세요!'); } catch { toast(url); } } };
setInterval(updateCountdown, 1000);
setInterval(() => { if (roundId && joinToken) refresh().catch(() => {}); }, 15000);
(async () => {
  const match = location.pathname.match(/^\/r\/([0-9a-f-]{36})$/i); if (!match) return;
  roundId = match[1]; const params = new URLSearchParams(location.search); joinToken = params.get('token') || sessionStorage.getItem(`jm-join-${roundId}`);
  if (!joinToken) { toast('초대 링크의 token 값이 없어요. 초대 링크를 다시 열어주세요.'); return; }
  sessionStorage.setItem(`jm-join-${roundId}`, joinToken);
  shareUrl = `${location.origin}/r/${roundId}?token=${encodeURIComponent(joinToken)}`;
  history.replaceState({}, '', location.pathname);
  try { await refresh(); } catch (error) { toast(error.message); }
})();
