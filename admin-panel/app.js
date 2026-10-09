/* loader 24 parts - full admin UI + hosting auth */
(async function () {
  try {
    const parts = await Promise.all([
      fetch("./app.part01.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p01 "+r.status); return r.text(); }),
      fetch("./app.part02.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p02 "+r.status); return r.text(); }),
      fetch("./app.part03.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p03 "+r.status); return r.text(); }),
      fetch("./app.part04.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p04 "+r.status); return r.text(); }),
      fetch("./app.part05.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p05 "+r.status); return r.text(); }),
      fetch("./app.part06.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p06 "+r.status); return r.text(); }),
      fetch("./app.part07.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p07 "+r.status); return r.text(); }),
      fetch("./app.part08.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p08 "+r.status); return r.text(); }),
      fetch("./app.part09.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p09 "+r.status); return r.text(); }),
      fetch("./app.part10.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p10 "+r.status); return r.text(); }),
      fetch("./app.part11.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p11 "+r.status); return r.text(); }),
      fetch("./app.part12.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p12 "+r.status); return r.text(); }),
      fetch("./app.part13.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p13 "+r.status); return r.text(); }),
      fetch("./app.part14.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p14 "+r.status); return r.text(); }),
      fetch("./app.part15.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p15 "+r.status); return r.text(); }),
      fetch("./app.part16.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p16 "+r.status); return r.text(); }),
      fetch("./app.part17.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p17 "+r.status); return r.text(); }),
      fetch("./app.part18.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p18 "+r.status); return r.text(); }),
      fetch("./app.part19.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p19 "+r.status); return r.text(); }),
      fetch("./app.part20.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p20 "+r.status); return r.text(); }),
      fetch("./app.part21.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p21 "+r.status); return r.text(); }),
      fetch("./app.part22.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p22 "+r.status); return r.text(); }),
      fetch("./app.part23.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p23 "+r.status); return r.text(); }),
      fetch("./app.part24.js", { cache: "no-store" }).then(r => { if (!r.ok) throw new Error("p24 "+r.status); return r.text(); })
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
