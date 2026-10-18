(() => {
  'use strict';
  const form = document.getElementById('campaign-checkout');
  const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
  if (form) {
    const platform = document.getElementById('platform');
    const service = document.getElementById('serviceType');
    const brief = document.getElementById('brief');
    const dialog = document.getElementById('checkout-confirm');
    const text = (id, value) => { document.getElementById(id).textContent = value; };
    const coupon = document.getElementById('couponCode');
    const couponStatus = document.getElementById('coupon-status');
    let confirmed = false, quote = null, quoteKey = '', quoteError = '', pendingQuote = false;
    let timer, quoteRequest;
    const currentKey = () => service.value + ':' + (coupon?.value.trim() || '');
    const update = () => {
      const option = service.selectedOptions[0];
      const price = option?.dataset.price;
      const platformTitle = platform.value ? platform.selectedOptions[0].textContent : 'Chưa chọn';
      const serviceTitle = option?.dataset.title || 'Chưa chọn';
      const key = currentKey();
      if (key !== quoteKey) {
        quoteKey = key; quote = null; quoteError = ''; pendingQuote = false;
        clearTimeout(timer); quoteRequest?.abort();
        if (coupon?.value.trim() && service.value) {
          pendingQuote = true;
          timer = setTimeout(async () => {
            quoteRequest = new AbortController();
            try {
              const params = new URLSearchParams({serviceType: service.value, couponCode: coupon.value.trim()});
              const response = await fetch('/campaigns/quote?' + params, {signal: quoteRequest.signal, headers:{Accept:'application/json'}});
              const data = await response.json();
              if (key !== quoteKey) return;
              if (!response.ok) { const error = new Error(data.error || 'Không thể kiểm tra mã.'); error.fromServer = response.status === 400 && typeof data.error === 'string'; throw error; }
              quote = data; quoteError = '';
            } catch (error) {
              if (error.name === 'AbortError' || key !== quoteKey) return;
              quoteError = error.fromServer ? error.message : 'Không thể kiểm tra mã. Kiểm tra kết nối hoặc đăng nhập lại rồi thử lại.';
            } finally { if (key === quoteKey) { pendingQuote = false; update(); } }
          }, 250);
        }
      }
      const formatted = quote ? money.format(quote.totalPriceVnd) : price ? money.format(Number(price)) : '— đ';
      text('summary-original', price ? money.format(quote?.originalPriceVnd ?? Number(price)) : '— đ');
      text('summary-discount', money.format(quote?.discountVnd || 0));
      text('confirm-coupon', quote?.couponCode || 'Không dùng mã');
      text('confirm-discount', money.format(quote?.discountVnd || 0));
      if (couponStatus) couponStatus.textContent = !coupon.value.trim() ? 'Mã được kiểm tra tại server. Xem trước không giữ lượt; hủy đơn không trả lượt.'
        : !service.value ? 'Chọn gói trước để kiểm tra mã.' : pendingQuote ? 'Đang kiểm tra mã…'
        : quoteError || (quote ? 'Mã ' + quote.couponCode + ': giảm dự kiến ' + money.format(quote.discountVnd) + '. Server sẽ kiểm tra lại khi tạo đơn.' : 'Đang chờ kiểm tra mã.');
      document.getElementById('checkout-submit').disabled = ![...service.options].some(o => o.value) || Boolean(coupon?.value.trim() && (!quote || pendingQuote || quoteError));
      for (const prefix of ['summary', 'confirm']) {
        text(prefix + '-platform', platformTitle);
        text(prefix + '-service', serviceTitle);
        text(prefix + '-price', formatted);
      }
      text('brief-count', `${brief.value.length} / 1.000 ký tự · Tối thiểu 10 ký tự`);
      text('confirm-url', document.getElementById('targetUrl').value);
      text('confirm-brief', brief.value);
    };
    document.getElementById('coupon-recheck')?.addEventListener('click', () => { quoteKey = ''; update(); });
    form.addEventListener('input', update);
    form.addEventListener('change', update);
    form.addEventListener('submit', event => {
      if (coupon?.value.trim() && (!quote || pendingQuote || quoteError || quoteKey !== currentKey())) {
        event.preventDefault(); confirmed = false; coupon.focus(); return;
      }
      if (!confirmed && typeof dialog.showModal === 'function') {
        event.preventDefault();
        update();
        dialog.showModal();
      } else {
        const button = document.getElementById('checkout-submit');
        button.disabled = true;
        button.textContent = 'Đang gửi yêu cầu…';
      }
    });
    document.getElementById('confirm-send').addEventListener('click', () => {
      if (!form.reportValidity()) { dialog.close(); return; }
      confirmed = true;
      dialog.close();
      form.requestSubmit();
    });
    dialog.querySelector('[data-close-dialog]').addEventListener('click', () => dialog.close());
    window.addEventListener('pageshow', () => {
      confirmed = false;
      const button = document.getElementById('checkout-submit');
      button.disabled = ![...service.options].some(option => option.value);
      button.textContent = 'Xem lại và gửi yêu cầu';
      update();
    });
    update();
  }
  const cancelDialog = document.getElementById('cancel-confirm');
  if (cancelDialog && typeof cancelDialog.showModal === 'function') {
    let pendingForm;
    document.querySelectorAll('form[data-cancel-order]').forEach(cancelForm => {
      cancelForm.addEventListener('submit', event => {
        event.preventDefault();
        pendingForm = cancelForm;
        document.getElementById('cancel-order-label').textContent = '#' + cancelForm.dataset.cancelOrder;
        cancelDialog.showModal();
      });
    });
    cancelDialog.querySelector('[data-close-dialog]').addEventListener('click', () => cancelDialog.close());
    document.getElementById('confirm-cancel').addEventListener('click', () => {
      if (!pendingForm) return;
      const button = pendingForm.querySelector('button');
      button.disabled = true;
      button.textContent = 'Đang hủy…';
      cancelDialog.close();
      HTMLFormElement.prototype.submit.call(pendingForm);
    });
    window.addEventListener('pageshow', () => {
      document.querySelectorAll('form[data-cancel-order] button').forEach(button => {
        button.disabled = false;
        button.textContent = 'Hủy yêu cầu';
      });
    });
  }
})();
