package com.theviking.lounge

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val store = Store(ctx)
    private val io = Executors.newSingleThreadExecutor()

    var st by mutableStateOf(store.load())
        private set
    var now by mutableLongStateOf(System.currentTimeMillis())
        private set
    var undo by mutableStateOf<UndoInfo?>(null)
        private set
    var stopRequest by mutableStateOf<String?>(null)
        private set
    val ringing = mutableStateListOf<String>()

    init {
        st.devices.forEach { syncAlarm(it.id) }
        viewModelScope.launch {
            while (true) {
                now = System.currentTimeMillis()
                checkAlarms()
                delay(1000)
            }
        }
    }

    // ---------- أساسيات ----------
    private fun commit(s: AppState) {
        st = s
        io.execute { store.save(s) }
    }
    private fun upd(f: (AppState) -> AppState) = commit(f(st))
    private fun dev(id: String): Device = st.devices.first { it.id == id }
    private fun updDev(id: String, f: (Device) -> Device) =
        upd { s -> s.copy(devices = s.devices.map { if (it.id == id) f(it) else it }) }
    private fun updSes(id: String, f: (Session) -> Session) =
        updDev(id) { d -> d.session?.let { d.copy(session = f(it)) } ?: d }

    fun m(n: Long): String = money(n, st.cfg.currency)
    fun setLang(l: String) = upd { it.copy(lang = l) }

    // ---------- المنبّه ----------
    private fun syncAlarm(id: String) {
        val d = st.devices.firstOrNull { it.id == id } ?: return
        Alarms.cancel(ctx, id)
        val s = d.session ?: return
        val cd = s.cd ?: return
        if (s.paused || cd.done) return
        val left = cd.allotMs - runMs(s, System.currentTimeMillis())
        Alarms.schedule(ctx, id, d.name, System.currentTimeMillis() + left.coerceAtLeast(500L))
    }

    private fun checkAlarms() {
        val n = now
        st.devices.forEach { d ->
            val s = d.session ?: return@forEach
            val cd = s.cd ?: return@forEach
            if (!s.paused && !cd.done && runMs(s, n) >= cd.allotMs) {
                updSes(d.id) { it.copy(cd = it.cd?.copy(done = true)) }
                if (d.id !in ringing) ringing.add(d.id)
                AlarmPlayer.start(ctx)
            }
        }
    }

    fun silence() {
        ringing.clear()
        AlarmPlayer.stop()
    }
    fun requestStop(id: String) { stopRequest = id }
    fun clearStopRequest() { stopRequest = null }

    // ---------- الجلسات ----------
    private fun startWith(id: String, mode: Int, mk: ((Long) -> Countdown)?) {
        val d = dev(id)
        if (d.session != null) return
        val rate = rateOf(d, st.cfg, mode)
        val n = System.currentTimeMillis()
        updDev(id) { it.copy(session = Session(listOf(Seg(mode, rate, n)), cd = mk?.invoke(rate))) }
        syncAlarm(id)
    }
    fun start(id: String, mode: Int) = startWith(id, mode, null)
    fun startPrice(id: String, mode: Int, price: Long) = startWith(id, mode) { rate -> Countdown(price, allotByPrice(price, rate)) }
    fun startPack(id: String, p: Pack) = startWith(id, p.mode) { Countdown(p.price, p.minutes * 60000L) }

    fun switchMode(id: String, mode: Int) {
        val d = dev(id)
        val s = d.session ?: return
        if (s.paused || s.cd != null || s.segs.last().mode == mode) return
        val n = System.currentTimeMillis()
        val segs = s.segs.dropLast(1) + s.segs.last().copy(end = n) + Seg(mode, rateOf(d, st.cfg, mode), n)
        updSes(id) { it.copy(segs = segs) }
    }

    fun pause(id: String) {
        val s = dev(id).session ?: return
        if (s.paused) return
        val n = System.currentTimeMillis()
        updSes(id) { it.copy(segs = it.segs.dropLast(1) + it.segs.last().copy(end = n), paused = true) }
        syncAlarm(id)
    }

    fun resume(id: String) {
        val s = dev(id).session ?: return
        if (!s.paused) return
        val last = s.segs.last()
        updSes(id) { it.copy(segs = it.segs + Seg(last.mode, last.rate, System.currentTimeMillis()), paused = false) }
        syncAlarm(id)
    }

    fun changeItem(id: String, itemId: String, delta: Int) {
        val m = st.menu.firstOrNull { it.id == itemId } ?: return
        updSes(id) { s ->
            val q = ((s.items[itemId] ?: 0) + delta).coerceAtLeast(0)
            val stock = m.stock
            if (delta > 0 && (m.price <= 0 || (stock != null && q > stock))) s
            else s.copy(items = s.items + (itemId to q))
        }
    }

    fun adjust(id: String, targetMs: Long) {
        val n = System.currentTimeMillis()
        updSes(id) { setElapsed(it, targetMs, n) }
        syncAlarm(id)
    }

    fun preview(id: String, at: Long, discount: Long, paid: Boolean, customer: String): Rec? {
        val d = dev(id)
        if (d.session == null) return null
        return buildRecord(d, at, st, discount, paid, customer)
    }

    fun stop(id: String, discount: Long, paid: Boolean, customer: String) {
        val d = dev(id)
        if (d.session == null) return
        val n = System.currentTimeMillis()
        val r = buildRecord(d, n, st, discount, paid, customer)
        upd { x ->
            x.copy(
                devices = x.devices.map { if (it.id == id) it.copy(session = null) else it },
                log = x.log + r,
                menu = applyStock(x.menu, emptyList(), r.items),
            )
        }
        Alarms.cancel(ctx, id)
        ringing.remove(id)
        if (ringing.isEmpty()) AlarmPlayer.stop()
        undo = UndoInfo(r.id, d, n + 60_000)
    }

    fun undoLast() {
        val u = undo ?: return
        undo = null
        if (System.currentTimeMillis() > u.until) return
        val pd = u.prevDevice
        if (pd != null && dev(pd.id).session != null) return // بدأت جلسة جديدة على الجهاز
        val r = st.log.firstOrNull { it.id == u.recordId }
        upd { x ->
            x.copy(
                devices = if (pd != null) x.devices.map { if (it.id == pd.id) pd else it } else x.devices,
                log = x.log.filter { it.id != u.recordId },
                menu = if (r != null) applyStock(x.menu, r.items, emptyList()) else x.menu,
            )
        }
        if (pd != null) syncAlarm(pd.id)
    }
    fun dismissUndo() { undo = null }

    // ---------- البيع المباشر ----------
    fun sell(cart: Map<String, Int>, freeAmt: Long, freeNote: String, discount: Long, paid: Boolean, customer: String) {
        val lines = st.menu.filter { (cart[it.id] ?: 0) > 0 }
            .map { ItemLine(it.id, it.label(st.lang), cart[it.id] ?: 0, it.price) } +
            (if (freeAmt > 0) listOf(ItemLine("free", freeNote.ifBlank { tr(st.lang, "freeAmt") }, 1, freeAmt)) else emptyList())
        if (lines.isEmpty()) return
        val n = System.currentTimeMillis()
        val r = Rec(uid(), n, st.shift, "sale", items = lines, discount = discount, paid = paid,
            paidShift = if (paid) st.shift else 0, customer = customer)
        upd { x -> x.copy(log = x.log + r, menu = applyStock(x.menu, emptyList(), r.items)) }
        undo = UndoInfo(r.id, null, n + 60_000)
    }

    // ---------- السجل ----------
    fun saveRecord(new: Rec) {
        val old = st.log.firstOrNull { it.id == new.id } ?: return
        if (new.copy(history = old.history) == old) return
        val n = System.currentTimeMillis()
        val h = "${tr(st.lang, "edited")} ${fmtDate(n)} ${fmtTime(n)}: ${old.total} → ${new.total}"
        val ps = if (!new.paid) 0 else if (old.paid) old.paidShift else st.shift
        val r = new.copy(history = old.history + h, paidShift = ps)
        upd { x -> x.copy(log = x.log.map { if (it.id == r.id) r else it }, menu = applyStock(x.menu, old.items, r.items)) }
    }

    fun deleteRecord(id: String) {
        val old = st.log.firstOrNull { it.id == id } ?: return
        upd { x -> x.copy(log = x.log.filter { it.id != id }, menu = applyStock(x.menu, old.items, emptyList())) }
    }

    fun settleDebt(id: String) {
        val n = System.currentTimeMillis()
        upd { x ->
            x.copy(log = x.log.map {
                if (it.id == id) it.copy(paid = true, paidShift = x.shift,
                    history = it.history + "${tr(x.lang, "settled")} ${fmtDate(n)} ${fmtTime(n)}") else it
            })
        }
    }

    // ---------- الصندوق والورديات ----------
    fun addExpense(title: String, amount: Long) {
        if (amount <= 0) return
        upd { x -> x.copy(expenses = x.expenses + Expense(uid(), System.currentTimeMillis(), x.shift, title, amount)) }
    }
    fun deleteExpense(id: String) = upd { x -> x.copy(expenses = x.expenses.filter { it.id != id }) }

    fun closeShift() {
        val s = st
        val n = System.currentTimeMillis()
        val recs = s.log.filter { it.shift == s.shift }
        val cs = ClosedShift(
            id = s.shift, opened = s.shiftOpened, closed = n,
            revenue = s.log.filter { it.paid && it.paidShift == s.shift }.sumOf { it.total },
            debts = recs.filter { !it.paid }.sumOf { it.total },
            expenses = s.expenses.filter { it.shift == s.shift }.sumOf { it.amount },
            count = recs.size,
        )
        val next = s.copy(shifts = s.shifts + cs, shift = s.shift + 1, shiftOpened = n)
        commit(next)
        io.execute { store.autoBackup(next) }
    }

    // ---------- الإعدادات ----------
    fun setCfg(c: Cfg) = upd { it.copy(cfg = c) }
    fun saveDevice(d: Device) = upd { x ->
        val ex = x.devices.firstOrNull { it.id == d.id }
        val nd = d.copy(session = ex?.session)
        x.copy(devices = if (ex == null) x.devices + nd else x.devices.map { if (it.id == d.id) nd else it })
    }
    fun deleteDevice(id: String) {
        if (dev(id).session != null) return
        upd { x -> x.copy(devices = x.devices.filter { it.id != id }) }
    }
    fun saveMenu(m: MenuItem) = upd { x ->
        x.copy(menu = if (x.menu.any { it.id == m.id }) x.menu.map { if (it.id == m.id) m else it } else x.menu + m)
    }
    fun deleteMenu(id: String) = upd { x -> x.copy(menu = x.menu.filter { it.id != id }) }
    fun savePack(p: Pack) = upd { x ->
        x.copy(packs = if (x.packs.any { it.id == p.id }) x.packs.map { if (it.id == p.id) p else it } else x.packs + p)
    }
    fun deletePack(id: String) = upd { x -> x.copy(packs = x.packs.filter { it.id != id }) }

    // ---------- النسخ الاحتياطي ----------
    fun needsBackup(): Boolean = st.log.isNotEmpty() && now - st.lastExport > 24 * 3_600_000L

    fun exportJson(uri: Uri) {
        runCatching { store.writeText(uri, store.encode(st)) }.onSuccess { upd { it.copy(lastExport = System.currentTimeMillis()) } }
    }
    fun exportCsv(uri: Uri) {
        runCatching { store.writeText(uri, store.csv(st)) }
    }
    fun importJson(uri: Uri, onResult: (Boolean) -> Unit) {
        runCatching { store.readJson(uri) }
            .onSuccess { imported ->
                store.autoBackup(st)
                commit(imported)
                imported.devices.forEach { syncAlarm(it.id) }
                onResult(true)
            }
            .onFailure { onResult(false) }
    }
}
