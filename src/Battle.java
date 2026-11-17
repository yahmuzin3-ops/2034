import java.util.*;

/**
 * 실제 펜싱 규칙을 따르는 경기 엔진.
 *
 * [경기 방식]
 *  - 개인전(선수 1명 출전): 15점 선취, 3분 x 3피리어드(사이에 1분 휴식), 동점이면 서든데스
 *  - 단체전(2~3명 출전): 릴레이. 선수마다 상대 선수와 한 번씩 겨룬다. (3명이면 9경기, 2명이면 4경기)
 *      경기마다 3분, 경기 번호 x 5점에 먼저 닿으면 그 경기가 끝나고 점수는 팀 전체로 누적된다. (45점 / 20점)
 *      마지막 경기까지 점수가 높은 팀이 승리, 동점이면 서든데스
 *  - 서든데스: 우선권을 추첨하고 1분 동안 먼저 득점한 쪽이 승리, 득점이 없으면 우선권을 가진 쪽이 승리
 *
 * [한 구간(프레이즈)]
 *  양쪽이 동시에 전술(공격 / 페인트 / 방어·리포스트 / 반격 / 전진 / 후퇴)을 고르고 한꺼번에 공개한다.
 *
 * [종목별 규칙]
 *  - 플뢰레: 몸통만 유효(밖을 맞히면 경기 중단), 우선권(Right of way) 있음, 방어 후 리포스트가 강하고 페인트가 잘 통함
 *  - 에페  : 전신 유효, 우선권 없음, 동시에 맞으면 동시타(양쪽 득점), 반격(스톱히트)이 강함
 *  - 사브르: 허리 위 유효, 우선권 있음, 속공이 빠르지만 방어가 약함
 *  - 우선권(플뢰레·사브르): 공격을 먼저 시작한 선수 또는 방어에 성공한 선수가 갖는다.
 *      둘 다 맞으면 우선권을 가진 쪽만 득점하고, 우선권 없이 동시에 공격하면 무득점으로 중단된다.
 *
 * [체력(HP)] 전술마다 체력을 쓰고, 체력이 낮으면 모든 동작의 성공률이 최대 18% 떨어진다. 휴식 때 일부 회복한다.
 * [날씨] 맑음: 체력 소모 -1 / 흐림: 무효 타격 감소 / 비: 미끄러운 바닥(전진·후퇴 효과 감소, 무효 타격 증가, 사브르 속공 약화)
 *        / 무더위: 체력 소모 +1. (논슬립 신발, 쿨링 재킷으로 막을 수 있다)
 *
 * 로직은 구간마다 한 번에 계산하고, 화면 연출용으로 Event 를 쌓아둔다. (FencingStage 가 하나씩 재생)
 */
public class Battle {

    /** 한 구간에서 고르는 전술 */
    public enum Tactic {
        ATTACK("공격", 4, "런지로 찌르거나 베어요. 우선권을 얻지만 방어에 막히고 반격에 약해요."),
        FEINT("페인트", 4, "가짜 공격으로 방어를 속여요. 방어에는 강하지만 반격에 약해요."),
        PARRY("방어·리포스트", 2, "공격을 막고 곧바로 반격해요. 공격에는 강하지만 페인트에 속아요."),
        COUNTER("반격", 3, "상대 공격에 맞춰 먼저 찔러요(스톱히트). 페인트와 전진에 강해요."),
        ADVANCE("전진", 2, "거리를 좁히며 압박해요. 반격과 공격 타이밍에 노출돼요."),
        RETREAT("후퇴", 2, "거리를 벌려 공격을 흘려요. 4번 연속 후퇴하면 후방 라인을 벗어나 실점해요.");

        private final String displayName;
        private final int cost;
        private final String hint;

        Tactic(String displayName, int cost, String hint) {
            this.displayName = displayName;
            this.cost = cost;
            this.hint = hint;
        }

        public String getDisplayName() { return displayName; }
        public int getCost() { return cost; }
        public String getHint() { return hint; }

        @Override
        public String toString() { return displayName; }
    }

    /** 한 구간의 결과 */
    public enum Outcome { CONTINUE, TOUCH_LEFT, TOUCH_RIGHT, DOUBLE, ANNUL }

    /** 화면 연출용 이벤트 (읽기 전용으로 사용) */
    public static class Event {
        public enum Kind { INFO, BANNER, BOUT, PHRASE, ITEM, END }

        public Kind kind = Kind.INFO;
        public String text = "";          // 로그에 남길 문장
        public String label;              // 화면에 크게 띄울 문구
        public Fencer left, right;        // 지금 피스트 위의 선수 (left = 우리 팀)
        public Tactic leftTactic, rightTactic;
        public Outcome outcome = Outcome.CONTINUE;
        public boolean parried;           // 방어가 성공했는지
        public boolean evaded;            // 후퇴로 공격을 흘렸는지
        public boolean offTarget;         // 무효 타격(백색등)인지
        public boolean exit;              // 후방 라인 이탈인지
        public int riposteSide = -1;      // 반격한 쪽 (0: 왼쪽, 1: 오른쪽, -1: 없음)
        public Fencer priority;           // 우선권을 가진 선수
        public int clock;                 // 남은 시간(초)
        public int leftScore, rightScore; // 팀 점수
        public int boutLeft, boutRight;   // 이번 경기 득점
        public int leftHp, rightHp;       // 체력
        public int value;                 // BOUT: 경기 번호, END: 승리면 1, ITEM 등
        public int leftRetreat, rightRetreat;   // 연속 후퇴 횟수 (4번이면 후방 라인 이탈)
        public int period;                // 개인전 피리어드
        public int target;                // 현재 목표 점수
        public boolean sudden;            // 서든데스 중인지
    }

    /** 경기 설정. 대회(토너먼트) 경기는 보상과 레이팅을 대회가 따로 처리하고, 예선 풀은 5점 1피리어드로 치른다. */
    public static class Options {
        public boolean rewards = true;        // 골드/경험치/전적(승리 수) 정산
        public boolean rated = true;          // 파워 레이팅 반영
        public String title;                  // 로그와 화면에 쓸 제목 (없으면 대회 이름)
        public int target = TARGET_SCORE;     // 개인전 목표 점수
        public int periods = PERIODS;         // 개인전 피리어드 수
    }

