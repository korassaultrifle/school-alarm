package io.github.korassaultrifle.schoolalarm

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : Activity() {
    private val tick = Handler(Looper.getMainLooper())
    private lateinit var label: TextView
    private lateinit var count: TextView
    private lateinit var sub: TextView
    private lateinit var leftBtn: Button
    private lateinit var list: LinearLayout
    private lateinit var next: TextView
    private lateinit var scroll: ScrollView

    private val FG = Color.parseColor("#ECEEF2")
    private val MUTED = Color.parseColor("#8D919C")
    private val GO = Color.parseColor("#FFB44A")
    private val LATE = Color.parseColor("#FF6B5E")
    private val OK = Color.parseColor("#6FCF97")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Alarms.channels(this)

        val side = (resources.displayMetrics.widthPixels * 0.14).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(side, dp(36), side, dp(48))
        }
        fun text(size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
            textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        label = text(13f, MUTED); col.addView(label)
        count = text(34f, GO, bold = true).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        col.addView(count)
        sub = text(12f, FG); col.addView(sub)
        leftBtn = Button(this).apply {
            text = "출발했어요"
            setOnClickListener {
                val p = getSharedPreferences("state", MODE_PRIVATE)
                val today = LocalDate.now().toString()
                p.edit().putString("left", if (p.getString("left", "") == today) "" else today).apply()
                render()
            }
        }
        col.addView(leftBtn)
        next = text(11f, MUTED).apply { setPadding(0, dp(10), 0, dp(6)) }
        col.addView(next)
        col.addView(text(12f, MUTED).apply { text = "오늘 일정"; setPadding(0, dp(8), 0, dp(4)) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(list)
        col.addView(Button(this).apply {
            text = "알람 테스트"
            setOnClickListener { Alarms.notify(this@MainActivity, "알람 테스트", "이렇게 울려요", strong = true) }
        })

        scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121315"))
            isVerticalScrollBarEnabled = true
            isFocusable = true
            isFocusableInTouchMode = true
            addView(col)
        }
        setContentView(scroll)

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Alarms.scheduleNext(this)
        renderList()
    }

    override fun onResume() {
        super.onResume()
        scroll.requestFocus() // 베젤 돌려서 스크롤
        tick.post(object : Runnable {
            override fun run() { render(); tick.postDelayed(this, 1000) }
        })
    }

    override fun onPause() {
        tick.removeCallbacksAndMessages(null)
        super.onPause()
    }

    private fun render() {
        val now = LocalDateTime.now()
        val p = getSharedPreferences("state", MODE_PRIVATE)
        val left = p.getString("left", "") == now.toLocalDate().toString()
        val h = hero(now, left)
        label.text = h.label
        count.text = h.count
        count.textSize = if (h.count.length > 6) 30f else 34f
        count.setTextColor(when (h.tone) { Tone.GO -> GO; Tone.LATE -> LATE; Tone.OK -> OK })
        sub.text = h.sub
        val dep = departureOn(now.toLocalDate())
        val nowM = now.hour * 60 + now.minute
        leftBtn.visibility = if (dep != null && nowM < dep.second.start) View.VISIBLE else View.GONE
        leftBtn.text = if (left) "출발함 ✓" else "출발했어요"
        next.text = if (Alarms.canExact(this)) "다음 알람 ${p.getString("next", "-")}"
        else "정확한 알람 권한이 꺼져 있어요. 여기를 눌러 켜 주세요."
        next.setOnClickListener {
            if (!Alarms.canExact(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
        if (now.second == 0 && now.minute == 0) renderList()
    }

    private fun renderList() {
        list.removeAllViews()
        val d = LocalDate.now()
        val rows = mutableListOf<Pair<Int, String>>()
        departureOn(d)?.let { (at, _) -> rows += at to "${hhmm(at)}  등교 출발" }
        itemsOn(d).forEach { rows += it.start to "${hhmm(it.start)}  ${it.name}" }
        rows.sortedBy { it.first }.forEach { (_, s) ->
            list.addView(TextView(this).apply {
                text = s; textSize = 12f; setTextColor(FG); setPadding(0, dp(3), 0, dp(3))
            })
        }
    }
}
