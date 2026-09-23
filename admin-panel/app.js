const ADMIN_USER = "admin";
const ADMIN_PASS = "123456";

const defaultAzkar = [
  {id:'1', text:'اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ', category:'sabah', repeat:1, fadl:'آية الكرسي من أعظم آيات القرآن.', source:'الحاكم', published:true},
  {id:'2', text:'أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ', category:'sabah', repeat:1, fadl:'', source:'مسلم', published:true},
  {id:'3', text:'اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ', category:'sabah', repeat:1, fadl:'سيد الاستغفار', source:'البخاري', published:true},
  {id:'4', text:'سُبْحَانَ اللَّهِ وَبِحَمْدِهِ', category:'sabah', repeat:100, fadl:'حطت خطاياه وإن كانت مثل زبد البحر', source:'مسلم', published:true},
  {id:'5', text:'أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ', category:'masaa', repeat:1, fadl:'', source:'مسلم', published:true},
  {id:'6', text:'اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا', category:'masaa', repeat:1, fadl:'', source:'الترمذي', published:true},
  {id:'7', text:'رَضِيتُ بِاللَّهِ رَبًّا وَبِالإِسْلَامِ دِينًا', category:'masaa', repeat:3, fadl:'', source:'أبو داود', published:true},
  {id:'8', text:'يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ', category:'masaa', repeat:1, fadl:'', source:'الترمذي', published:true}
];

let azkar = JSON.parse(localStorage.getItem('azkar_data') || 'null') || defaultAzkar;
const categories = [
  {icon:'☀', name:'أذكار الصباح', desc:'ورد الصباح', count: 4},
  {icon:'☾', name:'أذكار المساء', desc:'ورد المساء', count: 4}
];

const $ = s => document.querySelector(s); const $$ = s => document.querySelectorAll(s);
let editingIndex = null;

function saveToStorage() {
  localStorage.setItem('azkar_data', JSON.stringify(azkar));
}

function toast(msg) {
  const el = $('#toast');   if (el) {     el.textContent = msg;     el.classList.add('show');     setTimeout(() => el.classList.remove('show'), 2000);   } }  function go(view) {   $$('.view').forEach(x => x.classList.remove('active-view'));
  $('#' + view)?.classList.add('active-view');   $$('.nav-item').forEach(x => x.classList.toggle('active', x.dataset.view === view));
  const titles = { overview: 'نظرة عامة', azkar: 'مكتبة الأذكار', categories: 'التصنيفات', settings: 'إعدادات التطبيق' };
  if ($('#page-title')) $('#page-title').textContent = titles[view] || 'لوحة التحكم';
}

function renderTable() {
  const q = ($('#search')?.value || '').trim();
  const filter = $('#filter')?.value || 'all';
  const status = $('#status-filter')?.value || 'all';

  const rows = azkar.map((z, index) => ({ ...z, index }))
    .filter(z => (filter === 'all' || z.category === filter)
      && (status === 'all' || (status === 'published' ? z.published : !z.published))
      && z.text.includes(q));

  if ($('#stat-total')) $('#stat-total').textContent = azkar.length;

  const tbody = $('#azkar-table');
  if (!tbody) return;
  if (!rows.length) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 20px;">لا توجد أذكار مطابقة.</td></tr>';
    return;
  }

  tbody.innerHTML = rows.map(z => `
    <tr>
      <td>${z.text}</td>
      <td><span class="pill">${z.category === 'sabah' ? 'الصباح' : 'المساء'}</span></td>
      <td>${z.repeat}×</td>
      <td><span class="pill ${z.published ? 'status-published' : 'status-draft'}">${z.published ? 'منشور' : 'مسودة'}</span></td>
      <td>${z.source || '—'}</td>
      <td>
        <button class="icon-btn" data-edit="${z.index}">تعديل</button> ·
        <button class="icon-btn" data-delete="${z.index}">حذف</button>
      </td>
    </tr>`).join('');

  $$('[data-edit]').forEach(btn => btn.addEventListener('click', () => openEditor(Number(btn.dataset.edit))));$$
