import java.util.*;

/**
 * 턴제 팀 경기(단체전).
 * 플레이어 팀이 한 명씩 행동을 고르고, 모두 행동하면 적 팀이 행동한다. (이게 1라운드)
 * 데미지는 능력치, 종목 상성, 등급, 컨디션, 크리티컬, 회피, 방어 자세에 영향을 받는다.
 */
public class Battle {
    public enum Action { ATTACK, DEFEND, SKILL, ITEM }

    private static final int SKILL_COOLDOWN = 3;

    private final GameState state;
    private final Region region;
    private final List<Fencer> players;
    private final List<Fencer> enemies;
    private final Random rnd = new Random();

    private final Map<Fencer, Integer> cooldown = new HashMap<>();
    private final Set<Fencer> guarding = new HashSet<>();
    private final Set<Fencer> acted = new HashSet<>();
    private final List<String> pending = new ArrayList<>();

    private int round = 1;
    private boolean over = false;
    private boolean won = false;
    private boolean settled = false;

    public Battle(GameState state, Region region, List<Fencer> players) {
        this.state = state;
        this.region = region;
        this.players = new ArrayList<>(players);
        this.enemies = region.createEnemies(players.size());
        for (Fencer f : this.players) f.setBusy(true);
        log("=== " + region.getDisplayName() + " 시작! ===");
        StringBuilder sb = new StringBuilder("상대 팀: ");
        for (Fencer e : enemies) {
            sb.append(e.getName()).append("(").append(e.getWeapon().getDisplayName())
                    .append(" Lv.").append(e.getLevel()).append(") ");
        }
        log(sb.toString().trim());
        log("--- 라운드 1 ---");
    }

    // ---------- 상태 조회 ----------

    public List<Fencer> getPlayers() { return players; }
    public List<Fencer> getEnemies() { return enemies; }
    public int getRound() { return round; }
    public boolean isOver() { return over; }
    public boolean isWon() { return won; }

    /** 지금 행동할 플레이어 선수 (없으면 null) */
    public Fencer currentActor() {
        if (over) return null;
        for (Fencer p : players) {
            if (!p.isDown() && !acted.contains(p)) return p;
        }
        return null;
    }

    public boolean skillReady(Fencer f) { return cooldown.getOrDefault(f, 0) == 0; }

    public int getCooldown(Fencer f) { return cooldown.getOrDefault(f, 0); }

    public List<String> drainLog() {
        List<String> copy = new ArrayList<>(pending);
        pending.clear();
        return copy;
    }

    // ---------- 플레이어 행동 ----------

    /** 행동 실행. 턴을 소모했으면 true (대상이 없거나 쿨타임이면 false) */
    public boolean act(Action action, Fencer target, Item item) {
        Fencer actor = currentActor();
        if (actor == null) return false;

        switch (action) {
            case ATTACK:
                if (target == null || target.isDown()) return false;
                strike(actor, target, 1.0, false, "찌르기");
                break;
            case DEFEND:
                guarding.add(actor);
                log(actor.getName() + "이(가) 파라드 자세를 취했다! (받는 피해 절반)");
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
        acted.add(actor);
        checkEnd();
        if (!over && currentActor() == null) enemyPhase();
        return true;
    }

    public void surrender() {
        if (over) return;
        over = true;
        won = false;
        log("기권했다...");
    }

    // ---------- 내부 로직 ----------

    private void doSkill(Fencer a, Fencer t) {
        cooldown.put(a, SKILL_COOLDOWN);
        String skill = a.getWeapon().getSpecialSkill();
        switch (a.getWeapon()) {
            case FOIL:   // 반드시 명중
                strike(a, t, 1.5, true, skill);
                break;
            case EPEE:   // 공격 후 방어 자세
                strike(a, t, 1.4, false, skill);
                guarding.add(a);
                break;
            case SABRE:  // 강력하지만 반동 피해
                strike(a, t, 1.9, true, skill);
                a.takeDamage(5);
                log(a.getName() + "은(는) 무리한 돌격의 반동으로 5 피해를 입었다.");
                break;
        }
    }

    private void strike(Fencer a, Fencer d, double mult, boolean sureHit, String label) {
        double dodge = clamp(0.05 + (d.getAgility() - a.getAgility()) * 0.01, 0.02, 0.35);
        if (!sureHit && rnd.nextDouble() < dodge) {
            log(d.getName() + "이(가) " + a.getName() + "의 " + label + "을(를) 피했다!");
            return;
        }
        double base = Math.max(3, a.getAttack() * 1.5 + a.getTechnique() * 0.5 - d.getDefense() * 0.6);
        double matchup = a.getWeapon().matchupAgainst(d.getWeapon());
        double m = matchup * a.getGrade().getBonus() * mult * (0.9 + rnd.nextDouble() * 0.2);
        if (a.getFatigue() >= 70) m *= 0.85;
        if (a.isInjured()) m *= 0.8;
        boolean crit = rnd.nextDouble() < 0.05 + a.getTechnique() * 0.004;
        if (crit) m *= 1.5;
        if (guarding.contains(d)) m *= 0.5;

        int dmg = Math.max(1, (int) Math.round(base * m));
        d.takeDamage(dmg);
        String note = (crit ? " (크리티컬!)" : "") + (matchup > 1 ? " [상성 우위]" : matchup < 1 ? " [상성 불리]" : "");
        log(a.getName() + "의 " + label + "! " + d.getName() + "에게 " + dmg + " 피해" + note);
        if (d.isDown()) log(d.getName() + "이(가) 쓰러졌다!");
    }

    private void enemyPhase() {
        guarding.removeAll(enemies);
        for (Fencer e : enemies) {
            if (over) break;
            if (e.isDown()) continue;
            List<Fencer> alive = alive(players);
            if (alive.isEmpty()) break;

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
                strike(e, target, 1.0, false, "찌르기");
            }
            checkEnd();
        }
        guarding.removeAll(players);
        cooldown.replaceAll((k, v) -> Math.max(0, v - 1));
        acted.clear();
        round++;
        if (!over) log("--- 라운드 " + round + " ---");
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
        state.recordResult(won, region);
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
            double boost = state.consumeRewardBoost();
            int gold = (int) (region.getRewardGold() * boost);
            state.addGold(gold);
            sb.insert(0, region.getDisplayName() + " 승리!\n보상: " + gold + "G"
                    + (boost > 1 ? " (보너스 x" + boost + ")" : "") + "\n");
            for (Fencer f : players) {
                if (f.gainExp(region.getRewardExp())) {
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
