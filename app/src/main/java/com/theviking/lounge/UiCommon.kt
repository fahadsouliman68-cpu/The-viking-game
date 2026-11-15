package com.theviking.lounge

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BG: Color get() = Pal.cur.bg
val CardBg: Color get() = Pal.cur.card
val LineC: Color get() = Pal.cur.line
val Mut: Color get() = Pal.cur.mut
val Gold: Color get() = Pal.cur.accent
val Cy: Color get() = Pal.cur.accent2
val SurfV: Color get() = Pal.cur.surfV
val Ok = Color(0xFF2FE0A0)
val Bad = Color(0xFFD64A37)
val Blue = Color(0xFF2E7BFF)

fun String.num(): Long = filter { it.isDigit() }.toLongOrNull() ?: 0L

fun shareText(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    ctx.startActivity(Intent.createChooser(i, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun Panel(modifier: Modifier = Modifier, border: Color = LineC, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, border),
    ) { Column(Modifier.padding(14.dp), content = content) }
}

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Blue, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = when {
                color == Ok || color == Gold -> Color(0xFF111111)
                color == SurfV -> Pal.cur.text
                else -> Color.White
            },
        ),
    ) { Text(text, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
    )
}

@Composable
fun LNum(label: String, v: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) =
    NumField(label, v.toString(), { onChange(it.num()) }, modifier)

@Composable
fun TxtField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = modifier)
}

@Composable
fun Stepper(q: Int, onMinus: () -> Unit, onPlus: () -> Unit, plusEnabled: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onMinus, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)) { Text("−", fontSize = 20.sp) }
        Text(q.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 22.dp))
        FilledTonalButton(onClick = onPlus, enabled = plusEnabled, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)) { Text("+", fontSize = 20.sp) }
    }
}

@Composable
fun LineRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (bold) Gold else Color.Unspecified, fontSize = if (bold) 22.sp else 15.sp)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
}

@Composable
fun PayFields(disc: String, onDisc: (String) -> Unit, paid: Boolean, onPaid: (Boolean) -> Unit, cust: String, onCust: (String) -> Unit) {
    NumField(t("discount"), disc, onDisc, Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn(t("paid"), { onPaid(true) }, Modifier.weight(1f), if (paid) Ok else SurfV)
        Btn(t("debt"), { onPaid(false) }, Modifier.weight(1f), if (!paid) Bad else SurfV)
    }
    if (!paid) TxtField(t("customer"), cust, onCust, Modifier.fillMaxWidth())
}

@Composable
fun FormDialog(
    title: String, labels: List<String>, init: List<String>, numeric: List<Boolean>,
    onSave: (List<String>) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit,
) {
    val vals = remember { mutableStateListOf<String>().apply { addAll(init) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, l ->
                    OutlinedTextField(
                        value = vals[i],
                        onValueChange = { vals[i] = if (numeric[i]) it.filter { c -> c.isDigit() } else it },
                        label = { Text(l) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (numeric[i]) KeyboardType.Number else KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (onDelete != null) TextButton(onClick = onDelete) { Text(t("delete"), color = Bad) }
            }
        },
        confirmButton = { Button(onClick = { onSave(vals.toList()) }) { Text(t("save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("cancel")) } },
    )
}

@Composable
fun BackupBanner(vm: AppViewModel) {
    val l = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) vm.exportJson(u)
    }
    Panel(border = Gold) {
        Text(t("backupWarn"), color = Gold)
        Spacer(Modifier.height(8.dp))
        Btn(t("exportJson"), { l.launch("viking-backup-${fmtDate(System.currentTimeMillis())}.json") }, Modifier.fillMaxWidth())
    }
}
