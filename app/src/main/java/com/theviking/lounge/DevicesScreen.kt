package com.theviking.lounge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun DevicesScreen(vm: AppViewModel) {
    var stopFor by remember { mutableStateOf<String?>(null) }
    var adjFor by remember { mutableStateOf<String?>(null) }
    val req = vm.stopRequest
    LaunchedEffect(req) {
        if (req != null) { stopFor = req; vm.clearStopRequest() }
    }
    LazyColumn(
        contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Header(vm) }
        if (vm.needsBackup()) item { BackupBanner(vm) }
        items(vm.st.devices, key = { it.id }) { d -> DeviceCard(vm, d, { stopFor = d.id }, { adjFor = d.id }) }
        item { Spacer(Modifier.height(72.dp)) }
    }
    stopFor?.let { StopDialog(vm, it) { stopFor = null } }
    adjFor?.let { AdjustDialog(vm, it) { adjFor = null } }
}

@Composable
fun Header(vm: AppViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("THE VIKING", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
            Text(t("sub"), color = Mut, fontSize = 13.sp)
        }
        OutlinedButton(onClick = { vm.setLang(if (vm.st.lang == "ar") "en" else "ar") }) { Text(t("lang")) }
    }
}

@Composable
fun DeviceCard(vm: AppViewModel, d: Device, onStop: () -> Unit, onAdj: () -> Unit) {
    val s = d.session
    val paused = s?.paused == true
    Panel(border = if (s != null && !paused) Cy else LineC) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.name, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            val tag = when {
                s == null -> t("free")
                paused -> t("paused")
                else -> t(if (s.segs.last().mode == 2) "p2" else "p4") + (if (s.cd != null) " ⏳" else "")
            }
            Text(tag, color = if (s == null) Mut else if (paused) Gold else Cy)
        }
        if (s == null) FreeControls(vm, d) else RunningControls(vm, d, s, onStop, onAdj)
    }
}

@Composable
fun FreeControls(vm: AppViewModel, d: Device) {
    var price by remember(d.id) { mutableStateOf("") }
    val c = vm.st.cfg
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2, 4).forEach { m ->
            Btn("${t(if (m == 2) "p2" else "p4")}\n${vm.m(rateOf(d, c, m))} ${t("hour")}", { vm.start(d.id, m) }, Modifier.weight(1f), Ok)
        }
    }
    Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxWidth().border(1.dp, LineC, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text("⏳ " + t("byPrice"), color = Gold, fontSize = 13.sp)
        Row(
            Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumField(t("price"), price, { price = it }, Modifier.weight(1.4f))
            listOf(2, 4).forEach { m ->
                Btn(t(if (m == 2) "p2" else "p4"), {
                    val p = price.num()
                    if (p > 0) { vm.startPrice(d.id, m, p); price = "" }
                }, Modifier.weight(1f), SurfV)
            }
        }
        if (vm.st.packs.isNotEmpty()) {
            Text(t("packs"), color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.st.packs.forEach { p ->
                    Btn("${p.name} (${p.minutes}${t("min")})\n${vm.m(p.price)}", { vm.startPack(d.id, p) }, color = SurfV)
                }
            }
        }
    }
}

