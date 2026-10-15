/* Core admin panel: auth + data + table. Category UI also covered by app.overrides.js */
(function () {
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => document.querySelectorAll(s);
  function escapeHtml(v) {
    return String(v ?? "").replace(/&/g,"&").replace(/</g,"<").replace(/>/g,">").replace(/"/g,""");
  }
  function toast(msg) {
    const el = $("#toast");
    if (!el) return;
    el.textContent = msg;
    el.classList.add("show");
    setTimeout(() => el.classList.remove("show"), 2400);
  }
  window.__adminToast = toast;

  let db, auth, azkar = [], categories = [];
  const BUILTIN = [
    {id:"sabah",title:"أذكار الصباح",icon:"sun",builtin:true},
    {id:"masaa",title:"أذكار المساء",icon:"moon",builtin:true},
    {id:"sleep",title:"أذكار النوم",icon:"bed",builtin:true},
    {id:"travel",title:"أذكار السفر",icon:"car",builtin:true}
  ];
  const LABEL = {sabah:"الصباح",masaa:"المساء",sleep:"النوم",travel:"السفر",worry:"الهم",rizq:"الرزق",illness:"المرض",waking:"الاستيقاظ"};

  function itemsCol() { return db.collection("content").doc("azkar").collection("items"); }
  function catsCol() { return db.collection("content").doc("categories").collection("items"); }

  function go(view) {
    $$(".view").forEach(x => x.classList.remove("active-view"));
    $("#"+view)?.classList.add("active-view");
    $$(".nav-item").forEach(x => x.classList.toggle("active", x.dataset.view === view));
    const titles = {overview:"نظرة عامة",azkar:"مكتبة الأذكار",feedback:"صندوق الرسائل",notify:"إشعار عام",categories:"التصنيفات",settings:"إعدادات وأمان"};
    if ($("#page-title")) $("#page-title").textContent = titles[view] || "لوحة التحكم";
  }
  window.__adminGo = go;

  function renderStats() {
    const pub = azkar.filter(z => z.published).length;
    if ($("#stat-total")) $("#stat-total").textContent = String(azkar.length);
    if ($("#stat-published")) $("#stat-published").textContent = String(pub);
    if ($("#stat-draft")) $("#stat-draft").textContent = String(azkar.length - pub);
  }

  function renderTable() {
    const q = ($("#search")?.value || "").trim();
    const filter = $("#filter")?.value || "all";
    const status = $("#status-filter")?.value || "all";
    const rows = azkar.slice().sort((a,b)=>(a.order||0)-(b.order||0)).filter(z =>
      (filter==="all"||z.category===filter) &&
      (status==="all"||(status==="published"?z.published:!z.published)) &&
      ((z.text||"")+(z.source||"")).includes(q)
    );
    const tbody = $("#azkar-table");
    if (!tbody) return;
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px">لا توجد أذكار مطابقة.</td></tr>';
      return;
    }
    tbody.innerHTML = rows.map(z => `<tr>
      <td>${z.order??0}</td>
      <td>${escapeHtml((z.text||"").slice(0,90))}${(z.text||"").length>90?"…":""}</td>
      <td><span class="pill">${escapeHtml(LABEL[z.category]||z.category)}</span></td>
      <td>${Number(z.repeat)||1}×</td>
      <td><span class="pill ${z.published?"status-published":"status-draft"}">${z.published?"منشور":"مسودة"}</span></td>
      <td>${escapeHtml(z.source||"—")}</td>
      <td><button class="icon-btn" data-edit="${escapeHtml(z.id)}">تعديل</button></td>
    </tr>`).join("");
    $$("[data-edit]").forEach(btn => btn.addEventListener("click", () => openEditor(btn.dataset.edit)));
  }

  function openEditor(id) {
    const z = id ? azkar.find(x => x.id === id) : null;
    if ($("#modal-title")) $("#modal-title").textContent = z ? "تعديل ذكر" : "إضافة ذكر";
    if ($("#field-text")) $("#field-text").value = z?.text || "";
    if ($("#field-category")) $("#field-category").value = z?.category || "sabah";
    if ($("#field-repeat")) $("#field-repeat").value = z?.repeat || 1;
    if ($("#field-fadl")) $("#field-fadl").value = z?.fadl || "";
    if ($("#field-source")) $("#field-source").value = z?.source || "";
    if ($("#field-published")) $("#field-published").checked = z ? !!z.published : true;
    window.__editingZekrId = id || null;
    const m = $("#editor-modal");
    m?.classList.add("open");
    m?.setAttribute("aria-hidden","false");
  }
  window.__openEditor = openEditor;

  async function saveEditor() {
    const text = ($("#field-text")?.value || "").trim();
    if (!text) { toast("يرجى كتابة نص الذكر"); return; }
    const payload = {
      text,
      category: $("#field-category")?.value || "sabah",
      repeat: Math.min(1000, Math.max(1, Number($("#field-repeat")?.value) || 1)),
      fadl: ($("#field-fadl")?.value || "").trim(),
      source: ($("#field-source")?.value || "").trim(),
      published: !!$("#field-published")?.checked,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp()
    };
    const id = window.__editingZekrId;
    if (id) {
      await itemsCol().doc(id).set(payload, {merge:true});
      toast("تم حفظ التعديل");
    } else {
      const maxOrder = azkar.reduce((m,z)=>Math.max(m,Number(z.order)||0),0);
      await itemsCol().add({...payload, order: maxOrder+1, createdAt: firebase.firestore.FieldValue.serverTimestamp()});
      toast("تمت إضافة الذكر");
    }
    $("#editor-modal")?.classList.remove("open");
  }
  window.__saveEditor = saveEditor;

  function listen() {
    itemsCol().onSnapshot(snap => {
      azkar = snap.docs.map(doc => {
        const d = doc.data() || {};
        return {id:doc.id, text:d.text||"", category:d.category||"sabah", repeat:d.repeat||1, fadl:d.fadl||"", source:d.source||"", published:d.published!==false, order:d.order??0};
      });
      renderTable();
      renderStats();
    }, e => toast("تعذر تحميل الأذكار: "+e.message));

    catsCol().onSnapshot(snap => {
      const remote = snap.docs.map(doc => ({id:doc.id, ...doc.data()}));
      const map = new Map(BUILTIN.map(c => [c.id,{...c,name:c.title,desc:c.subtitle||""}]));
      remote.forEach(c => {
        if (!c.id) return;
        const title = c.title || c.name || c.id;
        map.set(c.id, {id:c.id, title, name:title, subtitle:c.subtitle||c.desc||"", desc:c.subtitle||c.desc||"", icon:c.icon||"star", order:c.order??100, published:c.published!==false, builtin:!!c.builtin||BUILTIN.some(b=>b.id===c.id)});
      });
      categories = Array.from(map.values()).sort((a,b)=>(a.order||0)-(b.order||0));
      categories.forEach(c => LABEL[c.id] = c.title || c.name);
      const grid = $("#category-grid");
      if (grid) {
        const iconEmoji = {sun:"☀",moon:"☾",bed:"🛏",car:"🚗",heart:"❤",book:"📖",star:"⭐",home:"🏠",food:"🍽",rain:"🌧",shield:"🛡",mosque:"🕌"};
        grid.innerHTML = categories.map(c => {
          const count = azkar.filter(z => z.category===c.id).length;
          const badge = c.builtin ? '<span class="pill">مدمج</span>' : '<span class="pill status-published">ديناميكي</span>';
          return `<article class="category-card" data-cat-id="${escapeHtml(c.id)}">
            <div class="symbol">${escapeHtml(iconEmoji[c.icon]||c.icon||"⭐")}</div>
            <h3>${escapeHtml(c.title||c.name)}</h3>
            <p>${escapeHtml(c.subtitle||c.desc||"")}</p>
            <p style="margin-top:12px;color:var(--green)">${count} أذكار · ${badge}</p>
            <div class="modal-actions" style="margin-top:12px">
              <button class="icon-btn" data-edit-cat="${escapeHtml(c.id)}">تعديل</button>
            </div>
          </article>`;
        }).join("");
      }
      const sel = $("#field-category");
      if (sel) {
        const cur = sel.value;
        sel.innerHTML = categories.map(c => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.title||c.name)}</option>`).join("");
        if (categories.some(c=>c.id===cur)) sel.value = cur;
      }
      const filter = $("#filter");
      if (filter) {
        const cur = filter.value;
        filter.innerHTML = '<option value="all">الكل</option>' + categories.map(c => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.title||c.name)}</option>`).join("");
        if (cur==="all"||categories.some(c=>c.id===cur)) filter.value = cur;
      }
    });
  }

  function bindCore() {
    $$(".nav-item").forEach(btn => btn.addEventListener("click", () => go(btn.dataset.view)));
    $$("button[data-view]").forEach(btn => {
      if (!btn.classList.contains("nav-item")) btn.addEventListener("click", () => go(btn.dataset.view));
    });
    $("#add-zekr")?.addEventListener("click", () => openEditor(null));
    $("#save-zekr")?.addEventListener("click", () => saveEditor().catch(e => toast(e.message)));
    $("#close-modal")?.addEventListener("click", () => $("#editor-modal")?.classList.remove("open"));
    $("#cancel-modal")?.addEventListener("click", () => $("#editor-modal")?.classList.remove("open"));
    $("#search")?.addEventListener("input", renderTable);
    $("#filter")?.addEventListener("change", renderTable);
    $("#status-filter")?.addEventListener("change", renderTable);
  }

  const initializeAdminPanel = async () => {
    if (window.firebaseHostingConfigPromise) await window.firebaseHostingConfigPromise.catch(() => null);
    const cfg = window.firebaseConfig || window.AZKAR_FIREBASE_CONFIG;
    if (!window.firebase || !cfg) {
      const msg = $("#auth-message");
      if (msg) msg.textContent = "تعذر تحميل إعداد Firebase.";
      return;
    }
    if (!firebase.apps.length) firebase.initializeApp(cfg);
    db = firebase.firestore();
    auth = firebase.auth();
    window.__adminDb = db;
    window.__adminAuth = auth;
    bindCore();

    auth.onAuthStateChanged(async (user) => {
      const gate = $("#auth-gate");
      const shell = $("#shell");
      if (!user) {
        gate?.classList.remove("hidden");
        if (gate) gate.style.display = "grid";
        shell?.classList.remove("ready");
        return;
      }
      try {
        const token = await user.getIdTokenResult(true);
        let ok = token.claims.super_admin === true || token.claims.admin === true;
        if (!ok) {
          const snap = await db.collection("admins").doc(user.uid).get();
          ok = snap.exists;
        }
        if (!ok) {
          toast("ليس لديك صلاحية مشرف");
          await auth.signOut();
          return;
        }
        gate?.classList.add("hidden");
        if (gate) gate.style.display = "none";
        shell?.classList.add("ready");
        if ($("#admin-email")) $("#admin-email").textContent = user.email || user.uid;
        listen();
      } catch (e) {
        toast(e.message || "خطأ في التحقق");
      }
    });

    $("#google-login-btn")?.addEventListener("click", async () => {
      const provider = new firebase.auth.GoogleAuthProvider();
      try { await auth.signInWithPopup(provider); }
      catch (e) {
        if ((e.code||"").includes("popup")) {
          try { await auth.signInWithRedirect(provider); } catch (re) { toast(re.message); }
        } else toast(e.message || "فشل الدخول");
      }
    });
    $("#logout-btn")?.addEventListener("click", () => auth.signOut());
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initializeAdminPanel, { once: true });
  } else {
    initializeAdminPanel();
  }
})();
