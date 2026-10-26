package io.github.korassaultrifle.schoolalarm

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDateTime
import java.time.ZoneId

object Alarms {
    private const val CHANNEL = "routine"
    private const val CHANNEL_STRONG = "depart"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_STRONG = "strong"
    const val EXTRA_AT = "at"

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "일정 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        })
        nm.createNotificationChannel(NotificationChannel(CHANNEL_STRONG, "등교 출발 알람", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 300, 800, 300, 800)
        })
    }

    fun canExact(ctx: Context): Boolean {
        val am = ctx.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    /** 다음 알람 하나만 걸어 두고, 울리면 그다음 것을 거는 방식 */
    fun scheduleNext(ctx: Context, after: LocalDateTime = LocalDateTime.now()) {
        val ev = nextAlarm(after) ?: return
        val millis = ev.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val fire = Intent(ctx, AlarmReceiver::class.java)
            .putExtra(EXTRA_TITLE, ev.title).putExtra(EXTRA_TEXT, ev.text)
            .putExtra(EXTRA_STRONG, ev.strong).putExtra(EXTRA_AT, ev.at.toString())
        val pi = PendingIntent.getBroadcast(ctx, 1, fire, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (canExact(ctx)) {
            val show = PendingIntent.getActivity(ctx, 2, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
        ctx.getSharedPreferences("state", Context.MODE_PRIVATE).edit()
            .putString("next", "${hhmm(ev.at.hour * 60 + ev.at.minute)} ${ev.title}").apply()
    }

    fun notify(ctx: Context, title: String, text: String, strong: Boolean) {
        channels(ctx)
        val open = PendingIntent.getActivity(ctx, 3, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(ctx, if (strong) CHANNEL_STRONG else CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (strong) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
        if (strong) {
            val full = Intent(ctx, AlarmActivity::class.java)
                .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            b.setFullScreenIntent(PendingIntent.getActivity(ctx, 4, full, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE), true)
        }
        try {
            NotificationManagerCompat.from(ctx).notify(if (strong) 10 else 11, b.build())
        } catch (_: SecurityException) {
            // 알림 권한이 없으면 진동만
            vibrate(ctx, longArrayOf(0, 400, 200, 400), -1)
        }
    }

    fun vibrator(ctx: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ctx.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)

    fun vibrate(ctx: Context, pattern: LongArray, repeat: Int) {
        vibrator(ctx).vibrate(VibrationEffect.createWaveform(pattern, repeat))
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val title = intent.getStringExtra(Alarms.EXTRA_TITLE) ?: "알림"
        val text = intent.getStringExtra(Alarms.EXTRA_TEXT) ?: ""
        val strong = intent.getBooleanExtra(Alarms.EXTRA_STRONG, false)
        Alarms.notify(ctx, title, text, strong)
        val at = intent.getStringExtra(Alarms.EXTRA_AT)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
        val now = LocalDateTime.now()
        Alarms.scheduleNext(ctx, if (at.isAfter(now)) at else now)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = Alarms.scheduleNext(ctx)
}

/** 등교 출발: 끌 때까지 1분 동안 진동 */
class AlarmActivity : Activity() {
    private val stop = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val title = intent.getStringExtra(Alarms.EXTRA_TITLE) ?: "지금 출발!"
        val text = intent.getStringExtra(Alarms.EXTRA_TEXT) ?: ""
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121315"))
            setPadding(40, 40, 40, 40)
        }
        root.addView(TextView(this).apply {
            this.text = title; textSize = 26f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFB44A")); paint.isFakeBoldText = true
        })
        root.addView(TextView(this).apply {
            this.text = text; textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#ECEEF2")); setPadding(0, 8, 0, 20)
        })
        root.addView(Button(this).apply {
            this.text = "출발했어요"
            setOnClickListener {
                getSharedPreferences("state", MODE_PRIVATE).edit()
                    .putString("left", java.time.LocalDate.now().toString()).apply()
                finish()
            }
        })
        setContentView(root)
        Alarms.vibrate(this, longArrayOf(0, 800, 400), 0)
        stop.postDelayed({ finish() }, 60_000)
    }

    override fun onDestroy() {
        stop.removeCallbacksAndMessages(null)
        Alarms.vibrator(this).cancel()
        getSystemService(NotificationManager::class.java).cancel(10)
        super.onDestroy()
    }
}
