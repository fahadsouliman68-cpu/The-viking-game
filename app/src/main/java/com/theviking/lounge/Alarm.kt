package com.theviking.lounge

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

object Visible { @Volatile var on = false }

object Alarms {
    // قناة جديدة بلا صوت إشعار: صوت المنبّه يُشغَّل مباشرة بتدفق USAGE_ALARM
    // فلا يتأثر بوضع الصامت/الاهتزاز ولا بصوت الوسائط.
    const val CH = "viking_alarm2"
    const val SVC_ID = 4711

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH) != null) return
        val ch = NotificationChannel(CH, "Alarms", NotificationManager.IMPORTANCE_HIGH)
        ch.setSound(null, null)
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
        AlarmPlayer.start(ctx)
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

/** تشغيل صوت المنبّه بتدفق ALARM: يعمل حتى والهاتف صامت أو على الاهتزاز، ولا يتبع صوت الوسائط. */
object AlarmPlayer {
    private var mp: MediaPlayer? = null

    /** يشغّل الخدمة الأمامية التي تحمل الصوت (تستمر حتى والتطبيق مغلق). */
    fun start(ctx: Context) {
        if (mp != null) return
        runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, AlarmService::class.java)) }
    }

    /** يُستدعى من الخدمة. */
    fun play(ctx: Context) {
        if (mp != null) return
        val chosen = runCatching { Store(ctx).load().cfg.alarmSound }.getOrDefault("")
        val candidates = listOfNotNull(
            chosen.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        )
        for (u in candidates) {
            val p = MediaPlayer()
            val ok = runCatching {
                p.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                p.isLooping = true
                p.setDataSource(ctx, u)
                p.prepare()
                p.start()
            }.isSuccess
            if (ok) { mp = p; return }
            p.release()
        }
    }

    fun stop(ctx: Context) {
        release()
        ctx.stopService(Intent(ctx, AlarmService::class.java))
    }

    fun release() {
        mp?.let { runCatching { it.stop() }; it.release() }
        mp = null
    }
}

class AlarmService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Alarms.ensureChannel(this)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, Alarms.CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ THE VIKING")
            .setContentText("انتهى الوقت / Time is up")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Alarms.SVC_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(Alarms.SVC_ID, n)
        }
        AlarmPlayer.play(this)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AlarmPlayer.release()
        super.onDestroy()
    }
}
