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
    let confirmed = false;
    const update = () => {
      const option = service.selectedOptions[0];
      const price = option?.dataset.price;
      const platformTitle = platform.value ? platform.selectedOptions[0].textContent : 'Chưa chọn';
      const serviceTitle = option?.dataset.title || 'Chưa chọn';
      const formatted = price ? money.format(Number(price)) : '— đ';
      for (const prefix of ['summary', 'confirm']) {
        text(prefix + '-platform', platformTitle);
        text(prefix + '-service', serviceTitle);
        text(prefix + '-price', formatted);
      }
      text('brief-count', `${brief.value.length} / 1.000 ký tự · Tối thiểu 10 ký tự`);
      text('confirm-url', document.getElementById('targetUrl').value);
      text('confirm-brief', brief.value);
    };
    form.addEventListener('input', update);
    form.addEventListener('change', update);
    form.addEventListener('submit', event => {
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
