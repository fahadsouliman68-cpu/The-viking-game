package com.theviking.lounge

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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
fun SettingsScreen(vm: AppViewModel) {
    val st = vm.st
    val c = st.cfg
    var r2 by remember(c) { mutableStateOf(c.rate2.toString()) }
    var r4 by remember(c) { mutableStateOf(c.rate4.toString()) }
    var step by remember(c) { mutableStateOf(c.roundStep.toString()) }
    var minm by remember(c) { mutableStateOf(c.minMinutes.toString()) }
    var cur by remember(c) { mutableStateOf(c.currency) }
    var devDlg by remember { mutableStateOf<Device?>(null) }
    var menuDlg by remember { mutableStateOf<MenuItem?>(null) }
    var packDlg by remember { mutableStateOf<Pack?>(null) }
    var msg by remember { mutableStateOf("") }
    val appCtx = LocalContext.current
    var bgHex by remember(c) { mutableStateOf(c.customBg) }
    var accHex by remember(c) { mutableStateOf(c.customAccent) }
    val soundTitle = remember(c.alarmSound) {
        if (c.alarmSound.isBlank()) tr(vm.st.lang, "defaultSound")
        else runCatching { RingtoneManager.getRingtone(appCtx, Uri.parse(c.alarmSound)).getTitle(appCtx) }
            .getOrDefault(tr(vm.st.lang, "customSound"))
    }
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val picked = res.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (res.resultCode == android.app.Activity.RESULT_OK) vm.setAlarmSound(picked?.toString() ?: "")
    }

    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) vm.exportJson(u)
    }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u ->
        if (u != null) vm.exportCsv(u)
    }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) vm.importJson(u) { ok -> msg = tr(vm.st.lang, if (ok) "importOk" else "importFail") }
    }

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(t("tab_set"), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }

        item {
            Panel {
                SectionTitle(t("language"))
                Btn(if (st.lang == "ar") "English" else "العربية", { vm.setLang(if (st.lang == "ar") "en" else "ar") }, Modifier.fillMaxWidth(), SurfV)
            }
        }

        item {
            Panel {
                SectionTitle(t("alarmSettings"))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("alarmSound") + ": " + soundTitle, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Btn(t("pickSound"), {
                            val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, t("alarmSound"))
                                .putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    if (c.alarmSound.isBlank()) null else Uri.parse(c.alarmSound),
                                )
                            pickSound.launch(i)
                        }, Modifier.weight(1f), Gold)
                        if (c.alarmSound.isNotBlank()) {
                            Btn(t("resetSound"), { vm.setAlarmSound("") }, Modifier.weight(1f), SurfV)
                        }
                    }
                    Text(t("alarmNote"), color = Mut, fontSize = 12.sp)
                }
            }
        }

        item {
            Panel {
                SectionTitle(t("theme"))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = THEME_PRESETS.map { it to "theme" + it.replaceFirstChar { ch -> ch.uppercase() } }
                    presets.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (id, key) ->
                                Btn(t(key), { vm.setTheme(id) }, Modifier.weight(1f),
                                    if (c.themeMode == id) Gold else SurfV)
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Btn(t("themeCustom"), { vm.setTheme("custom", bgHex, accHex) }, Modifier.fillMaxWidth(),
                        if (c.themeMode == "custom") Gold else SurfV)
                    if (c.themeMode == "custom") {
                        TxtField(t("bgHex"), bgHex, { bgHex = it.take(7) }, Modifier.fillMaxWidth())
                        TxtField(t("accentHex"), accHex, { accHex = it.take(7) }, Modifier.fillMaxWidth())
                        Btn(t("saveTheme"), {
                            if (parseHex(bgHex) == null || parseHex(accHex) == null) msg = tr(vm.st.lang, "badHex")
                            else vm.setTheme("custom", bgHex, accHex)
                        }, Modifier.fillMaxWidth(), Ok)
                    }
                }
            }
        }

        item {
            Panel {
                SectionTitle(t("rates"))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(t("rate2"), r2, { r2 = it }, Modifier.fillMaxWidth())
                    NumField(t("rate4"), r4, { r4 = it }, Modifier.fillMaxWidth())
                    NumField(t("roundStep"), step, { step = it }, Modifier.fillMaxWidth())
                    NumField(t("minMinutes"), minm, { minm = it }, Modifier.fillMaxWidth())
                    TxtField(t("currency"), cur, { cur = it }, Modifier.fillMaxWidth())
                    Btn(t("save"), {
                        vm.setCfg(c.copy(
                            rate2 = r2.num(), rate4 = r4.num(), roundStep = step.num().coerceAtLeast(1L),
                            minMinutes = minm.num().toInt(), currency = cur.ifBlank { c.currency },
                        ))
                    }, Modifier.fillMaxWidth(), Ok)
                }
            }
        }

        item {
            Panel {
                SectionTitle(t("devices"))
                st.devices.forEach { d ->
                    Row(Modifier.fillMaxWidth().clickable { devDlg = d }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(d.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${vm.m(rateOf(d, c, 2))} • ${vm.m(rateOf(d, c, 4))}", color = Mut, fontSize = 12.sp)
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                Btn("＋ " + t("addDevice"), { devDlg = Device(uid(), "") }, Modifier.fillMaxWidth(), SurfV)
            }
        }

        item {
            Panel {
                SectionTitle(t("menu"))
                st.menu.forEach { m ->
                    Row(Modifier.fillMaxWidth().clickable { menuDlg = m }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.label(st.lang), modifier = Modifier.weight(1f))
                        Text(
                            if (m.price <= 0) t("noPrice") else vm.m(m.price) + (m.stock?.let { " • $it" } ?: ""),
                            color = if (m.price <= 0) Gold else Mut, fontSize = 12.sp,
                        )
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                Btn("＋ " + t("addItem"), { menuDlg = MenuItem(uid(), "") }, Modifier.fillMaxWidth(), SurfV)
            }
        }

        item {
            Panel {
                SectionTitle(t("packsT"))
                st.packs.forEach { p ->
                    Row(Modifier.fillMaxWidth().clickable { packDlg = p }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${p.name} • ${modeName(st.lang, p.mode)} • ${p.minutes}${t("min")}", modifier = Modifier.weight(1f))
                        Text(vm.m(p.price), color = Mut, fontSize = 12.sp)
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                Btn("＋ " + t("add"), { packDlg = Pack(uid(), "", 2, 60, 0) }, Modifier.fillMaxWidth(), SurfV)
            }
        }

        item {
            Panel {
                SectionTitle(t("backup"))
                Text(
                    "${t("lastExport")}: " + if (st.lastExport == 0L) t("never") else "${fmtDate(st.lastExport)} ${fmtTime(st.lastExport)}",
                    color = Mut, fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(t("exportJson"), { exp.launch("viking-backup-${fmtDate(System.currentTimeMillis())}.json") }, Modifier.fillMaxWidth(), Ok)
                    Btn(t("importJson"), { imp.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), SurfV)
                    Btn(t("exportCsv"), { csv.launch("TheViking-${fmtDate(System.currentTimeMillis())}.csv") }, Modifier.fillMaxWidth(), SurfV)
                    if (msg.isNotEmpty()) Text(msg, color = Gold)
                    Text(t("sec"), color = Mut, fontSize = 12.sp)
                }
            }
        }
    }

    devDlg?.let { d ->
        val isNew = st.devices.none { it.id == d.id }
        val del: (() -> Unit)? = if (!isNew && d.session == null) ({ vm.deleteDevice(d.id); devDlg = null }) else null
        FormDialog(
            title = t("devices"),
            labels = listOf(t("deviceName"), t("rate2") + " (${t("rateBlank")})", t("rate4") + " (${t("rateBlank")})"),
            init = listOf(d.name, d.rate2?.toString() ?: "", d.rate4?.toString() ?: ""),
            numeric = listOf(false, true, true),
            onSave = { v ->
                if (v[0].isNotBlank()) vm.saveDevice(d.copy(name = v[0].trim(), rate2 = v[1].num().takeIf { it > 0 }, rate4 = v[2].num().takeIf { it > 0 }))
                devDlg = null
            },
            onDelete = del, onDismiss = { devDlg = null },
        )
    }
    menuDlg?.let { m ->
        val isNew = st.menu.none { it.id == m.id }
        val del: (() -> Unit)? = if (!isNew) ({ vm.deleteMenu(m.id); menuDlg = null }) else null
        FormDialog(
            title = t("menu"),
            labels = listOf(t("itemName"), t("itemNameEn"), t("price"), t("stock")),
            init = listOf(m.name, m.nameEn, m.price.toString(), m.stock?.toString() ?: ""),
            numeric = listOf(false, false, true, true),
            onSave = { v ->
                if (v[0].isNotBlank()) vm.saveMenu(m.copy(name = v[0].trim(), nameEn = v[1].trim(), price = v[2].num(), stock = if (v[3].isBlank()) null else v[3].num().toInt()))
                menuDlg = null
            },
            onDelete = del, onDismiss = { menuDlg = null },
        )
    }
    packDlg?.let { p ->
        val isNew = st.packs.none { it.id == p.id }
        val del: (() -> Unit)? = if (!isNew) ({ vm.deletePack(p.id); packDlg = null }) else null
        FormDialog(
            title = t("packsT"),
            labels = listOf(t("packName"), t("packMode"), t("packMinutes"), t("price")),
            init = listOf(p.name, p.mode.toString(), p.minutes.toString(), p.price.toString()),
            numeric = listOf(false, true, true, true),
            onSave = { v ->
                val mode = if (v[1].num() == 4L) 4 else 2
                if (v[0].isNotBlank() && v[2].num() > 0) vm.savePack(p.copy(name = v[0].trim(), mode = mode, minutes = v[2].num().toInt(), price = v[3].num()))
                packDlg = null
            },
            onDelete = del, onDismiss = { packDlg = null },
        )
    }
}
