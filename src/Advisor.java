import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 지금 하면 좋은 일을 알려 주는 도우미.
 *  - tips()   : 홈 화면의 "오늘의 추천" 카드 (급한 순)
 *  - badges() : 왼쪽 메뉴에 붙는 알림 표시 (예: 대회가 열려 있으면 "경기 · 대회" 옆에 ●)
 * 상태만 읽고 아무것도 바꾸지 않는다.
 */
public final class Advisor {
    public static final int URGENT = 0, IMPORTANT = 1, TIP = 2;

    /** 추천 한 줄 */
    public static final class Tip {
        public final int level;
        public final String title, detail;
        public final String screen;    // 눌렀을 때 이동할 화면 ("help" 는 도움말), 없으면 null
        public final String button;

        Tip(int level, String title, String detail, String screen, String button) {
            this.level = level;
            this.title = title;
            this.detail = detail;
            this.screen = screen;
            this.button = button;
        }
    }

    private static GameState cachedFor;
    private static int cachedDay = -1;
    private static long cachedAt;
    private static List<Tip> cachedTips = new ArrayList<>();
    private static Map<String, String> cachedBadges = new HashMap<>();

    private Advisor() {
    }

    /** 화면이 자주 다시 그려지므로 몇 초 동안은 계산 결과를 재사용한다. */
    public static synchronized List<Tip> cachedTips(GameState s) {
        refreshCache(s);
        return cachedTips;
    }

    public static synchronized Map<String, String> cachedBadges(GameState s) {
        refreshCache(s);
        return cachedBadges;
    }

    private static void refreshCache(GameState s) {
        long now = System.currentTimeMillis();
        if (cachedFor != s || cachedDay != s.getDay() || now - cachedAt > 2500) {
            cachedTips = tips(s);
            cachedBadges = badges(s);
            cachedFor = s;
            cachedDay = s.getDay();
            cachedAt = now;
        }
    }

    private static List<Fencer> of(GameState s, WeaponType w) {
        List<Fencer> list = new ArrayList<>();
        for (Fencer f : s.getRoster()) if (f.getWeapon() == w) list.add(f);
        return list;
    }

    private static String days(int n) {
        return n <= 0 ? "오늘" : n + "일 뒤";
    }

