package com.theviking.lounge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        Alarms.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        setContent { App(vm) }
    }

    override fun onResume() { super.onResume(); Visible.on = true }
    override fun onPause() { Visible.on = false; super.onPause() }
}

@Composable
fun App(vm: AppViewModel) {
    val lang = vm.st.lang
    CompositionLocalProvider(
        LocalLang provides lang,
        LocalLayoutDirection provides (if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr),
    ) {
        MaterialTheme(colorScheme = DarkScheme) {
            var tab by rememberSaveable { mutableIntStateOf(0) }
            val ctx = LocalContext.current
            val view = LocalView.current
            val keepOn = vm.st.devices.any { it.session?.paused == false }
            SideEffect { view.keepScreenOn = keepOn }

            val tabs = listOf("🎮" to "tab_dev", "🛒" to "tab_sale", "📒" to "tab_log", "💰" to "tab_cash", "⚙️" to "tab_set")
            Scaffold(
                containerColor = BG,
                bottomBar = {
                    NavigationBar(containerColor = CardBg) {
                        tabs.forEachIndexed { i, (emoji, key) ->
                            NavigationBarItem(
                                selected = tab == i, onClick = { tab = i },
                                icon = { Text(emoji, fontSize = 20.sp) },
                                label = { Text(t(key), fontSize = 11.sp, maxLines = 1) },
                            )
                        }
                    }
                },
            ) { pad ->
                Box(Modifier.fillMaxSize().padding(pad)) {
                    when (tab) {
                        0 -> DevicesScreen(vm)
                        1 -> SalesScreen(vm)
                        2 -> LogScreen(vm)
                        3 -> CashScreen(vm)
                        else -> SettingsScreen(vm)
                    }
                    val u = vm.undo
                    if (u != null && vm.now < u.until) {
                        Row(
                            Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth()
                                .background(Color(0xFF1F3A52), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(t("saved"), Modifier.weight(1f).padding(start = 8.dp))
                            TextButton(onClick = { vm.undoLast() }) { Text(t("undo"), color = Gold) }
                            TextButton(onClick = {
                                vm.st.log.firstOrNull { it.id == u.recordId }?.let { shareText(ctx, receiptText(it, vm.st, lang)) }
                            }) { Text(t("receipt")) }
                            TextButton(onClick = { vm.dismissUndo() }) { Text("✕") }
                        }
                    }
                }
            }

            if (vm.ringing.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("⏰ " + t("timeUp")) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            vm.ringing.toList().forEach { id ->
                                val d = vm.st.devices.firstOrNull { it.id == id }
                                if (d != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${t("device")} ${d.name}", Modifier.weight(1f), fontSize = 20.sp)
                                        Btn(t("stop"), { vm.silence(); vm.requestStop(id); tab = 0 }, color = Bad)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { Btn(t("silence"), { vm.silence() }, color = Gold) },
                )
            }
        }
    }
}