('[data-delete]').forEach(btn => btn.addEventListener('click', () => {
    if (confirm('حذف هذا الذكر؟')) {
      azkar.splice(Number(btn.dataset.delete), 1);
      saveToStorage();
      renderTable();
      toast('تم حذف الذكر بنجاح');
    }
  }));
}

function renderCategories() {
  const grid = $('#category-grid');
  if (grid) {
    grid.innerHTML = categories.map(c => `
      <article class="category-card">
        <div class="symbol">${c.icon}</div>
        <h3>${c.name}</h3>
        <p>${c.desc}</p>
        <p style="margin-top:16px;color:var(--green)">${c.count} أذكار</p>
      </article>`).join('');
  }
}

function openEditor(index = null) {
  editingIndex = index;
  const z = index === null
    ? { text: '', category: 'sabah', repeat: 1, fadl: '', source: '', published: true }
    : azkar[index];

  if ($('#modal-title')) $('#modal-title').textContent = index === null ? 'إضافة ذكر' : 'تعديل الذكر';
  if ($('#field-text')) $('#field-text').value = z.text;
  if ($('#field-category')) $('#field-category').value = z.category;
  if ($('#field-repeat')) $('#field-repeat').value = z.repeat;
  if ($('#field-fadl')) $('#field-fadl').value = z.fadl || '';
  if ($('#field-source')) $('#field-source').value = z.source || '';
  if ($('#field-published')) $('#field-published').checked = !!z.published;

  const modal = $('#editor-modal');
  if (modal) {
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');
  }
}

function closeEditor() {
  const modal = $('#editor-modal');
  if (modal) {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
  }
  editingIndex = null;
}

function saveEditor() {
  const text = $('#field-text')?.value.trim();
  if (!text) { toast('يرجى كتابة نص الذكر'); return; }

  const item = {
    id: editingIndex === null ? String(Date.now()) : azkar[editingIndex].id,
    text,
    category: $('#field-category')?.value || 'sabah',
    repeat: Number($('#field-repeat')?.value) || 1,
    fadl: $('#field-fadl')?.value.trim() || '',
    source: $('#field-source')?.value.trim() || '',
    published: !!$('#field-published')?.checked
  };

  if (editingIndex === null) azkar.unshift(item);
  else azkar[editingIndex] = item;

  saveToStorage();
  renderTable();
  closeEditor();
  toast('تم حفظ الذكر بنجاح');
}

function checkSession() {
  const isLoggedIn = sessionStorage.getItem('admin_logged_in') === 'true';
  const gate = $('#auth-gate');
  const shell = $('.shell');
  if (isLoggedIn) {
    if (gate) gate.style.display = 'none';
    if (shell) shell.classList.add('ready');
  } else {
    if (gate) gate.style.display = 'flex';
    if (shell) shell.classList.remove('ready');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  checkSession();

  $('#simple-login-form')?.addEventListener('submit', (e) => {
    e.preventDefault();
    const u = $('#login-username')?.value.trim();
    const p = $('#login-password')?.value.trim();
    const msg = $('#auth-message');

    if (u === ADMIN_USER && p === ADMIN_PASS) {
      sessionStorage.setItem('admin_logged_in', 'true');
      if (msg) msg.textContent = '';
      checkSession();
      toast('تم تسجيل الدخول بنجاح');
    } else {
      if (msg) msg.textContent = 'اسم المستخدم أو كلمة المرور غير صحيحة!';
    }
  });

  $('#logout-btn')?.addEventListener('click', () => {     sessionStorage.removeItem('admin_logged_in');     checkSession();     toast('تم تسجيل الخروج');   });    $$('[data-view]').forEach(el => el.addEventListener('click', () => go(el.dataset.view)));$('#search')?.addEventListener('input', renderTable);
  $('#filter')?.addEventListener('change', renderTable);
  $('#status-filter')?.addEventListener('change', renderTable);
  $('#add-zekr')?.addEventListener('click', () => openEditor());
  $('#save-zekr')?.addEventListener('click', saveEditor);
  $('#close-modal')?.addEventListener('click', closeEditor);
  $('#cancel-modal')?.addEventListener('click', closeEditor);

  renderTable();
  renderCategories();
});
