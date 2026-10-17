"use strict";

// Same-origin, explicit user action only. Never auto-submit or publish social content.
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("ai-content-form");
  if (!form) return;
  const status = document.getElementById("ai-content-status");
  const result = document.getElementById("ai-content-result");
  const submit = form.querySelector('button[type="submit"]');
  const token = document.querySelector('input[name="_csrf"]');
  const taskField = form.querySelector('[name="task"]');
  const daysField = form.querySelector('[name="days"]');
  const originalField = form.querySelector('[name="existingContent"]');
  const synchronizeFields = () => {
    daysField.disabled = taskField.value !== "CONTENT_PLAN";
    originalField.disabled = taskField.value !== "OPTIMIZE";
  };
  taskField.addEventListener("change", synchronizeFields);
  synchronizeFields();

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!form.reportValidity() || !token || submit.disabled) return;
    const data = new FormData(form);
    const payload = {
      platform: data.get("platform"),
      topic: String(data.get("topic") || "").trim(),
      audience: String(data.get("audience") || "").trim(),
      task: data.get("task"),
      language: data.get("language"),
      days: Number(data.get("days") || 7),
      existingContent: String(data.get("existingContent") || "").trim()
    };
    if (payload.task === "TIKTOK_IDEAS" && payload.platform !== "TIKTOK" ||
        payload.task === "YOUTUBE_SHORTS" && payload.platform !== "YOUTUBE") {
      status.textContent = "Hãy chọn đúng nền tảng cho ý tưởng TikTok hoặc YouTube Shorts.";
      return;
    }
    if (payload.task === "OPTIMIZE" && !payload.existingContent) {
      status.textContent = "Hãy nhập nội dung gốc cần tối ưu trước khi gửi yêu cầu.";
      originalField.focus();
      return;
    }
    result.value = "";
    status.textContent = "Đang gửi yêu cầu có giới hạn tới dịch vụ AI…";
    submit.disabled = true;
    try {
      const response = await fetch(form.dataset.apiUrl, {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": token.value },
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        if (response.status === 429) status.textContent = "Bạn đã dùng hết lượt AI tạm thời.";
        else if (response.status === 503) status.textContent = "AI chưa được cấu hình hoặc dịch vụ đã hết hạn mức.";
        else if (response.status === 504) status.textContent = "Dịch vụ AI phản hồi quá chậm.";
        else status.textContent = "Không thể tạo nội dung. Vui lòng kiểm tra dữ liệu và thử lại sau.";
        return;
      }
      const responseData = await response.json();
      if (typeof responseData.content !== "string" || responseData.content.length > 12000)
        throw new Error("Invalid response");
      // Use only textContent/value; never inject model output as HTML.
      result.value = responseData.content;
      status.textContent = "Đã nhận bản gợi ý. Hãy kiểm tra thông tin trước khi sử dụng.";
    } catch (_) {
      status.textContent = "Không thể kết nối dịch vụ AI.";
    } finally {
      submit.disabled = false;
    }
  });
});
