package io.github.korassaultrifle.schoolalarm

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// make_ics.py, index.html 과 같은 일정. 하나를 바꾸면 셋 다 바꿔요.

enum class Kind { CLASS, GYM, ROUTINE }

data class Item(
    val days: Set<DayOfWeek>,
    val name: String,
    val room: String,
    val start: Int, // 0시부터 분
    val end: Int,
    val kind: Kind = Kind.ROUTINE,
    val from: LocalDate = SEMESTER_START,
    val until: LocalDate = DIET_END,
) {
    val school get() = kind != Kind.ROUTINE
    fun activeOn(d: LocalDate) = d.dayOfWeek in days && !d.isBefore(from) && !d.isAfter(until)
}

data class AlarmEvent(val at: LocalDateTime, val title: String, val text: String, val strong: Boolean)

val SEMESTER_START: LocalDate = LocalDate.of(2026, 10, 19)
val CLASS_END: LocalDate = LocalDate.of(2026, 12, 18)
val DIET_END: LocalDate = LocalDate.of(2026, 12, 26)
const val LEAD = 90 // 학교 첫 일정 몇 분 전에 출발

private fun t(s: String): Int { val (h, m) = s.split(":").map { it.toInt() }; return h * 60 + m }
private val WEEKDAYS = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
private val WEEKEND = setOf(SATURDAY, SUNDAY)

val ITEMS: List<Item> = listOf(
    // 수업
    Item(setOf(MONDAY), "ICT시대의 디자인씽킹", "수리관 (A)-501", t("12:00"), t("14:50"), Kind.CLASS, until = CLASS_END),
    Item(setOf(MONDAY), "메타디자인", "대신관 (C)-303", t("15:00"), t("17:50"), Kind.CLASS, until = CLASS_END),
    Item(setOf(TUESDAY), "비주얼내러티브", "대신관 (C)-306", t("12:00"), t("14:50"), Kind.CLASS, until = CLASS_END),
    Item(setOf(TUESDAY), "3D컴퓨터그래픽스II", "대신관 (C)-302", t("15:00"), t("17:50"), Kind.CLASS, until = CLASS_END),
    Item(setOf(WEDNESDAY), "자기계발과 Life DESIGN", "", t("12:00"), t("12:50"), Kind.CLASS, until = CLASS_END),
    Item(setOf(FRIDAY), "인터페이스디자인", "대신관 (C)-303", t("13:00"), t("15:50"), Kind.CLASS, until = CLASS_END),
    // 헬스
    Item(setOf(MONDAY, TUESDAY, WEDNESDAY), "헬스", "학교 헬스장", t("10:30"), t("11:30"), Kind.GYM, until = CLASS_END),
    Item(setOf(FRIDAY), "헬스", "학교 헬스장", t("11:30"), t("12:30"), Kind.GYM, until = CLASS_END),
    // 유산소
    Item(setOf(THURSDAY), "자전거 15km", "", t("10:00"), t("10:50")),
    Item(setOf(WEDNESDAY), "자전거 15km", "", t("16:00"), t("16:50")),
    Item(setOf(FRIDAY), "자전거 15km", "", t("17:30"), t("18:20")),
    Item(setOf(SATURDAY), "자전거 15km", "", t("10:30"), t("11:20")),
    Item(setOf(SUNDAY), "자전거 15km", "", t("10:30"), t("11:20"), until = LocalDate.of(2026, 11, 14)),
    Item(setOf(SUNDAY), "달리기 5km", "천천히", t("10:30"), t("11:10"), from = LocalDate.of(2026, 11, 15)),
    // 빈 시간
    Item(setOf(WEDNESDAY), "과제 (밀린 것)", "", t("14:00"), t("15:30")),
    Item(setOf(THURSDAY), "과제 집중", "마감 가까운 것부터", t("14:00"), t("17:00")),
    // 저녁 루틴
    Item(WEEKDAYS, "과제", "", t("19:30"), t("21:00")),
    Item(WEEKDAYS, "그림 연습", "", t("21:00"), t("22:00")),
    Item(WEEKDAYS, "독서", "", t("22:00"), t("22:30")),
    Item(WEEKDAYS, "게임", "야식 금지", t("22:30"), t("24:00")),
    Item(setOf(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY), "게임 끝! 잘 준비", "", 0, 5),
    // 다이어트
    Item(WEEKDAYS, "체중 재기", "화장실 다녀와서", t("07:40"), t("07:45")),
    Item(WEEKEND, "체중 재기", "화장실 다녀와서", t("09:30"), t("09:35")),
    Item(WEEKDAYS, "아침", "단백질 먼저", t("08:00"), t("08:20")),
    Item(setOf(MONDAY, TUESDAY, WEDNESDAY), "점심", "헬스 끝 · 단백질 먼저", t("11:35"), t("11:55")),
    Item(setOf(THURSDAY), "점심", "단백질 먼저", t("12:00"), t("12:30")),
    Item(setOf(FRIDAY), "점심", "헬스 끝 · 단백질 먼저", t("12:35"), t("12:55")),
    Item(WEEKDAYS, "저녁", "단백질 먼저", t("19:00"), t("19:30")),
    Item(WEEKEND, "아침", "단백질 먼저", t("10:00"), t("10:20")),
    Item(WEEKEND, "점심", "단백질 먼저", t("13:00"), t("13:30")),
    Item(WEEKEND, "저녁", "단백질 먼저", t("18:30"), t("19:00")),
    Item(setOf(SATURDAY), "그림 연습", "", t("15:00"), t("16:00")),
    Item(setOf(SUNDAY), "다음 주 마감·과제 정리", "", t("21:00"), t("21:20")),
    Item(WEEKEND, "야식 금지", "", t("22:30"), t("22:35")),
)

