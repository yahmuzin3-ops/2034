import java.util.*;

/**
 * 턴제 팀 경기(단체전).
 * 라운드마다 민첩이 높은 순서대로 모든 선수(아군+적)가 한 명씩 번갈아 행동한다.
 * 아군 차례가 오면 입력을 기다리고, 적 차례는 자동으로 진행된다.
 *
 * 전투 시스템
 *  - 데미지: 능력치, 종목 상성, 등급, 컨디션, 크리티컬, 회피, 방어 자세
 *  - 리포스트: 방어 자세로 막아낸 뒤 확률적으로 반격
 *  - 기세 게이지: 가득 차면 쿨타임 무시 + 특수 기술 위력 상승
 *  - 연계 공격: 같은 라운드에 같은 대상을 다른 아군이 공격하면 위력 상승
 *  - 상태이상: 기절(다음 행동 불가), 둔화(공격력/회피 감소)
 *  - 결승전(보스전): 강한 챔피언 1명이 라운드마다 2번 행동, 보상 2배
 */
public class Battle {
    public enum Action { ATTACK, DEFEND, SKILL, ITEM }

    private static final int SKILL_COOLDOWN = 3;
    private static final double COMBO_BONUS = 1.25;
    private static final int MAX_ROUNDS = 60;

    private final GameState state;
    private final Region region;
    private final boolean boss;
    private final List<Fencer> players;
    private final List<Fencer> enemies;
    private final Random rnd = new Random();

    private final Map<Fencer, Integer> cooldown = new HashMap<>();
    private final Map<Fencer, Integer> momentum = new HashMap<>();   // 기세 게이지 (0~100)
    private final Map<Fencer, Integer> slowed = new HashMap<>();     // 둔화 남은 라운드
    private final Set<Fencer> stunned = new HashSet<>();             // 기절 (다음 행동 1회 불가)
    private final Set<Fencer> guarding = new HashSet<>();            // 자기 다음 턴 시작까지 유지
    private final List<String> pending = new ArrayList<>();

    // 이번 라운드의 행동 순서와 현재 위치
    private final List<Fencer> order = new ArrayList<>();
    private int turnIndex = 0;

    // 연계 공격 추적 (0: 아군, 1: 적)
    private final Fencer[] comboAtk = new Fencer[2];
    private final Fencer[] comboTgt = new Fencer[2];

    private int round = 1;
    private boolean over = false;
    private boolean won = false;
    private boolean settled = false;

    public Battle(GameState state, Region region, List<Fencer> players) {
        this(state, region, players, false);
    }

    public Battle(GameState state, Region region, List<Fencer> players, boolean boss) {
        this.state = state;
        this.region = region;
        this.boss = boss;
        this.players = new ArrayList<>(players);
        this.enemies = boss ? region.createBoss() : region.createEnemies(players.size());
        for (Fencer f : this.players) f.setBusy(true);

        log(boss ? "=== " + region.getDisplayName() + " 결승전 (보스전) ===" : "=== " + region.getDisplayName() + " 시작! ===");
        StringBuilder sb = new StringBuilder(boss ? "결승 상대: " : "상대 팀: ");
        for (Fencer e : enemies) {
            sb.append(e.getName()).append("(").append(e.getWeapon().getDisplayName())
                    .append(" Lv.").append(e.getLevel()).append(") ");
        }
        log(sb.toString().trim());
        if (boss) log("챔피언은 HP가 높고 라운드마다 2번 행동해요. 기절 효과는 통하지 않아요!");
        log("행동 순서는 민첩이 높은 선수부터예요.");
        log("--- 라운드 1 ---");
        buildOrder();
        advance();
    }

    // ---------- 상태 조회 ----------

    public List<Fencer> getPlayers() { return players; }
    public List<Fencer> getEnemies() { return enemies; }
    public int getRound() { return round; }
    public boolean isOver() { return over; }
    public boolean isWon() { return won; }
    public boolean isBoss() { return boss; }
    public boolean isStunned(Fencer f) { return stunned.contains(f); }
    public boolean isSlowed(Fencer f) { return slowed.getOrDefault(f, 0) > 0; }