    public static final int TARGET_SCORE = 15;     // 개인전 목표 점수
    private static final int BOUT_SECONDS = 180;   // 3분
    private static final int SUDDEN_SECONDS = 60;  // 서든데스 1분
    private static final int PERIODS = 3;          // 개인전 피리어드 수
    private static final int TEAM_BOUT_POINTS = 5; // 단체전 경기당 점수

    /** 구간을 풀이한 결과 */
    private static class Res {
        Fencer scorer;       // 단독 득점자
        boolean dbl;         // 동시타
        boolean annul;       // 득점 없이 중단
        boolean off;         // 무효 타격
        boolean exit;        // 후방 라인 이탈
        boolean parried, evaded;
        Fencer riposter;     // 방어 후 반격을 한 선수
        Fencer newPriority;  // 계속되는 경우 우선권을 갖게 되는 선수
        String text = "";
    }

    private enum Phase { PHRASE, BREAK, OVER }

    private enum Next { BOUT, PERIOD, SUDDEN }

    private final GameState state;
    private final Region region;
    private final Options opt;
    private final boolean boss;
    private final Weather weather;
    private final WeaponType eventWeapon;
    private final List<Fencer> players;
    private final List<Fencer> enemies;
    private final Random rnd = new Random();

    private final boolean individual;
    private final int totalBouts;
    private final int[][] order;

    private int boutIndex = 0;
    private Fencer left, right;
    private int leftScore = 0, rightScore = 0;
    private int boutLeft = 0, boutRight = 0;
    private int clock = BOUT_SECONDS;
    private int period = 1;
    private Fencer priority;            // 우선권 (플뢰레·사브르)
    private int idlePhrases = 0;        // 득점 없이 이어진 구간 수 (소극적 플레이 규칙)
    private boolean suddenDeath = false;
    private Fencer suddenPriority;      // 서든데스 우선권 추첨 결과

    private final Map<Fencer, Double> edge = new HashMap<>();          // 전진으로 얻은 압박 보너스
    private final Map<Fencer, Integer> retreatStreak = new HashMap<>();
    private final Set<Fencer> exhausted = new HashSet<>();
    private final List<Tactic> leftHistory = new ArrayList<>();
    private final List<Tactic> rightHistory = new ArrayList<>();

    private Phase phase = Phase.PHRASE;
    private Next next = Next.BOUT;
    private boolean won = false;
    private boolean settled = false;
    private boolean endedByScore = false;

    private final List<String> pending = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    public Battle(GameState state, Region region, List<Fencer> players) {
        this(state, region, players, false);
    }

    public Battle(GameState state, Region region, List<Fencer> players, boolean boss) {
        this(state, region, players, boss, null);
    }

    /** customEnemies 가 null 이 아니면 그 선수들을 상대로 쓴다. (테스트/확장용) */
    public Battle(GameState state, Region region, List<Fencer> players, boolean boss, List<Fencer> customEnemies) {
        this(state, region, players, boss, customEnemies, new Options());
    }

    public Battle(GameState state, Region region, List<Fencer> players, boolean boss, List<Fencer> customEnemies, Options options) {
        this.state = state;
        this.region = region;
        this.opt = options == null ? new Options() : options;
        this.boss = boss;
        this.weather = state.getWeather();
        this.players = new ArrayList<>(players);
        this.eventWeapon = weaponOf(players);
        int n = players.size();
        this.enemies = customEnemies != null ? new ArrayList<>(customEnemies)
                : boss ? region.createBoss(eventWeapon) : region.createEnemies(n, eventWeapon);
        for (Fencer f : this.players) f.setBusy(true);

        this.individual = n == 1;
        this.totalBouts = individual ? 1 : n * n;
        this.order = buildOrder(n);

        String title = opt.title != null ? opt.title
                : region.getDisplayName() + (eventWeapon != null ? " " + eventWeapon.getDisplayName() + "부" : "");
        log("=== " + title + (boss ? " 결승전 (보스전)" : "") + (individual ? " 개인전" : " 단체전") + " ===");
        StringBuilder sb = new StringBuilder(boss ? "결승 상대: " : "상대 팀: ");
        for (Fencer e : enemies) {
            sb.append(e.getName()).append("(").append(e.getWeapon().getDisplayName())
                    .append(" Lv.").append(e.getLevel()).append(") ");
        }
        log(sb.toString().trim());
        if (individual) {
            log("개인전(다이렉트 엘리미네이션): " + opt.target + "점 선취, 3분 x " + opt.periods
                    + "피리어드, 동점이면 서든데스(우선권 추첨 후 1분)");
        } else {
            log("단체전 릴레이: 선수마다 상대와 한 번씩 겨뤄 " + totalBouts + "경기, 경기마다 3분, 점수는 팀 전체로 누적 ("
                    + TEAM_BOUT_POINTS * totalBouts + "점). 끝까지 점수가 높은 팀이 승리, 동점이면 서든데스");
        }
        log("오늘 날씨: " + weather.getDisplayName() + " - " + weather.getBattleNote());
        if (eventWeapon != null) log(eventWeapon.getDisplayName() + " 규칙: " + eventWeapon.getRuleSummary());
        startBout("");
    }

    // ---------- 조회 ----------

