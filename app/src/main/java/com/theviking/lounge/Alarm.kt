package com.theviking.lounge

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build

object Visible { @Volatile var on = false }

object Alarms {
    const val CH = "viking_timer"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CH, "Timer", NotificationManager.IMPORTANCE_HIGH)
        ch.setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )
        ch.enableVibration(true)
        nm.createNotificationChannel(ch)
    }

    private fun pi(ctx: Context, id: String, name: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra("name", name)
        return PendingIntent.getBroadcast(ctx, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context, id: String, name: String, at: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val p = pi(ctx, id, name)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        }
    }

    fun cancel(ctx: Context, id: String) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pi(ctx, id, ""))
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (Visible.on) return // التطبيق ظاهر: المنبّه الداخلي يتولّى الأمر
        Alarms.ensureChannel(ctx)
        val name = intent.getStringExtra("name") ?: ""
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(ctx, Alarms.CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ " + name)
            .setContentText("انتهى الوقت / Time is up")
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(name.hashCode(), n)
    }
}

object AlarmPlayer {
    private var r: Ringtone? = null
    fun start(ctx: Context) {
        if (r?.isPlaying == true) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        r = RingtoneManager.getRingtone(ctx.applicationContext, uri)?.also {
            if (Build.VERSION.SDK_INT >= 28) it.isLooping = true
            it.play()
        }
    }
    fun stop() { r?.stop(); r = null }
}
