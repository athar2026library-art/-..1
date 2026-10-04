# حل تعارضات PR #1 (main ← feature/private-admin-panel)

السبب: الفرعان لهما تاريخ غير مشترك (unrelated histories) فكل الملفات المشتركة تظهر كـ add/add conflicts.

## الحل (من جهازك المصادق على GitHub)

```bash
git fetch origin
git checkout feature/private-admin-panel
git pull origin feature/private-admin-panel

# دمج main مع السماح بالتاريخ غير المشترك
git merge origin/main --allow-unrelated-histories --no-commit

# الإبقاء على نسخة feature لكل التعارضات
git checkout --ours .
git add -A
git commit -m "merge: resolve conflicts with main (keep feature versions)"
git push origin feature/private-admin-panel
```

بعد الدفع حدّث صفحة PR #1 — يفترض أن يختفي التنبيه الأحمر ويظهر زر الدمج الأخضر.

## بديل: فرع جاهز من قاعدة main

يوجد فرع `resolve-merge-main` مبني من `main`. بعد اكتمال نسخ الملفات إليه يمكن دمجه مباشرة في `main` بدون تعارضات تاريخ.