    public List<Fencer> getPlayers() { return players; }
    public List<Fencer> getEnemies() { return enemies; }
    public Fencer getLeft() { return left; }
    public Fencer getRight() { return right; }
    public boolean isOver() { return phase == Phase.OVER; }
    public boolean isWon() { return won; }
    public boolean isBoss() { return boss; }
    public boolean isIndividual() { return individual; }
    public boolean isOnBreak() { return phase == Phase.BREAK; }
    public boolean isSuddenDeath() { return suddenDeath; }
    public Weather getWeather() { return weather; }
    public int getPlayerScore() { return leftScore; }
    public int getEnemyScore() { return rightScore; }
    public int getBoutLeft() { return boutLeft; }
    public int getBoutRight() { return boutRight; }
    public int getBoutNumber() { return boutIndex + 1; }
    public int getBoutCount() { return totalBouts; }
    public int getPeriod() { return period; }
    public int getClock() { return clock; }
    public Fencer getPriority() { return priority; }
    public Fencer getSuddenPriority() { return suddenPriority; }
    public boolean isEndedByScore() { return endedByScore; }
    public boolean isExhausted(Fencer f) { return exhausted.contains(f); }
    public int getRetreatStreak(Fencer f) { return retreatStreak.getOrDefault(f, 0); }

    /** 이번 경기에서 상대 선수가 최근에 고른 전술 (최신이 마지막) */
    public List<Tactic> getRightHistory() { return new ArrayList<>(rightHistory); }

    /** 팀 경기의 현재 목표 점수 (개인전은 15점) */
    public int getTargetScore() { return individual ? opt.target : TEAM_BOUT_POINTS * (boutIndex + 1); }

    /** 최종 목표 점수 (단체전은 경기 수 x 5점) */
    public int getFinalScore() { return individual ? opt.target : TEAM_BOUT_POINTS * totalBouts; }

    /** 상대 선수의 성향 안내 (경기 전에 상대를 살펴본 정보) */
    public String getScoutReport() {
        if (right == null) return "";
        double[] w = baseWeights(right);
        Tactic[] ts = Tactic.values();
        int a = 0, b = -1;
        for (int i = 1; i < w.length; i++) if (w[i] > w[a]) a = i;
        for (int i = 0; i < w.length; i++) if (i != a && (b < 0 || w[i] > w[b])) b = i;
        return right.getName() + " (" + right.getWeapon().getDisplayName() + "·" + right.getPersonality().getDisplayName()
                + ") - " + ts[a].getDisplayName() + "과(와) " + ts[b].getDisplayName() + "을(를) 즐겨 써요";
    }

    public List<String> drainLog() {
        List<String> copy = new ArrayList<>(pending);
        pending.clear();
        return copy;
    }

    public List<Event> drainEvents() {
        List<Event> copy = new ArrayList<>(events);
        events.clear();
        return copy;
    }

    // ---------- 진행 ----------

    /** 우리 선수의 전술을 고르고 한 구간을 진행한다. 진행했으면 true */
    public boolean choose(Tactic mine) {
        if (phase != Phase.PHRASE || mine == null) return false;
        Tactic theirs = ai(right, left, leftHistory, boutRight, boutLeft);
        phrase(mine, theirs);
        return true;
    }

    /** 컴퓨터가 추천하는 우리 선수의 전술 (자동 진행에 사용) */
    public Tactic suggest() {
        return ai(left, right, rightHistory, boutLeft, boutRight);
    }

    /** 휴식 시간에만 아이템을 쓸 수 있다. */
    public boolean useItem(Item item, Fencer target) {
        if (phase != Phase.BREAK || item == null || target == null) return false;
        String msg = state.useItem(target, item);
        Event e = ev(Event.Kind.ITEM, msg);
        e.value = target.getHp();
        e.left = target;   // 체력이 바뀐 선수
        return true;
    }

    /** 휴식을 마치고 다음 경기 / 피리어드 / 서든데스를 시작한다. */
    public boolean continueFromBreak() {
        if (phase != Phase.BREAK) return false;
        int heal = individual ? 20 : 15;   // 휴식 시간에 체력 일부 회복
        for (Fencer f : players) f.heal(heal);
        for (Fencer f : enemies) f.heal(heal);
        switch (next) {
            case BOUT:
                boutIndex++;
                startBout("");
                break;
            case PERIOD:
                period++;
                clock = BOUT_SECONDS;
                resetPhraseState();
                phase = Phase.PHRASE;
                emitBout(period + "피리어드 시작");
                break;
            default:
                startSuddenDeath();
                break;
        }
        return true;
    }

    public void surrender() {
        if (phase == Phase.OVER) return;
        finish(false, "기권했다...");
    }

    // ---------- 경기 시작 / 종료 ----------

    private static int[][] buildOrder(int n) {
        if (n == 3) {   // 공식 단체전 순서: A-X, B-Z, C-Y, A-Z, C-X, B-Y, A-Y, C-Z, B-X
            return new int[][]{{0, 0}, {1, 2}, {2, 1}, {0, 2}, {2, 0}, {1, 1}, {0, 1}, {2, 2}, {1, 0}};
        }
        if (n == 2) return new int[][]{{0, 0}, {1, 1}, {0, 1}, {1, 0}};
        return new int[][]{{0, 0}};
    }

    private void startBout(String extra) {
        left = players.get(order[boutIndex][0] % players.size());
        right = enemies.get(order[boutIndex][1] % enemies.size());
        boutLeft = 0;
        boutRight = 0;
        clock = BOUT_SECONDS;
        period = 1;
        leftHistory.clear();
        rightHistory.clear();
        resetPhraseState();
        phase = Phase.PHRASE;
        String banner = individual ? "개인전 1피리어드" : "제 " + (boutIndex + 1) + "경기  " + left.getName() + " vs " + right.getName();
        emitBout(banner + extra);
    }

    private void startSuddenDeath() {
        suddenDeath = true;
        left = bestOf(players);
        right = bestOf(enemies);
        boutLeft = 0;
        boutRight = 0;
        clock = SUDDEN_SECONDS;
        leftHistory.clear();
        rightHistory.clear();
        resetPhraseState();
        suddenPriority = rnd.nextBoolean() ? left : right;
        phase = Phase.PHRASE;
        log("서든데스! 우선권 추첨 결과: " + suddenPriority.getName() + ". 1분 안에 먼저 득점하면 승리, 득점이 없으면 우선권을 가진 쪽이 승리해요.");
        emitBout("서든데스");
    }

