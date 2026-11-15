package com.theviking.lounge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalLang = compositionLocalOf { "ar" }

@Composable fun t(k: String): String = tr(LocalLang.current, k)

fun tr(lang: String, k: String): String =
    (if (lang == "en") EN else AR)[k] ?: (if (lang == "en") EN_X else AR_X)[k] ?: k

private val AR = mapOf(
    "sub" to "صالة بلايستيشن", "tab_dev" to "الأجهزة", "tab_sale" to "بيع مباشر", "tab_log" to "السجل",
    "tab_cash" to "الصندوق", "tab_set" to "الإعدادات",
    "free" to "متاح", "p2" to "لاعبين", "p4" to "أربع لاعبين", "hour" to "/ ساعة",
    "byPrice" to "فتح وقت بسعر", "price" to "السعر", "packs" to "الباقات", "time" to "الوقت", "drinks" to "المشروبات",
    "pause" to "إيقاف مؤقت", "resume" to "متابعة", "paused" to "متوقف مؤقتاً", "stop" to "إيقاف وحساب",
    "adjust" to "تعديل الوقت", "elapsed" to "الوقت المنقضي (دقائق)", "startedAt" to "بدأ الساعة", "apply" to "تطبيق",
    "cancel" to "إلغاء", "save" to "حفظ", "close" to "إغلاق", "delete" to "حذف", "confirm" to "تأكيد",
    "total" to "المجموع", "discount" to "خصم", "paid" to "مدفوع", "debt" to "دين", "customer" to "اسم الزبون",
    "note" to "ملاحظة", "left" to "المتبقي", "over" to "تجاوز الوقت", "timeUp" to "انتهى الوقت", "silence" to "إسكات",
    "undo" to "تراجع", "saved" to "تم الحفظ", "receipt" to "إيصال", "noPrice" to "⚠ حدّد السعر من الإعدادات",
    "stockLeft" to "المتبقي", "duration" to "المدة", "device" to "الجهاز", "lang" to "EN",
    "directSale" to "بيع مباشر", "cart" to "السلة", "sell" to "بيع", "freeAmt" to "مبلغ حر", "freeNote" to "البيان",
    "log" to "السجل", "exportCsv" to "تصدير Excel (CSV)", "shiftN" to "الوردية", "current" to "الحالية",
    "noRecords" to "لا توجد عمليات بعد.", "editRecord" to "تعديل السجل", "min" to "د", "qty" to "العدد",
    "history" to "سجل التعديلات", "edited" to "تعديل", "addItem" to "إضافة بند", "confirmDelete" to "اضغط مرة ثانية للحذف",
    "settled" to "تسديد دين",
    "shiftSummary" to "ملخص الوردية الحالية", "revenue" to "المقبوض", "debts" to "الديون", "expenses" to "المصاريف",
    "net" to "الصافي", "addExpense" to "إضافة مصروف", "expTitle" to "البيان", "amount" to "المبلغ",
    "closeShift" to "إغلاق الوردية", "closeShiftMsg" to "سيتم ترحيل الأرقام للأرشيف وبدء وردية جديدة، ونسخة احتياطية تلقائية.",
    "runningWarn" to "تنبيه: توجد أجهزة ما زالت تعمل (ستبقى مفتوحة وتُحسب في الوردية القادمة).",
    "pastShifts" to "الورديات السابقة", "perDevice" to "الدخل لكل جهاز", "settle" to "تسديد", "noDebts" to "لا ديون.",
    "language" to "اللغة", "rates" to "الأسعار والحساب", "rate2" to "سعر ساعة لاعبين", "rate4" to "سعر ساعة أربع لاعبين",
    "roundStep" to "التقريب لأقرب (مبلغ)", "minMinutes" to "حد أدنى للجلسة (دقائق)", "currency" to "العملة",
    "devices" to "الأجهزة", "addDevice" to "إضافة جهاز", "deviceName" to "اسم الجهاز", "rateBlank" to "فارغ = الافتراضي",
    "menu" to "المشروبات والبنود", "itemName" to "الاسم (عربي)", "itemNameEn" to "الاسم (إنكليزي)", "stock" to "المخزون (فارغ = غير محدود)",
    "packsT" to "الباقات", "packName" to "اسم الباقة", "packMode" to "النوع (2 = لاعبين، 4 = أربع لاعبين)", "packMinutes" to "المدة بالدقائق",
    "backup" to "النسخ الاحتياطي", "exportJson" to "تصدير نسخة احتياطية", "importJson" to "استيراد نسخة", "lastExport" to "آخر تصدير",
    "never" to "لم يتم", "backupWarn" to "لم تأخذ نسخة احتياطية منذ أكثر من يوم. احفظ نسخة الآن.", "importOk" to "تم الاستيراد",
    "importFail" to "ملف غير صالح", "newItem" to "جديد", "add" to "إضافة", "stopQ" to "حساب الجهاز",
    "sec" to "تنبيه الوقت يعمل حتى عند إغلاق الشاشة بشرط السماح بالإشعارات والمنبّهات.",
)

