/* loader: full admin app */
(async function () {
  try {
    const [a, b] = await Promise.all([
      fetch("./app.part1.js", { cache: "no-store" }).then((r) => r.text()),
      fetch("./app.part2.js", { cache: "no-store" }).then((r) => r.text()),
    ]);
    const s = document.createElement("script");
    s.textContent = a + b;
    document.head.appendChild(s);
  } catch (e) {
    console.error(e);
    const msg = document.querySelector("#auth-message");
    if (msg) msg.textContent = "تعذر تحميل سكربت اللوحة: " + e.message;
  }
})();
