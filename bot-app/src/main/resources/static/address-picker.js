function mountAddressPicker(input) {
  const label = input.closest('label');
  if (!label || label.nextElementSibling?.classList.contains('address-picker')) return;
  const picker = document.createElement('div');
  picker.className = 'address-picker';
  const search = document.createElement('button');
  search.type = 'button';
  search.className = 'address-search-button';
  search.textContent = '🔎 주소 검색';
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

  async function findAddress() {
    const query = input.value.trim();
    if (query.length < 2) { status.textContent = '주소를 두 글자 이상 입력해주세요.'; return; }
    search.disabled = true;
    status.textContent = '주소를 찾고 있어요…';
    results.replaceChildren();
    try {
      const response = await fetch(`/api/addresses/search?q=${encodeURIComponent(query)}`);
      const places = await response.json();
      if (!response.ok) throw Error(places.error || '주소를 찾지 못했어요.');
      status.textContent = places.length ? '주소를 선택하면 지도에 표시돼요.' : '검색 결과가 없어요. 도로명 주소를 다시 입력해주세요.';
      for (const place of places) {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'address-result';
        item.textContent = `📍 ${place.address}`;
        item.onclick = () => {
          input.value = place.address;
          results.replaceChildren();
          status.textContent = '선택한 주소를 아래 지도에서 확인하세요.';
          showMap(place);
        };
        results.append(item);
      }
    } catch (error) { status.textContent = error.message; }
    finally { search.disabled = false; }
  }
  search.onclick = findAddress;
  input.addEventListener('keydown', event => {
    if (event.key === 'Enter') { event.preventDefault(); findAddress(); }
  });
  input.addEventListener('input', () => {
    results.replaceChildren(); preview.replaceChildren(); status.textContent = '';
  });
}