    private static Fencer bestOf(List<Fencer> list) {
        Fencer best = list.get(0);
        for (Fencer f : list) {
            if (f.getHp() / (double) f.getMaxHp() > best.getHp() / (double) best.getMaxHp()) best = f;
        }
        return best;
    }

    private void emitBout(String banner) {
        Event e = ev(Event.Kind.BOUT, banner);
        e.label = banner;
        e.value = boutIndex + 1;
    }

    private void resetPhraseState() {
        priority = null;
        edge.clear();
        retreatStreak.clear();
    }

    private void finish(boolean win, String text) {
        phase = Phase.OVER;
        won = win;
        Event e = ev(Event.Kind.END, text);
        e.value = win ? 1 : 0;
    }

    // ---------- 한 구간 ----------

    private void phrase(Tactic tl, Tactic tr) {
        Res res = resolve(left, tl, right, tr);

        // 후방 라인 이탈: 4번 연속 후퇴하면 상대 득점
        updateStreak(left, tl);
        updateStreak(right, tr);
        boolean touch = res.scorer != null || res.dbl;
        if (!touch && !res.annul) {
            boolean lx = getRetreatStreak(left) >= 4, rx = getRetreatStreak(right) >= 4;
            if (lx || rx) {
                Fencer loser = lx && rx ? (rnd.nextBoolean() ? left : right) : lx ? left : right;
                Fencer other = loser == left ? right : left;
                res.scorer = other;
                res.exit = true;
                res.text = loser.getName() + "이(가) 후방 라인을 벗어났다! " + other.getName() + " 득점";
            }
        }

        spendStamina(left, tl);
        spendStamina(right, tr);
        int dt = 8 + rnd.nextInt(7);   // 한 구간에 8~14초
        clock = Math.max(0, clock - dt);

        int dl = 0, dr = 0;
        if (res.dbl) {
            dl = 1;
            dr = 1;
        } else if (res.scorer == left) {
            dl = 1;
        } else if (res.scorer == right) {
            dr = 1;
        }
        leftScore += dl;
        rightScore += dr;
        boutLeft += dl;
        boutRight += dr;

        boolean ended = dl + dr > 0 || res.annul;   // 득점 또는 중단 후에는 앙가르드 선으로 돌아간다
        idlePhrases = (dl + dr > 0) ? 0 : idlePhrases + 1;
        if (ended) {
            resetPhraseState();
        } else if (res.newPriority != null && res.newPriority.getWeapon() != WeaponType.EPEE) {
            priority = res.newPriority;
        }
        decayEdge(tl, tr);

        Event e = ev(Event.Kind.PHRASE, null);
        e.leftTactic = tl;
        e.rightTactic = tr;
        e.parried = res.parried;
        e.evaded = res.evaded;
        e.offTarget = res.off;
        e.exit = res.exit;
        e.riposteSide = res.riposter == null ? -1 : res.riposter == left ? 0 : 1;
        e.outcome = res.dbl ? Outcome.DOUBLE : res.scorer == left ? Outcome.TOUCH_LEFT
                : res.scorer == right ? Outcome.TOUCH_RIGHT : res.annul ? Outcome.ANNUL : Outcome.CONTINUE;
        String line = left.getName() + " " + tl.getDisplayName() + "  /  " + right.getName() + " " + tr.getDisplayName()
                + "  →  " + res.text;
        if (dl + dr > 0) line += "\n   점수  우리 팀 " + leftScore + " : " + rightScore + " 상대";
        e.text = line;
        pending.add(line);

        leftHistory.add(tl);
        rightHistory.add(tr);
        checkProgress(e.outcome);
    }

    private void updateStreak(Fencer f, Tactic t) {
        if (t == Tactic.RETREAT) retreatStreak.merge(f, 1, Integer::sum);
        else retreatStreak.put(f, 0);
    }

    private void spendStamina(Fencer f, Tactic t) {
        int c = t.getCost();
        if (weather == Weather.HOT && !f.hasTag(GearTag.HEAT_PROOF)) c += 1;   // 무더위
        if (weather == Weather.SUNNY && c > 1) c -= 1;                          // 관중 열기
        f.takeDamage(c);
        if (f.getHp() <= 0) exhausted.add(f);
    }

    private void decayEdge(Tactic tl, Tactic tr) {
        edge.replaceAll((k, v) -> v * 0.4);
        boolean slippery = weather == Weather.RAINY;
        if (tl == Tactic.ADVANCE && !(slippery && !left.hasTag(GearTag.RAIN_PROOF))) edge.merge(left, 0.05, (a, b) -> Math.min(0.12, a + b));
        if (tr == Tactic.ADVANCE && !(slippery && !right.hasTag(GearTag.RAIN_PROOF))) edge.merge(right, 0.05, (a, b) -> Math.min(0.12, a + b));
    }

    // ---------- 진행 상황 확인 ----------

