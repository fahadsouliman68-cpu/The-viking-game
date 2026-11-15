package com.theviking.lounge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CashScreen(vm: AppViewModel) {
    val st = vm.st
    val sh = st.shift
    val recs = st.log.filter { it.shift == sh }
    val revenue = st.log.filter { it.paid && it.paidShift == sh }.sumOf { it.total }
    val newDebts = recs.filter { !it.paid }.sumOf { it.total }
    val ex = st.expenses.filter { it.shift == sh }
    val exTot = ex.sumOf { it.amount }
    var expDlg by remember { mutableStateOf(false) }
    var closeDlg by remember { mutableStateOf(false) }
    val directName = t("directSale")
    val perDev = recs.groupBy { if (it.kind == "sale") directName else it.device }.mapValues { e -> e.value.sumOf { it.total } }
    val debts = st.log.filter { !it.paid }.sortedByDescending { it.ts }

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(t("tab_cash"), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
        if (vm.needsBackup()) item { BackupBanner(vm) }
        item {
            Panel {
                SectionTitle("${t("shiftSummary")} #$sh")
                Text(fmtDate(st.shiftOpened) + " " + fmtTime(st.shiftOpened), color = Mut, fontSize = 12.sp)
                LineRow(t("revenue"), vm.m(revenue))
                LineRow(t("debts"), vm.m(newDebts))
                LineRow(t("expenses"), vm.m(exTot))
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                LineRow(t("net"), vm.m(revenue - exTot), bold = true)
            }
        }
        if (perDev.isNotEmpty()) item {
            Panel {
                SectionTitle(t("perDevice"))
                perDev.forEach { (k, v) -> LineRow(k, vm.m(v)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(t("addExpense"), { expDlg = true }, Modifier.weight(1f), SurfV)
                Btn(t("closeShift"), { closeDlg = true }, Modifier.weight(1f), Gold)
            }
        }
        if (ex.isNotEmpty()) item {
            Panel {
                SectionTitle(t("expenses"))
                ex.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${fmtTime(e.ts)}  ${e.title}", Modifier.weight(1f))
                        Text(vm.m(e.amount), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { vm.deleteExpense(e.id) }) { Text("✕", color = Bad) }
                    }
                }
            }
        }
        item {
            Panel {
                SectionTitle(t("debts"))
                if (debts.isEmpty()) Text(t("noDebts"), color = Mut)
                debts.forEach { r ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.customer.ifBlank { "—" }, fontWeight = FontWeight.SemiBold)
                            Text(fmtDate(r.ts), color = Mut, fontSize = 12.sp)
                        }
                        Text(vm.m(r.total), color = Bad, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.padding(4.dp))
                        Btn(t("settle"), { vm.settleDebt(r.id) }, color = Ok)
                    }
                }
            }
        }
        if (st.shifts.isNotEmpty()) item {
            Panel {
                SectionTitle(t("pastShifts"))
                st.shifts.reversed().forEach { s ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text("${t("shiftN")} ${s.id} • ${fmtDate(s.closed)}", fontWeight = FontWeight.Bold)
                        Text(
                            "${t("revenue")} ${vm.m(s.revenue)} • ${t("expenses")} ${vm.m(s.expenses)} • ${t("net")} ${vm.m(s.revenue - s.expenses)}",
                            color = Mut, fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (expDlg) {
        FormDialog(
            title = t("addExpense"), labels = listOf(t("expTitle"), t("amount")), init = listOf("", ""),
            numeric = listOf(false, true),
            onSave = { v -> vm.addExpense(v[0], v[1].num()); expDlg = false },
            onDelete = null, onDismiss = { expDlg = false },
        )
    }
    if (closeDlg) {
        AlertDialog(
            onDismissRequest = { closeDlg = false },
            title = { Text(t("closeShift")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("closeShiftMsg"))
                    LineRow(t("net"), vm.m(revenue - exTot), bold = true)
                    if (st.devices.any { it.session != null }) Text(t("runningWarn"), color = Gold, fontSize = 13.sp)
                }
            },
            confirmButton = { Btn(t("confirm"), { vm.closeShift(); closeDlg = false }, color = Gold) },
            dismissButton = { TextButton(onClick = { closeDlg = false }) { Text(t("cancel")) } },
        )
    }
}
