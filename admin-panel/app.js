const azkar = [];
let feedbackItems = [];
let selectedFeedbackId = null;
let editingIndex = null;
let firestore = null;
let dragFromIndex = null;
let feedbackUnsub = null;

const CAT_LABEL = {
  sabah: 'الصباح',
  masaa: 'المساء',
  sleep: 'النوم',
  travel: 'السفر'
};

const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function toast(message) {
  const el = $('#toast');
  if (!el) return;
  el.textContent = message;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 2600);
}

function go(view) {
  $$('.view').forEach((x) => x.classList.remove('active-view'));
  $('#' + view)?.classList.add('active-view');
  $$('.nav-item').forEach((x) =>
    x.classList.toggle('active', x.dataset.view === view)
  );
  const titles = {
    overview: 'نظرة عامة',
    azkar: 'مكتبة الأذكار',
    feedback: 'الشكاوى',
    categories: 'التصنيفات',
    settings: 'الإعدادات',
    notify: 'الإشعارات'
  };
  const title = $('#page-title');
  if (title) title.textContent = titles[view] || view;
  if (view === 'feedback') renderFeedbackList();
  if (view === 'categories') renderCategories();
  if (view === 'overview') updateStats();
}

$$('[data-view]').forEach((el) =>
  el.addEventListener('click', () => go(el.dataset.view))
);

function statusPill(published) {
  return `<span class="pill ${published ? 'status-published' : 'status-draft'}">${
    published ? 'منشور' : 'مسودة'
  }</span>`;
}

function categoryLabel(c) {
  return CAT_LABEL[c] || c || '—';
}

function updateStats() {
  const active = azkar.filter((z) => !z.archived);
  const total = active.length;
  const published = active.filter((z) => z.published).length;
  const drafts = total - published;
  const feedbackNew = feedbackItems.filter(
    (f) => (f.status || 'new') === 'new'
  ).length;

  const set = (id, v) => {
    const el = $(id);
    if (el) el.textContent = String(v);
  };
  set('#stat-total', total);
  set('#stat-published', published);
  set('#stat-drafts', drafts);
  set('#stat-feedback-new', feedbackNew);

  const badge = $('#feedback-badge');
  if (badge) {
    if (feedbackNew > 0) {
      badge.textContent = String(feedbackNew);
      badge.classList.remove('hidden');
    } else badge.classList.add('hidden');
  }
}

async function enqueueAdminJob(type, payload) {
  if (!firestore) return;
  try {
    await firestore.collection('admin_jobs').add({
      type,
      payload,
      status: 'pending',
      createdAt: firebase.firestore.FieldValue.serverTimestamp()
    });
  } catch (e) {
    console.warn('admin_jobs write failed', e);
  }
}

