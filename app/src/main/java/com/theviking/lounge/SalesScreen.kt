package com.theviking.lounge

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SalesScreen(vm: AppViewModel) {
    val cart = remember { mutableStateMapOf<String, Int>() }
    var free by remember { mutableStateOf("") }
    var freeNote by remember { mutableStateOf("") }
    var dlg by remember { mutableStateOf(false) }
    val menu = vm.st.menu
    val total = menu.sumOf { (cart[it.id] ?: 0) * it.price } + free.num()

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(t("directSale"), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
            items(menu, key = { it.id }) { mi ->
                val q = cart[mi.id] ?: 0
                val stock = mi.stock
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(mi.label(vm.st.lang), fontWeight = FontWeight.SemiBold)
                            val sub = if (mi.price <= 0) t("noPrice") else vm.m(mi.price) + (if (stock != null) "  •  ${t("stockLeft")} $stock" else "")
                            Text(sub, color = if (mi.price <= 0) Gold else Mut, fontSize = 12.sp)
                        }
                        Stepper(
                            q,
                            { cart[mi.id] = (q - 1).coerceAtLeast(0) },
                            { cart[mi.id] = q + 1 },
                            mi.price > 0 && (stock == null || q < stock),
                        )
                    }
                }
            }
            item {
                Panel {
                    SectionTitle(t("freeAmt"))
                    NumField(t("amount"), free, { free = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    TxtField(t("freeNote"), freeNote, { freeNote = it }, Modifier.fillMaxWidth())
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(CardBg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t("total"), color = Mut, fontSize = 12.sp)
                Text(vm.m(total), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            Btn(t("sell"), { dlg = true }, color = Ok, enabled = total > 0)
        }
    }

    if (dlg) {
        var disc by remember { mutableStateOf("") }
        var paid by remember { mutableStateOf(true) }
        var cust by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dlg = false },
            title = { Text(t("directSale")) },
            text = {
                Column {
                    PayFields(disc, { disc = it }, paid, { paid = it }, cust, { cust = it })
                    LineRow(t("total"), vm.m((total - disc.num()).coerceAtLeast(0L)), bold = true)
                }
            },
            confirmButton = {
                Btn(t("confirm"), {
                    vm.sell(cart.toMap(), free.num(), freeNote, disc.num(), paid, cust)
                    cart.clear(); free = ""; freeNote = ""; dlg = false
                }, color = Ok, enabled = paid || cust.isNotBlank())
            },
            dismissButton = { TextButton(onClick = { dlg = false }) { Text(t("cancel")) } },
        )
    }
}