    private void checkProgress(Outcome o) {
        if (phase == Phase.OVER) return;

        if (suddenDeath) {   // 서든데스: 먼저 득점하면 승리, 동시타면 우선권 보유자 승리
            if (o == Outcome.TOUCH_LEFT) endMatch(true, "서든데스 득점! 승리!");
            else if (o == Outcome.TOUCH_RIGHT) endMatch(false, "서든데스에서 상대가 득점했다... 패배");
            else if (o == Outcome.DOUBLE) endMatch(suddenPriority == left, "서든데스 동시타! 우선권 " + suddenPriority.getName() + "의 승리");
            else if (clock <= 0) endMatch(suddenPriority == left, "서든데스 시간 종료! 우선권 " + suddenPriority.getName() + "의 승리");
            return;
        }

        if (individual) {
            boolean l = leftScore >= opt.target, r = rightScore >= opt.target;
            if (l || r) {
                endedByScore = true;
                if (leftScore == rightScore) pauseFor(Next.SUDDEN, opt.target + "점 동점! 서든데스로 넘어가요.");
                else endMatch(leftScore > rightScore, leftScore > rightScore ? opt.target + "점 선취! 승리!" : "상대가 " + opt.target + "점에 먼저 도달했다... 패배");
                return;
            }
            if (clock <= 0) {
                if (period < opt.periods) {
                    pauseFor(Next.PERIOD, period + "피리어드 종료 (" + leftScore + " : " + rightScore + "). 1분 휴식!");
                } else if (leftScore == rightScore) {
                    pauseFor(Next.SUDDEN, "" + opt.periods + "피리어드 종료, " + leftScore + " : " + rightScore + " 동점! 서든데스로 넘어가요.");
                } else {
                    endMatch(leftScore > rightScore, "경기 종료! " + leftScore + " : " + rightScore + (leftScore > rightScore ? " 로 승리" : " 로 패배"));
                }
            }
            return;
        }

        // 단체전
        int target = TEAM_BOUT_POINTS * (boutIndex + 1);
        boolean boutDone = leftScore >= target || rightScore >= target || clock <= 0;
        if (!boutDone) return;
        boolean last = boutIndex >= totalBouts - 1;
        if (!last) {
            endedByScore = leftScore >= target || rightScore >= target;
            pauseFor(Next.BOUT, "제 " + (boutIndex + 1) + "경기 종료 (팀 점수 " + leftScore + " : " + rightScore + "). 다음 경기 준비!");
        } else if (leftScore == rightScore) {
            pauseFor(Next.SUDDEN, "마지막 경기 종료, " + leftScore + " : " + rightScore + " 동점! 서든데스로 넘어가요.");
        } else {
            endedByScore = true;
            endMatch(leftScore > rightScore, "경기 종료! " + leftScore + " : " + rightScore + (leftScore > rightScore ? " 로 승리" : " 로 패배"));
        }
    }

    private void pauseFor(Next n, String text) {
        phase = Phase.BREAK;
        next = n;
        Event e = ev(Event.Kind.BANNER, text + "  (휴식 시간: 아이템을 쓸 수 있어요)");
        e.label = text.length() > 18 ? text.substring(0, text.indexOf('.') > 0 ? text.indexOf('.') : 18) : text;
    }

    private void endMatch(boolean win, String text) {
        finish(win, "=== " + text + " ===");
    }

    // ---------- 규칙 풀이 ----------

    private static boolean isAtk(Tactic t) { return t == Tactic.ATTACK || t == Tactic.FEINT; }

    private boolean roll(double p) { return rnd.nextDouble() < p; }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private Res resolve(Fencer l, Tactic tl, Fencer r, Tactic tr) {
        boolean al = isAtk(tl), ar = isAtk(tr);
        if (al && ar) return clash(l, tl, r, tr);
        if (al) return attackVs(l, tl, r, tr, false);
        if (ar) return attackVs(r, tr, l, tl, false);

        // 공격이 없는 경우
        if (tl == Tactic.PARRY && tr == Tactic.COUNTER) return attackVs(r, Tactic.ATTACK, l, Tactic.PARRY, true);
        if (tr == Tactic.PARRY && tl == Tactic.COUNTER) return attackVs(l, Tactic.ATTACK, r, Tactic.PARRY, true);
        if (tl == Tactic.COUNTER && tr == Tactic.ADVANCE) return counterOnAdvance(l, r);
        if (tr == Tactic.COUNTER && tl == Tactic.ADVANCE) return counterOnAdvance(r, l);
        Res res = new Res();
        if (tl == Tactic.ADVANCE && tr == Tactic.ADVANCE) {
            if (roll(0.15)) {
                res.annul = true;
                res.text = "두 선수가 부딪혔어요 (코르 아 코르) - 경기 중단, 무득점";
            } else {
                res.text = "두 선수가 거리를 좁히며 대치";
            }
        } else if (tl == Tactic.RETREAT || tr == Tactic.RETREAT) {
            res.text = tl == Tactic.ADVANCE || tr == Tactic.ADVANCE ? "한쪽이 압박하고 한쪽이 물러남" : "거리를 벌리며 탐색";
        } else {
            res.text = "서로 상대의 움직임을 살핌";
        }
        return res;
    }

    /** x 가 공격(또는 페인트)하고 y 는 공격이 아닌 전술을 골랐을 때 */
    private Res attackVs(Fencer x, Tactic tx, Fencer y, Tactic ty, boolean counterMode) {
        Res r = new Res();
        boolean feint = tx == Tactic.FEINT;
        String xn = x.getName(), yn = y.getName();
        double hitX = hit(x, y) - (counterMode ? 0.05 : 0);

        switch (ty) {
            case PARRY: {
                boolean parried;
                if (feint) parried = !roll(feintChance(x, y));   // 페인트는 방어를 속인다
                else parried = roll(parry(y, x));
                if (parried) return riposteFlow(x, y, r);
                if (feint || roll(hitX)) return landing(x, y, xn + "의 " + (feint ? "페인트가 방어를 속여 " : "공격이 방어를 뚫고 ") + "적중");
                r.text = xn + "의 공격이 빗나감";
                r.newPriority = counterMode ? null : x;
                return r;
            }
            case COUNTER: {
                if (feint) {   // 반격이 페인트를 끊는다
                    if (roll(clamp(0.45 + tempo(y, x) * 0.5, 0.3, 0.82))) return landing(y, x, yn + "의 스톱히트가 페인트를 끊음");
                    if (roll(hitX * 0.8)) return landing(x, y, xn + "의 공격이 적중");
                    r.text = "페인트와 반격이 모두 빗나감";
                    return r;
                }
                boolean hx = roll(hitX), hy = roll(clamp(hit(y, x) + 0.05, 0.15, 0.92));
                if (hx && hy) {
                    if (x.getWeapon() == WeaponType.EPEE) {   // 에페: 거의 동시에 닿으면 동시타
                        if (roll(0.6)) return doubleTouch(r);
                        return roll(tempo(y, x)) ? landing(y, x, yn + "의 스톱히트가 먼저 닿음") : landing(x, y, xn + "의 공격이 먼저 닿음");
                    }
                    // 플뢰레·사브르: 공격에 우선권이 있어서, 반격이 확실히 먼저 들어간 경우에만 반격이 인정된다
                    if (roll(tempo(y, x) * 0.55)) return landing(y, x, yn + "의 스톱히트가 공격 전에 닿음");
                    return landing(x, y, xn + "의 공격(우선권)이 인정됨");
                }
                if (hx) return landing(x, y, xn + "의 공격이 적중");
                if (hy) return landing(y, x, yn + "의 스톱히트가 적중");
                r.text = "공격과 반격이 서로 빗나감";
                r.newPriority = x;
                return r;
            }
            case ADVANCE: {   // 전진(준비 동작)을 공격: 준비 동작에 대한 공격은 유효
                double p = clamp(hitX + 0.10, 0.15, 0.92) * (feint ? 0.9 : 1.0);
                if (roll(p)) return landing(x, y, xn + "이(가) " + yn + "의 전진을 공격해 적중");
                r.text = xn + "의 공격이 전진하는 상대에게 빗나감";
                r.newPriority = counterMode ? null : x;
                return r;
            }
            default: {   // RETREAT
                if (roll(evade(y, x))) {
                    r.evaded = true;
                    if (!feint && roll(clamp(0.5 * hit(y, x) + 0.1, 0.1, 0.6))) {
                        r.riposter = y;
                        return landing(y, x, yn + "이(가) 후퇴로 공격을 흘리고 반격 적중", r);
                    }
                    r.text = xn + "의 공격이 거리가 모자라 닿지 않음 (" + yn + " 후퇴로 회피)";
                    return r;
                }
                if (!feint && roll(clamp(hitX - 0.05, 0.1, 0.9))) return landing(x, y, xn + "이(가) 물러나는 상대를 따라가 적중");
                r.text = xn + "의 " + (feint ? "페인트" : "공격") + "이 닿지 않음";
                return r;
            }
        }
    }