    /** 지금 행동할 아군 선수 (적 차례이거나 경기가 끝났으면 null) */
    public Fencer currentActor() {
        if (over || turnIndex >= order.size()) return null;
        Fencer f = order.get(turnIndex);
        return players.contains(f) ? f : null;
    }

    /** 이번 라운드에 남은 행동 순서 (현재 차례 포함, 쓰러진 선수 제외) */
    public List<Fencer> getTurnOrder() {
        List<Fencer> r = new ArrayList<>();
        for (int i = turnIndex; i < order.size(); i++) {
            if (!order.get(i).isDown()) r.add(order.get(i));
        }
        return r;
    }

    /** 현재 행동하는 선수가 연계 공격을 이어갈 수 있는 대상 (없으면 null) */
    public Fencer getComboTarget() {
        Fencer t = comboTgt[0];
        if (t == null || t.isDown() || comboAtk[0] == currentActor()) return null;
        return t;
    }

    /** 쿨타임이 끝났거나, 기세가 가득 차 있으면 특수 기술 사용 가능 */
    public boolean skillReady(Fencer f) { return cooldown.getOrDefault(f, 0) == 0 || getMomentum(f) >= 100; }

    public int getCooldown(Fencer f) { return cooldown.getOrDefault(f, 0); }

    public int getMomentum(Fencer f) { return momentum.getOrDefault(f, 0); }

    private void addMomentum(Fencer f, int amount) {
        momentum.put(f, Math.min(100, getMomentum(f) + amount));
    }

    public List<String> drainLog() {
        List<String> copy = new ArrayList<>(pending);
        pending.clear();
        return copy;
    }

    // ---------- 턴 진행 ----------

    /** 라운드 시작: 살아있는 모든 선수를 민첩 순으로 정렬 (동률이면 아군 먼저) */
    private void buildOrder() {
        List<Fencer> all = new ArrayList<>(alive(players));
        all.addAll(alive(enemies));
        all.sort((x, y) -> Integer.compare(y.getAgility(), x.getAgility()));
        order.clear();
        order.addAll(all);
        if (boss) {
            for (Fencer e : enemies) if (!e.isDown()) order.add(e);   // 보스는 라운드 끝에 한 번 더 행동
        }
        turnIndex = 0;
        Arrays.fill(comboAtk, null);
        Arrays.fill(comboTgt, null);
    }

    /** 다음 아군 차례까지 적 행동/기절 처리를 자동으로 진행한다. */
    private void advance() {
        while (!over) {
            if (turnIndex >= order.size()) {
                endRound();
                continue;
            }
            Fencer f = order.get(turnIndex);
            if (f.isDown()) {
                turnIndex++;
                continue;
            }
            guarding.remove(f);   // 방어 자세는 자기 다음 턴 시작까지만 유지
            if (stunned.remove(f)) {
                log(f.getName() + "은(는) 기절해서 행동하지 못했다!");
                turnIndex++;
                continue;
            }
            if (players.contains(f)) return;   // 아군 차례: 입력 대기
            enemyAct(f);
            checkEnd();
            turnIndex++;
        }
    }

    private void endRound() {
        cooldown.replaceAll((k, v) -> Math.max(0, v - 1));
        slowed.replaceAll((k, v) -> v - 1);
        slowed.values().removeIf(v -> v <= 0);
        round++;
        if (round > MAX_ROUNDS) {
            over = true;
            won = false;
            log("=== 승부를 가리지 못했다... (패배 처리) ===");
            return;
        }
        log("--- 라운드 " + round + " ---");
        buildOrder();
    }

    // ---------- 플레이어 행동 ----------

