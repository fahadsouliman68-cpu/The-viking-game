package com.theviking.lounge

import kotlinx.serialization.Serializable

/** جزء زمني من الجلسة: النوع (2 أو 4 لاعبين) + سعر الساعة المثبّت لحظة البدء. */
@Serializable data class Seg(val mode: Int, val rate: Long, val start: Long, val end: Long? = null)

@Serializable data class Countdown(val price: Long, val allotMs: Long, val done: Boolean = false)

@Serializable data class Session(
    val segs: List<Seg>,
    val paused: Boolean = false,
    val cd: Countdown? = null,
    val items: Map<String, Int> = emptyMap(),
)

@Serializable data class Device(
    val id: String,
    val name: String,
    val rate2: Long? = null,
    val rate4: Long? = null,
    val session: Session? = null,
)

@Serializable data class MenuItem(
    val id: String,
    val name: String,
    val nameEn: String = "",
    val price: Long = 0,
    val stock: Int? = null,
)

@Serializable data class Pack(val id: String, val name: String, val mode: Int, val minutes: Int, val price: Long)

@Serializable data class Cfg(
    val rate2: Long = 7000,
    val rate4: Long = 14000,
    val roundStep: Long = 100,
    val minMinutes: Int = 0,
    val currency: String = "ل.س",
    val alarmSound: String = "",          // URI لصوت المنبّه، فارغ = الصوت الأساسي للنظام
    val themeMode: String = "viking",     // viking | light | ocean | forest | crimson | custom
    val customBg: String = "0A0F14",
    val customAccent: String = "D9A441",
)

@Serializable data class TimeLine(val mode: Int, val minutes: Int, val amount: Long, val pack: Boolean = false)

@Serializable data class ItemLine(val id: String, val name: String, val qty: Int, val price: Long)

@Serializable data class Rec(
    val id: String,
    val ts: Long,
    val shift: Int,
    val kind: String, // "session" | "sale"
    val device: String = "",
    val times: List<TimeLine> = emptyList(),
    val items: List<ItemLine> = emptyList(),
    val discount: Long = 0,
    val paid: Boolean = true,
    val paidShift: Int = 0,
    val customer: String = "",
    val note: String = "",
    val history: List<String> = emptyList(),
) {
    val total: Long
        get() = (times.sumOf { it.amount } + items.sumOf { it.price * it.qty } - discount).coerceAtLeast(0L)
}

@Serializable data class Expense(val id: String, val ts: Long, val shift: Int, val title: String, val amount: Long)

@Serializable data class ClosedShift(
    val id: Int,
    val opened: Long,
    val closed: Long,
    val revenue: Long,
    val debts: Long,
    val expenses: Long,
    val count: Int,
)

@Serializable data class AppState(
    val devices: List<Device>,
    val menu: List<MenuItem>,
    val packs: List<Pack> = emptyList(),
    val cfg: Cfg = Cfg(),
    val log: List<Rec> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val shifts: List<ClosedShift> = emptyList(),
    val shift: Int = 1,
    val shiftOpened: Long = System.currentTimeMillis(),
    val lang: String = "ar",
    val lastExport: Long = 0,
)

data class UndoInfo(val recordId: String, val prevDevice: Device?, val until: Long)

fun uid(): String = java.util.UUID.randomUUID().toString().take(8)

fun defaultState() = AppState(
    devices = listOf("I", "II", "III", "IV").mapIndexed { i, n -> Device("d$i", n) },
    menu = listOf(
        MenuItem("indomie", "اندومي", "Indomie"),
        MenuItem("hot", "مشروب ساخن", "Hot drink"),
        MenuItem("tea", "شاي", "Tea"),
        MenuItem("nescafe", "نسكافيه", "Nescafe"),
        MenuItem("energy", "مشروب طاقة", "Energy drink"),
    ),
)

fun MenuItem.label(lang: String): String = if (lang == "en" && nameEn.isNotBlank()) nameEn else name
