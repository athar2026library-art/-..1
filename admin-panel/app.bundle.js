/* الباقيات — لوحة المالك (تفاعلية + وصولية + ميزات عملية) */
(function () {
  "use strict";
  const $ = (s, root) => (root || document).querySelector(s);
  const $$ = (s, root) => Array.from((root || document).querySelectorAll(s));

  const ALLOWED_ICONS = ["sun","moon","bed","car","heart","book","star","home","food","rain","shield","mosque"];
  const ICON_EMOJI = {sun:"☀",moon:"☾",bed:"🛏",car:"🚗",heart:"❤",book:"📖",star:"⭐",home:"🏠",food:"🍽",rain:"🌧",shield:"🛡",mosque:"🕌"};
  const RESERVED = ["sabah","masaa","sleep","travel","worry","rizq","illness","waking","clothing","toilet","wudu","home","mosque","prayer","food","distress","weather","anger","fasting","gathering","children","pain","hisn","favorites"];
  const BUILTIN = [
    {id:"sabah",icon:"sun",title:"أذكار الصباح",subtitle:"ورد الصباح",order:0,builtin:true},
    {id:"masaa",icon:"moon",title:"أذكار المساء",subtitle:"ورد المساء",order:1,builtin:true},
    {id:"sleep",icon:"bed",title:"أذكار النوم",subtitle:"طمأنينة قبل النوم",order:2,builtin:true},
    {id:"travel",icon:"car",title:"أذكار السفر",subtitle:"حفظ وأمان",order:3,builtin:true},
    {id:"worry",icon:"heart",title:"الهم والقلق",subtitle:"أذكار عند الضيق والخوف",order:4,builtin:true},
    {id:"rizq",icon:"food",title:"الرزق",subtitle:"دعاء بسعة الخير",order:5,builtin:true},
    {id:"illness",icon:"heart",title:"المرض والشفاء",subtitle:"دعاء للمريض",order:6,builtin:true},
    {id:"waking",icon:"sun",title:"الاستيقاظ",subtitle:"ذكر بداية اليوم",order:7,builtin:true},
    {id:"clothing",icon:"book",title:"الملابس",subtitle:"أذكار اللباس",order:8,builtin:true},
    {id:"toilet",icon:"home",title:"دخول الخلاء والخروج منه",subtitle:"أذكار قضاء الحاجة",order:9,builtin:true},
    {id:"wudu",icon:"shield",title:"الوضوء",subtitle:"الذكر بعد الوضوء",order:10,builtin:true},
    {id:"home",icon:"home",title:"المنزل",subtitle:"الذكر عند الدخول والخروج",order:11,builtin:true},
    {id:"mosque",icon:"mosque",title:"المسجد",subtitle:"أذكار الدخول والخروج",order:12,builtin:true},
    {id:"prayer",icon:"book",title:"بعد الصلاة",subtitle:"أذكار ما بعد السلام",order:13,builtin:true},
    {id:"food",icon:"food",title:"الطعام والشراب",subtitle:"الذكر قبل الطعام وبعده",order:14,builtin:true},
    {id:"distress",icon:"heart",title:"الكرب والشدة",subtitle:"أدعية عند الكرب",order:15,builtin:true},
    {id:"weather",icon:"rain",title:"المطر",subtitle:"أدعية المطر",order:16,builtin:true},
    {id:"anger",icon:"heart",title:"الغضب",subtitle:"ما يقال عند الغضب",order:17,builtin:true},
    {id:"fasting",icon:"moon",title:"الصيام والإفطار",subtitle:"ذكر الإفطار",order:18,builtin:true},
    {id:"gathering",icon:"star",title:"المجالس والآداب",subtitle:"كفارة المجلس",order:19,builtin:true},
    {id:"children",icon:"shield",title:"تحصين الأولاد",subtitle:"دعاء التعويذ",order:20,builtin:true},
    {id:"pain",icon:"heart",title:"الألم والوجع",subtitle:"دعاء عند ألم الجسد",order:21,builtin:true},
    {id:"hisn",icon:"book",title:"حصن المسلم",subtitle:"133 فصلًا · 302 ذكر من الكتاب والسنة",order:22,builtin:true}
  ];

  let db=null, auth=null, azkar=[], categories=[...BUILTIN], feedbackItems=[];
  let hisnBook=null;
  let selectedFeedbackId=null, editingZekrId=null, editingCategoryId=null;
  let lastNotifyAt=0, unsubAzkar=null, unsubFeedback=null, unsubCategories=null, busy=false;

  function escapeHtml(v){
    return String(v??"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;");
  }
  function toast(msg, type){
    const el=$("#toast"); if(!el) return;
    el.textContent=msg;
    el.classList.remove("toast-ok","toast-err");
    if(type==="ok") el.classList.add("toast-ok");
    if(type==="err") el.classList.add("toast-err");
    el.classList.add("show");
    clearTimeout(toast._t);
    toast._t=setTimeout(()=>el.classList.remove("show"),2800);
  }
  function setBusy(on, btn){
    busy=!!on;
    if(!btn) return;
    btn.disabled=!!on;
    btn.setAttribute("aria-busy", on?"true":"false");
    if(on){ btn.dataset.prevText=btn.textContent; btn.textContent="جاري..."; }
    else if(btn.dataset.prevText){ btn.textContent=btn.dataset.prevText; }
  }
  function itemsCol(){ return db.collection("content").doc("azkar").collection("items"); }
  function catsCol(){ return db.collection("content").doc("categories").collection("items"); }

  function go(view){
    $$(".view").forEach(x=>{ x.classList.remove("active-view"); x.setAttribute("hidden",""); });
    const t=$("#"+view);
    if(t){ t.classList.add("active-view"); t.removeAttribute("hidden"); }
    $$(".nav-item").forEach(x=>{
      const a=x.dataset.view===view;
      x.classList.toggle("active",a);
      x.setAttribute("aria-current", a?"page":"false");
    });
    const titles={overview:"نظرة عامة",azkar:"مكتبة الأذكار",hisn:"حصن المسلم",feedback:"صندوق الرسائل",notify:"إشعار عام",categories:"التصنيفات",settings:"إعدادات وأمان"};
    if($("#page-title")) $("#page-title").textContent=titles[view]||"لوحة التحكم";
  }
  function openModal(sel){
    const m=$(sel); if(!m) return;
    m.classList.add("open"); m.setAttribute("aria-hidden","false");
    const f=m.querySelector("input,textarea,select,button:not(.close)");
    setTimeout(()=>f&&f.focus(),40);
  }
  function closeModal(sel){
    const m=$(sel); if(!m) return;
    m.classList.remove("open"); m.setAttribute("aria-hidden","true");
  }

  function renderStats(){
    const pub=azkar.filter(z=>z.published).length;
    if($("#stat-total")) $("#stat-total").textContent=String(azkar.length);
    if($("#stat-published")) $("#stat-published").textContent=String(pub);
    if($("#stat-draft")) $("#stat-draft").textContent=String(azkar.length-pub);
    const unread=feedbackItems.filter(f=>f.status==="new"||!f.adminReply).length;
    if($("#stat-feedback")) $("#stat-feedback").textContent=String(unread);
    const badge=$("#feedback-badge");
    if(badge){ badge.textContent=String(unread); badge.classList.toggle("hidden", unread===0); }
  }

  function mergeCategories(remote){
    const map=new Map();
    BUILTIN.forEach(c=>map.set(c.id,{...c,name:c.title,desc:c.subtitle}));
    (remote||[]).forEach(c=>{
      if(!c||!c.id) return;
      // فصول الكتاب مراجع داخلية وليست تصنيفات عامة في الصفحة الرئيسية للوحة.
      if(String(c.id).startsWith("hisn_")) return;
      // التصنيفات المدمجة مصدرها نسخة التطبيق؛ لا تستبدلها وثائق Firestore.
      if(BUILTIN.some(b=>b.id===c.id)) return;
      const title=c.title||c.name||c.id;
      const subtitle=c.subtitle||c.desc||"";
      map.set(c.id,{
        id:c.id, icon:ALLOWED_ICONS.includes(c.icon)?c.icon:(c.icon||"star"),
        title, name:title, subtitle, desc:subtitle,
        order:c.order??100, published:c.published!==false,
        builtin:!!c.builtin||BUILTIN.some(b=>b.id===c.id)
      });
    });
    categories=Array.from(map.values()).sort((a,b)=>(a.order??0)-(b.order??0));
  }

  function isBookChapterId(id){
    return !!hisnBook?.chapters?.some(ch=>ch.id===id);
  }
  function categoryTitleFor(id){
    const ordinary=categories.find(c=>c.id===id);
    if(ordinary) return ordinary.title||ordinary.name||id;
    const chapter=hisnBook?.chapters?.find(ch=>ch.id===id);
    return chapter ? chapter.title : id;
  }
  function optionChapterMarkup(){
    return (hisnBook?.chapters||[]).map(ch=>
      `<option value="${escapeHtml(ch.id)}">${ch.order}. ${escapeHtml(ch.title)}</option>`
    ).join("");
  }
  function hasKnownCategory(id){
    return categories.some(c=>c.id===id)||isBookChapterId(id);
  }
  function renderCategories(){
    const grid=$("#category-grid"); if(!grid) return;
    if(!categories.length){ grid.innerHTML='<p class="empty-hint">لا تصنيفات بعد.</p>'; return; }
    grid.innerHTML=categories.map(c=>{
      const count=c.id==="hisn"?importedHisnCount():azkar.filter(z=>z.category===c.id).length;
      const badge=c.builtin?'<span class="pill">مدمج في التطبيق</span>':'<span class="pill status-published">ديناميكي</span>';
      const ic=ICON_EMOJI[c.icon]||c.icon||"⭐";
      return `<article class="category-card" data-cat-id="${escapeHtml(c.id)}" tabindex="0">
        <div class="symbol" aria-hidden="true">${escapeHtml(ic)}</div>
        <h3>${escapeHtml(c.title||c.name)}</h3>
        <p>${escapeHtml(c.subtitle||c.desc||"")}</p>
        <p style="margin-top:12px;color:var(--green)">${count} سجلات في Firestore · ${badge}</p>
        <div class="modal-actions" style="margin-top:12px">
          ${c.builtin
            ? `<button type="button" class="icon-btn" data-open-category="${escapeHtml(c.id)}">عرض القسم في المكتبة</button>`
            : `<button type="button" class="icon-btn" data-edit-cat="${escapeHtml(c.id)}">تعديل</button>
               <button type="button" class="icon-btn" data-del-cat="${escapeHtml(c.id)}">حذف</button>`}
        </div>
      </article>`;
    }).join("");
    $$("[data-edit-cat]",grid).forEach(b=>b.addEventListener("click",()=>openCategoryEditor(b.dataset.editCat)));
    $$("[data-del-cat]",grid).forEach(b=>b.addEventListener("click",()=>deleteCategory(b.dataset.delCat)));
    $$("[data-open-category]",grid).forEach(b=>b.addEventListener("click",()=>{
      const category=b.dataset.openCategory;
      if(category==="hisn"){ go("hisn"); renderHisnChapters(); return; }
      go("azkar");
      const filter=$("#filter");
      if(filter){ filter.value=category; renderTable(); }
      const field=$("#field-category");
      if(field) field.value=category;
    }));
    const sel=$("#field-category");
    if(sel){
      const cur=sel.value;
      sel.innerHTML=categories.map(c=>`<option value="${escapeHtml(c.id)}">${escapeHtml(c.title||c.name)}</option>`).join("")+
        ((hisnBook?.chapters?.length)?`<optgroup label="فصول حصن المسلم">${optionChapterMarkup()}</optgroup>`:"");
      if(hasKnownCategory(cur)) sel.value=cur;
    }
    const filter=$("#filter");
    if(filter){
      const cur=filter.value;
      filter.innerHTML='<option value="all">كل التصنيفات</option>'+categories.map(c=>`<option value="${escapeHtml(c.id)}">${escapeHtml(c.title||c.name)}</option>`).join("")+
        ((hisnBook?.chapters?.length)?`<optgroup label="فصول حصن المسلم">${optionChapterMarkup()}</optgroup>`:"");
      if(cur==="all"||hasKnownCategory(cur)) filter.value=cur;
    }
  }

  function openCategoryEditor(id){
    editingCategoryId=id||null;
    const c=id?categories.find(x=>x.id===id):null;
    if($("#cat-modal-title")) $("#cat-modal-title").textContent=c?"تعديل التصنيف":"إضافة تصنيف";
    const idEl=$("#cat-id");
    if(idEl){ idEl.value=c?.id||""; idEl.disabled=!!c; }
    if($("#cat-title")) $("#cat-title").value=c?.title||c?.name||"";
    if($("#cat-subtitle")) $("#cat-subtitle").value=c?.subtitle||c?.desc||"";
    if($("#cat-order")) $("#cat-order").value=c?.order??100;
    if($("#cat-icon")){ const ic=c?.icon||"star"; $("#cat-icon").value=ALLOWED_ICONS.includes(ic)?ic:"star"; }
    if($("#cat-published")) $("#cat-published").checked=c?c.published!==false:true;
    openModal("#category-modal");
  }

  async function saveCategory(){
    if(busy) return;
    const idRaw=($("#cat-id")?.value||"").trim().toLowerCase();
    const title=($("#cat-title")?.value||"").trim();
    if(!idRaw||!title){ toast("أدخل المعرّف والعنوان","err"); return; }
    if(!/^[a-z0-9_-]{2,30}$/.test(idRaw)||idRaw.startsWith("wird_")||idRaw.startsWith("hisn_")){ toast("المعرّف: 2–30 حرفاً إنجليزياً صغيراً؛ hisn_ محجوز لفصول الكتاب","err"); return; }
    if(RESERVED.includes(idRaw)&&!editingCategoryId){ toast("معرّف محجوز","err"); return; }
    const iconRaw=($("#cat-icon")?.value||"star").trim();
    const icon=ALLOWED_ICONS.includes(iconRaw)?iconRaw:"star";
    const subtitle=(($("#cat-subtitle")?.value||"").trim()).slice(0,80);
    const orderNum=Number($("#cat-order")?.value);
    const payload={
      title:title.slice(0,40), subtitle, icon,
      order:Number.isFinite(orderNum)?orderNum:100,
      published:!!$("#cat-published")?.checked,
      updatedAt:firebase.firestore.FieldValue.serverTimestamp()
    };
    const btn=$("#save-category"); setBusy(true,btn);
    try{
      if(editingCategoryId){
        await catsCol().doc(editingCategoryId).set(payload);
        toast("تم حفظ التصنيف","ok");
      } else {
        if(categories.some(c=>c.id===idRaw)){ toast("المعرّف موجود","err"); return; }
        await catsCol().doc(idRaw).set({...payload, createdAt:firebase.firestore.FieldValue.serverTimestamp()});
        toast("تمت إضافة التصنيف","ok");
      }
      closeModal("#category-modal"); editingCategoryId=null;
    }catch(e){ toast(e.message||"تعذر الحفظ","err"); }
    finally{ setBusy(false,btn); }
  }

  async function deleteCategory(id){
    if(!id||BUILTIN.some(b=>b.id===id)){ toast("لا يمكن حذف المدمج","err"); return; }
    const count=azkar.filter(z=>z.category===id).length;
    if(count>0){ toast(`لا يمكن الحذف: ${count} ذكر مرتبط`,"err"); return; }
    if(!confirm("حذف هذا التصنيف؟")) return;
    try{ await catsCol().doc(id).delete(); toast("تم الحذف","ok"); }
    catch(e){ toast(e.message||"تعذر الحذف","err"); }
  }

  function importedHisnCount(chapterId){
    if(!hisnBook?.chapters) return 0;
    const existingIds=new Set(azkar.map(z=>String(z.id)));
    const chapters=chapterId?hisnBook.chapters.filter(ch=>ch.id===chapterId):hisnBook.chapters;
    return chapters.reduce((sum,ch)=>sum+(ch.adhkar||[]).filter(item=>existingIds.has(`hisn_${item.id}`)).length,0);
  }

  function renderHisnBookStats(){
    const chapters=hisnBook?.chapters||[];
    const total=Number(hisnBook?.book?.dhikrCount)||chapters.reduce((sum,ch)=>sum+(ch.adhkar||[]).length,0);
    const stored=importedHisnCount();
    if($("#hisn-stat-chapters")) $("#hisn-stat-chapters").textContent=String(chapters.length||"—");
    if($("#hisn-stat-adhkar")) $("#hisn-stat-adhkar").textContent=String(total||"—");
    if($("#hisn-stat-imported")) $("#hisn-stat-imported").textContent=chapters.length?`${stored} / ${total}`:"—";
    const status=$("#hisn-import-status");
    if(status&&hisnBook){
      status.textContent=stored===total
        ?`تم رصد ${stored} ذكرًا في Firestore. يمكنك إعادة الاستيراد لتحديث النصوص والمراجع دون إنشاء نسخ إضافية.`
        :`البيانات جاهزة: ${chapters.length} فصلًا و${total} ذكرًا. الموجود حاليًا في Firestore: ${stored}. اضغط زر الاستيراد لنشر/تحديث الكتاب.`;
    }
  }

  function renderHisnChapters(){
    const list=$("#hisn-chapters-list");
    if(!list) return;
    if(!hisnBook?.chapters){
      list.innerHTML='<p class="empty-hint">فهرس الكتاب غير محمّل. حدّث الصفحة أو راجع ملف hisn_al_muslim.json.</p>';
      renderHisnBookStats();
      return;
    }
    const q=($("#hisn-chapter-search")?.value||"").trim().toLocaleLowerCase();
    const chapters=hisnBook.chapters.filter(ch=>!q||String(ch.title||"").toLocaleLowerCase().includes(q));
    if(!chapters.length){ list.innerHTML='<p class="empty-hint">لا توجد فصول مطابقة.</p>'; return; }
    list.innerHTML=chapters.map(ch=>{
      const count=(ch.adhkar||[]).length;
      const stored=importedHisnCount(ch.id);
      const badge=stored===count?'<span class="pill status-published">موجود في Firestore</span>':'<span class="pill">غير مستورد بالكامل</span>';
      const audio=ch.audioUrl?'<span class="pill">يتوفر تسجيل للفصل</span>':"";
      return `<article class="category-card">
        <div class="symbol" aria-hidden="true">📖</div>
        <h3>${ch.order}. ${escapeHtml(ch.title)}</h3>
        <p>${count} ذكرًا · ${stored} في Firestore</p>
        <p>${badge} ${audio}</p>
        <div class="modal-actions" style="margin-top:12px">
          <button type="button" class="icon-btn" data-hisn-open="${escapeHtml(ch.id)}">عرض في المكتبة</button>
          ${ch.audioUrl?`<a class="icon-btn" href="${escapeHtml(ch.audioUrl)}" target="_blank" rel="noopener noreferrer">استماع لتسجيل الفصل</a>`:""}
        </div>
      </article>`;
    }).join("");
    $$("[data-hisn-open]",list).forEach(btn=>btn.addEventListener("click",()=>{
      const category=btn.dataset.hisnOpen;
      go("azkar");
      const filter=$("#filter");
      if(filter){ filter.value=category; renderTable(); }
      const field=$("#field-category");
      if(field) field.value=category;
    }));
    renderHisnBookStats();
  }

  async function loadHisnBook(){
    if(hisnBook) return hisnBook;
    const response=await fetch("./hisn_al_muslim.json",{cache:"no-store"});
    if(!response.ok) throw new Error("تعذر تحميل ملف حصن المسلم: HTTP "+response.status);
    const data=await response.json();
    if(!data?.book||!Array.isArray(data.chapters)) throw new Error("بنية ملف حصن المسلم غير صحيحة");
    const count=data.chapters.reduce((sum,ch)=>sum+(Array.isArray(ch.adhkar)?ch.adhkar.length:0),0);
    if(data.chapters.length!==133||count!==302) throw new Error(`فشل فحص فهرس الكتاب: ${data.chapters.length} فصلًا و${count} ذكرًا`);
    hisnBook=data;
    renderHisnBookStats();
    renderHisnChapters();
    renderCategories();
    renderTable();
    return hisnBook;
  }

  async function importHisnBook(){
    if(busy) return;
    const btn=$("#import-hisn-book");
    setBusy(true,btn);
    try{
      await loadHisnBook();
      const chapters=hisnBook.chapters;
      const total=chapters.reduce((sum,ch)=>sum+(ch.adhkar||[]).length,0);
      const already=azkar.filter(z=>String(z.category||"").startsWith("hisn_")).length;
      if(!confirm(`سيُنشر ${total} ذكرًا في ${chapters.length} فصلًا داخل مكتبة التطبيق، بما في ذلك كل الأذكار والمراجع والتكرارات. الموجود حاليًا: ${already}. ستُحدّث السجلات ذات المعرفات الثابتة بدل تكرارها. هل تتابع؟`)) return;
      // قواعد Firestore تشترط وجود وثيقة تصنيف لكل فئة ليست من الفئات الثماني القديمة.
      // أنشئ مراجع الفصول أولًا؛ لا تضفها إلى قائمة التصنيفات العامة في اللوحة/التطبيق.
      const categoryBatch=db.batch();
      chapters.forEach(ch=>{
        categoryBatch.set(catsCol().doc(ch.id),{
          title:String(ch.title||("فصل "+ch.order)).slice(0,40),
          subtitle:("فصل "+ch.order+" من حصن المسلم").slice(0,80),
          icon:"book",
          order:Number(ch.order)||0,
          published:true,
          updatedAt:firebase.firestore.FieldValue.serverTimestamp()
        },{merge:true});
      });
      await categoryBatch.commit();

      const records=[];
      chapters.forEach(ch=>{
        (ch.adhkar||[]).forEach(item=>{
          const id=Number(item.id);
          if(!Number.isSafeInteger(id)||id<=0||!String(item.text||"").trim()) throw new Error("عنصر غير صالح في الفصل: "+ch.title);
          records.push({
            docId:`hisn_${id}`,
            data:{
              id,
              text:item.text,
              category:ch.id,
              repeat:Math.max(1,Number(item.repeatCount)||1),
              fadl:item.fadl||"",
              source:item.source||"",
              published:true,
              order:Number(ch.order)*1000+Number(item.order||1),
              bookId:"hisn_al_muslim",
              bookTitle:hisnBook.book.title||"حصن المسلم",
              bookChapterTitle:ch.title,
              bookChapterOrder:Number(ch.order),
              bookItemOrder:Number(item.order||1),
              updatedAt:firebase.firestore.FieldValue.serverTimestamp()
            }
          });
        });
      });
      if(records.length!==total) throw new Error("عدد عناصر الاستيراد لا يطابق العدد المتوقع");
      for(let offset=0;offset<records.length;offset+=400){
        const batch=db.batch();
        records.slice(offset,offset+400).forEach(record=>{
          batch.set(itemsCol().doc(record.docId),record.data,{merge:true});
        });
        await batch.commit();
      }
      toast(`تم نشر حصن المسلم: ${total} ذكرًا في ${chapters.length} فصلًا`,"ok");
      renderHisnChapters();
      renderHisnBookStats();
    }catch(e){
      toast(e.message||"تعذر استيراد حصن المسلم","err");
      if($("#hisn-import-status")) $("#hisn-import-status").textContent=e.message||"تعذر استيراد حصن المسلم.";
    }finally{
      setBusy(false,btn);
    }
  }

  function renderTable(){
    const q=($("#search")?.value||"").trim();
    const filter=$("#filter")?.value||"all";
    const status=$("#status-filter")?.value||"all";
    const rows=azkar.slice().sort((a,b)=>(a.order??0)-(b.order??0)).filter(z=>
      (filter==="all"||z.category===filter)&&
      (status==="all"||(status==="published"?z.published:!z.published))&&
      ((z.text||"")+(z.source||"")).includes(q)
    );
    const tbody=$("#azkar-table"); if(!tbody) return;
    if(!rows.length){
      tbody.innerHTML='<tr><td colspan="7" style="text-align:center;padding:24px" class="empty-hint">لا توجد أذكار مطابقة.</td></tr>';
      return;
    }
    tbody.innerHTML=rows.map(z=>`<tr data-id="${escapeHtml(z.id)}">
      <td>${z.order??0}</td>
      <td title="${escapeHtml(z.text||"")}">${escapeHtml((z.text||"").slice(0,90))}${(z.text||"").length>90?"…":""}</td>
      <td><span class="pill">${escapeHtml(categoryTitleFor(z.category))}</span></td>
      <td>${Number(z.repeat)||1}×</td>
      <td><span class="pill ${z.published?"status-published":"status-draft"}">${z.published?"منشور":"مسودة"}</span></td>
      <td>${escapeHtml(z.source||"—")}</td>
      <td class="row-actions">
        <button type="button" class="icon-btn" data-preview="${escapeHtml(z.id)}">معاينة</button>
        <button type="button" class="icon-btn" data-edit="${escapeHtml(z.id)}">تعديل</button>
        <button type="button" class="icon-btn" data-dup="${escapeHtml(z.id)}">نسخ</button>
        <button type="button" class="icon-btn" data-toggle="${escapeHtml(z.id)}">${z.published?"إخفاء":"نشر"}</button>
      </td>
    </tr>`).join("");
    $$("[data-preview]",tbody).forEach(b=>b.addEventListener("click",()=>openPreview(b.dataset.preview)));
    $$("[data-edit]",tbody).forEach(b=>b.addEventListener("click",()=>openEditor(b.dataset.edit)));
    $$("[data-dup]",tbody).forEach(b=>b.addEventListener("click",()=>duplicateZekr(b.dataset.dup)));
    $$("[data-toggle]",tbody).forEach(b=>b.addEventListener("click",()=>togglePublished(b.dataset.toggle)));
  }

  function openEditor(id){
    editingZekrId=id||null;
    const z=id?azkar.find(x=>x.id===id):null;
    if($("#modal-title")) $("#modal-title").textContent=z?"تعديل ذكر":"إضافة ذكر";
    if($("#field-text")) $("#field-text").value=z?.text||"";
    const selectedFilter=$("#filter")?.value;
    const defaultCategory=selectedFilter&&selectedFilter!=="all"&&hasKnownCategory(selectedFilter)?selectedFilter:"sabah";
    if($("#field-category")) $("#field-category").value=z?.category||defaultCategory;
    if($("#field-repeat")) $("#field-repeat").value=z?.repeat||1;
    if($("#field-fadl")) $("#field-fadl").value=z?.fadl||"";
    if($("#field-source")) $("#field-source").value=z?.source||"";
    if($("#field-audio-url")) $("#field-audio-url").value=z?.audioUrl||"";
    if($("#field-audio-mishary")) $("#field-audio-mishary").value=z?.audioUrlsByReciter?.mishary||"";
    if($("#field-audio-abdulbasit")) $("#field-audio-abdulbasit").value=z?.audioUrlsByReciter?.abdulbasit||"";
    if($("#field-audio-maher")) $("#field-audio-maher").value=z?.audioUrlsByReciter?.maher||"";
    if($("#field-audio-yasser")) $("#field-audio-yasser").value=z?.audioUrlsByReciter?.yasser||"";
    if($("#field-published")) $("#field-published").checked=z?!!z.published:true;
    openModal("#editor-modal");
  }

  async function saveEditor(){
    if(busy) return;
    const text=($("#field-text")?.value||"").trim();
    if(!text){ toast("اكتب نص الذكر","err"); $("#field-text")?.focus(); return; }
    const audioUrl=($("#field-audio-url")?.value||"").trim();
    const audioUrlsByReciter=Object.fromEntries([
      ["mishary",($("#field-audio-mishary")?.value||"").trim()],
      ["abdulbasit",($("#field-audio-abdulbasit")?.value||"").trim()],
      ["maher",($("#field-audio-maher")?.value||"").trim()],
      ["yasser",($("#field-audio-yasser")?.value||"").trim()]
    ].filter(([,u])=>u));
    const urls=[audioUrl,...Object.values(audioUrlsByReciter)].filter(Boolean);
    if(urls.some(u=>!/^https:\/\/\S+$/i.test(u))){ toast("روابط الصوت يجب أن تبدأ بـ HTTPS","err"); return; }
    const payload={
      text, category:$("#field-category")?.value||"sabah",
      repeat:Math.min(1000,Math.max(1,Number($("#field-repeat")?.value)||1)),
      fadl:($("#field-fadl")?.value||"").trim(), source:($("#field-source")?.value||"").trim(),
      audioUrl, audioUrlsByReciter, published:!!$("#field-published")?.checked,
      updatedAt:firebase.firestore.FieldValue.serverTimestamp()
    };
    const btn=$("#save-zekr"); setBusy(true,btn);
    try{
      if(editingZekrId){
        await itemsCol().doc(editingZekrId).set(payload,{merge:true});
        toast("تم حفظ التعديل","ok");
      } else {
        const maxOrder=azkar.reduce((m,z)=>Math.max(m,Number(z.order)||0),0);
        await itemsCol().add({...payload, order:maxOrder+1, createdAt:firebase.firestore.FieldValue.serverTimestamp()});
        toast("تمت إضافة الذكر","ok");
      }
      closeModal("#editor-modal"); editingZekrId=null;
    }catch(e){ toast(e.message||"تعذر الحفظ","err"); }
    finally{ setBusy(false,btn); }
  }

  async function duplicateZekr(id){
    const z=azkar.find(x=>x.id===id); if(!z) return;
    try{
      const maxOrder=azkar.reduce((m,i)=>Math.max(m,Number(i.order)||0),0);
      await itemsCol().add({
        text:z.text, category:z.category, repeat:z.repeat, fadl:z.fadl||"", source:z.source||"",
        audioUrl:z.audioUrl||"", audioUrlsByReciter:z.audioUrlsByReciter||{},
        published:false, order:maxOrder+1, createdAt:firebase.firestore.FieldValue.serverTimestamp()
      });
      toast("تم نسخ الذكر كمسودة","ok");
    }catch(e){ toast(e.message||"تعذر النسخ","err"); }
  }

  async function togglePublished(id){
    const z=azkar.find(x=>x.id===id); if(!z) return;
    try{
      await itemsCol().doc(id).update({published:!z.published, updatedAt:firebase.firestore.FieldValue.serverTimestamp()});
      toast(z.published?"تم الإخفاء":"تم النشر","ok");
    }catch(e){ toast(e.message||"تعذر التحديث","err"); }
  }

  function openPreview(id){
    const z=id?azkar.find(x=>x.id===id):azkar[0];
    if(!z){ toast("لا يوجد ذكر","err"); return; }
    if($("#preview-meta")) $("#preview-meta").textContent=(categories.find(c=>c.id===z.category)?.title||z.category)+" · "+(z.published?"منشور":"مسودة")+" · "+(Number(z.repeat)||1)+"×";
    if($("#preview-zekr")) $("#preview-zekr").textContent=z.text||"";
    if($("#preview-fadl")) $("#preview-fadl").textContent=z.fadl||"—";
    if($("#preview-source")) $("#preview-source").textContent=z.source||"—";
    openModal("#preview-modal");
  }

  function renderFeedbackList(){
    const list=$("#feedback-list"); if(!list) return;
    if(!feedbackItems.length){ list.innerHTML='<p class="empty-hint">لا رسائل بعد.</p>'; return; }
    list.innerHTML=feedbackItems.map(f=>{
      const active=f.id===selectedFeedbackId?" active":"";
      return `<button type="button" class="feedback-item${active}" data-fid="${escapeHtml(f.id)}">
        <strong>${escapeHtml(f.title||f.type||"رسالة")}</strong>
        <small>${escapeHtml(f.userEmail||f.userId||"")} · ${escapeHtml(f.status||"new")}</small>
        <span>${escapeHtml((f.message||"").slice(0,80))}</span>
      </button>`;
    }).join("");
    $$("[data-fid]",list).forEach(btn=>btn.addEventListener("click",()=>{
      selectedFeedbackId=btn.dataset.fid; renderFeedbackList(); renderFeedbackDetail();
    }));
  }

  function renderFeedbackDetail(){
    const box=$("#feedback-detail"); if(!box) return;
    const f=feedbackItems.find(x=>x.id===selectedFeedbackId);
    if(!f){ box.innerHTML='<p class="empty-hint">اختر رسالة من القائمة.</p>'; return; }
    box.innerHTML=`
      <p class="eyebrow">${escapeHtml(f.type||"رسالة")}</p>
      <h3>${escapeHtml(f.title||"بدون عنوان")}</h3>
      <small>${escapeHtml(f.userEmail||f.userId||"")}</small>
      <p style="margin-top:14px;line-height:1.8">${escapeHtml(f.message||"")}</p>
      ${f.adminReply?`<div class="preview-block" style="margin-top:16px"><strong>رد المشرف</strong><p>${escapeHtml(f.adminReply)}</p></div>`:""}
      <label style="margin-top:16px">رد المشرف<textarea id="feedback-reply" rows="3">${escapeHtml(f.adminReply||"")}</textarea></label>
      <div class="modal-actions"><button type="button" class="primary" id="save-feedback-reply">حفظ الرد</button></div>`;
    $("#save-feedback-reply")?.addEventListener("click", async ()=>{
      const reply=($("#feedback-reply")?.value||"").trim();
      try{
        await db.collection("feedback").doc(f.id).update({
          adminReply:reply, status:reply?"replied":"open", replyUnread:!!reply,
          updatedAt:firebase.firestore.FieldValue.serverTimestamp()
        });
        toast("تم حفظ الرد","ok");
      }catch(e){ toast(e.message||"تعذر حفظ الرد","err"); }
    });
  }

  async function sendNotify(){
    if(busy) return;
    const title=($("#notify-title")?.value||"").trim();
    const body=($("#notify-body")?.value||"").trim();
    if(!title||!body){ toast("أدخل العنوان والنص","err"); return; }
    if(Date.now()-lastNotifyAt<60000){ toast("انتظر دقيقة بين الإشعارات","err"); return; }
    const btn=$("#send-notify"); setBusy(true,btn);
    try{
      await db.collection("admin_jobs").add({
        type:"broadcast", title, body, topic:"all", status:"pending",
        createdAt:firebase.firestore.FieldValue.serverTimestamp(),
        createdBy:auth.currentUser?.uid||""
      });
      lastNotifyAt=Date.now();
      $("#notify-title").value=""; $("#notify-body").value="";
      toast("تم جدولة الإشعار","ok");
    }catch(e){ toast(e.message||"تعذر الإرسال","err"); }
    finally{ setBusy(false,btn); }
  }

  function exportJson(){
    const blob=new Blob([JSON.stringify({azkar,categories,exportedAt:new Date().toISOString()},null,2)],{type:"application/json"});
    const a=document.createElement("a");
    a.href=URL.createObjectURL(blob);
    a.download="baqiyat-backup-"+new Date().toISOString().slice(0,10)+".json";
    a.click(); URL.revokeObjectURL(a.href);
    toast("تم التصدير","ok");
  }

  function listenData(){
    if(unsubAzkar) unsubAzkar();
    unsubAzkar=itemsCol().onSnapshot(snap=>{
      azkar=snap.docs.map(doc=>{
        const d=doc.data()||{};
        return {id:doc.id,text:d.text||"",category:d.category||"sabah",repeat:d.repeat||1,fadl:d.fadl||"",source:d.source||"",
          audioUrl:d.audioUrl||"",audioUrlsByReciter:d.audioUrlsByReciter||{},published:d.published!==false,order:d.order??0};
      });
      renderTable(); renderStats(); renderCategories(); renderHisnBookStats(); renderHisnChapters();
    }, e=>toast("تعذر تحميل الأذكار: "+e.message,"err"));

    if(unsubCategories) unsubCategories();
    unsubCategories=catsCol().onSnapshot(snap=>{
      mergeCategories(snap.docs.map(doc=>({id:doc.id,...(doc.data()||{})})));
      renderCategories(); renderTable();
    }, ()=>{ mergeCategories([]); renderCategories(); });

    if(unsubFeedback) unsubFeedback();
    try{
      unsubFeedback=db.collection("feedback").orderBy("createdAt","desc").limit(100).onSnapshot(snap=>{
        feedbackItems=snap.docs.map(doc=>({id:doc.id,...doc.data()}));
        renderFeedbackList(); renderStats();
      });
    }catch(_){}
  }

  function bindUi(){
    $$(".nav-item").forEach(btn=>btn.addEventListener("click",()=>go(btn.dataset.view)));
    $$("button[data-view]").forEach(btn=>{ if(!btn.classList.contains("nav-item")) btn.addEventListener("click",()=>go(btn.dataset.view)); });
    $("#add-zekr")?.addEventListener("click",()=>openEditor(null));
    $("#import-hisn-book")?.addEventListener("click",importHisnBook);
    $("#hisn-chapter-search")?.addEventListener("input",renderHisnChapters);
    $("#save-zekr")?.addEventListener("click",()=>saveEditor());
    $("#close-modal")?.addEventListener("click",()=>closeModal("#editor-modal"));
    $("#cancel-modal")?.addEventListener("click",()=>closeModal("#editor-modal"));
    $("#add-category")?.addEventListener("click",()=>openCategoryEditor(null));
    $("#save-category")?.addEventListener("click",()=>saveCategory());
    $("#close-cat-modal")?.addEventListener("click",()=>closeModal("#category-modal"));
    $("#cancel-cat-modal")?.addEventListener("click",()=>closeModal("#category-modal"));
    $("#close-preview")?.addEventListener("click",()=>closeModal("#preview-modal"));
    $("#preview-btn")?.addEventListener("click",()=>openPreview(null));
    $("#export-json")?.addEventListener("click",exportJson);
    $("#send-notify")?.addEventListener("click",()=>sendNotify());
    $("#search")?.addEventListener("input",renderTable);
    $("#filter")?.addEventListener("change",renderTable);
    $("#status-filter")?.addEventListener("change",renderTable);
    ["#editor-modal","#category-modal","#preview-modal"].forEach(sel=>{
      $(sel)?.addEventListener("click",e=>{ if(e.target===e.currentTarget) closeModal(sel); });
    });
    document.addEventListener("keydown",e=>{
      if(e.key==="Escape"){ closeModal("#editor-modal"); closeModal("#category-modal"); closeModal("#preview-modal"); }
      if((e.ctrlKey||e.metaKey)&&e.key==="s"){
        e.preventDefault();
        if($("#editor-modal")?.classList.contains("open")) saveEditor();
        else if($("#category-modal")?.classList.contains("open")) saveCategory();
      }
      if(e.key==="/"&&!/INPUT|TEXTAREA|SELECT/.test(document.activeElement?.tagName||"")){
        e.preventDefault(); go("azkar"); $("#search")?.focus();
      }
    });
  }

  async function startSession(user){
    const token=await user.getIdTokenResult(true);
    let ok=token.claims.super_admin===true||token.claims.admin===true;
    let role=token.claims.super_admin?"super_admin":token.claims.admin?"admin":"";
    if(!ok){ const snap=await db.collection("admins").doc(user.uid).get(); ok=snap.exists; role=snap.data()?.role||"admin"; }
    if(!ok){ toast("ليس لديك صلاحية مشرف","err"); await auth.signOut(); return; }
    const gate=$("#auth-gate"), shell=$("#shell");
    gate?.classList.add("hidden"); if(gate) gate.style.display="none";
    shell?.classList.add("ready");
    if($("#admin-email")) $("#admin-email").textContent=user.email||user.uid;
    if($("#admin-role-badge")) $("#admin-role-badge").textContent="RBAC · "+role;
    listenData();
  }
  function stopSession(){
    if(unsubAzkar) unsubAzkar(); if(unsubCategories) unsubCategories(); if(unsubFeedback) unsubFeedback();
    const gate=$("#auth-gate"), shell=$("#shell");
    gate?.classList.remove("hidden"); if(gate) gate.style.display="grid";
    shell?.classList.remove("ready");
  }
  async function signInWithGoogle(){
    const provider=new firebase.auth.GoogleAuthProvider();
    const message=$("#auth-message");
    try{ await auth.signInWithPopup(provider); }
    catch(e){
      if((e.code||"").includes("popup")){ try{ await auth.signInWithRedirect(provider); return; }catch(re){ if(message) message.textContent=re.message; return; } }
      if(message) message.textContent=e.message||"فشل الدخول";
    }
  }

  const init=async()=>{
    if(window.firebaseHostingConfigPromise) await window.firebaseHostingConfigPromise.catch(()=>null);
    const cfg=window.firebaseConfig||window.AZKAR_FIREBASE_CONFIG;
    if(!window.firebase||!cfg){ const msg=$("#auth-message"); if(msg) msg.textContent="تعذر تحميل إعداد Firebase."; return; }
    if(!firebase.apps.length) firebase.initializeApp(cfg);
    db=firebase.firestore(); auth=firebase.auth();
    try{ await loadHisnBook(); }catch(e){ console.warn("Hisn al-Muslim data could not load",e); if($("#hisn-import-status")) $("#hisn-import-status").textContent=e.message||"تعذر تحميل فهرس الكتاب."; }
    bindUi();
    $$(".view").forEach(v=>{ if(!v.classList.contains("active-view")) v.setAttribute("hidden",""); });
    auth.onAuthStateChanged(user=>{ if(user) startSession(user).catch(e=>toast(e.message,"err")); else stopSession(); });
    $("#google-login-btn")?.addEventListener("click",signInWithGoogle);
    $("#logout-btn")?.addEventListener("click",()=>auth.signOut());
  };
  if(document.readyState==="loading") document.addEventListener("DOMContentLoaded",init,{once:true});
  else init();
})();