    /** y 가 방어에 성공했을 때: 우선권을 얻고 곧바로 리포스트를 시도한다. */
    private Res riposteFlow(Fencer x, Fencer y, Res r) {
        r.parried = true;
        r.riposter = y;
        if (roll(riposte(y, x))) {
            return landing(y, x, yn(y) + "의 방어 성공 후 리포스트 적중", r);
        }
        r.text = yn(y) + "이(가) 막아냈지만 리포스트가 빗나감 (우선권 " + yn(y) + ")";
        r.newPriority = y;
        return r;
    }

    private static String yn(Fencer f) { return f.getName(); }

    /** 양쪽이 동시에 공격(또는 페인트)한 경우: 종목마다 규칙이 다르다. */
    private Res clash(Fencer l, Tactic tl, Fencer r, Tactic tr) {
        Res res = new Res();
        boolean hl = roll(hit(l, r) * (tl == Tactic.FEINT ? 0.85 : 1.0));
        boolean hr = roll(hit(r, l) * (tr == Tactic.FEINT ? 0.85 : 1.0));

        if (l.getWeapon() == WeaponType.EPEE) {   // 에페: 우선권 없음, 동시에 맞으면 동시타
            if (hl && hr) {
                if (roll(0.75)) return doubleTouch(res);
                return roll(tempo(l, r)) ? landing(l, r, l.getName() + "의 공격이 조금 먼저 닿음")
                        : landing(r, l, r.getName() + "의 공격이 조금 먼저 닿음");
            }
            if (hl) return landing(l, r, l.getName() + "의 공격이 적중");
            if (hr) return landing(r, l, r.getName() + "의 공격이 적중");
            res.text = "서로의 공격이 빗나감";
            return res;
        }

        // 플뢰레·사브르: 우선권(Right of way)
        Fencer holder = priority;
        if (holder == null) {
            double t = tempo(l, r);
            if (Math.abs(t - 0.5) < 0.08) {   // 거의 동시에 시작 → 우선권 판정 불가
                res.annul = true;
                res.text = "동시 공격 - 우선권 없음, 무득점으로 중단";
                return res;
            }
            holder = roll(t) ? l : r;   // 먼저 공격을 시작한 선수가 우선권
        }
        Fencer other = holder == l ? r : l;
        boolean holderHit = holder == l ? hl : hr;
        boolean otherHit = holder == l ? hr : hl;
        if (holderHit) return landing(holder, other, holder.getName() + "의 공격(우선권)이 인정됨");
        if (otherHit) return landing(other, holder, holder.getName() + "의 공격이 빗나가서 " + other.getName() + "의 공격이 인정됨");
        res.text = "서로의 공격이 빗나감 (우선권 " + holder.getName() + ")";
        res.newPriority = holder;
        return res;
    }

    private Res counterOnAdvance(Fencer c, Fencer adv) {
        Res r = new Res();
        if (roll(clamp(0.30 + tempo(c, adv) * 0.45, 0.2, 0.75))) {
            return landing(c, adv, c.getName() + "이(가) 전진하는 상대에게 스톱히트 적중");
        }
        r.text = c.getName() + "의 반격이 전진하는 상대에게 닿지 않음";
        return r;
    }

    private Res doubleTouch(Res r) {
        r.dbl = true;
        r.text = "동시타! 에페는 우선권이 없어서 양쪽 모두 득점";
        return r;
    }

    private Res landing(Fencer s, Fencer v, String how) {
        return landing(s, v, how, new Res());
    }

    /** 타격이 닿았을 때: 유효 타깃이 아니면 득점 없이 중단된다. (에페는 전신이 유효) */
    private Res landing(Fencer s, Fencer v, String how, Res r) {
        if (roll(offTargetChance(s))) {
            r.annul = true;
            r.off = true;
            r.scorer = null;
            r.text = s.getName() + "의 타격이 유효 타깃이 아니에요 (" + targetRuleText(s) + ") - 백색등, 무득점으로 중단";
            return r;
        }
        r.scorer = s;
        r.text = how + " → " + s.getName() + " 득점";
        return r;
    }

    // ---------- 확률 ----------

    /** 체력이 낮으면 모든 동작의 성공률이 떨어진다 (최대 -18%) */
    private double form(Fencer f) {
        double ratio = f.getMaxHp() == 0 ? 1 : f.getHp() / (double) f.getMaxHp();
        return ratio >= 0.5 ? 1.0 : 0.82 + 0.18 * (ratio / 0.5);
    }

