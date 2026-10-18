(() => {
  'use strict';
  const select = document.getElementById('service-options');
  if (!select) return;
  const details = {
    PROFILE_AUDIT: 'Đánh giá hồ sơ hiện tại và đề xuất tối ưu cách giới thiệu kênh.',
    CONTENT_PLAN: 'Định hướng chủ đề và xây dựng kế hoạch nội dung trong 7 ngày.',
    AD_STRATEGY: 'Tư vấn chiến lược quảng cáo phù hợp với mục tiêu của kênh. Không bao gồm ngân sách chạy quảng cáo.'
  };
  const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
  const update = () => {
    const option = select.selectedOptions[0];
    const price = option && option.dataset.price;
    const label = price ? money.format(Number(price)) : '— đ';
    document.getElementById('unit-price').textContent = label;
    document.getElementById('total-price').textContent = label;
    document.getElementById('service-detail').textContent = details[select.value] || 'Chọn gói để xem giá và nội dung tư vấn.';
    document.querySelectorAll('[data-service]').forEach(button => {
      const active = button.dataset.service === select.value;
      button.classList.toggle('active', active);
      button.setAttribute('aria-pressed', String(active));
    });
  };
  const choose = code => {
    if (![...select.options].some(option => option.value === code)) return;
    select.value = code;
    update();
    document.getElementById('order-panel').scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'start' });
    select.focus({ preventScroll: true });
  };
  select.addEventListener('change', update);
  document.querySelectorAll('[data-service]').forEach(button => button.addEventListener('click', () => choose(button.dataset.service)));
  const search = document.getElementById('service-search');
  const results = document.getElementById('search-results');
  const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').toLowerCase();
  search.addEventListener('input', () => {
    results.replaceChildren();
    const query = normalize(search.value.trim());
    results.hidden = !query;
    if (!query) return;
    const options = [...select.options].filter(option => option.value && normalize(option.textContent).includes(query));
    options.forEach(option => {
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = option.textContent;
      button.addEventListener('click', () => { choose(option.value); results.hidden = true; });
      results.append(button);
    });
    if (!options.length) {
      const empty = document.createElement('p');
      empty.textContent = 'Không tìm thấy gói phù hợp. Thử “nội dung” hoặc “hồ sơ”.';
      results.append(empty);
    }
  });
  search.closest('form').addEventListener('submit', event => {
    event.preventDefault();
    const first = results.querySelector('button');
    if (first) first.click();
  });
  document.addEventListener('keydown', event => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); search.focus(); }
    if (event.key === 'Escape') results.hidden = true;
  });
  document.addEventListener('click', event => { if (!search.closest('form').contains(event.target)) results.hidden = true; });
  window.addEventListener('pageshow', update);
  update();
})();
