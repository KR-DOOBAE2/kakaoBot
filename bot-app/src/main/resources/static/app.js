const $ = selector => document.querySelector(selector);
let roundId, joinToken, shareUrl, roundData, ownerToken, diningMode = '매장', spinning = false, revealed = false, lastDrawAt;
let restaurantLoading = false, restaurantLoaded = false;
let weatherLoading = false, weatherAddress = null, weatherCheckedAt = 0;
let foodCatalog = [];
let suggestionKey = null, suggestionCheckedAt = 0;
const emojis = ['🍜', '🍚', '🥟', '🍛', '🍣', '🍲', '🥗', '🍔'];
const characters = [
  { id: 'chiikawa', name: '치이카와', plateY: 65, happyPlateY: 58 },
  { id: 'usagi', name: '우사기', plateY: 72, happyPlateY: 62 },
  { id: 'hachiware', name: '하치와레', plateY: 68, happyPlateY: 60 },
  { id: 'momonga', name: '모몽가', plateY: 64, happyPlateY: 61 },
  { id: 'rakko', name: '랏코', plateY: 56, happyPlateY: 53 },
  { id: 'kurimanju', name: '쿠리만쥬', plateY: 67, happyPlateY: 60 }
];
function characterFor(menu, drawAt) {
  let hash = 2166136261;
  for (const c of `${drawAt}|${menu}`) hash = Math.imul(hash ^ c.charCodeAt(0), 16777619);
  return characters[(hash >>> 0) % characters.length];
}
function foodFor(menu) {
  const name = menu.replace(/^(매장|배달)\s*·\s*/, '');
  if (/초밥|스시|일식/.test(name)) return '🍣';
  if (/라멘|우동|국수|면/.test(name)) return '🍜';
  if (/치킨|닭/.test(name)) return '🍗';
  if (/햄버거|버거|패스트/.test(name)) return '🍔';
  if (/피자|양식/.test(name)) return '🍕';
  if (/중식|짜장|짬뽕|마라/.test(name)) return '🥟';
  if (/김치|찌개|국밥|탕|국물/.test(name)) return '🍲';
  if (/샐러드|채식/.test(name)) return '🥗';
  if (/한식|밥|비빔/.test(name)) return '🍚';
  return '🍽️';
}
function catalogFoodFor(menu) {
  const name = menu.replace(/^(매장|배달)\s*·\s*/, '').replace(/\s+/g, '');
  const aliases = { 돈가스: '돈까스', 자장면: '짜장면', 스시: '초밥', 라면사리: '라면' };
  const normalized = aliases[name] || name;
  return foodCatalog.find(food => food.name.replace(/\s+/g, '') === normalized);
}
async function loadFoodCatalog() {
  try {
    foodCatalog = await api('/api/foods');
    const suggestions = $('#foodSuggestions');
    if (suggestions) suggestions.innerHTML = foodCatalog.map(food => `<option value="${escapeHtml(food.name)}"></option>`).join('');
    if (roundData?.candidates?.length) renderIllustrations(roundData);
  } catch (error) { console.warn('Food catalog unavailable', error); }
}
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
  else if (roundData.status === 'DRAWN') $('#countdown').textContent = '🎉 오늘의 결과가 공개됐어요!';
  else if (ms > 0) { const seconds = Math.ceil(ms / 1000), hours = Math.floor(seconds / 3600), minutes = Math.floor(seconds % 3600 / 60); $('#countdown').textContent = `결과 공개까지 ${hours ? `${hours}시간 ` : ''}${minutes}분 ${seconds % 60}초 남았어요 🐣`; }
  else $('#countdown').textContent = '두구두구… 룰렛 결과가 준비됐어요!';
  $('#spinBtn').disabled = spinning || !(roundData.status === 'DRAWN' && roundData.winner || roundData.status === 'OPEN' && ms <= 0 && roundData.candidates.length > 0);
}
function showRound(data) {
  if (lastDrawAt && lastDrawAt !== data.drawAt) {
    revealed = false; restaurantLoaded = false;
    $('#winner').classList.add('hidden'); $('#winner').classList.remove('revealed');
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
  $('#schedule').textContent = data.status === 'DRAWN' ? `${drawTime(data)} 결과 확정 · 메뉴 입력 마감` : data.drawAt ? `${drawTime(data)} 메뉴 마감·추첨 · 점심 종료 후 만족도 평가` : '이전 방식의 방 · 새 점심방을 만들어주세요';
  $('#count').textContent = `${data.candidates.length}개 메뉴 후보`;
  const accepting = data.status === 'OPEN' && data.drawAt && Date.now() < new Date(data.drawAt).getTime();
  const mine = data.entries.filter(entry => entry.mine);
  $('#myMenus').replaceChildren();
  for (const entry of mine) {
    const row = document.createElement('div'); row.className = 'my-menu';
    const name = document.createElement('span'); name.textContent = `🥚 ${entry.menu}`;
    row.append(name);
    if (accepting) {
      const remove = document.createElement('button'); remove.type = 'button'; remove.textContent = '취소';
      remove.onclick = async () => {
        try { showRound(await api(`/api/rounds/${roundId}/entries/${encodeURIComponent(entry.id)}?token=${encodeURIComponent(joinToken)}`, { method: 'DELETE', headers: { 'X-Participant-Id': participantId() } })); toast('메뉴 하나를 취소했어요.'); }
        catch (error) { toast(error.message); }
      };
      row.append(remove);
    }
    $('#myMenus').append(row);
  }
  $('#menuForm').classList.toggle('hidden', !accepting); $('#cancelBtn').classList.toggle('hidden', !accepting || !mine.length);
  $('#drawNowBtn').classList.toggle('hidden', !ownerToken || data.status !== 'OPEN' || !data.candidates.length);
  updateCountdown();
  $('#illustratedMenus').classList.remove('hidden');
  $('#candidateLabel').classList.toggle('hidden', data.candidates.length === 0);
  if (data.candidates.length) renderIllustrations(data);
  else $('#menuIllustrations').replaceChildren();
  document.querySelectorAll('.suggestion-add').forEach(button => button.disabled = !accepting);
  loadSuggestions();
  if (data.winner && !revealed && !spinning) animate(data);
  $('#restaurantCard').classList.toggle('hidden', !data.winner);
  if (data.winner && !restaurantLoaded && !restaurantLoading) loadRestaurants();
  loadSatisfaction(data).catch(() => {});
  if (data.officeAddress && !weatherLoading && (weatherAddress !== data.officeAddress || Date.now() - weatherCheckedAt > 30 * 60 * 1000)) loadWeather(data.officeAddress);
}
function renderIllustrations(data) {
  const ordered = [...data.candidates].sort((a, b) => Number(b === data.winner && revealed) - Number(a === data.winner && revealed));
  $('#menuIllustrations').innerHTML = ordered.map(menu => {
    const catalogFood = catalogFoodFor(menu), c = catalogFood ? characters.find(item => item.id === catalogFood.characterId) : characterFor(menu, data.drawAt);
    const won = menu === data.winner && revealed;
    const art = catalogFood
      ? `<img src="${escapeHtml(catalogFood.imagePath)}" alt="${escapeHtml(menu)} 음식 그림" loading="lazy">`
      : `<img src="/characters/${c.id}${won ? '-happy' : ''}.png" alt="${escapeHtml(menu)} 음식을 들고 있는 그림" loading="lazy"><span class="plate-food" aria-hidden="true"><span>${foodFor(menu)}</span></span>`;
    return `<div class="menu-illustration ${won ? 'is-winner' : ''}"><div class="character-art" style="--plate-y:${won ? c.happyPlateY : c.plateY}%">${art}</div><strong>${escapeHtml(menu)}</strong>${won ? '<small>당첨! 🎉</small>' : ''}</div>`;
  }).join('');
}
async function loadSuggestions() {
  if (!roundId || !joinToken || !roundData) return;
  const mode = diningMode === '매장' ? 'store' : 'delivery';
  const key = `${roundId}|${roundData.officeAddress}|${roundData.drawAt}|${mode}`;
  if (key === suggestionKey && Date.now() - suggestionCheckedAt < 30 * 60 * 1000) return;
  suggestionKey = key; suggestionCheckedAt = Date.now();
  $('#suggestionLabel').textContent = mode === 'store' ? '날씨와 회사 주변 1km 식당을 확인 중이에요…' : '날씨에 맞는 배달 메뉴를 고르고 있어요…';
  $('#suggestedMenus').replaceChildren();
  try {
    const result = await api(`/api/rounds/${roundId}/menu-suggestions?token=${encodeURIComponent(joinToken)}&mode=${mode}`);
    if (key !== suggestionKey) return;
    const weather = { sunny: '맑은 날', cloudy: '흐린 날', rain: '비 오는 날', snow: '눈 오는 날' }[result.weatherTheme] || '오늘';
    $('#suggestionLabel').textContent = `${result.weatherAvailable ? weather : '날씨 정보가 없어 기본 추천으로'} · ${mode === 'store' ? '1km 안에서 매장이 확인된' : '배달용'} 메뉴 ${result.suggestions.length}가지`;
    for (const suggestion of result.suggestions) {
      const card = document.createElement('div'); card.className = 'menu-illustration suggestion-card';
      if (/^\/food-art\/[a-z0-9-]+\.png$/.test(suggestion.imagePath)) {
        const image = document.createElement('img'); image.src = suggestion.imagePath;
        image.alt = `${suggestion.name} 음식 그림`; image.loading = 'lazy'; card.append(image);
      }
      const name = document.createElement('strong'); name.textContent = suggestion.name; card.append(name);
      if (suggestion.nearbyPlace) {
        const place = document.createElement('small');
        place.textContent = `📍 ${suggestion.nearbyPlace.name} · ${suggestion.nearbyPlace.distanceMeters}m`;
        card.append(place);
        if (/^https:\/\//.test(suggestion.nearbyPlace.placeUrl || '')) {
          const link = document.createElement('a'); link.href = suggestion.nearbyPlace.placeUrl;
          link.target = '_blank'; link.rel = 'noopener noreferrer'; link.textContent = '지도 보기 ↗'; card.append(link);
        }
      }
      const add = document.createElement('button'); add.type = 'button'; add.className = 'suggestion-add';
      add.textContent = '이 메뉴 등록';
      add.disabled = roundData.status !== 'OPEN' || Date.now() >= new Date(roundData.drawAt).getTime();
      add.onclick = () => { $('#menu').value = suggestion.name; $('#menuForm').requestSubmit(); };
      card.append(add); $('#suggestedMenus').append(card);
    }
    if (!result.suggestions.length) $('#suggestionLabel').textContent = mode === 'store'
      ? '회사 주변 1km 안에서 날씨에 맞는 식당을 확인하지 못했어요. 직접 메뉴를 입력해 주세요.'
      : '오늘의 추천 메뉴를 준비하지 못했어요. 직접 메뉴를 입력해 주세요.';
  } catch (error) {
    if (key !== suggestionKey) return;
    $('#suggestionLabel').textContent = `추천 메뉴를 불러오지 못했어요. ${error.message}`;
    suggestionCheckedAt = Date.now() - 29 * 60 * 1000;
  }
}
async function loadWeather(address) {
  weatherLoading = true;
  $('#weatherStatus').textContent = '🌤️ 오늘 날씨를 확인 중이에요…';
  try {
    const result = await api(`/api/rounds/${roundId}/weather?token=${encodeURIComponent(joinToken)}`);
    if (roundData.officeAddress !== address) return;
    weatherAddress = address; weatherCheckedAt = Date.now();
    const icon = { sunny: '☀️', cloudy: '☁️', rain: '🌧️', snow: '❄️' }[result.theme] || '🌤️';
    $('#weatherStatus').textContent = `${icon} 오늘의 날씨 ${result.label} · ${Math.round(result.temperatureCelsius)}°C`;
    $('#weatherCredit').classList.remove('hidden');
    $('#weatherHero').classList.remove('weather-sunny', 'weather-cloudy', 'weather-rain', 'weather-snow');
    $('#weatherHero').classList.add(`weather-${result.theme}`);
  } catch (error) {
    weatherAddress = address; weatherCheckedAt = Date.now();
    $('#weatherStatus').textContent = '🌤️ 현재 날씨를 확인하지 못했어요.';
    $('#weatherCredit').classList.add('hidden');
  } finally { weatherLoading = false; }
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
      : `🥚 회사 주변 1km에서 ${result.winningMenu} 관련 식당을 찾지 못했어요.`)
      + (result.winningMenu.startsWith('배달 ·') ? ' 배달 가능 여부는 매장에 확인해주세요.' : '');
    $('#restaurantList').innerHTML = result.restaurants.map((p, i) => {
      const safeUrl = /^https:\/\//i.test(p.placeUrl || '') ? escapeHtml(p.placeUrl) : '';
      const priceLabel = p.price
        ? `✅ ${escapeHtml(p.price.menu)} ${p.price.won.toLocaleString('ko-KR')}원 · ${escapeHtml(p.price.source)} (${escapeHtml(p.price.asOf)} 기준)`
        : '가격 미확인 · 방문 전 메뉴판 확인';
      return `<div class="restaurant"><div class="restaurant-rank">${i === 0 ? '👑' : i + 1}</div><div class="restaurant-main"><strong>${escapeHtml(p.name)}</strong>${p.recentlyFound ? '<span class="recent">최근 발견</span>' : ''}<p>${escapeHtml(p.address || '주소 정보 없음')}</p><small>${p.distanceMeters}m · 메뉴 언급 ${p.menuMentions}회 · 평가 ${p.ratingScore > 0 ? '+' : ''}${p.ratingScore}</small><small>${priceLabel}</small>${safeUrl ? `<a href="${safeUrl}" target="_blank" rel="noopener noreferrer">지도에서 보기 ↗</a>` : ''}</div></div>`;
    }).join('');
    restaurantLoaded = true;
  } catch (error) {
    $('#restaurantStatus').textContent = error.message;
    $('#restaurantRetry').classList.remove('hidden');
  } finally { restaurantLoading = false; }
  await showNaverResults();
}
$('#restaurantRetry').onclick = loadRestaurants;
async function showNaverResults() {
  const address = roundData?.officeAddress, menu = roundData?.winner;
  if (!address || !menu) return;
  const card = $('#restaurantCard');
  let section = $('#naverPlaces');
  if (!section) {
    section = document.createElement('div'); section.id = 'naverPlaces';
    card.append(section);
  }
  const cleanMenu = menu.replace(/^(매장|배달)\s*·\s*/, '');
  const query = `${address} ${cleanMenu}`;
  section.replaceChildren();
  const heading = document.createElement('h3'); heading.textContent = '🔎 네이버 지역 검색';
  section.append(heading);
  try {
    const result = await api('/api/naver/local-search', {
      method: 'POST', body: JSON.stringify({ address, menu: cleanMenu })
    });
    if (roundData?.winner !== menu || roundData?.officeAddress !== address) return;
    if (result.configured) {
      for (const place of result.places) {
        const row = document.createElement('div'); row.className = 'restaurant';
        const main = document.createElement('div'); main.className = 'restaurant-main';
        const name = document.createElement('strong'); name.textContent = place.title.replace(/<\/?b>/gi, '');
        const detail = document.createElement('p'); detail.textContent = `${place.category} · ${place.roadAddress || place.address}`;
        main.append(name, detail);
        if (/^https:\/\//i.test(place.link)) {
          const link = document.createElement('a'); link.href = place.link;
          link.target = '_blank'; link.rel = 'noopener noreferrer'; link.textContent = '업체 정보 ↗';
          main.append(link);
        }
        row.append(main); section.append(row);
      }
      if (!result.places.length) {
        const empty = document.createElement('p'); empty.textContent = '네이버에서도 검색 결과가 없어요.';
        section.append(empty);
      }
      const note = document.createElement('small');
      note.textContent = '자료: NAVER API HUB · 네이버 제공 순서 · 반경 1km 보장 없음';
      section.append(note);
    }
  } catch (_) {
    const error = document.createElement('p'); error.textContent = '네이버 검색을 완료하지 못했어요.';
    section.append(error);
  }
  const link = document.createElement('a'); link.href = `https://map.naver.com/p/search/${encodeURIComponent(query)}`;
  link.target = '_blank'; link.rel = 'noopener noreferrer'; link.textContent = '네이버 지도에서 직접 보기 ↗';
  section.append(link);
}
function animate(data) {
  if (spinning || !data.winner) return;
  spinning = true; revealed = false; renderIllustrations(data); $('#winner').classList.remove('hidden'); $('#winner').classList.add('spinning');
  const slices = Array.from({length:6},(_,i) => `<span class="reveal-slice" style="--i:${i};--pos:${i*20}%"></span>`).join('');
  $('#winner').innerHTML = `<div class="reveal-stage" role="img" aria-label="여섯 친구가 수채화 풍경에서 오늘의 점심을 공개해요">${slices}<div class="reveal-food"><span class="reveal-icon">${foodFor(data.winner)}</span><strong class="reveal-label">두구두구…</strong></div></div>`;
  const choices = data.candidates.length ? data.candidates : [data.winner];
  let step = 0;
  const timer = setInterval(() => { const label=$('#winner .reveal-label'); if(label)label.textContent=choices[step++ % choices.length]; }, 100);
  updateCountdown();
  setTimeout(() => { clearInterval(timer); $('#winner').classList.remove('spinning'); $('#winner').classList.add('revealed'); const label=$('#winner .reveal-label'); if(label)label.textContent=`🎉 ${data.winner}`; spinning = false; revealed = true; renderIllustrations(data); updateCountdown(); }, 2700);
}
function satisfactionOpenAt(data) {
  if (!data.drawAt || !data.lunchEndTime) return null;
  const day = new Date(new Date(data.drawAt).getTime() + 9 * 3600 * 1000).toISOString().slice(0,10);
  return new Date(`${day}T${String(data.lunchEndTime).slice(0,5)}:00+09:00`);
}
async function loadSatisfaction(data) {
  const card=$('#satisfactionCard');
  if (!data.winner || !data.lunchEndTime) { card.classList.add('hidden'); return; }
  card.classList.remove('hidden');
  const opensAt=satisfactionOpenAt(data), open=opensAt && Date.now()>=opensAt.getTime();
  document.querySelectorAll('[data-satisfaction]').forEach(button => button.disabled=!open);
  if (!open) { $('#satisfactionStatus').textContent=`점심 종료 ${String(data.lunchEndTime).slice(0,5)}부터 평가할 수 있어요.`; $('#satisfactionCounts').textContent=''; return; }
  const result=await api(`/api/rounds/${roundId}/satisfaction?token=${encodeURIComponent(joinToken)}`,{headers:{'X-Participant-Id':participantId()}});
  $('#satisfactionStatus').textContent=result.mine ? '내 평가를 저장했어요. 언제든 변경할 수 있어요.' : '오늘의 점심을 평가해주세요.';
  $('#satisfactionCounts').textContent=`좋아요 ${result.counts.LIKE||0} · 보통이다 ${result.counts.NEUTRAL||0} · 싫어요 ${result.counts.DISLIKE||0}`;
  document.querySelectorAll('[data-satisfaction]').forEach(button => button.classList.toggle('selected',button.dataset.satisfaction===result.mine));
}
document.querySelectorAll('[data-satisfaction]').forEach(button => button.onclick=async () => {
  try { await api(`/api/rounds/${roundId}/satisfaction?token=${encodeURIComponent(joinToken)}`,{method:'PUT',headers:{'X-Participant-Id':participantId()},body:JSON.stringify({satisfaction:button.dataset.satisfaction})}); await loadSatisfaction(roundData); toast('만족도 평가를 저장했어요!'); }
  catch(error){toast(error.message);}
});
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
mountAddressPicker($('#officeAddress'));
mountAddressPicker($('#settingsAddress'));
$('#menuForm').onsubmit = async event => {
  event.preventDefault();
  try {
    const category = $('#menu').value.trim();
    if (!category) throw Error('먹고 싶은 음식 카테고리를 입력해주세요.');
    const data = await api(`/api/rounds/${roundId}/entries?token=${encodeURIComponent(joinToken)}`, { method: 'PUT', headers: { 'X-Participant-Id': participantId() }, body: JSON.stringify({ menu: `${diningMode} · ${category}` }) });
    showRound(data); $('#menu').value = ''; toast(`${diningMode} · ${category} 메뉴를 익명으로 추가했어요!`);
  } catch (error) { toast(error.message); }
};
$('#cancelBtn').onclick = async () => { try { showRound(await api(`/api/rounds/${roundId}/entries/me?token=${encodeURIComponent(joinToken)}`, { method: 'DELETE', headers: { 'X-Participant-Id': participantId() } })); toast('내 메뉴를 모두 취소했어요.'); } catch (error) { toast(error.message); } };
$('#spinBtn').onclick = async () => { try { await refresh(); if (roundData.winner) { revealed = false; animate(roundData); } else toast('결과를 준비 중이에요. 잠시 후 다시 눌러주세요.'); } catch (error) { toast(error.message); } };
$('#drawNowBtn').onclick = async () => {
  const button = $('#drawNowBtn'); button.disabled = true;
  try {
    await api(`/api/rounds/${roundId}/draw`, { method: 'POST', headers: { 'X-Owner-Token': ownerToken } });
    await refresh();
    toast('결과를 확정했어요. 모두 같은 결과를 볼 수 있어요!');
  } catch (error) { toast(error.message); button.disabled = false; }
};
$('#shareBtn').onclick = async () => { const url = shareUrl || location.href; const text = `🥚 점메추리알에서 익명으로 오늘 점심 메뉴를 골라요!\n점심 시작 30분 전 메뉴 마감·추첨 · 점심 종료 후 만족도 평가\n${url}`; if (navigator.share) { try { await navigator.share({ title: '점메추리알 🥚', text, url }); } catch {} } else { try { await navigator.clipboard.writeText(text); toast('초대 링크를 복사했어요. 카카오톡 방에 붙여넣어주세요!'); } catch { toast(url); } } };
document.querySelectorAll('[data-mode]').forEach(button => button.onclick = () => {
  diningMode = button.dataset.mode;
  document.querySelectorAll('[data-mode]').forEach(item => {
    const active = item.dataset.mode === diningMode;
    item.classList.toggle('active', active);
    item.setAttribute('aria-pressed', String(active));
  });
  if (roundData) loadSuggestions();
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
loadFoodCatalog();
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