private val EN = mapOf(
    "sub" to "PlayStation Lounge", "tab_dev" to "Devices", "tab_sale" to "Direct sale", "tab_log" to "Log",
    "tab_cash" to "Cash", "tab_set" to "Settings",
    "free" to "Free", "p2" to "2 Players", "p4" to "4 Players", "hour" to "/ hour",
    "byPrice" to "Open time by price", "price" to "Price", "packs" to "Packages", "time" to "Time", "drinks" to "Drinks",
    "pause" to "Pause", "resume" to "Resume", "paused" to "Paused", "stop" to "Stop & bill",
    "adjust" to "Adjust time", "elapsed" to "Elapsed time (minutes)", "startedAt" to "Started at", "apply" to "Apply",
    "cancel" to "Cancel", "save" to "Save", "close" to "Close", "delete" to "Delete", "confirm" to "Confirm",
    "total" to "Total", "discount" to "Discount", "paid" to "Paid", "debt" to "Debt", "customer" to "Customer name",
    "note" to "Note", "left" to "Time left", "over" to "Overtime", "timeUp" to "Time is up", "silence" to "Silence",
    "undo" to "Undo", "saved" to "Saved", "receipt" to "Receipt", "noPrice" to "⚠ Set the price in Settings",
    "stockLeft" to "Left", "duration" to "Duration", "device" to "Device", "lang" to "ع",
    "directSale" to "Direct sale", "cart" to "Cart", "sell" to "Sell", "freeAmt" to "Custom amount", "freeNote" to "Description",
    "log" to "Log", "exportCsv" to "Export Excel (CSV)", "shiftN" to "Shift", "current" to "current",
    "noRecords" to "Nothing logged yet.", "editRecord" to "Edit record", "min" to "min", "qty" to "Qty",
    "history" to "Edit history", "edited" to "Edited", "addItem" to "Add item", "confirmDelete" to "Tap again to delete",
    "settled" to "Debt settled",
    "shiftSummary" to "Current shift summary", "revenue" to "Collected", "debts" to "Debts", "expenses" to "Expenses",
    "net" to "Net", "addExpense" to "Add expense", "expTitle" to "Description", "amount" to "Amount",
    "closeShift" to "Close shift", "closeShiftMsg" to "Figures will be archived, a new shift starts, and an automatic backup is saved.",
    "runningWarn" to "Note: some devices are still running (they stay open and count in the next shift).",
    "pastShifts" to "Past shifts", "perDevice" to "Revenue per device", "settle" to "Settle", "noDebts" to "No debts.",
    "language" to "Language", "rates" to "Rates & billing", "rate2" to "2 Players hourly rate", "rate4" to "4 Players hourly rate",
    "roundStep" to "Round to nearest", "minMinutes" to "Minimum session (minutes)", "currency" to "Currency",
    "devices" to "Devices", "addDevice" to "Add device", "deviceName" to "Device name", "rateBlank" to "blank = default",
    "menu" to "Drinks & items", "itemName" to "Name (Arabic)", "itemNameEn" to "Name (English)", "stock" to "Stock (blank = unlimited)",
    "packsT" to "Packages", "packName" to "Package name", "packMode" to "Type (2 = 2 Players, 4 = 4 Players)", "packMinutes" to "Minutes",
    "backup" to "Backup", "exportJson" to "Export backup", "importJson" to "Import backup", "lastExport" to "Last export",
    "never" to "never", "backupWarn" to "No backup for over a day. Save a copy now.", "importOk" to "Imported",
    "importFail" to "Invalid file", "newItem" to "New", "add" to "Add", "stopQ" to "Bill device",
    "sec" to "Timer alerts work with the screen off if notifications and alarms are allowed.",
)

private val AR_X = mapOf(
    "alarmSettings" to "المنبّه", "alarmSound" to "صوت المنبّه", "pickSound" to "اختيار الصوت",
    "defaultSound" to "الصوت الأساسي للنظام", "customSound" to "صوت مخصص", "resetSound" to "استعادة الصوت الأساسي",
    "alarmNote" to "يعمل بتدفق المنبّه: يرنّ حتى والهاتف صامت أو على الاهتزاز، ولا يتأثر بصوت الوسائط.",
    "theme" to "الثيم", "themeViking" to "الافتراضي", "themeLight" to "فاتح", "themeOcean" to "محيط",
    "themeForest" to "غابة", "themeCrimson" to "قرمزي", "themeCustom" to "يدوي",
    "bgHex" to "لون الخلفية (HEX)", "accentHex" to "لون التمييز (HEX)", "saveTheme" to "حفظ الثيم",
    "badHex" to "صيغة اللون غير صحيحة، مثال: 1E3A52",
)

private val EN_X = mapOf(
    "alarmSettings" to "Alarm", "alarmSound" to "Alarm sound", "pickSound" to "Choose sound",
    "defaultSound" to "System default alarm", "customSound" to "Custom sound", "resetSound" to "Reset to default",
    "alarmNote" to "Plays on the alarm stream: rings even when the phone is silent or on vibrate, and ignores media volume.",
    "theme" to "Theme", "themeViking" to "Default", "themeLight" to "Light", "themeOcean" to "Ocean",
    "themeForest" to "Forest", "themeCrimson" to "Crimson", "themeCustom" to "Manual",
    "bgHex" to "Background color (HEX)", "accentHex" to "Accent color (HEX)", "saveTheme" to "Save theme",
    "badHex" to "Invalid color, example: 1E3A52",
)