function renderTable() {
  const q = ($('#search')?.value || '').trim();
  const filter = $('#filter')?.value || 'all';
  const status = $('#status-filter')?.value || 'all';
  const showArchived = ($('#show-archived')?.value || 'hide') === 'show';

  const rows = azkar
    .map((z, index) => ({ ...z, index }))
    .filter((z) => {
      if (!showArchived && z.archived) return false;
      if (filter !== 'all' && z.category !== filter) return false;
      if (status === 'published' && !z.published) return false;
      if (status === 'draft' && z.published) return false;
      if (status === 'archived' && !z.archived) return false;
      if (q) {
        const hay = `${z.text || ''} ${z.source || ''} ${z.fadl || ''}`;
        if (!hay.includes(q)) return false;
      }
      return true;
    })
    .sort((a, b) => (Number(a.order) || 0) - (Number(b.order) || 0));

  updateStats();

  const tbody = $('#azkar-table');
  if (!tbody) return;
  if (!rows.length) {
    tbody.innerHTML =
      '<tr><td colspan="8">لا توجد نتائج مطابقة.</td></tr>';
    return;
  }

  tbody.innerHTML = rows
    .map(
      (z) => `
    <tr draggable="true" data-row-index="${z.index}" class="draggable-row${z.archived ? ' archived-row' : ''}">
      <td class="drag-handle" title="اسحب">⋮⋮</td>
      <td class="order-cell">
        <button type="button" class="icon-btn" data-up="${z.index}">↑</button>
        <button type="button" class="icon-btn" data-down="${z.index}">↓</button>
        <span class="order-num">${Number(z.order) || '—'}</span>
      </td>
      <td>${escapeHtml(z.text)}${z.archived ? ' <span class="pill status-draft">مؤرشف</span>' : ''}</td>
      <td><span class="pill">${categoryLabel(z.category)}</span></td>
      <td>${Number(z.repeat) || 1}×</td>
      <td>${statusPill(!!z.published)}</td>
      <td>${escapeHtml(z.source) || '—'}</td>
      <td class="actions-cell">
        <button type="button" class="icon-btn" data-preview="${z.index}">معاينة</button>
        <button type="button" class="icon-btn" data-edit="${z.index}">تعديل</button>
        <button type="button" class="icon-btn" data-dup="${z.index}">نسخ</button>
        <button type="button" class="icon-btn" data-archive="${z.index}">${z.archived ? 'استعادة' : 'أرشفة'}</button>
        <button type="button" class="icon-btn" data-delete="${z.index}">حذف</button>
      </td>
    </tr>`
    )
    .join('');

  $$('[data-edit]').forEach((btn) =>
    btn.addEventListener('click', () => openEditor(Number(btn.dataset.edit)))
  );
  $$('[data-preview]').forEach((btn) =>
    btn.addEventListener('click', () => openPreview(Number(btn.dataset.preview)))
  );
  $$('[data-dup]').forEach((btn) =>
    btn.addEventListener('click', () => duplicateZekr(Number(btn.dataset.dup)))
  );
  $$('[data-archive]').forEach((btn) =>
    btn.addEventListener('click', () => toggleArchive(Number(btn.dataset.archive)))
  );
  $$('[data-delete]').forEach((btn) =>
    btn.addEventListener('click', () => {
      if (!confirm('حذف نهائي لهذا الذكر؟')) return;
      const idx = Number(btn.dataset.delete);
      const item = azkar[idx];
      azkar.splice(idx, 1);
      persistDelete(item);
      renderTable();
      toast('تم الحذف');
    })
  );
  $$('[data-up]').forEach((btn) =>
    btn.addEventListener('click', () => moveOrder(Number(btn.dataset.up), -1))
  );
  $$('[data-down]').forEach((btn) =>
    btn.addEventListener('click', () => moveOrder(Number(btn.dataset.down), 1))
  );

  $$('.draggable-row').forEach((row) => {
    row.addEventListener('dragstart', (e) => {
      dragFromIndex = Number(row.dataset.rowIndex);
      row.classList.add('dragging');
      e.dataTransfer.effectAllowed = 'move';
    });
    row.addEventListener('dragend', () => {
      row.classList.remove('dragging');
      dragFromIndex = null;
      $$('.draggable-row').forEach((r) => r.classList.remove('drag-over'));
    });
    row.addEventListener('dragover', (e) => {
      e.preventDefault();
      row.classList.add('drag-over');
    });
    row.addEventListener('dragleave', () => row.classList.remove('drag-over'));
    row.addEventListener('drop', async (e) => {
      e.preventDefault();
      row.classList.remove('drag-over');
      const toIndex = Number(row.dataset.rowIndex);
      if (dragFromIndex == null || dragFromIndex === toIndex) return;
      await reorderByDrag(dragFromIndex, toIndex);
    });
  });
}

async function duplicateZekr(index) {
  const src = azkar[index];
  if (!src) return;
  const copy = {
    ...src,
    id: crypto.randomUUID(),
    text: (src.text || '') + ' (نسخة)',
    published: false,
    archived: false,
    order: azkar.length + 1,
    updatedAt: new Date().toISOString()
  };
  azkar.unshift(copy);
  await persistSave(copy);
  renderTable();
  toast('تم إنشاء نسخة كمسودة');
}

async function toggleArchive(index) {
  const item = azkar[index];
  if (!item) return;
  item.archived = !item.archived;
  if (item.archived) item.published = false;
  item.updatedAt = new Date().toISOString();
  await persistSave(item);
  renderTable();
  toast(item.archived ? 'تمت الأرشفة' : 'تمت الاستعادة');
}

