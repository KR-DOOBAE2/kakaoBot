function mountAddressPicker(input) {
  const label = input.closest('label');
  if (!label || label.nextElementSibling?.classList.contains('address-picker')) return;
  const picker = document.createElement('div');
  picker.className = 'address-picker';
  const search = document.createElement('button');
  search.type = 'button';
  search.className = 'address-search-button';
  search.textContent = '🔎 주소 찾기';
  const status = document.createElement('small');
  status.className = 'address-search-status';
  status.setAttribute('role', 'status');
  const results = document.createElement('div');
  results.className = 'address-search-results';
  const preview = document.createElement('div');
  preview.className = 'address-map-preview';
  picker.append(search, status, results, preview);
  label.after(picker);

  function showMap(place) {
    const lat = Number(place.latitude), lon = Number(place.longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lon)) return;
    const box = [lon - .004, lat - .003, lon + .004, lat + .003].join(',');
    const frame = document.createElement('iframe');
    frame.title = '선택한 회사 주소 지도';
    frame.loading = 'lazy';
    frame.referrerPolicy = 'strict-origin-when-cross-origin';
    frame.src = `https://www.openstreetmap.org/export/embed.html?bbox=${encodeURIComponent(box)}&layer=mapnik&marker=${encodeURIComponent(`${lat},${lon}`)}`;
    const credit = document.createElement('small');
    credit.textContent = '지도: ';
    const link = document.createElement('a');
    link.href = 'https://www.openstreetmap.org/copyright';
    link.target = '_blank'; link.rel = 'noopener noreferrer';
    link.textContent = '© OpenStreetMap contributors';
    credit.append(link);
    preview.replaceChildren(frame, credit);
  }

  async function findAddress(query = input.value.trim(), officialAddress = '') {
    if (query.length < 2) { status.textContent = '주소를 두 글자 이상 입력해주세요.'; return; }
    search.disabled = true;
    status.textContent = '지도에서 위치를 확인하고 있어요…';
    results.replaceChildren();
    try {
      const response = await fetch(`/api/addresses/search?q=${encodeURIComponent(query)}`);
      const places = await response.json();
      if (!response.ok) throw Error(places.error || '주소를 찾지 못했어요.');
      if (officialAddress && places.length) {
        input.value = officialAddress;
        showMap(places[0]);
        status.textContent = '선택한 도로명주소를 지도에 표시했어요. 위치가 맞는지 확인해주세요.';
        return;
      }
      status.textContent = places.length ? '지도 위치를 선택해주세요.' : '지도에서 위치를 찾지 못했어요. 주소를 다시 검색해주세요.';
      for (const place of places) {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'address-result';
        item.textContent = `📍 ${place.address.split(',').slice(0, 4).join(', ')}`;
        item.onclick = () => {
          input.value = place.address;
          results.replaceChildren();
          status.textContent = '선택한 주소를 아래 지도에서 확인하세요.';
          showMap(place);
        };
        results.append(item);
      }
    } catch (error) { status.textContent = `지도 확인 실패: ${error.message}`; }
    finally { search.disabled = false; }
  }

  function openPostcode() {
    if (!window.kakao?.Postcode) { findAddress(); return; }
    const overlay = document.createElement('div');
    overlay.className = 'postcode-overlay';
    overlay.setAttribute('role', 'dialog');
    overlay.setAttribute('aria-modal', 'true');
    overlay.setAttribute('aria-label', '회사 도로명주소 찾기');
    const panel = document.createElement('div'); panel.className = 'postcode-panel';
    const heading = document.createElement('div'); heading.className = 'postcode-heading';
    const title = document.createElement('strong'); title.textContent = '회사 주소 찾기';
    const close = document.createElement('button'); close.type = 'button'; close.textContent = '✕ 닫기';
    close.onclick = () => overlay.remove();
    heading.append(title, close);
    const content = document.createElement('div'); content.className = 'postcode-content';
    panel.append(heading, content); overlay.append(panel); document.body.append(overlay);
    close.focus();
    overlay.addEventListener('keydown', event => {
      if (event.key === 'Escape') overlay.remove();
    });
    new window.kakao.Postcode({
      oncomplete(data) {
        const address = (data.roadAddress || data.jibunAddress || data.address || '').trim();
        overlay.remove();
        if (!address) { status.textContent = '주소를 선택하지 못했어요. 다시 검색해주세요.'; return; }
        input.value = address;
        input.dispatchEvent(new Event('input', { bubbles: true }));
        findAddress(address, address);
      }
    }).embed(content, { q: input.value.trim() });
  }

  search.onclick = openPostcode;
  input.addEventListener('keydown', event => {
    if (event.key === 'Enter') { event.preventDefault(); openPostcode(); }
  });
  input.addEventListener('input', () => {
    results.replaceChildren(); preview.replaceChildren(); status.textContent = '';
  });
}