    /** 행동 실행. 턴을 소모했으면 true (대상이 없거나 쿨타임이면 false) */
    public boolean act(Action action, Fencer target, Item item) {
        Fencer actor = currentActor();
        if (actor == null) return false;

        switch (action) {
            case ATTACK:
                if (target == null || target.isDown()) return false;
                strike(actor, target, 1.0, false, "찌르기", true);
                break;
            case DEFEND:
                guarding.add(actor);
                log(actor.getName() + "이(가) 파라드 자세를 취했다! (다음 내 차례까지 피해 절반, 반격 기회)");
                break;
            case SKILL:
                if (target == null || target.isDown()) return false;
                if (!skillReady(actor)) {
                    log("특수 기술은 아직 쓸 수 없어요. (쿨타임 " + getCooldown(actor) + ")");
                    return false;
                }
                doSkill(actor, target);
                break;
            case ITEM:
                if (item == null || target == null) return false;
                log(state.useItem(target, item));
                break;
        }
        checkEnd();
        if (!over) {
            turnIndex++;
            advance();
        }
        return true;
    }

    public void surrender() {
        if (over) return;
        over = true;
        won = false;
        log("기권했다...");
    }

    // ---------- 내부 로직 ----------

    private void enemyAct(Fencer e) {
        List<Fencer> alive = alive(players);
        if (alive.isEmpty()) return;

        Fencer target = alive.get(rnd.nextInt(alive.size()));
        if (rnd.nextDouble() < 0.4) {
            for (Fencer p : alive) if (p.getHp() < target.getHp()) target = p;
        }
        double r = rnd.nextDouble();
        if (e.getHp() < e.getMaxHp() * 0.3 && r < 0.3) {
            guarding.add(e);
            log(e.getName() + "이(가) 방어 자세를 취했다!");
        } else if (skillReady(e) && r < 0.4) {
            doSkill(e, target);
        } else {
            strike(e, target, 1.0, false, "찌르기", true);
        }
    }

    private void doSkill(Fencer a, Fencer t) {
        boolean burst = getMomentum(a) >= 100;
        cooldown.put(a, SKILL_COOLDOWN);
        double boost = 1.0;
        if (burst) {
            momentum.put(a, 0);
            boost = 1.3;
            log(a.getName() + "의 기세가 폭발한다! (특수 기술 위력 x1.3)");
        }
        String skill = a.getWeapon().getSpecialSkill();
        switch (a.getWeapon()) {
            case FOIL:   // 반드시 명중
                strike(a, t, 1.5 * boost, true, skill, true);
                break;
            case EPEE:   // 공격 후 방어 자세 + 상대 둔화
                strike(a, t, 1.4 * boost, false, skill, true);
                guarding.add(a);
                if (!t.isDown()) {
                    slowed.put(t, 2);
                    log(t.getName() + "이(가) 둔화됐다! (공격력 -20%, 회피 -10%p)");
                }
                break;
            case SABRE:  // 강력하지만 반동 피해
                strike(a, t, 1.9 * boost, true, skill, true);
                a.takeDamage(5);
                log(a.getName() + "은(는) 무리한 돌격의 반동으로 5 피해를 입었다.");
                break;
        }
    }

