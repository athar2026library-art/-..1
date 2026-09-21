const azkar = [
  {id:'ayatul-kursi',text:'اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ', category:'sabah', repeat:1, fadl:'آية الكرسي من أعظم آيات القرآن.', source:'رواه الحاكم وصححه الألباني', published:true, order:1},
  {id:'asbahna',text:'أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ', category:'sabah', repeat:1, fadl:'', source:'رواه مسلم', published:true, order:2},
  {id:'sayyid-istighfar',text:'اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ', category:'sabah', repeat:1, fadl:'سيد الاستغفار.', source:'رواه البخاري', published:true, order:3},
  {id:'bismillah',text:'بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ', category:'sabah', repeat:3, fadl:'', source:'رواه أبو داود والترمذي', published:true, order:4},
  {id:'subhanallah',text:'سُبْحَانَ اللَّهِ وَبِحَمْدِهِ', category:'sabah', repeat:100, fadl:'حطت خطاياه وإن كانت مثل زبد البحر.', source:'رواه مسلم', published:false, order:5},
  {id:'amsayna',text:'أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ', category:'masaa', repeat:1, fadl:'', source:'رواه مسلم', published:true, order:6},
  {id:'allahumma-bika',text:'اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا', category:'masaa', repeat:1, fadl:'', source:'رواه الترمذي', published:true, order:7},
  {id:'raditu',text:'رَضِيتُ بِاللَّهِ رَبًّا وَبِالإِسْلَامِ دِينًا', category:'masaa', repeat:3, fadl:'', source:'رواه أبو داود والترمذي', published:false, order:8}
];
const categories = [{icon:'☀',name:'أذكار الصباح',desc:'ورد بداية اليوم',count:8},{icon:'☾',name:'أذكار المساء',desc:'ورد نهاية اليوم',count:8},{icon:'✦',name:'أذكار مختارة',desc:'مجموعة مخصصة',count:0}];
const $=s=>document.querySelector(s); const$$=s=>document.querySelectorAll(s); let editingIndex=null; let firestore=null;

function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function toast(message){const el=$('#toast');el.textContent=message;el.classList.add('show');setTimeout(()=>el.classList.remove('show'),2600)}
function go(view){$$('.view').forEach(x=>x.classList.remove('active-view'));$('#'+view).classList.add('active-view');$$('.nav-item').forEach(x=>x.classList.toggle('active',x.dataset.view===view));$('#page-title').textContent={overview:'نظرة عامة',azkar:'مكتبة الأذكار',categories:'التصنيفات',settings:'إعدادات التطبيق'}[view]}
$$('[data-view]').forEach(el=>el.addEventListener('click',()=>go(el.dataset.view)));

function statusPill(published){return `<span class="pill ${published?'status-published':'status-draft'}">${published?'منشور':'مسودة'}</span>`}

function renderTable(){
  const q=($('#search')?.value||'').trim();
  const filter=$('#filter')?.value||'all';
  const status=$('#status-filter')?.value||'all';
  const rows=azkar.map((z,index)=>({...z,index}))
    .filter(z=>(filter==='all'||z.category===filter)
      &&(status==='all'||(status==='published'?z.published:!z.published))
      &&z.text.includes(q));

  // Real count instead of fake +8
  $('#stat-total').textContent = azkar.length;

  const tbody = $('#azkar-table');
  if (!rows.length) {
    tbody.innerHTML = '<tr><td colspan="6">لا توجد نتائج مطابقة.</td></tr>';
    return;
  }

  tbody.innerHTML = rows.map(z => `
    <tr>
      <td>${escapeHtml(z.text)}</td>
      <td><span class="pill">${z.category==='sabah'?'الصباح':'المساء'}</span></td>
      <td>${Number(z.repeat)||1}×</td>
      <td>${statusPill(!!z.published)}</td>
      <td>${escapeHtml(z.source)||'—'}</td>
      <td>
        <button class="icon-btn" data-edit="${z.index}">تعديل</button> ·
        <button class="icon-btn" data-delete="${z.index}">حذف</button>
      </td>
    </tr>`).join('');

  $$('[data-edit]').forEach(btn=>btn.addEventListener('click',()=>openEditor(Number(btn.dataset.edit))));
  $$('[data-delete]').forEach(btn=>btn.addEventListener('click',()=>{
    if(confirm('حذف هذا الذكر؟')){
      const item=azkar[Number(btn.dataset.delete)];
      azkar.splice(Number(btn.dataset.delete),1);
      persistDelete(item);
      renderTable();
      toast('تم حذف الذكر');
    }
  }));
}

function renderCategories(){$('#category-grid').innerHTML=categories.map(c=>`<article class="category-card"><div class="symbol">${c.icon}</div><h3>${c.name}</h3><p>${c.desc}</p><p style="margin-top:16px;color:var(--green)">${c.count} أذكار</p></article>`).join('')}

function openEditor(index=null){
  editingIndex=index;
  const z=index===null
    ?{id:crypto.randomUUID(),text:'',category:'sabah',repeat:1,fadl:'',source:'',published:false,order:azkar.length+1}
    :azkar[index];
  $('#modal-title').textContent=index===null?'إضافة ذكر':'تعديل الذكر';
  $('#field-text').value=z.text;
  $('#field-category').value=z.category;
  $('#field-repeat').value=z.repeat;
  $('#field-fadl').value=z.fadl||'';
  $('#field-source').value=z.source||'';
  $('#field-published').checked=!!z.published;
  $('#editor-modal').classList.add('open');
  $('#editor-modal').setAttribute('aria-hidden','false');
  setTimeout(()=>$('#field-text').focus(),50);
}

