"use strict";

// Local preview with the viewer's own entered values. No platform API or auto-publishing.
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("content-metrics-form");
  if (!form) return;
  const status = document.getElementById("content-metrics-status");
  const result = document.getElementById("content-metrics-result");
  const token = document.querySelector('input[name="_csrf"]');
  const button = form.querySelector('button[type="submit"]');

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!form.reportValidity() || !token || button.disabled) return;
    const data = new FormData(form);
    const post = {
      platform: String(data.get("platform") || ""),
      publishedAtUtc: String(data.get("publishedAtUtc") || ""),
      views: Number(data.get("views")),
      likes: Number(data.get("likes")),
      comments: Number(data.get("comments")),
      shares: Number(data.get("shares")),
      saves: Number(data.get("saves"))
    };
    const counts = [post.views, post.likes, post.comments, post.shares, post.saves];
    if (!counts.every(n => Number.isSafeInteger(n) && n >= 0 && n <= 1_000_000_000)
        || post.views === 0) {
      status.textContent = "Số liệu phải là số nguyên không âm; lượt xem cần lớn hơn 0.";
      return;
    }
    result.textContent = "";
    status.textContent = "Đang phân tích dữ liệu do bạn nhập…";
    button.disabled = true;
    try {
      const response = await fetch(form.dataset.apiUrl, {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": token.value },
        body: JSON.stringify({ posts: [post] })
      });
      if (!response.ok) {
        status.textContent = response.status === 429 ? "Đã vượt giới hạn yêu cầu." :
          "Dữ liệu không hợp lệ hoặc dịch vụ tạm thời không khả dụng.";
        return;
      }
      const summary = await response.json();
      if (summary.source !== "USER_SUPPLIED_UNVERIFIED")
        throw new Error("Unexpected provenance");
      result.textContent = [
        "Số bài: " + summary.postCount,
        "Lượt xem đã nhập: " + summary.totalViews,
        "Tổng lượt tương tác đã nhập: " + summary.totalInteractions,
        "Lượt tương tác / 100 lượt xem: " + summary.interactionsPer100Views,
        "Gợi ý khung giờ: chưa đủ mẫu để kết luận.",
        String(summary.caveat || "")
      ].join("\n");
      status.textContent = "Hoàn tất tính toán từ số liệu do bạn cung cấp.";
    } catch (_) {
      status.textContent = "Không thể xử lý dữ liệu phân tích.";
    } finally {
      button.disabled = false;
    }
  });
});