    private double edgeOf(Fencer f) { return edge.getOrDefault(f, 0.0); }

    private boolean slippery(Fencer f) { return weather == Weather.RAINY && !f.hasTag(GearTag.RAIN_PROOF); }

    /** a 의 공격이 d 에게 닿을 확률 */
    private double hit(Fencer a, Fencer d) {
        double p = 0.60 + (a.getTechnique() - d.getDefense()) * 0.003 + (a.getAgility() - d.getAgility()) * 0.002
                + (a.getAttack() - d.getDefense()) * 0.001 + edgeOf(a);
        p += a.getWeapon() == WeaponType.SABRE ? 0.03 : a.getWeapon() == WeaponType.EPEE ? -0.02 : 0;
        return clamp(p * form(a), 0.30, 0.82);
    }

    /** d 가 a 의 공격을 막을 확률 (플뢰레는 방어가 좋고 사브르는 약하다) */
    private double parry(Fencer d, Fencer a) {
        double p = 0.48 + (d.getDefense() - a.getAttack()) * 0.003 + (d.getTechnique() - a.getTechnique()) * 0.002
                + (d.getAgility() - a.getAgility()) * 0.001;
        p += d.getWeapon() == WeaponType.FOIL ? 0.10 : d.getWeapon() == WeaponType.EPEE ? 0.05 : -0.05;
        return clamp(p * form(d), 0.25, 0.72);
    }

    /** 방어 직후 리포스트가 닿을 확률 (플뢰레가 특히 강하다) */
    private double riposte(Fencer d, Fencer a) {
        double p = 0.52 + (d.getTechnique() - a.getDefense()) * 0.003 + (d.getAgility() - a.getAgility()) * 0.0015;
        p += d.getWeapon() == WeaponType.FOIL ? 0.15 : d.getWeapon() == WeaponType.EPEE ? 0.05 : 0;
        return clamp(p * form(d), 0.30, 0.80);
    }

    /** c 의 동작이 a 보다 먼저 닿을 확률 (사브르는 빠르고 에페는 반격 타이밍이 좋다) */
    private double tempo(Fencer c, Fencer a) {
        double p = 0.50 + (c.getAgility() - a.getAgility()) * 0.004 + (c.getTechnique() - a.getTechnique()) * 0.002
                + edgeOf(c) - edgeOf(a);
        if (c.getWeapon() == WeaponType.SABRE && !slippery(c)) p += 0.06;   // 속공 (미끄러운 바닥에서는 약해짐)
        if (c.getWeapon() == WeaponType.EPEE) p += 0.05;
        return clamp(p * form(c), 0.30, 0.70);
    }

    /** 후퇴로 공격을 흘릴 확률 */
    private double evade(Fencer f, Fencer a) {
        double p = 0.40 + (f.getAgility() - a.getAgility()) * 0.005;
        if (f.getWeapon() == WeaponType.SABRE) p -= 0.05;
        if (slippery(f)) p *= 0.6;   // 미끄러운 바닥
        return clamp(p * form(f), 0.15, 0.60);
    }

    /** 페인트가 방어를 속일 확률 */
    private double feintChance(Fencer f, Fencer d) {
        double p = 0.58 + (f.getTechnique() - d.getDefense()) * 0.0035 + (f.getWeapon() == WeaponType.FOIL ? 0.06 : 0);
        return clamp(p * form(f), 0.35, 0.80);
    }

    /** 유효 타깃이 아닌 곳을 맞힐 확률. 기술이 높을수록 낮다. (에페는 전신 유효) */
    private double offTargetChance(Fencer a) {
        double base;
        switch (a.getWeapon()) {
            case FOIL:  base = clamp(0.22 - a.getTechnique() * 0.006, 0.04, 0.22); break;   // 몸통만 유효
            case SABRE: base = clamp(0.12 - a.getTechnique() * 0.002, 0.05, 0.12); break;   // 허리 위 유효
            default:    return 0;                                                            // 에페: 전신 유효
        }
        if (slippery(a)) base += a.getWeapon() == WeaponType.FOIL ? 0.04 : 0.03;   // 손이 미끄러워 빗나감
        if (weather == Weather.CLOUDY) base -= 0.02;                               // 차분해서 정확함
        return Math.max(0.02, base);
    }

    private static String targetRuleText(Fencer a) {
        return a.getWeapon() == WeaponType.FOIL ? "플뢰레는 몸통만 유효" : "사브르는 허리 위만 유효";
    }

    // ---------- 컴퓨터 선수의 전술 선택 ----------

    /** 종목과 성향에 따른 기본 선택 비중: 공격, 페인트, 방어, 반격, 전진, 후퇴 */
    private double[] baseWeights(Fencer f) {
        double[] w;
        switch (f.getWeapon()) {
            case FOIL:  w = new double[]{26, 16, 28, 10, 12, 8}; break;
            case EPEE:  w = new double[]{18, 8, 16, 30, 16, 12}; break;
            default:    w = new double[]{38, 10, 8, 10, 26, 8}; break;
        }
        switch (f.getPersonality()) {
            case PASSIONATE:  w[0] += 10; w[4] += 4; break;
            case CAREFUL:     w[2] += 8; w[3] += 4; break;
            case LAZY:        w[5] += 6; w[2] += 6; w[0] -= 8; break;
            default:          w[0] += 6; w[1] += 6; w[3] += 2; break;
        }
        return w;
    }

