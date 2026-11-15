package com.theviking.lounge

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogScreen(vm: AppViewModel) {
    val st = vm.st
    var edit by remember { mutableStateOf<String?>(null) }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u ->
        if (u != null) vm.exportCsv(u)
    }
    val groups = st.log.groupBy { it.shift }.entries.sortedByDescending { it.key }

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("log"), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Btn(t("exportCsv"), { csv.launch("TheViking-${fmtDate(System.currentTimeMillis())}.csv") }, color = Ok)
            }
        }
        if (st.log.isEmpty()) item { Text(t("noRecords"), color = Mut) }
        groups.forEach { (sh, recs) ->
            item(key = "h$sh") {
                val opened = if (sh == st.shift) st.shiftOpened else (st.shifts.firstOrNull { it.id == sh }?.opened ?: recs.first().ts)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${t("shiftN")} $sh" + (if (sh == st.shift) " (${t("current")})" else "") + " • ${fmtDate(opened)}", color = Gold, fontWeight = FontWeight.Bold)
                    Text(vm.m(recs.sumOf { it.total }), color = Gold)
                }
            }
            items(recs.sortedByDescending { it.ts }, key = { it.id }) { r -> RecRow(vm, r) { edit = r.id } }
        }
    }
    edit?.let { RecordEditor(vm, it) { edit = null } }
}

@Composable
fun RecRow(vm: AppViewModel, r: Rec, onClick: () -> Unit) {
    Panel(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val title = if (r.kind == "sale") t("directSale") else "${t("device")} ${r.device}"
                Text("${fmtTime(r.ts)} — $title", fontWeight = FontWeight.SemiBold)
                val parts = r.times.map { "${modeName(vm.st.lang, it.mode)} ${it.minutes}${t("min")}" } + r.items.map { "${it.name} ×${it.qty}" }
                Text(parts.joinToString(" • "), color = Mut, fontSize = 12.sp)
                if (r.history.isNotEmpty()) Text("✎ ${r.history.size}", color = Mut, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(vm.m(r.total), color = Gold, fontWeight = FontWeight.ExtraBold)
                if (!r.paid) Text("${t("debt")}: ${r.customer}", color = Bad, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RecordEditor(vm: AppViewModel, id: String, onDone: () -> Unit) {
    val r = vm.st.log.firstOrNull { it.id == id }
    if (r == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val ctx = LocalContext.current
    val lang = vm.st.lang
    val times = remember(id) { mutableStateListOf<TimeLine>().apply { addAll(r.times) } }
    val items = remember(id) { mutableStateListOf<ItemLine>().apply { addAll(r.items) } }
    var disc by remember(id) { mutableStateOf(r.discount) }
    var paid by remember(id) { mutableStateOf(r.paid) }
    var cust by remember(id) { mutableStateOf(r.customer) }
    var note by remember(id) { mutableStateOf(r.note) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDel by remember { mutableStateOf(false) }
    fun draft() = r.copy(times = times.toList(), items = items.toList(), discount = disc, paid = paid, customer = cust, note = note)

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(t("editRecord")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${fmtDate(r.ts)} ${fmtTime(r.ts)} • " + (if (r.kind == "sale") t("directSale") else r.device), color = Mut)
                times.forEachIndexed { i, tl ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(modeName(lang, tl.mode) + (if (tl.pack) " ⏳" else ""), Modifier.weight(1f), fontSize = 13.sp)
                        LNum(t("min"), tl.minutes.toLong(), { times[i] = tl.copy(minutes = it.toInt()) }, Modifier.weight(0.9f))
                        LNum(t("amount"), tl.amount, { times[i] = tl.copy(amount = it) }, Modifier.weight(1.3f))
                    }
                }
                items.forEachIndexed { i, il ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(il.name, Modifier.weight(1f), fontSize = 13.sp)
                        LNum(t("qty"), il.qty.toLong(), { items[i] = il.copy(qty = it.toInt()) }, Modifier.weight(0.8f))
                        LNum(t("price"), il.price, { items[i] = il.copy(price = it) }, Modifier.weight(1.2f))
                        TextButton(onClick = { items.removeAt(i) }) { Text("✕", color = Bad) }
                    }
                }
                Box {
                    Btn("+ " + t("addItem"), { menuOpen = true }, color = SurfV)
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        vm.st.menu.forEach { mi ->
                            DropdownMenuItem(
                                text = { Text(mi.label(lang)) },
                                onClick = { items.add(ItemLine(mi.id, mi.label(lang), 1, mi.price)); menuOpen = false },
                            )
                        }
                    }
                }
                LNum(t("discount"), disc, { disc = it }, Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (paid) t("paid") else t("debt"), Modifier.weight(1f))
                    Switch(checked = paid, onCheckedChange = { paid = it })
                }
                TxtField(t("customer"), cust, { cust = it }, Modifier.fillMaxWidth())
                TxtField(t("note"), note, { note = it }, Modifier.fillMaxWidth())
                LineRow(t("total"), vm.m(draft().total), bold = true)
                if (r.history.isNotEmpty()) {
                    Text(t("history"), color = Gold, fontSize = 13.sp)
                    r.history.forEach { Text(it, color = Mut, fontSize = 11.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(t("receipt"), { shareText(ctx, receiptText(draft(), vm.st, lang)) }, Modifier.weight(1f), SurfV)
                    Btn(
                        if (confirmDel) t("confirmDelete") else t("delete"),
                        { if (confirmDel) { vm.deleteRecord(id); onDone() } else confirmDel = true },
                        Modifier.weight(1f), Bad,
                    )
                }
            }
        },
        confirmButton = { Btn(t("save"), { vm.saveRecord(draft()); onDone() }, color = Ok) },
        dismissButton = { TextButton(onClick = onDone) { Text(t("cancel")) } },
    )
}