function exportJson() {
  const data = azkar
    .filter((z) => !z.archived)
    .sort((a, b) => (Number(a.order) || 0) - (Number(b.order) || 0));
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json'
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `baqiyat-azkar-${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
  toast('تم تصدير JSON');
}

async function reorderByDrag(fromIndex, toIndex) {
  const sorted = [...azkar].sort(
    (a, b) => (Number(a.order) || 0) - (Number(b.order) || 0)
  );
  const fromItem = azkar[fromIndex];
  const toItem = azkar[toIndex];
  if (!fromItem || !toItem) return;
  const fromPos = sorted.findIndex((z) => z.id === fromItem.id);
  const toPos = sorted.findIndex((z) => z.id === toItem.id);
  if (fromPos < 0 || toPos < 0) return;
  const [moved] = sorted.splice(fromPos, 1);
  sorted.splice(toPos, 0, moved);
  const writes = [];
  sorted.forEach((z, i) => {
    const newOrder = i + 1;
    if (Number(z.order) !== newOrder) {
      z.order = newOrder;
      writes.push(persistSave(z));
    }
  });
  await Promise.all(writes);
  renderTable();
  toast('تم تحديث الترتيب');
}

async function moveOrder(index, direction) {
  if (index < 0 || index >= azkar.length) return;
  const sorted = [...azkar].sort(
    (a, b) => (Number(a.order) || 0) - (Number(b.order) || 0)
  );
  const item = azkar[index];
  const pos = sorted.findIndex((z) => z.id === item.id);
  const swapPos = pos + direction;
  if (swapPos < 0 || swapPos >= sorted.length) return;
  const other = sorted[swapPos];
  const orderA = Number(item.order) || pos + 1;
  const orderB = Number(other.order) || swapPos + 1;
  item.order = orderB;
  other.order = orderA;
  await persistSave(item);
  await persistSave(other);
  renderTable();
  toast('تم تحديث الترتيب');
}

function openPreview(index) {
  const z = azkar[index];
  if (!z) return;
  const modal = $('#preview-modal');
  if (!modal) return;
  $('#preview-cat').textContent = categoryLabel(z.category);
  $('#preview-text').textContent = z.text || '';
  $('#preview-repeat').textContent = `${Number(z.repeat) || 1}×`;
  $('#preview-fadl').textContent = z.fadl || '—';
  $('#preview-source').textContent = z.source || '—';
  $('#preview-status').textContent = z.published ? 'منشور' : 'مسودة';
  modal.classList.add('open');
  modal.setAttribute('aria-hidden', 'false');
}

function closePreview() {
  const modal = $('#preview-modal');
  if (!modal) return;
  modal.classList.remove('open');
  modal.setAttribute('aria-hidden', 'true');
}

function renderCategories() {
  const counts = { sabah: 0, masaa: 0, sleep: 0, travel: 0 };
  azkar.filter((z) => !z.archived).forEach((z) => {
    if (counts[z.category] != null) counts[z.category]++;
  });
  const list = [
    { icon: '☀', name: 'أذكار الصباح', desc: 'ورد بداية اليوم', key: 'sabah' },
    { icon: '☾', name: 'أذكار المساء', desc: 'ورد نهاية اليوم', key: 'masaa' },
    { icon: '☽', name: 'أذكار النوم', desc: 'قبل النوم', key: 'sleep' },
    { icon: '✈', name: 'أذكار السفر', desc: 'عند السفر', key: 'travel' }
  ];
  const grid = $('#category-grid');
  if (!grid) return;
  grid.innerHTML = list
    .map(
      (c) => `
    <article class="category-card">
      <div class="symbol">${c.icon}</div>
      <h3>${c.name}</h3>
      <p>${c.desc}</p>
      <p style="margin-top:16px;color:var(--green)">${counts[c.key] || 0} أذكار</p>
    </article>`
    )
    .join('');
}

function openEditor(index = null) {
  editingIndex = index;
  const z =
    index === null
      ? {
          id: crypto.randomUUID(),
          text: '',
          category: 'sabah',
          repeat: 1,
          fadl: '',
          source: '',
          published: false,
          archived: false,
          order: azkar.length + 1
        }
      : azkar[index];

  if ($('#modal-title'))
    $('#modal-title').textContent =
      index === null ? 'إضافة ذكر' : 'تعديل الذكر';
  if ($('#field-text')) $('#field-text').value = z.text || '';
  if ($('#field-category')) $('#field-category').value = z.category || 'sabah';
  if ($('#field-repeat')) $('#field-repeat').value = z.repeat || 1;
  if ($('#field-fadl')) $('#field-fadl').value = z.fadl || '';
  if ($('#field-source')) $('#field-source').value = z.source || '';
  if ($('#field-published')) $('#field-published').checked = !!z.published;

  const modal = $('#editor-modal');
  if (modal) {
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');
  }
  setTimeout(() => $('#field-text')?.focus(), 50);
}

function closeEditor() {
  const modal = $('#editor-modal');
  if (modal) {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
  }
  editingIndex = null;
}

async function saveEditor() {
  const text = $('#field-text')?.value.trim();
  if (!text) {
    toast('اكتب نص الذكر أولاً');
    return;
  }
  const wasNew = editingIndex === null;
  const prevPublished =
    editingIndex === null ? false : !!azkar[editingIndex].published;

  const item = {
    id: wasNew ? crypto.randomUUID() : azkar[editingIndex].id,
    text,
    category: $('#field-category')?.value || 'sabah',
    repeat: Math.max(1, Number($('#field-repeat')?.value) || 1),
    fadl: $('#field-fadl')?.value.trim() || '',
    source: $('#field-source')?.value.trim() || '',
    published: !!$('#field-published')?.checked,
    archived: wasNew ? false : !!azkar[editingIndex].archived,
    order: wasNew
      ? azkar.length + 1
      : azkar[editingIndex].order || editingIndex + 1,
    updatedAt: new Date().toISOString()
  };
  if (wasNew) azkar.unshift(item);
  else azkar[editingIndex] = item;

  await persistSave(item);

  if (item.published && !prevPublished) {
    await enqueueAdminJob('content_published', {
      zekrId: item.id,
      category: item.category,
      title: 'تحديث في الباقيات',
      body: 'تمت إضافة أو نشر ذكر جديد. افتح التطبيق للاطلاع.'
    });
  }

  renderTable();
  renderCategories();
  closeEditor();
  toast(item.published ? 'تم حفظ الذكر ونشره' : 'تم حفظ الذكر كمسودة');
}

async function persistSave(item) {
  if (!firestore) return;
  try {
    const payload = { ...item, order: Number(item.order) || 0 };
    await firestore
      .collection('content')
      .doc('azkar')
      .collection('items')
      .doc(item.id)
      .set(payload, { merge: true });
  } catch (e) {
    console.error(e);
    toast('تم الحفظ محلياً، تعذّر الحفظ السحابي');
  }
}

async function persistDelete(item) {
  if (!firestore || !item?.id) return;
  try {
    await firestore
      .collection('content')
      .doc('azkar')
      .collection('items')
      .doc(item.id)
      .delete();
  } catch (e) {
    console.error(e);
  }
}

// ---- Feedback ----
function renderFeedbackList() {
  const filter = $('#feedback-status-filter')?.value || 'all';
  const list = $('#feedback-list');
  if (!list) return;

  let items = [...feedbackItems].sort(
    (a, b) => (b.createdAt || 0) - (a.createdAt || 0)
  );
  if (filter !== 'all') {
    items = items.filter((f) => (f.status || 'new') === filter);
  }

  if (!items.length) {
    list.innerHTML = '<p class="empty-hint">لا توجد رسائل.</p>';
    return;
  }

  list.innerHTML = items
    .map((f) => {
      const active = f.id === selectedFeedbackId ? ' active' : '';
      const st = f.status || 'new';
      return `
      <button type="button" class="feedback-item${active}" data-fid="${escapeHtml(f.id)}">
        <strong>${escapeHtml(f.title || f.type || 'بدون عنوان')}</strong>
        <small>${escapeHtml((f.message || '').slice(0, 80))}${(f.message || '').length > 80 ? '…' : ''}</small>
        <span class="pill status-${st === 'resolved' ? 'published' : 'draft'}">${statusLabel(st)}</span>
      </button>`;
    })
    .join('');

  $$('[data-fid]').forEach((btn) =>
    btn.addEventListener('click', () => {
      selectedFeedbackId = btn.dataset.fid;
      renderFeedbackList();
      renderFeedbackDetail();
    })
  );
}

function statusLabel(st) {
  if (st === 'new') return 'جديد';
  if (st === 'in_progress') return 'قيد المعالجة';
  if (st === 'resolved') return 'مغلق';
  return st;
}

function renderFeedbackDetail() {
  const box = $('#feedback-detail');
  if (!box) return;
  const f = feedbackItems.find((x) => x.id === selectedFeedbackId);
  if (!f) {
    box.innerHTML = '<p class="empty-hint">اختر رسالة من القائمة.</p>';
    return;
  }

  const urls = Array.isArray(f.attachmentUrls) ? f.attachmentUrls : [];
  const attachmentsHtml = urls.length
    ? `<div class="attachments"><strong>مرفقات</strong>${urls
        .map(
          (u, i) =>
            `<a href="${escapeHtml(u)}" target="_blank" rel="noopener">مرفق ${i + 1}</a>`
        )
        .join(' · ')}</div>`
    : '';

  box.innerHTML = `
    <div class="feedback-detail-head">
      <h3>${escapeHtml(f.title || 'بدون عنوان')}</h3>
      <span class="pill">${statusLabel(f.status || 'new')}</span>
    </div>
    <p class="meta">النوع: ${escapeHtml(f.type || '—')} · المستخدم: ${escapeHtml(f.userEmail || f.userId || '—')}</p>
    <div class="feedback-body">${escapeHtml(f.message || '')}</div>
    ${attachmentsHtml}
    ${f.adminReply ? `<div class="admin-reply"><strong>ردك السابق:</strong><p>${escapeHtml(f.adminReply)}</p></div>` : ''}
    <label>الرد على المستخدم
      <textarea id="reply-text" rows="4">${escapeHtml(f.adminReply || '')}</textarea>
    </label>
    <label>تحديث الحالة
      <select id="reply-status">
        <option value="new" ${(f.status || 'new') === 'new' ? 'selected' : ''}>جديد</option>
        <option value="in_progress" ${f.status === 'in_progress' ? 'selected' : ''}>قيد المعالجة</option>
        <option value="resolved" ${f.status === 'resolved' ? 'selected' : ''}>مغلق</option>
      </select>
    </label>
    <label class="publish-toggle" style="margin-top:8px">
      <span><strong>طلب إشعار للمستخدم</strong></span>
      <input id="reply-notify" type="checkbox" checked />
    </label>
    <div class="modal-actions" style="margin-top:12px">
      <button type="button" class="primary" id="send-reply">حفظ الرد</button>
    </div>`;

  $('#send-reply')?.addEventListener('click', () => submitReply(f.id));
}

async function submitReply(feedbackId) {
  if (!firestore) {
    toast('غير متصل بـ Firebase');
    return;
  }
  const reply = $('#reply-text')?.value.trim() || '';
  const status = $('#reply-status')?.value || 'in_progress';
  const wantNotify = !!$('#reply-notify')?.checked;
  const f = feedbackItems.find((x) => x.id === feedbackId);

  try {
    const payload = {
      adminReply: reply,
      status,
      replyUnread: true,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp()
    };
    await firestore.collection('feedback').doc(feedbackId).set(payload, { merge: true });

    if (wantNotify && f) {
      await enqueueAdminJob('feedback_reply', {
        feedbackId,
        userId: f.userId || null,
        title: 'رد على رسالتك',
        body: reply ? reply.slice(0, 120) : 'يوجد رد جديد من فريق الباقيات'
      });
    }

    const idx = feedbackItems.findIndex((x) => x.id === feedbackId);
    if (idx >= 0) {
      feedbackItems[idx] = {
        ...feedbackItems[idx],
        adminReply: reply,
        status,
        replyUnread: true
      };
    }
    updateStats();
    renderFeedbackList();
    renderFeedbackDetail();
    toast('تم حفظ الرد');
  } catch (e) {
    console.error(e);
    toast('تعذر حفظ الرد');
  }
}

function subscribeFeedback() {
  if (!firestore) return;
  if (feedbackUnsub) {
    feedbackUnsub();
    feedbackUnsub = null;
  }
  try {
    feedbackUnsub = firestore.collection('feedback').onSnapshot(
      (snap) => {
        feedbackItems = snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            ...data,
            createdAt: data.createdAt?.toMillis?.() || data.createdAt || 0
          };
        });
        updateStats();
        if ($('#feedback')?.classList.contains('active-view')) {
          renderFeedbackList();
          renderFeedbackDetail();
        }
      },
      (err) => console.error('feedback listener', err)
    );
  } catch (e) {
    console.error(e);
  }
}

async function sendBroadcastFromForm() {
  const title = $('#broadcast-title')?.value.trim();
  const body = $('#broadcast-body')?.value.trim();
  if (!title || !body) {
    toast('اكتب عنواناً ونصاً');
    return;
  }
  await enqueueAdminJob('broadcast', { title, body });
  toast('تم تسجيل طلب الإشعار');
  if ($('#broadcast-title')) $('#broadcast-title').value = '';
  if ($('#broadcast-body')) $('#broadcast-body').value = '';
}

async function loadRemote() {
  if (!firestore) return;
  try {
    const snap = await firestore
      .collection('content')
      .doc('azkar')
      .collection('items')
      .get();
    if (!snap.empty) {
      azkar.splice(
        0,
        azkar.length,
        ...snap.docs.map((d) => ({ id: d.id, ...d.data() }))
      );
      azkar.sort((a, b) => (Number(a.order) || 0) - (Number(b.order) || 0));
      renderTable();
      renderCategories();
      toast('تم تحديث المحتوى');
    } else renderTable();
    subscribeFeedback();
  } catch (e) {
    console.error(e);
    toast('تعذر تحميل المحتوى السحابي');
  }
}

async function setupAuth() {
  const config = window.AZKAR_FIREBASE_CONFIG;
  try {
    if (typeof firebase === 'undefined') {
      if ($('#auth-message'))
        $('#auth-message').textContent = 'مكتبة Firebase غير محملة.';
      return;
    }
    if (!firebase.apps.length) firebase.initializeApp(config);
    firestore = firebase.firestore();

    firebase.auth().onAuthStateChanged(async (user) => {
      if (!user) {
        $('#auth-gate')?.classList.remove('hidden');
        $('.shell')?.classList.remove('ready');
        if (feedbackUnsub) {
          feedbackUnsub();
          feedbackUnsub = null;
        }
        if ($('#auth-message'))
          $('#auth-message').textContent = 'سجّل الدخول بحساب المشرف.';
        return;
      }
      const emailLower = (user.email || '').toLowerCase().trim();
      const isOwner =
        emailLower === 'mgedh.9ali@gmail.com' ||
        emailLower.includes('mgedh.9ali');
      if (!isOwner) {
        await firebase.auth().signOut();
        if ($('#auth-message'))
          $('#auth-message').textContent = 'هذا الحساب ليس ضمن المشرفين.';
        return;
      }
      $('#auth-gate')?.classList.add('hidden');
      $('.shell')?.classList.add('ready');
      if ($('#admin-email')) $('#admin-email').textContent = user.email || '';
      await loadRemote();
    });
  } catch (e) {
    console.error(e);
    if ($('#auth-message'))
      $('#auth-message').textContent = 'تعذر تهيئة Firebase: ' + e.message;
  }
}

$('#login-btn')?.addEventListener('click', async () => {
  try {
    if (typeof firebase === 'undefined') return;
    const config = window.AZKAR_FIREBASE_CONFIG;
    if (!firebase.apps.length) firebase.initializeApp(config);
    await firebase.auth().signInWithPopup(new firebase.auth.GoogleAuthProvider());
  } catch (e) {
    console.error(e);
    if ($('#auth-message'))
      $('#auth-message').textContent = 'فشل الدخول: ' + e.message;
  }
});

$('#logout-btn')?.addEventListener('click', async () => {
  try {
    await firebase.auth().signOut();
    toast('تم تسجيل الخروج');
  } catch (e) {
    console.error(e);
  }
});

$('#search')?.addEventListener('input', renderTable);
$('#filter')?.addEventListener('change', renderTable);
$('#status-filter')?.addEventListener('change', renderTable);
$('#show-archived')?.addEventListener('change', renderTable);
$('#feedback-status-filter')?.addEventListener('change', renderFeedbackList);
$('#add-zekr')?.addEventListener('click', () => openEditor());
$('#save-zekr')?.addEventListener('click', () => saveEditor());
$('#close-modal')?.addEventListener('click', closeEditor);
$('#cancel-modal')?.addEventListener('click', closeEditor);
$('#editor-modal')?.addEventListener('click', (e) => {
  if (e.target.id === 'editor-modal') closeEditor();
});
$('#close-preview')?.addEventListener('click', closePreview);
$('#preview-modal')?.addEventListener('click', (e) => {
  if (e.target.id === 'preview-modal') closePreview();
});
$('#refresh-btn')?.addEventListener('click', () => loadRemote());
$('#preview-btn')?.addEventListener('click', () =>
  toast('افتح التطبيق لمعاينة المحتوى المنشور')
);
$('#send-broadcast')?.addEventListener('click', () => sendBroadcastFromForm());
$('#export-json')?.addEventListener('click', () => exportJson());

renderTable();
renderCategories();
setupAuth();
