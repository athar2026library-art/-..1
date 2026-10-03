# المرحلة 2 — الأمان والخصوصية (ما تم في الكود + ما تفعله أنت)

## تم في المستودع

- `generateGemini` Callable: `europe-west1`، `enforceAppCheck`، مصادقة، `mode`/`text`، حد طول 500، سر `GEMINI_API_KEY`، system prompt على الخادم، حد معدل `rateLimits/{uid}` (3/دقيقة، 30/يوم).
- `deleteAccount` Callable: حذف `users/{uid}` وفروعه + feedback + Auth.
- `AiRepository` يستدعي الدالة فقط (لا مفتاح في APK).
- قواعد Firestore/Storage موحّدة: claims/`admins` فقط (لا إيميل)، `rateLimits` و`audit_logs` للكتابة من الخادم فقط، feedback ≤ 1000 حرف.
- استبعاد قاعدة Room من cloud backup.
- صفحات `docs/legal/privacy.html` و`delete-account.html`.
- زر حذف الحساب في الإعدادات.

## أوامر عندك (بعد `firebase login`)

```bash
cd firebase
firebase use svrpmtt
firebase functions:secrets:set GEMINI_API_KEY
# عيّن claim أدمن (عدّل UID في make_admin.js):
cd functions && npm ci && node make_admin.js && cd ..
firebase deploy --only firestore:rules,storage,functions
```

## App Check (ترتيب)

1. سجّل التطبيق: Play Integrity + SHA-256.
2. Debug token من Logcat → Console.
3. Monitor أسبوعاً.
4. بعدها Enforce على Functions/Firestore/Storage.

## فوترة

Google Cloud → Billing → Budget alert على مشروع `svrpmtt` بحد منخفض.

## نظافة المفاتيح

```bash
# محلياً إن وُجد gitleaks
gitleaks detect --source . --log-opts="--all"
```

إن ظهر مفتاح Gemini قديماً في أي commit: **أعد توليده** واحفظه كـ Secret فقط.
قيّد مفتاح Android (`AIza...`) بحزمة `com.baqiyat.azkar` وبصمة التوقيع.

## استضافة الصفحات القانونية

انشر `docs/legal/` على Firebase Hosting أو أي استضافة عامة، وضع الروابط في Play Console → Data safety / Account deletion.
