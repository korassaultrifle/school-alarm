# 2026-2학기 시간표 + 하루 루틴 → 갤럭시(삼성 캘린더)용 .ics
# 등교일: 첫 일정(헬스) 1시간 30분 전 출발 알림 / 수업: 10분 전 / 루틴: 시작할 때
from datetime import date, timedelta

START = date(2026, 10, 19)          # 이번 주 월요일부터
UNTIL = "20261218T235959Z"          # 종강 예상일. 다르면 여기만 고치고 다시 실행
LEAD = 90                           # 등교 출발: 학교 첫 일정 몇 분 전

# (요일들 0=월, 이름, 장소, 시작, 끝, 알림 분 목록, 학교 일정인지)
EVENTS = [
    # 수업
    ([0], "ICT시대의 디자인씽킹", "수리관 (A)-501", "1200", "1450", [10], True),
    ([0], "메타디자인",           "대신관 (C)-303", "1500", "1750", [10], True),
    ([1], "비주얼내러티브",       "대신관 (C)-306", "1200", "1450", [10], True),
    ([1], "3D컴퓨터그래픽스II",   "대신관 (C)-302", "1500", "1750", [10], True),
    ([2], "자기계발과 Life DESIGN", "",             "1200", "1250", [10], True),
    ([4], "인터페이스디자인",     "대신관 (C)-303", "1300", "1550", [10], True),
    # 운동: 수업 날은 첫 수업 1시간 30분 전 학교 헬스장(끝나고 30분 씻기), 목요일은 동네에서
    ([0, 1, 2], "헬스",          "학교 헬스장",    "1030", "1130", [0], True),
    ([4],       "헬스",          "학교 헬스장",    "1130", "1230", [0], True),
    ([3],       "산책·달리기·자전거", "",          "1000", "1100", [0], False),
    # 저녁 루틴 (월~금)
    ([0, 1, 2, 3, 4], "과제",   "", "1930", "2100", [0], False),
    ([0, 1, 2, 3, 4], "그림 연습", "", "2100", "2200", [0], False),
    ([0, 1, 2, 3, 4], "독서",   "", "2200", "2230", [0], False),
    ([0, 1, 2, 3, 4], "게임 (야식 금지)", "", "2230", "2400", [0], False),
    # 게임 종료 알림 (자정 = 다음 날 00:00)
    ([1, 2, 3, 4, 5], "게임 끝! 잘 준비", "", "0000", "0005", [0], False),
    # 다이어트: 아침 체중, 끼니마다 단백질 먼저 (하루 2,000kcal · 단백질 130g)
    ([0, 1, 2, 3, 4], "체중 재기", "화장실 다녀와서", "0740", "0745", [0], False),
    ([5, 6],          "체중 재기", "화장실 다녀와서", "0930", "0935", [0], False),
    ([0, 1, 2, 3, 4], "아침 (단백질 먼저)", "", "0800", "0820", [0], False),
    ([0, 1, 2],       "점심 (헬스 끝, 단백질 먼저)", "", "1135", "1155", [0], False),
    ([3],             "점심 (단백질 먼저)", "", "1200", "1230", [0], False),
    ([4],             "점심 (헬스 끝, 단백질 먼저)", "", "1235", "1255", [0], False),
    ([0, 1, 2, 3, 4], "저녁 (단백질 먼저)", "", "1900", "1930", [0], False),
    ([5, 6],          "야식 금지", "", "2230", "2235", [0], False),
]
DAY = ["MO", "TU", "WE", "TH", "FR", "SA", "SU"]

first_school = {}
for days, _n, _r, s, _e, _a, school in EVENTS:
    if school:
        for d in days:
            first_school[d] = min(first_school.get(d, s), s)

def t(s):
    return f"{int(s[:2]):02d}:{s[2:]}"

def alarm(minutes, text):
    trig = "TRIGGER:PT0M" if minutes == 0 else f"TRIGGER:-PT{minutes}M"
    return ["BEGIN:VALARM", "ACTION:DISPLAY", f"DESCRIPTION:{text}", trig, "END:VALARM"]

lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//school-alarm//KO",
         "CALSCALE:GREGORIAN", "X-WR-CALNAME:2026-2 시간표·루틴", "X-WR-TIMEZONE:Asia/Seoul",
         "BEGIN:VTIMEZONE", "TZID:Asia/Seoul", "BEGIN:STANDARD", "DTSTART:19700101T000000",
         "TZOFFSETFROM:+0900", "TZOFFSETTO:+0900", "TZNAME:KST", "END:STANDARD", "END:VTIMEZONE"]

uid = 0
for days, name, room, s, e, alarms, school in EVENTS:
    # 시작 시각이 같은 요일끼리 한 반복 일정으로. 등교 첫 일정이 낀 요일은 따로 뗀다
    groups = {}
    for d in days:
        is_first = school and first_school.get(d) == s
        groups.setdefault(is_first, []).append(d)
    for is_first, ds in groups.items():
        d0 = START + timedelta(days=min(ds))
        end_day = d0 + timedelta(days=1) if e == "2400" else d0
        e_ = "0000" if e == "2400" else e
        lines += ["BEGIN:VEVENT", f"UID:ev-{uid}-2026-2@school-alarm", "DTSTAMP:20261019T000000Z",
                  f"DTSTART;TZID=Asia/Seoul:{d0:%Y%m%d}T{s}00",
                  f"DTEND;TZID=Asia/Seoul:{end_day:%Y%m%d}T{e_}00",
                  f"RRULE:FREQ=WEEKLY;BYDAY={','.join(DAY[d] for d in ds)};UNTIL={UNTIL}",
                  f"SUMMARY:{name}"]
        if room:
            lines.append(f"LOCATION:{room}")
        if is_first:
            lines += alarm(LEAD, f"지금 출발! {name} {t(s)} 시작")
        for m in alarms:
            lines += alarm(m, f"{name} 시작 {room}".strip() if m == 0 else f"{name} {m}분 전 {room}".strip())
        lines.append("END:VEVENT")
        uid += 1

lines.append("END:VCALENDAR")
with open("timetable.ics", "w", encoding="utf-8", newline="") as f:
    f.write("\r\n".join(lines) + "\r\n")
print("ok", uid, "events")