    /**
     * 공격 1회 처리.
     * @param main true 이면 일반 공격(연계/리포스트 대상), false 이면 반격이라서 연계도 재반격도 없다.
     */
    private void strike(Fencer a, Fencer d, double mult, boolean sureHit, String label, boolean main) {
        double dodge = 0.05 + (d.getAgility() - a.getAgility()) * 0.01;
        if (isSlowed(d)) dodge -= 0.10;
        dodge = clamp(dodge, 0.02, 0.35);
        if (!sureHit && rnd.nextDouble() < dodge) {
            log(d.getName() + "이(가) " + a.getName() + "의 " + label + "을(를) 피했다!");
            addMomentum(d, 15);
            return;
        }

        int side = players.contains(a) ? 0 : 1;
        double base = Math.max(3, a.getAttack() * 1.5 + a.getTechnique() * 0.5 - d.getDefense() * 0.6);
        double matchup = a.getWeapon().matchupAgainst(d.getWeapon());
        double m = matchup * a.getGrade().getBonus() * mult * (0.9 + rnd.nextDouble() * 0.2);
        if (a.getFatigue() >= 70) m *= 0.85;
        if (a.isInjured()) m *= 0.8;
        if (isSlowed(a)) m *= 0.8;
        boolean crit = rnd.nextDouble() < 0.05 + a.getTechnique() * 0.004;
        if (crit) m *= 1.5;
        boolean combo = main && comboTgt[side] == d && comboAtk[side] != null && comboAtk[side] != a;
        if (combo) m *= COMBO_BONUS;
        boolean guarded = guarding.contains(d);
        if (guarded) m *= 0.5;

        int dmg = Math.max(1, (int) Math.round(base * m));
        d.takeDamage(dmg);
        String note = (crit ? " (크리티컬!)" : "") + (combo ? " (연계 공격!)" : "") + (guarded ? " (방어)" : "")
                + (matchup > 1 ? " [상성 우위]" : matchup < 1 ? " [상성 불리]" : "");
        log(a.getName() + "의 " + label + "! " + d.getName() + "에게 " + dmg + " 피해" + note);
        if (d.isDown()) log(d.getName() + "이(가) 쓰러졌다!");

        // 기세 게이지: 공격 성공 +20(크리티컬 +30), 피격 +10
        addMomentum(a, crit ? 30 : 20);
        addMomentum(d, 10);

        if (main) {
            comboAtk[side] = a;
            comboTgt[side] = d;
        }

        // 크리티컬은 30% 확률로 기절 (보스에게는 통하지 않음)
        if (crit && !d.isDown() && !(boss && enemies.contains(d)) && rnd.nextDouble() < 0.3 && stunned.add(d)) {
            log(d.getName() + "이(가) 기절했다! (다음 행동 불가)");
        }

        // 리포스트: 방어 자세로 막아낸 뒤 기술 수치에 따라 반격
        if (main && guarded && !d.isDown() && !a.isDown()
                && rnd.nextDouble() < 0.35 + d.getTechnique() * 0.005) {
            log(d.getName() + "의 리포스트(반격)!");
            strike(d, a, 0.6, false, "리포스트", false);
        }
    }

    private void checkEnd() {
        if (alive(enemies).isEmpty()) {
            over = true;
            won = true;
            log("=== 승리! ===");
        } else if (alive(players).isEmpty()) {
            over = true;
            won = false;
            log("=== 패배... ===");
        }
    }

    private static List<Fencer> alive(List<Fencer> list) {
        List<Fencer> r = new ArrayList<>();
        for (Fencer f : list) if (!f.isDown()) r.add(f);
        return r;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void log(String s) { pending.add(s); }

    // ---------- 정산 ----------

    /** 경기 종료 후 보상/부상/피로를 반영하고 결과 문구를 반환 (한 번만 실행됨) */
    public String settle() {
        if (settled) return "";
        settled = true;
        StringBuilder sb = new StringBuilder();
        state.recordResult(won, region, boss);
        int clinic = state.getClinicLevel();

        for (Fencer f : players) {
            f.setBusy(false);
            f.addFatigue(15);
            if (f.isDown()) {
                f.heal(Math.max(1, f.getMaxHp() / 5));
                if (rnd.nextDouble() < 0.5 - 0.08 * clinic) {
                    f.injure();
                    sb.append(f.getName()).append("이(가) 부상을 입었어요.\n");
                }
            }
        }

        if (won) {
            int times = boss ? 2 : 1;
            double boost = state.consumeRewardBoost();
            int gold = (int) (region.getRewardGold() * times * boost);
            int exp = region.getRewardExp() * times;
            state.addGold(gold);
            sb.insert(0, region.getDisplayName() + (boss ? " 결승전" : "") + " 승리!\n보상: " + gold + "G"
                    + (boost > 1 ? " (보너스 x" + boost + ")" : "") + "\n");
            for (Fencer f : players) {
                if (f.gainExp(exp)) {
                    sb.append(f.getName()).append("이(가) Lv.").append(f.getLevel()).append("(으)로 성장했어요!\n");
                }
            }
        } else {
            sb.insert(0, "패배했어요. 훈련을 더 하고 다시 도전해요!\n");
        }
        for (String m : state.checkAchievements()) sb.append(m).append("\n");
        return sb.toString().trim();
    }
}
