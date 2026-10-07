# المرحلة 1: النظام البصري للتطبيق

## الملفات
| الملف | الدور |
|---|---|
| `ui/theme/Color.kt` | أربع لوحات (نهار، فجر، مغرب، ليل) + `resolveTimeMode` |
| `ui/theme/Theme.kt` | `BaqiyatTheme(mode)` مع انتقال لوني 500م.ث، وتثبيت RTL، ودعم «تقليل الحركة» |
| `ui/theme/Type.kt` | Amiri للذكر (`ZekrTextStyle`) وIBM Plex Sans Arabic للواجهة، والخطوط مضمّنة في `res/font` |
| `ui/theme/Shapes.kt` | سلّم الأنصاف 8/12/20/28/36 و`MihrabShape` |
| `ui/components/BaqiyatComponents.kt` | `BaqiyatBackground`, `GlassCard`, `MihrabCard`, `BaqiyatButton`, `BaqiyatChip`, `ProgressRing`, `FloatingNavBar` |
| `design/tokens.json` | المرجع المشترك مع اللوحة (المرحلة 6) |

## قواعد الاستعمال
- الشاشات تستعمل `containerColor = Transparent` فوق `BaqiyatBackground` الموضوعة في الجذر.
- `MihrabCard` لبطاقة الذكر فقط. باقي البطاقات `GlassCard`.
- الألوان من `MaterialTheme.colorScheme` أو `Baqiyat.colors`، ولا ألوان ثابتة داخل الشاشات.
- الوضع الفاتح = نهار دائماً. الوضع الداكن يتبدل بين فجر (3–6) ومغرب (17–18) وليل. تُستبدل الساعات بمواقيت الصلاة في المرحلة 7.
- «الزجاج» هنا سطح شبه شفاف بحد متدرج، وليس ضبابية حقيقية للخلفية (أخف على الأجهزة الضعيفة).

## ما لم يُنقل بعد
شاشات الإعدادات والإحصائيات والشكاوى والمساعد والبحث ترث الألوان والخطوط والأشكال تلقائياً، لكن مكوّناتها ما زالت Material الافتراضية. تُعاد صياغتها في المراحل 2–4.