    /** 급한 순으로 정렬한 추천 (최대 6개) */
    public static List<Tip> tips(GameState s) {
        List<Tip> out = new ArrayList<>();
        int day = s.getDay();

        // 1. 부상, 지침, 배고픔, 기분 (선수마다 가장 급한 하나만)
        boolean hasFood = false;
        for (Map.Entry<Item, Integer> e : s.getInventory().snapshot().entrySet()) if (e.getKey() instanceof Food && e.getValue() > 0) hasFood = true;
        for (Fencer f : s.getRoster()) {
            if (f.isInjured()) {
                out.add(new Tip(URGENT, f.getName() + "이(가) 다쳤어요", "부상 중에는 훈련 효율이 떨어져요. 치료 아이템을 쓰거나 푹 쉬게 해주세요.", "inventory", "인벤토리 열기"));
            } else if (f.getFatigue() >= 85) {
                out.add(new Tip(URGENT, f.getName() + "의 피로가 높아요 (" + f.getFatigue() + ")", "무리하면 경기에서 힘을 못 써요. 쉬게 하거나 회복 아이템을 써보세요.", "athletes", "선수 관리"));
            } else if (f.getHunger() >= 80) {
                out.add(new Tip(URGENT, f.getName() + "이(가) 배고파요 (" + f.getHunger() + ")", hasFood ? "배고프면 훈련 효율이 떨어져요. 인벤토리의 식단을 먹여주세요." : "배고프면 훈련 효율이 떨어져요. 인벤토리에 식단이 없으니 상점에서 사 오세요.",
                        hasFood ? "inventory" : "shop", hasFood ? "인벤토리 열기" : "상점 가기"));
            } else if (f.getMood() <= 20) {
                out.add(new Tip(IMPORTANT, f.getName() + "의 기분이 안 좋아요 (" + f.getMood() + ")", "기분이 낮으면 실력을 다 못 내요. 맛있는 식단이나 휴식으로 기분을 올려주세요.", "athletes", "선수 관리"));
            }
        }

        // 2. 주급 걱정
        int weekly = s.getStaffWeeklyCost();
        int untilPay = (Staff.PAY_DAYS - day % Staff.PAY_DAYS) % Staff.PAY_DAYS;
        if (weekly > 0 && untilPay <= 2 && s.getGold() < weekly) {
            out.add(new Tip(URGENT, "스태프 주급 " + weekly + "G가 " + days(untilPay) + " 나가요", "보유 골드가 " + s.getGold() + "G뿐이에요. 모자라면 비싼 스태프부터 떠나요. 경기에서 골드를 벌어두세요.", "match", "경기하러 가기"));
        }

        // 3. 대회가 열려 있고 나갈 수 있을 때
        for (Region r : new Region[]{Region.NATIONAL, Region.WORLD, Region.OLYMPIC}) {
            SeasonCalendar.Event ev = SeasonCalendar.current(r, day);
            if (ev == null) continue;
            List<String> ok = new ArrayList<>();
            for (WeaponType w : WeaponType.values()) {
                List<Fencer> team = of(s, w);
                if (!team.isEmpty() && s.canEnter(r, w, team) == null) ok.add(w.getDisplayName());
            }
            if (!ok.isEmpty()) {
                out.add(new Tip(IMPORTANT, r.getDisplayName() + " 개최 중! (" + (ev.end - day + 1) + "일 남음)", String.join(", ", ok) + "부로 출전할 수 있어요. 기간이 끝나기 전에 도전하세요.", "match", "대회 출전하기"));
            }
        }

        // 4. 곧 열리는 대회
        for (Region r : new Region[]{Region.NATIONAL, Region.WORLD, Region.OLYMPIC}) {
            if (SeasonCalendar.current(r, day) != null) continue;
            SeasonCalendar.Event nx = SeasonCalendar.next(r, day);
            if (nx == null || nx.start - day > 10) continue;
            boolean can = false;
            for (WeaponType w : WeaponType.values()) {
                if (!of(s, w).isEmpty() && r.isUnlocked(s.getWins(w))) can = true;
            }
            if (can) {
                out.add(new Tip(IMPORTANT, r.getDisplayName() + "이(가) " + days(nx.start - day) + " 열려요", "그때까지 훈련으로 실력을 올리고 컨디션을 맞춰 두세요.", "training", "훈련하러 가기"));
            }
        }

        // 5. 다음 대회 해금까지 남은 승리
        String bestMsg = null;
        int bestLeft = Integer.MAX_VALUE;
        for (WeaponType w : WeaponType.values()) {
            if (of(s, w).isEmpty()) continue;
            for (Region r : Region.values()) {
                if (r.isUnlocked(s.getWins(w))) continue;
                int left = r.getRequiredWins() - s.getWins(w);
                if (left < bestLeft) {
                    bestLeft = left;
                    bestMsg = w.getDisplayName() + "부 " + r.getDisplayName() + " 해금까지 승리 " + left + "번 남았어요";
                }
                break;   // 이 종목에서 처음 잠긴 대회만 본다
            }
        }
        if (bestMsg != null) out.add(new Tip(TIP, bestMsg, "클럽 대회와 도 대회에서 승리를 쌓으면 더 큰 대회가 열려요.", "match", "경기하러 가기"));

        // 6. 올림픽 출전선
        for (WeaponType w : WeaponType.values()) {
            List<Fencer> team = of(s, w);
            if (team.isEmpty() || s.getWins(w) < Region.OLYMPIC.getRequiredWins() - 5) continue;
            int rank = s.bestRank(w, team);
            if (rank == 0 || rank > 8) {
                out.add(new Tip(TIP, w.getDisplayName() + "부 올림픽 출전선: 파워 랭킹 8위 이내", "지금 " + (rank == 0 ? "-" : rank + "위") + "예요. 훈련과 대회 성적으로 레이팅을 올려요.", "ranking", "랭킹 보기"));
                break;
            }
        }

        // 7. 스태프
        if (s.getStaff().isEmpty() && s.getGold() >= 700) {
            out.add(new Tip(TIP, "코치·스태프를 영입해 보세요", "수석 코치는 훈련 효율을, 트레이너는 회복을, 분석관은 상대 전술을, 영양사는 식단 효과를 높여줘요.", "staff", "스태프 보기"));
        }

        // 8. 쉬고 있는 선수
        List<String> idle = new ArrayList<>();
        for (Fencer f : s.getRoster()) {
            if (!f.isInjured() && !f.isTraining() && !f.isResting() && !f.isBusy() && f.getFatigue() < 60) idle.add(f.getName());
        }
        if (!idle.isEmpty() && s.getMatches() > 0) {
            out.add(new Tip(TIP, String.join(", ", idle.subList(0, Math.min(2, idle.size()))) + (idle.size() > 2 ? " 외 " + (idle.size() - 2) + "명" : "") + "이(가) 훈련하고 있지 않아요",
                    "훈련은 시간이 걸리지만 능력치와 파워 레이팅이 올라요.", "training", "훈련하러 가기"));
        }

        // 9. 의뢰
        int missions = 0;
        for (Mission m : s.getMissions()) if (!m.isDone()) missions++;
        if (missions > 0) out.add(new Tip(TIP, "진행 중인 의뢰가 " + missions + "개 있어요", "의뢰를 끝내면 보상 골드를 받아요.", "goals", "의뢰 보기"));

        // 10. 처음 시작
        if (s.getMatches() == 0) {
            out.add(0, new Tip(IMPORTANT, "첫 경기에 도전해 보세요", "경기 · 대회 메뉴에서 클럽 대회를 골라 선수를 선택하고 '경기 시작'을 누르세요.", "match", "경기하러 가기"));
            out.add(new Tip(TIP, "규칙이 궁금하다면 도움말을 읽어보세요", "전술 상성, 대회 방식, 레이팅까지 한 번에 정리돼 있어요. (F1 키)", "help", "도움말 열기"));
        }

        if (out.isEmpty()) {
            out.add(new Tip(TIP, "지금은 급한 일이 없어요", "훈련으로 실력을 키우거나, 시즌 캘린더에서 다음 대회를 확인해 보세요.", "calendar", "시즌 캘린더"));
        }
        out.sort((a, b) -> Integer.compare(a.level, b.level));   // 안정 정렬: 같은 단계는 위에서 만든 순서 유지
        return out.size() > 6 ? new ArrayList<>(out.subList(0, 6)) : out;
    }

    /** 메뉴 옆에 붙일 알림 표시 (화면 키 -> 표시 문자) */
    public static Map<String, String> badges(GameState s) {
        Map<String, String> b = new HashMap<>();
        int day = s.getDay();
        for (Tip t : tips(s)) {
            if (t.level == URGENT && t.screen != null) b.put(t.screen, "!");
        }
        for (Region r : new Region[]{Region.NATIONAL, Region.WORLD, Region.OLYMPIC}) {
            SeasonCalendar.Event ev = SeasonCalendar.current(r, day);
            if (ev != null) {
                for (WeaponType w : WeaponType.values()) {
                    List<Fencer> team = of(s, w);
                    if (!team.isEmpty() && s.canEnter(r, w, team) == null) b.putIfAbsent("match", "●");
                }
            }
            SeasonCalendar.Event nx = SeasonCalendar.next(r, day);
            if (nx != null && nx.start - day <= 3 && ev == null) b.putIfAbsent("calendar", "●");
        }
        return b;
    }
}
