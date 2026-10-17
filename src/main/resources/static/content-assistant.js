"use strict";

// Same-origin, explicit user action only. Never auto-submit or publish social content.
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("ai-content-form");
  if (!form) return;
  const status = document.getElementById("ai-content-status");
  const result = document.getElementById("ai-content-result");
  const submit = form.querySelector('button[type="submit"]');
  const token = document.querySelector('input[name="_csrf"]');
  const copyToDraft = document.getElementById("copy-ai-to-draft");
  const draftForm = document.getElementById("create-content-form");
  if (copyToDraft && draftForm) {
    copyToDraft.addEventListener("click", () => {
      const draftText = String(result.value || "").trim();
      const draftBody = draftForm.querySelector('[name="body"]');
      const draftTitle = draftForm.querySelector('[name="title"]');
      const draftPlatform = draftForm.querySelector('[name="platform"]');
      if (!draftText || draftText.length > 3000) {
        status.textContent = "Bản gợi ý quá dài hoặc trống; hãy rút gọn trước khi chuyển.";
        return;
      }
      if (draftBody.value.trim() || draftTitle.value.trim()) {
        status.textContent = "Form bản nháp đã có nội dung; vui lòng lưu hoặc xóa nội dung cũ trước.";
        return;
      }
      draftBody.value = draftText;
      draftTitle.value = String(form.querySelector('[name="topic"]').value || "").trim().slice(0,120);
      draftPlatform.value = form.querySelector('[name="platform"]').value;
      status.textContent = "Đã đưa gợi ý vào form, hãy chỉnh sửa và bấm Lưu nội dung khi sẵn sàng.";
      draftBody.focus();
    });
  }

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
    if (copyToDraft) copyToDraft.disabled = true;
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
        else if (response.status === 503) {
          status.textContent = "AI chưa được cấu hình hoặc dịch vụ đã hết hạn mức.";
          if (payload.task === "CONTENT_PLAN" && payload.topic.length <= 100) {
            const params = new URLSearchParams({
              platform: payload.platform, topic: payload.topic, days: String(payload.days)
            });
            const local = await fetch(form.dataset.planFallbackUrl + "?" + params.toString(), {
              credentials: "same-origin"
            });
            if (local.ok) {
              const template = await local.json();
              if (template.mode === "TEMPLATE_ONLY" && Array.isArray(template.suggestions)
                  && template.suggestions.length === payload.days
                  && template.suggestions.every(item =>
                      Number.isInteger(item.day) && item.day >= 1 && item.day <= 30 &&
                      typeof item.format === "string" && item.format.length <= 100 &&
                      typeof item.idea === "string" && item.idea.length <= 500)) {
                result.value = template.suggestions.map(item =>
                  "Ngày " + item.day + ": " + item.format + " — " + item.idea).join("\n");
                if (copyToDraft) copyToDraft.disabled = result.value.length > 3000;
                status.textContent = "Đang dùng KẾ HOẠCH THEO MẪU, KHÔNG phải AI hay lịch tự động.";
              }
            }
          }
          if (payload.task === "CAPTION" && payload.topic.length <= 100) {
            const params = new URLSearchParams({ platform: payload.platform, topic: payload.topic });
            const local = await fetch(form.dataset.fallbackUrl + "?" + params.toString(), {
              credentials: "same-origin"
            });
            if (local.ok) {
              const template = await local.json();
              if (template.mode === "TEMPLATE_ONLY" && Array.isArray(template.suggestions)
                  && template.suggestions.length <= 3
                  && template.suggestions.every(item => typeof item === "string" && item.length <= 500)) {
                result.value = template.suggestions.join("\n\n");
                if (copyToDraft) copyToDraft.disabled = false;
                status.textContent = "Đang dùng MẪU CÓ SẴN, không phải nội dung do AI tạo.";
              }
            }
          }
        }
        else if (response.status === 504) status.textContent = "Dịch vụ AI phản hồi quá chậm.";
        else status.textContent = "Không thể tạo nội dung. Vui lòng kiểm tra dữ liệu và thử lại sau.";
        return;
      }
      const responseData = await response.json();
      if (typeof responseData.content !== "string" || responseData.content.length > 12000)
        throw new Error("Invalid response");
      // Use only textContent/value; never inject model output as HTML.
      result.value = responseData.content;
      if (copyToDraft) copyToDraft.disabled = false;
      status.textContent = "Đã nhận bản gợi ý. Hãy kiểm tra thông tin trước khi sử dụng.";
    } catch (_) {
      status.textContent = "Không thể kết nối dịch vụ AI.";
    } finally {
      submit.disabled = false;
    }
  });
});
