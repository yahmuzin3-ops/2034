import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 게임의 모든 진행 상황. 저장/불러오기는 이 객체 하나만 다루면 된다.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 2L;
    public static final int MAX_ROSTER = 8;
    public static final int MAX_FACILITY_LEVEL = 5;
    private static final Random RANDOM = new Random();

    private int gold = 1000;
    private int day = 1;
    private Weather weather = Weather.SUNNY;

    private final List<Fencer> roster = new CopyOnWriteArrayList<>();
    private final Inventory inventory = new Inventory();

    private int wins = 0;
    private int matches = 0;
    private int trainingHallLevel = 0;
    private int clinicLevel = 0;
    private double rewardBoost = 1.0;
    private final EnumSet<Achievement> achievements = EnumSet.noneOf(Achievement.class);
    private final EnumSet<Region> cleared = EnumSet.noneOf(Region.class);

    /** 새 게임: 선수 1명 + 기본 아이템 */
    public static GameState newGame() {
        GameState s = new GameState();
        s.addFencer(new Fencer("아델", WeaponType.SABRE, Personality.PASSIONATE));
        s.getInventory().add(ItemCatalog.ALL.get(0), 3); // 에너지바
        s.getInventory().add(ItemCatalog.ALL.get(3), 1); // 파스
        return s;
    }

    // ---------- 재화 ----------

    public synchronized int getGold() { return gold; }

    public synchronized void addGold(int amount) { gold += amount; }

    public synchronized boolean spendGold(int amount) {
        if (amount < 0 || gold < amount) return false;
        gold -= amount;
        return true;
    }

    // ---------- 시간 / 날씨 ----------

    public synchronized int getDay() { return day; }

    public synchronized Weather getWeather() { return weather; }

    /** 하루 경과: 일차 +1, 날씨 랜덤 변경, 모든 선수 tick */
    public synchronized void nextDay() {
        day++;
        weather = Weather.random();
        for (Fencer f : roster) {
            f.tick();
        }
    }

    // ---------- 선수 ----------

    public void addFencer(Fencer fencer) { roster.add(fencer); }

    public List<Fencer> getRoster() { return roster; }

    public synchronized int recruitCost() { return 400 + 300 * roster.size(); }

    /** 선수 영입. 재화 부족/정원 초과면 null */
    public synchronized Fencer recruit(String name, WeaponType weapon) {
        if (roster.size() >= MAX_ROSTER) return null;
        if (!spendGold(recruitCost())) return null;
        Personality p = Personality.values()[RANDOM.nextInt(Personality.values().length)];
        Fencer f = new Fencer(name, weapon, p);
        roster.add(f);
        return f;
    }

    // ---------- 아이템 ----------

    public Inventory getInventory() { return inventory; }

    public String useItem(Fencer target, Item item) {
        if (!inventory.remove(item, 1)) {
            return item.getName() + "이(가) 없어요.";
        }
        return item.use(target);
    }

    // ---------- 훈련 ----------

    /** 날씨와 훈련장 레벨이 반영된 외부 배율 */
    public synchronized double getTrainingMultiplier() {
        return weather.getTrainingModifier() * (1 + 0.05 * trainingHallLevel);
    }

    public String train(Fencer f, TrainingType t) {
        if (f.isBusy()) return f.getName() + "은(는) 지금 시합 중이에요.";
        if (f.getFatigue() >= 95) return f.getName() + "은(는) 너무 지쳐서 훈련할 수 없어요. 휴식이 필요해요.";

        int h0 = f.getMaxHp(), a0 = f.getAttack(), d0 = f.getDefense(), g0 = f.getAgility(), t0 = f.getTechnique();
        double eff = f.applyTraining(t.getHp(), t.getAtk(), t.getDef(), t.getAgi(), t.getTec(),
                t.getFatigueCost(), getTrainingMultiplier());

        StringBuilder sb = new StringBuilder();
        sb.append(f.getName()).append(" - ").append(t.getDisplayName())
                .append(String.format(" 완료 (효율 %d%%)", Math.round(eff * 100)));
        String gain = gainText(f.getMaxHp() - h0, f.getAttack() - a0, f.getDefense() - d0,
                f.getAgility() - g0, f.getTechnique() - t0);
        sb.append(gain.isEmpty() ? " | 효율이 낮아 능력치는 오르지 않았어요" : " | " + gain);
        return sb.toString();
    }

    private static String gainText(int hp, int atk, int def, int agi, int tec) {
        StringBuilder sb = new StringBuilder();
        if (hp > 0) sb.append("최대HP +").append(hp).append(" ");
        if (atk > 0) sb.append("공격 +").append(atk).append(" ");
        if (def > 0) sb.append("방어 +").append(def).append(" ");
        if (agi > 0) sb.append("민첩 +").append(agi).append(" ");
        if (tec > 0) sb.append("기술 +").append(tec).append(" ");
        return sb.toString().trim();
    }

    // ---------- 시설 ----------

    public synchronized int getTrainingHallLevel() { return trainingHallLevel; }

    public synchronized int getClinicLevel() { return clinicLevel; }

    /** 의무실 레벨에 따른 휴식 보너스 */
    public synchronized int getRestBonus() { return clinicLevel * 3; }

    public synchronized int upgradeCost(int level) { return 300 * (level + 1); }

    public synchronized String upgradeTrainingHall() {
        if (trainingHallLevel >= MAX_FACILITY_LEVEL) return "이미 최대 레벨이에요.";
        if (!spendGold(upgradeCost(trainingHallLevel))) return "재화가 모자라요.";
        trainingHallLevel++;
        return "훈련장이 Lv." + trainingHallLevel + "이 되었어요!";
    }

    public synchronized String upgradeClinic() {
        if (clinicLevel >= MAX_FACILITY_LEVEL) return "이미 최대 레벨이에요.";
        if (!spendGold(upgradeCost(clinicLevel))) return "재화가 모자라요.";
        clinicLevel++;
        return "의무실이 Lv." + clinicLevel + "이 되었어요!";
    }

    // ---------- 경기 기록 / 목표 ----------

    public synchronized int getWins() { return wins; }

    public synchronized int getMatches() { return matches; }

    public synchronized void recordResult(boolean won, Region region) {
        matches++;
        if (won) {
            wins++;
            cleared.add(region);
        }
    }

    public synchronized boolean isCleared(Region r) { return cleared.contains(r); }

    public synchronized double consumeRewardBoost() {
        double b = rewardBoost;
        rewardBoost = 1.0;
        return b;
    }

    public synchronized void setRewardBoost(double boost) { rewardBoost = boost; }

    public synchronized boolean hasAchievement(Achievement a) { return achievements.contains(a); }

    /** 새로 달성한 목표를 확인하고 보상을 지급한다. 달성 메시지 목록 반환 */
    public synchronized List<String> checkAchievements() {
        List<String> out = new ArrayList<>();
        boolean pro = false, master = false;
        for (Fencer f : roster) {
            if (f.getGrade().ordinal() >= Grade.PRO.ordinal()) pro = true;
            if (f.getGrade() == Grade.MASTER) master = true;
        }
        check(Achievement.FIRST_WIN, wins >= 1, out);
        check(Achievement.WINS_10, wins >= 10, out);
        check(Achievement.COLLECTOR, roster.size() >= 3, out);
        check(Achievement.PRO_GRADE, pro, out);
        check(Achievement.MASTER_GRADE, master, out);
        check(Achievement.RICH, gold >= 5000, out);
        check(Achievement.WORLD_CHAMPION, cleared.contains(Region.WORLD), out);
        return out;
    }

    private void check(Achievement a, boolean cond, List<String> out) {
        if (cond && achievements.add(a)) {
            gold += a.getReward();
            out.add("목표 달성: " + a.getDisplayName() + " (+" + a.getReward() + "G)");
        }
    }

    // 저장 중에 값이 바뀌지 않도록 잠금
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
