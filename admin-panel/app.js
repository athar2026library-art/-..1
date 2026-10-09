/* loader: full admin app (3 parts) */
(async function () {
  try {
    const parts = await Promise.all([
      fetch("./app.part1.js", { cache: "no-store" }).then((r) => { if (!r.ok) throw new Error("part1 " + r.status); return r.text(); }),
      fetch("./app.part2.js", { cache: "no-store" }).then((r) => { if (!r.ok) throw new Error("part2 " + r.status); return r.text(); }),
      fetch("./app.part3.js", { cache: "no-store" }).then((r) => { if (!r.ok) throw new Error("part3 " + r.status); return r.text(); }),
    ]);
    const s = document.createElement("script");
    s.textContent = parts.join("");
    document.head.appendChild(s);
  } catch (e) {
    console.error(e);
    const msg = document.querySelector("#auth-message");
    if (msg) msg.textContent = "تعذر تحميل سكربت اللوحة: " + (e && e.message ? e.message : e);
  }
})();
