      toast("لا يوجد ذكر للمعاينة");
      return;
    }
    $("#preview-meta").textContent = `${CATEGORY_LABEL[z.category] || z.category} · ${z.repeat || 1}×`;
    $("#preview-zekr").textContent = z.text || "";
    $("#preview-fadl").textContent = z.fadl || "—";
    $("#preview-source").textContent = z.source || "—";
    const modal = $("#preview-modal");
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
  }

  function closePreview() {
    const modal = $("#preview-modal");
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
  }

  function exportJson() {
    const blob = new Blob([JSON.stringify(azkar, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "baqiyat-azkar.json";
    a.click();
    URL.revokeObjectURL(url);
    toast("تم تصدير المكتبة");
  }

  async function sendNotify() {
    const now = Date.now();
    if (now - lastNotifyAt < 60000) {
      toast("انتظر دقيقة قبل إرسال إشعار آخر");
      return;
    }
    const title = $("#notify-title")?.value.trim();
