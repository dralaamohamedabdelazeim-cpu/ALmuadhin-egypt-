# ALmuadhin - App module fixed structure

تم إصلاح هيكلة مجلد app بحيث تكون ملفات Android في أماكنها الصحيحة:

- AndroidManifest.xml -> app/src/main/AndroidManifest.xml
- Resources (res) -> app/src/main/res/...
- Kotlin sources -> app/src/main/java/com/example/almuadhin/...

كذلك:
- تم حذف مجلد build داخل app لأنه ناتج بناء ولا يلزم مشاركته.
- تم ضبط اسم التطبيق إلى ALmuadhin في strings.xml.
- تم إضافة values-night/themes.xml لضمان وجود الثيم في الوضع الليلي.

## ملاحظة مهمة للبناء (Compose / Ads / Navigation / Hilt)
هذا الملف هو "app module" فقط. لكي ينجح build لمشروعك بالكامل، تأكد أن app/build.gradle.kts لديك يحتوي على إعدادات Compose واعتماديات:
- androidx.compose (BOM)
- material3
- navigation-compose
- play-services-ads

وإذا كنت على Kotlin 2.x ستحتاج Plugin:
- org.jetbrains.kotlin.plugin.compose  (نسخته يجب أن تطابق نسخة Kotlin)

مصادر النسخ الحديثة:
- Compose BOM (مثال 2025.12.00) من مدونة Android Developers.
- play-services-ads (مثال 24.9.0) من Google/مستودع Maven.
- navigation-compose (مثال 2.9.6).
- Hilt (إن رغبت لاحقًا) مثل 2.58.