    /** 상대의 최근 전술을 읽고, 점수와 시간과 체력을 보며 전술을 고른다. */
    private Tactic ai(Fencer me, Fencer foe, List<Tactic> foeHistory, int myPoints, int foePoints) {
        double[] w = baseWeights(me);
        int diff = myPoints - foePoints;
        boolean timeLow = clock < 45;
        if (diff <= -3 || (timeLow && diff < 0)) {   // 지고 있으면 공격적으로
            w[0] += 12; w[1] += 4; w[2] -= 6; w[5] = 0;
        } else if ((diff >= 3 && timeLow) || (timeLow && diff > 0)) {   // 이기고 있으면 수비적으로
            w[2] += 10; w[5] += 8; w[0] -= 12;
        }
        if (me.getHp() / (double) me.getMaxHp() < 0.3) {   // 체력이 바닥나면 아낀다
            w[5] += 6; w[2] += 6; w[0] -= 8;
        }
        if (idlePhrases >= 2) {   // 소극적 플레이 규칙: 득점 없이 길어지면 공격을 늘린다
            double push = Math.min(30, 8 * (idlePhrases - 1));
            w[0] += push; w[1] += push / 2; w[2] -= push / 3; w[5] -= push / 4;
        }
        if (getRetreatStreak(me) >= 3) {   // 후방 라인 이탈을 피한다
            w[5] = 0; w[0] += 12; w[3] += 6;
        }
        if (foeHistory.size() >= 2 && roll(0.6)) {   // 상대가 같은 전술을 반복하면 읽고 대응
            Tactic last = foeHistory.get(foeHistory.size() - 1);
            if (last == foeHistory.get(foeHistory.size() - 2)) {
                switch (last) {
                    case ATTACK:  w[2] += 14; w[3] += 8; break;
                    case FEINT:   w[3] += 14; break;
                    case PARRY:   w[1] += 16; break;
                    case COUNTER: w[4] += 10; w[5] += 4; break;
                    case ADVANCE: w[3] += 12; w[0] += 6; break;
                    default:      w[4] += 12; w[0] += 8; break;
                }
            }
        }
        double total = 0;
        for (int i = 0; i < w.length; i++) {
            if (w[i] < 0) w[i] = 1;
            total += w[i];
        }
        double r = rnd.nextDouble() * total, acc = 0;
        for (int i = 0; i < w.length; i++) {
            acc += w[i];
            if (r < acc) return Tactic.values()[i];
        }
        return Tactic.ATTACK;
    }

    // ---------- 보조 ----------

    private static WeaponType weaponOf(List<Fencer> list) {
        if (list.isEmpty()) return null;
        WeaponType w = list.get(0).getWeapon();
        for (Fencer f : list) if (f.getWeapon() != w) return null;
        return w;
    }

    private void log(String s) {
        Event e = ev(Event.Kind.INFO, s);
        e.left = left;
        e.right = right;
    }

    private Event ev(Event.Kind kind, String text) {
        Event e = new Event();
        e.kind = kind;
        e.text = text == null ? "" : text;
        e.left = left;
        e.right = right;
        e.priority = priority;
        e.clock = clock;
        e.leftScore = leftScore;
        e.rightScore = rightScore;
        e.boutLeft = boutLeft;
        e.boutRight = boutRight;
        e.leftHp = left == null ? 0 : left.getHp();
        e.rightHp = right == null ? 0 : right.getHp();
        e.leftRetreat = left == null ? 0 : getRetreatStreak(left);
        e.rightRetreat = right == null ? 0 : getRetreatStreak(right);
        e.period = period;
        e.target = getTargetScore();
        e.sudden = suddenDeath;
        events.add(e);
        if (!e.text.isEmpty() && kind != Event.Kind.PHRASE) pending.add(e.text);
        return e;
    }

    // ---------- 정산 ----------

    /** 경기 종료 후 보상/부상/피로를 반영하고 결과 문구를 반환 (한 번만 실행됨) */
    public String settle() {
        if (settled) return "";
        settled = true;
        StringBuilder sb = new StringBuilder();
        if (opt.rewards) state.recordResult(won, region, boss, eventWeapon);
        if (opt.rated) {   // 파워 레이팅: 상대 평균 레이팅 기준으로 반영 (보스전은 더 크게)
            double sum = 0;
            for (Fencer e : enemies) sum += RatingSystem.baseRating(e);
            double enemyRating = enemies.isEmpty() ? RatingSystem.BASE : sum / enemies.size();
            for (Fencer f : players) f.applyRating(enemyRating, won, boss ? 24 : 16);
        }
        state.addRecord(new MatchRecord(state.getDay(), eventWeapon, region, boss, weather, leftScore, rightScore, won));
        int clinic = state.getClinicLevel();

        for (Fencer f : players) {
            f.setBusy(false);
            f.addFatigue(15);
            if (exhausted.contains(f)) {   // 체력이 바닥난 채로 뛴 선수는 다칠 수 있다
                f.heal(Math.max(1, f.getMaxHp() / 5));
                if (rnd.nextDouble() < 0.35 - 0.07 * clinic) {
                    f.injure();
                    sb.append(f.getName()).append("이(가) 무리해서 부상을 입었어요.\n");
                }
            }
        }

        if (!opt.rewards) {   // 대회 경기: 보상과 승리 수는 대회가 끝난 뒤 따로 정산한다.
            sb.insert(0, (won ? "승리! " : "패배... ") + "최종 점수 " + leftScore + " : " + rightScore + "\n");
        } else if (won) {
            int times = boss ? 2 : 1;
            double boost = state.consumeRewardBoost();
            int gold = (int) (region.getRewardGold() * times * boost);
            int exp = region.getRewardExp() * times;
            state.addGold(gold);
            sb.insert(0, region.getDisplayName() + (boss ? " 결승전" : "") + " 승리!\n최종 점수 " + leftScore + " : " + rightScore
                    + "\n보상: " + gold + "G" + (boost > 1 ? " (보너스 x" + boost + ")" : "") + "\n");
            for (Fencer f : players) {
                if (f.gainExp(exp)) {
                    sb.append(f.getName()).append("이(가) Lv.").append(f.getLevel()).append("(으)로 성장했어요!\n");
                }
            }
        } else {
            sb.insert(0, "패배했어요. 훈련을 더 하고 다시 도전해요!\n최종 점수 " + leftScore + " : " + rightScore + "\n");
        }
        for (String m : state.checkAchievements()) sb.append(m).append("\n");
        return sb.toString().trim();
    }
}