function closeEditor(){$('#editor-modal').classList.remove('open');$('#editor-modal').setAttribute('aria-hidden','true');editingIndex=null}

function saveEditor(){
  const text=$('#field-text').value.trim();
  if(!text){toast('اكتب نص الذكر أولاً');return}
  const item={
    id: editingIndex===null ? crypto.randomUUID() : azkar[editingIndex].id,
    text,
    category: $('#field-category').value,
    repeat: Math.max(1, Number($('#field-repeat').value)||1),
    fadl: $('#field-fadl').value.trim(),
    source: $('#field-source').value.trim(),
    published: $('#field-published').checked,
    order: editingIndex===null ? (azkar.length + 1) : (azkar[editingIndex].order || editingIndex + 1),
    updatedAt: new Date().toISOString()
  };
  if(editingIndex===null) azkar.unshift(item);
  else azkar[editingIndex]=item;
  persistSave(item);
  renderTable();
  closeEditor();
  toast(item.published?'تم حفظ الذكر ونشره':'تم حفظ الذكر كمسودة');
}

async function persistSave(item){
  if(!firestore) return;
  try{
    // Ensure order is a number so Android doc.getLong("order") works
    const payload = { ...item, order: Number(item.order) || 0 };
    await firestore.collection('content').doc('azkar').collection('items').doc(item.id).set(payload,{merge:true});
  }catch(e){
    console.error(e);
    toast('تم الحفظ محلياً، تعذّر الحفظ السحابي');
  }
}

async function persistDelete(item){
  if(!firestore||!item?.id) return;
  try{
    await firestore.collection('content').doc('azkar').collection('items').doc(item.id).delete();
  }catch(e){console.error(e)}
}

async function loadRemote(){
  if(!firestore) return;
  try{
    // Admin panel must see drafts too – remove published==true filter
    const snap = await firestore.collection('content').doc('azkar').collection('items').get();
    if(!snap.empty){
      azkar.splice(0, azkar.length, ...snap.docs.map(d => ({id: d.id, ...d.data()})));
      // Sort by order if present
      azkar.sort((a,b) => (Number(a.order)||0) - (Number(b.order)||0));
      renderTable();
      toast('تم تحديث المحتوى من Firebase');
    }
  }catch(e){
    console.error(e);
    toast('تعذر تحميل المحتوى السحابي');
  }
}

async function setupAuth(){
  const config=window.AZKAR_FIREBASE_CONFIG;
  if(!config?.apiKey||config.apiKey.includes('YOUR_')){
    $('#auth-message').textContent='أضف firebase-config.js من القالب ثم أعد تحميل الصفحة.';
    return;
  }
  try{
    firebase.initializeApp(config);
    firestore=firebase.firestore();
    firebase.auth().onAuthStateChanged(async user=>{
      if(!user){
        $('#auth-message').textContent='سجّل الدخول بحساب المشرف للمتابعة.';
        return;
      }
      const emailLower=(user.email||'').toLowerCase().trim();
      // Prefer custom claims in production. Hard-coded email is only a temporary gate.
      const isOwner = emailLower==='mgedh.9ali@gmail.com' || emailLower.includes('mgedh.9ali');
      if(!isOwner){
        await firebase.auth().signOut();
        $('#auth-message').textContent='هذا الحساب ليس ضمن المشرفين.';
        return;
      }
      $('#auth-gate').classList.add('hidden');
      $('.shell').classList.add('ready');
      $('#auth-message').textContent=`مرحباً ${user.displayName||user.email}`;
      await loadRemote();
    });
  }catch(e){
    console.error(e);
    $('#auth-message').textContent='تعذر تهيئة Firebase. راجع إعدادات المشروع.';
  }
}

$('#login-btn')?.addEventListener('click',async()=>{
  if(!window.AZKAR_FIREBASE_CONFIG?.apiKey){toast('أضف إعداد Firebase أولاً');return}
  try{
    await firebase.auth().signInWithPopup(new firebase.auth.GoogleAuthProvider());
  }catch(e){
    console.error(e);
    $('#auth-message').textContent='فشل تسجيل الدخول أو تم إلغاؤه.';
  }
});

$('#search')?.addEventListener('input',renderTable);
$('#filter')?.addEventListener('change',renderTable);
$('#status-filter')?.addEventListener('change',renderTable);
$('#add-zekr')?.addEventListener('click',()=>openEditor());
$('#save-zekr')?.addEventListener('click',saveEditor);
$('#close-modal')?.addEventListener('click',closeEditor);
$('#cancel-modal')?.addEventListener('click',closeEditor);
$('#editor-modal')?.addEventListener('click',e=>{if(e.target.id==='editor-modal')closeEditor()});
$('#add-category')?.addEventListener('click',()=>{const name=prompt('اسم التصنيف الجديد:');if(name?.trim()){categories.push({icon:'✦',name:name.trim(),desc:'تصنيف جديد',count:0});renderCategories();toast('تمت إضافة التصنيف')}});
$('#preview-btn')?.addEventListener('click',()=>toast('المعاينة ستتصل بتطبيق Android بعد تفعيل مزامنة Firebase'));

renderTable();
renderCategories();
setupAuth();
