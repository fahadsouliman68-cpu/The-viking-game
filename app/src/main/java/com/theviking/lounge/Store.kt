package com.theviking.lounge

import android.content.Context
import android.net.Uri
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Store(private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(ctx.filesDir, "state.json")
    private val backups = File(ctx.filesDir, "backups")

    fun encode(s: AppState): String = json.encodeToString(AppState.serializer(), s)
    fun decode(t: String): AppState = json.decodeFromString(AppState.serializer(), t)

    fun load(): AppState {
        if (!file.exists()) return defaultState()
        return try {
            decode(file.readText())
        } catch (e: Exception) {
            runCatching { file.copyTo(File(ctx.filesDir, "state_corrupt_${System.currentTimeMillis()}.json"), true) }
            latestBackup() ?: defaultState()
        }
    }

    /** كتابة ذرّية: ملف مؤقت ثم إعادة تسمية، فلا يتلف الملف إذا انقطع التشغيل أثناء الكتابة. */
    fun save(s: AppState) {
        val tmp = File(ctx.filesDir, "state.json.tmp")
        tmp.writeText(encode(s))
        tmp.renameTo(file)
    }

    fun autoBackup(s: AppState) {
        runCatching {
            backups.mkdirs()
            val name = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
            File(backups, "backup_$name.json").writeText(encode(s))
            backups.listFiles()?.sortedBy { it.name }?.dropLast(15)?.forEach { it.delete() }
        }
    }

    private fun latestBackup(): AppState? = runCatching {
        backups.listFiles()?.sortedBy { it.name }?.asReversed()?.firstNotNullOfOrNull { f -> runCatching { decode(f.readText()) }.getOrNull() }
    }.getOrNull()

    fun writeText(uri: Uri, text: String) {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    fun readJson(uri: Uri): AppState {
        val t = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("empty")
        return decode(t)
    }

    fun csv(s: AppState): String {
        fun q(x: String) = "\"" + x.replace("\"", "\"\"") + "\""
        val sb = StringBuilder("\uFEFF")
        sb.append("date,time,shift,type,device,time_min,time_amount,items,discount,total,status,customer,note\n")
        s.log.forEach { r ->
            val items = r.items.joinToString(" | ") { "${it.name} x${it.qty}" }
            sb.append(listOf(
                fmtDate(r.ts), fmtTime(r.ts), r.shift.toString(), r.kind, q(r.device),
                r.times.sumOf { it.minutes }.toString(), r.times.sumOf { it.amount }.toString(), q(items),
                r.discount.toString(), r.total.toString(), if (r.paid) "paid" else "debt", q(r.customer), q(r.note),
            ).joinToString(",")).append('\n')
        }
        return sb.toString()
    }
}
