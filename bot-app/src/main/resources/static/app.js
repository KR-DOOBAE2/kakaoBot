const $ = (s) => document.querySelector(s);
let roundId, joinToken, ownerToken, admin = false, shareUrl;
const emojis = ['🍜','🍚','🥟','🍛','🍣','🍲','🥗','🍔'];
function toast(message) { const el = $('#toast'); el.textContent = message; el.classList.add('on'); setTimeout(() => el.classList.remove('on'), 2600); }
function participantId() { const key = `jm-p-${roundId}`; let id = localStorage.getItem(key); if (!id) { id = crypto.randomUUID(); localStorage.setItem(key, id); } return id; }
async function api(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { 'Content-Type': 'application/json', ...(options.headers || {}) } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw Error(data.error || '요청을 처리하지 못했어요.');
  return data;
}
function showRound(data) {
  $('#create').classList.add('hidden'); $('#roomView').classList.remove('hidden');
  $('#roundTitle').textContent = data.title; $('#roomName').textContent = '💬 ' + data.roomName;
  $('#status').textContent = { OPEN: '모집 중', DRAWN: '추첨 완료', CLOSED: '모집 마감' }[data.status];
  $('#count').textContent = `${data.candidates.length}개 메뉴 후보`;
  $('#entries').innerHTML = data.entries.length ? data.entries.map((entry, i) => `<div class="entry"><div class="entry-icon">${emojis[i % emojis.length]}</div><div class="entry-info"><div class="entry-name">${escapeHtml(entry.participantName)}</div><div class="entry-menu">${escapeHtml(entry.menu)}</div></div></div>`).join('') : '<div class="empty">아직 메뉴가 없어요.<br>첫 메뉴를 등록해볼까요? 🐣</div>';
  $('#winner').classList.toggle('hidden', !data.winner);
  if (data.winner) $('#winner').innerHTML = `<small>오늘의 점메추리알 당첨!</small>🎉 ${escapeHtml(data.winner)}`;
  $('#admin').classList.toggle('hidden', !admin); $('#menuForm').classList.toggle('hidden', data.status !== 'OPEN'); $('#cancelBtn').classList.toggle('hidden', data.status !== 'OPEN');
}
function escapeHtml(value) { return String(value).replace(/[&<>"']/g, c => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' }[c])); }
async function refresh() { showRound(await api(`/api/rounds/${roundId}?token=${encodeURIComponent(joinToken)}`, { headers: { 'X-Participant-Id': participantId() } })); }
$('#createBtn').onclick = async () => {
  try {
    const data = await api('/api/rounds', { method: 'POST', body: JSON.stringify({ roomName: $('#room').value || '우리 점심팟', title: $('#title').value || '오늘 점심 메뉴 모집' }) });
    roundId = data.id; joinToken = data.joinToken; ownerToken = data.ownerToken; shareUrl = data.joinUrl; admin = true;
    localStorage.setItem(`jm-owner-${roundId}`, ownerToken); history.replaceState({}, '', `/r/${roundId}`); await refresh();
    toast('방이 만들어졌어요! 링크를 단톡방에 공유해보세요 🥚');
  } catch (error) { toast(error.message); }
};
$('#menuForm').onsubmit = async event => {
  event.preventDefault();
  try {
    const data = await api(`/api/rounds/${roundId}/entries?token=${encodeURIComponent(joinToken)}`, { method: 'PUT', headers: { 'X-Participant-Id': participantId() }, body: JSON.stringify({ participantName: $('#name').value, menu: $('#menu').value }) });
    localStorage.setItem(`jm-p-${roundId}`, data.participantId); showRound(data); $('#menu').value = ''; toast('메뉴를 담았어요! 🍽️');
  } catch (error) { toast(error.message); }
};
$('#cancelBtn').onclick = async () => { try { showRound(await api(`/api/rounds/${roundId}/entries/me?token=${encodeURIComponent(joinToken)}`, { method: 'DELETE', headers: { 'X-Participant-Id': participantId() } })); toast('내 메뉴를 취소했어요.'); } catch (error) { toast(error.message); } };
async function ownerAction(action) { try { showRound(await api(`/api/rounds/${roundId}/${action}`, { method: 'POST', headers: { 'X-Owner-Token': ownerToken } })); toast(action === 'draw' ? '두구두구… 메뉴가 정해졌어요! 🎉' : '완료했어요!'); } catch (error) { toast(error.message); } }
$('#drawBtn').onclick = () => ownerAction('draw'); $('#rerollBtn').onclick = () => ownerAction('reroll'); $('#closeBtn').onclick = () => ownerAction('close');
$('#shareBtn').onclick = async () => { const url = shareUrl || location.href; const text = `🥚 점메추리알에서 오늘 점심 메뉴를 같이 골라요!\n${url}`; if (navigator.share) { try { await navigator.share({ title: '점메추리알 🥚', text, url }); } catch {} } else { try { await navigator.clipboard.writeText(text); toast('초대 링크를 복사했어요. 카카오톡 방에 붙여넣어주세요!'); } catch { toast(url); } } };
(async () => {
  const match = location.pathname.match(/^\/r\/([0-9a-f-]{36})$/i); if (!match) return;
  roundId = match[1]; const params = new URLSearchParams(location.search); joinToken = params.get('token');
  if (!joinToken) { toast('초대 링크의 token 값이 없어요. 초대 링크를 다시 열어주세요.'); return; }
  ownerToken = localStorage.getItem(`jm-owner-${roundId}`); admin = !!ownerToken; shareUrl = location.href;
  history.replaceState({}, '', location.pathname);
  try { await refresh(); } catch (error) { toast(error.message); }
})();
