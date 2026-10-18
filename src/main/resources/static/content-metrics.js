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

  const batchForm = document.getElementById("content-metrics-batch-form");
  if (batchForm) {
    const batchStatus = document.getElementById("content-metrics-batch-status");
    const batchResult = document.getElementById("content-metrics-batch-result");
    const batchButton = batchForm.querySelector('button[type="submit"]');
    batchForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (!batchForm.reportValidity() || !token || batchButton.disabled) return;
      batchResult.textContent = "";
      const raw = String(new FormData(batchForm).get("rows") || "");
      const lines = raw.trim().split(/\r?\n/).map(line => line.trim());
      if (lines.length < 1 || lines.length > 100 || raw.length > 20000) {
        batchStatus.textContent = "Nhập từ 1 đến 100 dòng, tối đa 20.000 ký tự.";
        return;
      }
      const posts = [];
      for (const [index, line] of lines.entries()) {
        const values = line.split(",").map(value => value.trim());
        if (values.length !== 7 ||
            !["FACEBOOK", "INSTAGRAM", "TIKTOK", "YOUTUBE"].includes(values[0]) ||
            !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?$/.test(values[1])) {
          batchStatus.textContent = "Dòng " + (index + 1) + " sai nền tảng, thời gian UTC hoặc số cột.";
          return;
        }
        const counts = values.slice(2).map(value =>
          /^\d{1,10}$/.test(value) ? Number(value) : NaN);
        if (!counts.every(n => Number.isSafeInteger(n) && n >= 0 && n <= 1_000_000_000) ||
            counts[0] < 1) {
          batchStatus.textContent = "Dòng " + (index + 1) + " có số liệu không hợp lệ.";
          return;
        }
        posts.push({
          platform: values[0],
          publishedAtUtc: values[1],
          views: counts[0], likes: counts[1], comments: counts[2],
          shares: counts[3], saves: counts[4]
        });
      }
      batchButton.disabled = true;
      batchStatus.textContent = "Đang tính toán trên " + posts.length + " bài do bạn nhập…";
      try {
        const response = await fetch(batchForm.dataset.apiUrl, {
          method: "POST",
          credentials: "same-origin",
          headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": token.value },
          body: JSON.stringify({ posts })
        });
        if (!response.ok) {
          batchStatus.textContent = response.status === 429 ? "Đã vượt giới hạn phân tích." :
            "Dữ liệu không hợp lệ hoặc không thể phân tích lúc này.";
          return;
        }
        const summary = await response.json();
        if (summary.source !== "USER_SUPPLIED_UNVERIFIED") throw new Error("Unexpected provenance");
        const hint = Number.isInteger(summary.candidateHourUtc) &&
            Number.isInteger(summary.supportingPosts)
          ? "Giờ UTC có tín hiệu từ mẫu: " + summary.candidateHourUtc +
            ":00 (" + summary.supportingPosts + " bài), không bảo đảm hiệu quả."
          : "Chưa đủ dữ liệu cùng một nền tảng để gợi ý giờ UTC.";
        batchResult.textContent = [
          "Nguồn: dữ liệu tự nhập, chưa xác minh",
          "Số bài: " + summary.postCount,
          "Lượt xem: " + summary.totalViews,
          "Lượt tương tác: " + summary.totalInteractions,
          "Tương tác / 100 lượt xem: " + summary.interactionsPer100Views,
          hint,
          String(summary.caveat || "")
        ].join("\n");
        batchStatus.textContent = "Đã tính xong. Đây chỉ là phân tích mô tả dữ liệu bạn nhập.";
      } catch (_) {
        batchStatus.textContent = "Không thể kết nối dịch vụ phân tích.";
      } finally {
        batchButton.disabled = false;
      }
    });
  }

});