fun itemsOn(d: LocalDate): List<Item> = ITEMS.filter { it.activeOn(d) }.sortedBy { it.start }

/** 그날 등교 출발: (출발 시각 분, 학교 첫 일정) */
fun departureOn(d: LocalDate): Pair<Int, Item>? =
    itemsOn(d).firstOrNull { it.school }?.let { it.start - LEAD to it }

fun hhmm(m: Int): String = "%02d:%02d".format((m / 60) % 24, m % 60)

fun alarmsOn(d: LocalDate): List<AlarmEvent> {
    val at = { m: Int -> d.atStartOfDay().plusMinutes(m.toLong()) }
    val out = mutableListOf<AlarmEvent>()
    departureOn(d)?.let { (dep, first) ->
        out += AlarmEvent(at(dep), "지금 출발!", "${first.name} ${hhmm(first.start)}", strong = true)
    }
    for (it in itemsOn(d)) {
        out += if (it.kind == Kind.CLASS)
            AlarmEvent(at(it.start - 10), "${it.name} 10분 전", it.room, strong = false)
        else
            AlarmEvent(at(it.start), it.name, it.room, strong = false)
    }
    return out
}

/** now 이후 가장 가까운 알람들 (같은 시각이면 하나로 합쳐요) */
fun nextAlarm(now: LocalDateTime): AlarmEvent? {
    val all = (0L..8L).flatMap { alarmsOn(now.toLocalDate().plusDays(it)) }
        .filter { it.at.isAfter(now) }
        .sortedBy { it.at }
    val first = all.firstOrNull() ?: return null
    val same = all.filter { it.at == first.at }
    if (same.size == 1) return first
    return AlarmEvent(
        first.at,
        same.joinToString(" · ") { it.title },
        same.map { it.text }.filter { it.isNotEmpty() }.joinToString(" · "),
        same.any { it.strong },
    )
}

/** 시계 화면 맨 위 큰 숫자 */
data class Hero(val label: String, val count: String, val sub: String, val tone: Tone)
enum class Tone { GO, LATE, OK }

fun hero(now: LocalDateTime, left: Boolean): Hero {
    val d = now.toLocalDate()
    val nowM = now.hour * 60 + now.minute
    val sec = now.second
    val items = itemsOn(d)
    val dep = departureOn(d)
    fun cd(m: Int): String {
        val total = m * 60 - sec
        if (total <= 0) return "0:00"
        val h = total / 3600; val mm = (total % 3600) / 60; val ss = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, mm, ss) else "%d:%02d".format(mm, ss)
    }
    if (dep != null && !left && nowM < dep.second.start) {
        val (at, first) = dep
        return if (nowM < at) Hero("등교 출발까지", cd(at - nowM), "${hhmm(at)} 출발 · ${first.name}", Tone.GO)
        else Hero("지금 나가야 해요", cd(first.start - nowM), "${first.name}까지", Tone.LATE)
    }
    val cur = items.firstOrNull { nowM >= it.start && nowM < it.end }
    val next = items.firstOrNull { nowM < it.start }
    return when {
        cur != null -> Hero("${cur.name} 끝나기까지", cd(cur.end - nowM),
            next?.let { "다음: ${it.name} ${hhmm(it.start)}" } ?: "오늘 마지막 일정", Tone.OK)
        next != null -> Hero("${next.name}까지", cd(next.start - nowM), next.room, Tone.GO)
        else -> {
            val tmr = d.plusDays(1)
            val td = departureOn(tmr)
            val f = itemsOn(tmr).firstOrNull()
            Hero("오늘 일정 끝", "잘 시간",
                td?.let { "내일 ${hhmm(it.first)} 출발" } ?: f?.let { "내일 ${hhmm(it.start)} ${it.name}" } ?: "", Tone.OK)
        }
    }
}
