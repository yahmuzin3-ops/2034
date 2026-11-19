import java.util.ArrayList;
import java.util.List;

/**
 * 시즌 캘린더. 1시즌은 120일(10일씩 12달)이다.
 *  - 자국 선수권: 시즌마다 2번 (20일째, 80일째부터 8일간)
 *  - 세계 선수권: 시즌마다 1번 (50일째부터 10일간)
 *  - 올림픽: 4시즌마다 1번 (2, 6, 10... 시즌의 100일째부터 15일간)
 * 클럽 대회와 도 대회는 언제든 열려 있다.
 */
public final class SeasonCalendar {
    public static final int YEAR = 120;
    public static final int MONTH = 10;

    public static final class Event {
        public final Region region;
        public final int year;     // 0부터 시작하는 시즌 번호
        public final int start, end;   // 날짜(일차), 양 끝 포함

        Event(Region region, int year, int start, int end) {
            this.region = region;
            this.year = year;
            this.start = start;
            this.end = end;
        }

        public String getName() {
            return region == Region.OLYMPIC ? "올림픽" : region.getDisplayName();
        }

        public boolean isOpen(int day) { return day >= start && day <= end; }

        public String getPeriodText() {
            return dateText(start) + " ~ " + (yearOf(start) == yearOf(end) ? dateText(end).substring(dateText(end).indexOf('·') + 2) : dateText(end));
        }

        /** 이 대회를 구분하는 값 (같은 대회에 두 번 출전하지 못하게 할 때 쓴다) */
        public String key() { return region.name() + "@" + start; }
    }

    private SeasonCalendar() {
    }

    public static int yearOf(int day) { return Math.max(0, (day - 1) / YEAR); }

    public static int dayOfYear(int day) { return (day - 1) % YEAR + 1; }

    /** 예: 시즌 2 · 4월 3일 */
    public static String dateText(int day) {
        int doy = dayOfYear(day);
        return "시즌 " + (yearOf(day) + 1) + " · " + ((doy - 1) / MONTH + 1) + "월 " + ((doy - 1) % MONTH + 1) + "일";
    }

    private static List<Event> eventsOfYear(int year) {
        int base = year * YEAR;
        List<Event> list = new ArrayList<>();
        list.add(new Event(Region.NATIONAL, year, base + 20, base + 27));
        list.add(new Event(Region.WORLD, year, base + 50, base + 59));
        list.add(new Event(Region.NATIONAL, year, base + 80, base + 87));
        if (year % 4 == 1) list.add(new Event(Region.OLYMPIC, year, base + 100, base + 114));
        return list;
    }

    /** from~to 사이에 걸치는 대회 (날짜순) */
    public static List<Event> between(int from, int to) {
        List<Event> out = new ArrayList<>();
        for (int y = yearOf(from); y <= yearOf(to); y++) {
            for (Event e : eventsOfYear(y)) if (e.end >= from && e.start <= to) out.add(e);
        }
        return out;
    }

    /** 지금 열려 있는 이 지역의 대회 (없으면 null) */
    public static Event current(Region r, int day) {
        for (Event e : between(day, day)) if (e.region == r) return e;
        return null;
    }

    /** 앞으로 열릴 이 지역의 첫 대회 */
    public static Event next(Region r, int day) {
        for (int y = yearOf(day); y < yearOf(day) + 9; y++) {
            for (Event e : eventsOfYear(y)) if (e.region == r && e.start > day) return e;
        }
        return null;
    }

    /** 선택 목록 등에 붙일 상태 문구 */
    public static String statusText(Region r, int day) {
        Event cur = current(r, day);
        if (cur != null) return "개최 중, " + (cur.end - day + 1) + "일 남음";
        Event nx = next(r, day);
        return nx == null ? "일정 없음" : "다음 대회 " + (nx.start - day) + "일 뒤";
    }
}
