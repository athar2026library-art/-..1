const azkar = [
  {text:'اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ', category:'sabah', repeat:1, source:'رواه الحاكم وصححه الألباني'},
  {text:'أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ', category:'sabah', repeat:1, source:'رواه مسلم'},
  {text:'اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ', category:'sabah', repeat:1, source:'رواه البخاري'},
  {text:'بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ', category:'sabah', repeat:3, source:'رواه أبو داود والترمذي'},
  {text:'سُبْحَانَ اللَّهِ وَبِحَمْدِهِ', category:'sabah', repeat:100, source:'رواه مسلم'},
  {text:'أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ', category:'masaa', repeat:1, source:'رواه مسلم'},
  {text:'اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا', category:'masaa', repeat:1, source:'رواه الترمذي'},
  {text:'رَضِيتُ بِاللَّهِ رَبًّا وَبِالإِسْلَامِ دِينًا', category:'masaa', repeat:3, source:'رواه أبو داود والترمذي'}
];
const categories = [{icon:'☀',name:'أذكار الصباح',desc:'ورد بداية اليوم',count:8},{icon:'☾',name:'أذكار المساء',desc:'ورد نهاية اليوم',count:8},{icon:'✦',name:'أذكار مختارة',desc:'قريباً · مجموعة مخصصة',count:0}];
const $=s=>document.querySelector(s); const $$=s=>document.querySelectorAll(s);
function toast(message){const el=$('#toast');el.textContent=message;el.classList.add('show');setTimeout(()=>el.classList.remove('show'),2600)}
function go(view){$$('.view').forEach(x=>x.classList.remove('active-view'));$('#'+view).classList.add('active-view');$$('.nav-item').forEach(x=>x.classList.toggle('active',x.dataset.view===view));$('#page-title').textContent={overview:'نظرة عامة',azkar:'مكتبة الأذكار',categories:'التصنيفات',settings:'إعدادات التطبيق'}[view]}
$$('[data-view]').forEach(el=>el.addEventListener('click',()=>go(el.dataset.view)));
function renderTable(){const q=($('#search')?.value||'').trim();const filter=$('#filter')?.value||'all';const rows=azkar.filter(z=>(filter==='all'||z.category===filter)&&z.text.includes(q));$('#stat-total').textContent=azkar.length+8;$('#azkar-table').innerHTML=rows.map((z,i)=>`<tr><td>${z.text}</td><td><span class="pill">${z.category==='sabah'?'الصباح':'المساء'}</span></td><td>${z.repeat}×</td><td>${z.source}</td><td><button class="icon-btn" data-delete="${i}">حذف</button></td></tr>`).join('')||'<tr><td colspan="5">لا توجد نتائج مطابقة.</td></tr>';$$('[data-delete]').forEach(btn=>btn.addEventListener('click',()=>{if(confirm('حذف هذا الذكر من المسودة المحلية؟')){azkar.splice(Number(btn.dataset.delete),1);renderTable();toast('تم حذف الذكر من المسودة')}}))}
function renderCategories(){$('#category-grid').innerHTML=categories.map(c=>`<article class="category-card"><div class="symbol">${c.icon}</div><h3>${c.name}</h3><p>${c.desc}</p><p style="margin-top:16px;color:var(--green)">${c.count} أذكار</p></article>`).join('')}
$('#search')?.addEventListener('input',renderTable);$('#filter')?.addEventListener('change',renderTable);
$('#add-zekr')?.addEventListener('click',()=>{const text=prompt('اكتب نص الذكر:');if(!text?.trim())return;azkar.unshift({text:text.trim(),category:prompt('التصنيف: sabah أو masaa','sabah')==='masaa'?'masaa':'sabah',repeat:Number(prompt('عدد التكرار','1'))||1,source:'يُضاف المصدر لاحقاً'});renderTable();toast('تمت إضافة الذكر إلى المسودة المحلية')});
$('#add-category')?.addEventListener('click',()=>{const name=prompt('اسم التصنيف الجديد:');if(name?.trim()){categories.push({icon:'✦',name:name.trim(),desc:'تصنيف جديد',count:0});renderCategories();toast('تمت إضافة التصنيف')}});
$('#preview-btn')?.addEventListener('click',()=>toast('المعاينة ستتصل بتطبيق Android عند إضافة backend للنشر'));
renderTable();renderCategories();