@Composable
fun RunningControls(vm: AppViewModel, d: Device, s: Session, onStop: () -> Unit, onAdj: () -> Unit) {
    val now = vm.now
    val run = runMs(s, now)
    val cd = s.cd
    val mode = s.segs.last().mode
    Spacer(Modifier.height(10.dp))
    if (cd != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⏳ " + t("byPrice"), color = Mut)
            Text(vm.m(cd.price), fontWeight = FontWeight.Bold)
        }
    } else {
        Row(Modifier.fillMaxWidth().background(Color(0xFF0B1117), RoundedCornerShape(12.dp)).padding(4.dp)) {
            listOf(2, 4).forEach { m ->
                Btn(t(if (m == 2) "p2" else "p4"), { vm.switchMode(d.id, m) }, Modifier.weight(1f),
                    if (mode == m) Blue else Color.Transparent, enabled = !s.paused)
            }
        }
    }
    val rem = (cd?.allotMs ?: 0L) - run
    val over = cd != null && rem < 0
    val txt = if (cd != null) (if (over) "+" else "") + clock(abs(rem)) else clock(run)
    Text(
        txt, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
        color = if (over) Bad else if (s.paused) Gold else Cy, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
    if (cd != null) Text(if (over) t("over") else t("left"), color = Mut, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onAdj) { Text("✎ " + t("adjust"), color = Mut) }
    }
    vm.st.menu.forEach { mi ->
        val q = s.items[mi.id] ?: 0
        val stock = mi.stock
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp).background(Color(0xFF0D141B), RoundedCornerShape(10.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(mi.label(vm.st.lang))
                val sub = if (mi.price <= 0) t("noPrice") else vm.m(mi.price) + (if (stock != null) "  •  ${t("stockLeft")} $stock" else "")
                Text(sub, color = if (mi.price <= 0) Gold else Mut, fontSize = 12.sp)
            }
            Stepper(q, { vm.changeItem(d.id, mi.id, -1) }, { vm.changeItem(d.id, mi.id, 1) },
                mi.price > 0 && (stock == null || q < stock))
        }
    }
    val tc = timeLines(s, now, vm.st.cfg).sumOf { it.amount }
    val ic = itemLines(s, vm.st.menu, vm.st.lang).sumOf { it.price * it.qty }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${t("time")}: ${vm.m(tc)}", color = Mut, fontSize = 14.sp)
        Text("${t("drinks")}: ${vm.m(ic)}", color = Mut, fontSize = 14.sp)
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn(if (s.paused) "▶ " + t("resume") else "⏸ " + t("pause"),
            { if (s.paused) vm.resume(d.id) else vm.pause(d.id) }, Modifier.weight(1f), Gold)
        Btn(t("stop"), onStop, Modifier.weight(1f), Bad)
    }
}

@Composable
fun StopDialog(vm: AppViewModel, id: String, onDone: () -> Unit) {
    var disc by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf(true) }
    var cust by remember { mutableStateOf("") }
    val rec = vm.preview(id, vm.now, disc.num(), paid, cust)
    if (rec == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val lang = vm.st.lang
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("${t("device")} ${rec.device}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                rec.times.forEach {
                    LineRow("${modeName(lang, it.mode)}${if (it.pack) " ⏳" else ""} (${it.minutes} ${t("min")})", vm.m(it.amount))
                }
                rec.items.forEach { LineRow("${it.name} × ${it.qty}", vm.m(it.price * it.qty)) }
                PayFields(disc, { disc = it }, paid, { paid = it }, cust, { cust = it })
                LineRow(t("total"), vm.m(rec.total), bold = true)
            }
        },
        confirmButton = {
            Btn(t("confirm"), { vm.stop(id, disc.num(), paid, cust); onDone() }, color = Ok, enabled = paid || cust.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDone) { Text(t("cancel")) } },
    )
}

@Composable
fun AdjustDialog(vm: AppViewModel, id: String, onDone: () -> Unit) {
    val s = vm.st.devices.firstOrNull { it.id == id }?.session
    if (s == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    var mins by remember { mutableStateOf((runMs(s, System.currentTimeMillis()) / 60000).toString()) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("${t("adjust")} — ${vm.st.devices.first { it.id == id }.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NumField(t("elapsed"), mins, { mins = it }, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-10, -5, 5, 10).forEach { dlt ->
                        Btn((if (dlt > 0) "+" else "") + dlt, { mins = (mins.num() + dlt).coerceAtLeast(0L).toString() }, Modifier.weight(1f), SurfV)
                    }
                }
                Text("${t("startedAt")}: ${fmtTime(System.currentTimeMillis() - mins.num() * 60000)}", color = Mut)
            }
        },
        confirmButton = { Btn(t("apply"), { vm.adjust(id, mins.num() * 60000L); onDone() }, color = Ok) },
        dismissButton = { TextButton(onClick = onDone) { Text(t("cancel")) } },
    )
}
