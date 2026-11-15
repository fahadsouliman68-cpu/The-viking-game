package com.theviking.lounge

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

const val HOUR_MS = 3_600_000.0

fun roundMoney(v: Double, step: Long): Long {
    val s = max(step, 1L)
    return Math.round(v / s) * s
}

fun runMs(s: Session, now: Long): Long = s.segs.sumOf { (it.end ?: now) - it.start }

fun rateOf(d: Device, c: Cfg, mode: Int): Long = if (mode == 2) (d.rate2 ?: c.rate2) else (d.rate4 ?: c.rate4)

fun allotByPrice(price: Long, rate: Long): Long = (price.toDouble() / max(rate, 1L) * HOUR_MS).toLong()

/**
 * حساب الوقت: يُجمَّع الزمن لكل (نوع + سعر مثبّت) ثم يُقرَّب مرة واحدة فقط،
 * فلا يتراكم خطأ التقريب مع الإيقاف المؤقت والتبديل. الأسعار مثبّتة لحظة بدء كل جزء.
 */
fun timeLines(s: Session, now: Long, c: Cfg): List<TimeLine> {
    val run = runMs(s, now)
    val cd = s.cd
    if (cd != null) {
        val over = max(0L, run - cd.allotMs)
        val extra = roundMoney(over / HOUR_MS * s.segs[0].rate, c.roundStep)
        return listOf(TimeLine(s.segs[0].mode, (run / 60000).toInt(), cd.price + extra, true))
    }
    val groups = LinkedHashMap<Pair<Int, Long>, Long>()
    s.segs.forEach { g ->
        val k = g.mode to g.rate
        groups[k] = (groups[k] ?: 0L) + ((g.end ?: now) - g.start)
    }
    val minMs = c.minMinutes * 60000L
    if (run < minMs && groups.isNotEmpty()) {
        val k = groups.keys.first()
        groups[k] = (groups[k] ?: 0L) + (minMs - run)
    }
    return groups.map { (k, ms) -> TimeLine(k.first, (ms / 60000).toInt(), roundMoney(ms / HOUR_MS * k.second, c.roundStep)) }
}

fun itemLines(s: Session, menu: List<MenuItem>, lang: String): List<ItemLine> =
    menu.filter { (s.items[it.id] ?: 0) > 0 }.map { ItemLine(it.id, it.label(lang), s.items[it.id] ?: 0, it.price) }

fun buildRecord(d: Device, now: Long, st: AppState, discount: Long, paid: Boolean, customer: String): Rec {
    val s = d.session ?: error("no session")
    return Rec(
        id = uid(), ts = now, shift = st.shift, kind = "session", device = d.name,
        times = timeLines(s, now, st.cfg), items = itemLines(s, st.menu, st.lang),
        discount = discount, paid = paid, paidShift = if (paid) st.shift else 0, customer = customer,
    )
}

fun applyStock(menu: List<MenuItem>, old: List<ItemLine>, new: List<ItemLine>): List<MenuItem> = menu.map { m ->
    val stock = m.stock
    if (stock == null) m else {
        val o = old.filter { it.id == m.id }.sumOf { it.qty }
        val n = new.filter { it.id == m.id }.sumOf { it.qty }
        m.copy(stock = (stock + o - n).coerceAtLeast(0))
    }
}

fun setElapsed(s: Session, target: Long, now: Long): Session {
    val delta = target - runMs(s, now)
    var segs = s.segs
    if (delta > 0) {
        segs = segs.toMutableList().also { it[0] = it[0].copy(start = it[0].start - delta) }
    } else if (delta < 0) {
        var r = -delta
        segs = segs.map { g ->
            if (r <= 0) g else {
                val dur = (g.end ?: now) - g.start
                val k = minOf(dur, r)
                r -= k
                g.copy(start = g.start + k)
            }
        }
    }
    val moved = s.copy(segs = segs)
    val cd = s.cd
    return if (cd != null) moved.copy(cd = cd.copy(done = runMs(moved, now) >= cd.allotMs)) else moved
}

fun money(n: Long, cur: String): String = String.format(Locale.US, "%,d", n) + " " + cur
fun clock(ms: Long): String {
    val s = max(ms, 0L) / 1000
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60)
}
fun fmtDate(ts: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))
fun fmtTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(ts))
fun modeName(lang: String, m: Int): String = tr(lang, if (m == 2) "p2" else "p4")

fun receiptText(r: Rec, st: AppState, lang: String): String {
    val ar = lang != "en"
    val cur = st.cfg.currency
    val sb = StringBuilder("THE VIKING\n${fmtDate(r.ts)} ${fmtTime(r.ts)}\n")
    if (r.device.isNotBlank()) sb.append(if (ar) "الجهاز: " else "Device: ").append(r.device).append('\n')
    r.times.forEach { sb.append("${modeName(lang, it.mode)} ${it.minutes} ${if (ar) "د" else "min"}: ${money(it.amount, cur)}\n") }
    r.items.forEach { sb.append("${it.name} × ${it.qty}: ${money(it.price * it.qty, cur)}\n") }
    if (r.discount > 0) sb.append(tr(lang, "discount")).append(": -").append(money(r.discount, cur)).append('\n')
    sb.append(tr(lang, "total")).append(": ").append(money(r.total, cur))
    if (!r.paid) sb.append("\n").append(tr(lang, "debt")).append(": ").append(r.customer)
    return sb.toString()
}
