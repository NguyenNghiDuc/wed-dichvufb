/* Customer quote preview only: never authorize purchase or set a server price. */
(() => {
  "use strict";
  const selector = document.getElementById("serviceType");
  const message = document.getElementById("quotePreview");
  if (!selector || !message) return;

  let currentRequest = null;
  const money = new Intl.NumberFormat("vi-VN");

  async function refresh() {
    if (currentRequest) currentRequest.abort();
    const value = selector.value;
    if (!value) {
      message.textContent = "Chọn gói để xem giá báo từ hệ thống. Giá cuối cùng được xác nhận khi tạo đơn.";
      return;
    }
    const controller = new AbortController();
    currentRequest = controller;
    message.textContent = "Đang kiểm tra báo giá từ hệ thống…";
    try {
      const result = await fetch("/api/v1/campaign-orders/quote?serviceType=" + encodeURIComponent(value), {
        method: "GET",
        credentials: "same-origin",
        headers: { Accept: "application/json" },
        cache: "no-store",
        signal: controller.signal
      });
      if (currentRequest !== controller) return;
      if (!result.ok) throw new Error("Quote unavailable");
      const quote = await result.json();
      if (currentRequest !== controller) return;
      if (quote.serviceType !== value || !Number.isSafeInteger(quote.priceVnd) || quote.priceVnd < 0) {
        throw new Error("Unexpected quote response");
      }
      message.textContent = "Giá dự kiến: " + money.format(quote.priceVnd) +
        " VND — server xác nhận lại giá và tình trạng gói khi gửi đơn.";
    } catch (error) {
      if (error.name === "AbortError" || currentRequest !== controller) return;
      message.textContent = "Chưa lấy được giá hiện hành. Bạn có thể thử chọn lại gói; giá sẽ được xác nhận khi gửi đơn.";
    } finally {
      if (currentRequest === controller) currentRequest = null;
    }
  }
  selector.addEventListener("change", refresh);
  if (selector.value) refresh();
})();
