import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.function.ToIntFunction;

/**
 * 펜서(선수) 모델.
 * - 능력치: HP, 공격, 방어, 민첩, 기술 (성장에 따라 증가)
 * - 상태  : 배고픔, 피로도, 기분, 부상 (0~100, 시간에 따라 변화)
 * - 멀티스레딩: 값을 바꾸는 모든 메서드는 synchronized
 */
public class Fencer implements Serializable {
    private static final long serialVersionUID = 2L;

    public static final class TrainingSession {
        private final TrainingType type;
        private final double efficiency;

        private TrainingSession(TrainingType type, double efficiency) {
            this.type = type;
            this.efficiency = efficiency;
        }

        public TrainingType getType() { return type; }
        public double getEfficiency() { return efficiency; }
    }

    private final String name;
    private final WeaponType weapon;
    private final Personality personality;

    private int maxHp;
    private int hp;
    private int attack;
    private int defense;
    private int agility;
    private int technique;

    private int level = 1;
    private int exp = 0;
    private EnumMap<TrainingType, Integer> trainingLevels = createTrainingLevels();
    private boolean personalityBonusesApplied;

    // hunger: 높을수록 배고픔 / fatigue: 높을수록 지침 / mood: 높을수록 좋음
    private int hunger = 30;
    private int fatigue = 0;
    private int mood = 70;
    private boolean injured = false;
    private boolean resting = false;
    private TrainingType activeTraining;
    private long trainingEndsAtMillis;
    private double activeTrainingEfficiency = 1.0;

    // 장착한 장비 (부위마다 1개). 예전 저장 파일에는 없을 수 있다.
    private EnumMap<EquipSlot, Equipment> gear;

    // 시합 중에는 자율 행동을 하지 않는다 (저장하지 않음)
    private transient volatile boolean busy = false;

    // ---- 자율 행동 ----
    /** 자율 행동으로 고를 수 있는 행동 */
    public enum AutoAction { NONE, REST, MEAL, REFRESH, SHADOW }

    private boolean autoDisabled = false;               // true 면 자율 행동을 끈다 (기본: 켜짐)
    private boolean autoRest = false;                   // 스스로 시작한 휴식인지 (충분히 쉬면 스스로 멈춘다)
    private transient volatile long nextAutoAt = 0;     // 다음 자율 행동 판단 시각
    private transient volatile String lastAuto = "";    // 가장 최근 자율 행동 (화면 표시용)

    public Fencer(String name, WeaponType weapon, Personality personality) {
        this.name = name;
        this.weapon = weapon;
        this.personality = personality;
        this.maxHp = weapon.getBaseHp();
        this.hp = maxHp;
        this.attack = weapon.getBaseAttack();
        this.defense = weapon.getBaseDefense();
        this.agility = weapon.getBaseAgility();
        this.technique = weapon.getBaseTechnique();
        applyPersonalityBonuses();
        hp = maxHp;
        personalityBonusesApplied = true;
    }

    private void applyPersonalityBonuses() {
        maxHp += personality.getHpBonus();
        attack += personality.getAttackBonus();
        defense += personality.getDefenseBonus();
        agility += personality.getAgilityBonus();
        technique += personality.getTechniqueBonus();
    }

    // ---------- 성장 ----------

    public synchronized int expToNext() {
        return level * 50;
    }

    public synchronized int getTrainingLevel(TrainingType type) {
        if (type == null) return 0;
        if (trainingLevels == null) trainingLevels = createTrainingLevels();
        return trainingLevels.getOrDefault(type, 1);
    }

    public synchronized int increaseTrainingLevel(TrainingType type) {
        if (type == null) return 0;
        int next = Math.min(TrainingType.MAX_LEVEL, getTrainingLevel(type) + 1);
        trainingLevels.put(type, next);
        return next;
    }

    private static EnumMap<TrainingType, Integer> createTrainingLevels() {
        EnumMap<TrainingType, Integer> levels = new EnumMap<>(TrainingType.class);
        for (TrainingType type : TrainingType.values()) levels.put(type, 1);
        return levels;
    }

    /** 경험치 획득. 레벨업이 일어나면 true */
    public synchronized boolean gainExp(int amount) {
        exp += amount;
        boolean leveled = false;
        while (exp >= expToNext()) {
            exp -= expToNext();
            levelUp();
            leveled = true;
        }
        return leveled;
    }

    private void levelUp() {
        level++;
        maxHp += weapon.getGrowHp();
        attack += weapon.getGrowAttack();
        defense += weapon.getGrowDefense();
        agility += weapon.getGrowAgility();
        technique += weapon.getGrowTechnique();
        hp = Math.min(maxHp, hp + weapon.getGrowHp());
    }

    // ---------- 훈련 ----------

    /** 훈련 효율. 배고픔 / 부상 / 기분이 영향을 준다. */
    public synchronized double getTrainingEfficiency() {
        double eff = 1.0;
        if (hunger >= 80) eff *= 0.8;
        if (injured) eff *= 0.3;
        if (mood >= 70) eff *= 1.1;
        else if (mood < 30) eff *= 0.9;
        return eff;
    }

    /**
     * 훈련 적용. extra 는 날씨/시설 같은 외부 배율.
     * @return 실제로 적용된 효율(상태 효율 x extra)
     */
    public synchronized double applyTraining(int hpGain, int atk, int def, int agi, int tec,
                                             int fatigueCost, double extra) {
        double eff = getTrainingEfficiency() * extra;
        return applyTrainingAtEfficiency(hpGain, atk, def, agi, tec, fatigueCost, eff);
    }

    public synchronized double applyTrainingAtEfficiency(int hpGain, int atk, int def, int agi, int tec,
                                                         int fatigueCost, double eff) {
        int h = (int) Math.round(hpGain * eff);
        maxHp += h;
        hp = Math.min(maxHp, hp + h);
        attack += Math.round(atk * eff);
        defense += Math.round(def * eff);
        agility += Math.round(agi * eff);
        technique += Math.round(tec * eff);
        fatigue = clamp(fatigue + fatigueCost);
        hunger = clamp(hunger + 10);
        gainExp(12);
        return eff;
    }

    public synchronized boolean startTraining(TrainingType type, long durationMillis, double efficiency) {
        if (type == null || durationMillis <= 0 || busy || resting || activeTraining != null) return false;
        activeTraining = type;
        trainingEndsAtMillis = System.currentTimeMillis() + durationMillis;
        activeTrainingEfficiency = efficiency;
        return true;
    }

    public synchronized TrainingSession finishTrainingIfDue(long nowMillis) {
        if (activeTraining == null || busy || nowMillis < trainingEndsAtMillis) return null;
        return clearActiveTraining();
    }

    public synchronized TrainingSession finishTrainingImmediately() {
        if (activeTraining == null || busy) return null;
        return clearActiveTraining();
    }

    private TrainingSession clearActiveTraining() {
        TrainingSession session = new TrainingSession(activeTraining, activeTrainingEfficiency);
        activeTraining = null;
        trainingEndsAtMillis = 0;
        activeTrainingEfficiency = 1.0;
        return session;
    }

    public synchronized boolean isTraining() { return activeTraining != null; }
    public synchronized TrainingType getActiveTraining() { return activeTraining; }
    public synchronized long getTrainingRemainingMillis() {
        return activeTraining == null ? 0 : Math.max(0, trainingEndsAtMillis - System.currentTimeMillis());
    }

    public synchronized double applyTraining(int atk, int def, int agi, int tec, int fatigueCost) {
        return applyTraining(0, atk, def, agi, tec, fatigueCost, 1.0);
    }

    // ---------- 관리 (먹이 / 휴식 / 치료) ----------

    public synchronized void eat(int nutrition, int moodBonus) {
        hunger = clamp(hunger - nutrition);
        mood = clamp(mood + moodBonus);
    }

    public synchronized void rest(int bonus) {
        fatigue = clamp(fatigue - (25 + bonus));
        hp = Math.min(maxHp, hp + 10 + bonus);
        mood = clamp(mood + 3);
    }

    public synchronized void rest() {
        rest(0);
    }

    public synchronized boolean startRest() {
        if (busy || activeTraining != null || (fatigue == 0 && mood == 100 && hp == maxHp)) return false;
        resting = true;
        return true;
    }

    public synchronized void stopRest() {
        resting = false;
        autoRest = false;
    }
    public synchronized boolean isResting() { return resting; }

    /**
     * 선수 작업 스레드에서 휴식 회복과 부상 중 피로 증가를 갱신한다.
     * @return 스스로 시작한 휴식을 이번에 마쳤으면 true
     */
    public synchronized boolean processWorkerTick(int restBonus) {
        if (busy || activeTraining != null) return false;
        if (resting) {
            int recovery = 1 + Math.max(0, restBonus / 3);
            fatigue = clamp(fatigue - recovery);
            hp = Math.min(maxHp, hp + recovery);
            mood = clamp(mood + 1);
            if (fatigue == 0 && mood == 100 && hp == maxHp) {
                resting = false;
                autoRest = false;
            } else if (autoRest && fatigue <= 20 && hp * 10 >= maxHp * 7) {   // 스스로 쉬는 중이면 어느 정도 회복하면 멈춘다
                resting = false;
                autoRest = false;
                return true;
            }
        } else if (injured) {
            fatigue = clamp(fatigue + 1);
        }
        return false;
    }

    public synchronized int heal(int amount) {
        int previousHp = hp;
        hp = Math.min(maxHp, hp + Math.max(0, amount));
        return hp - previousHp;
    }

    public synchronized void cure() {
        injured = false;
    }

    public synchronized void addStats(int atk, int def, int agi, int tec) {
        attack += atk;
        defense += def;
        agility += agi;
        technique += tec;
    }

    /** 보스처럼 최대 HP를 배율로 키울 때 사용 (HP도 가득 채운다) */
    public synchronized void boostMaxHp(double factor) {
        maxHp = (int) Math.round(maxHp * factor);
        hp = maxHp;
    }

    public synchronized void addFatigue(int amount) {
        fatigue = clamp(fatigue + amount);
    }

    public synchronized void addMood(int amount) {
        mood = clamp(mood + amount);
    }

    // ---------- 자율 행동 ----------

    /** 섀도 펜싱(혼자 하는 연습): 경험치를 조금 얻고 피로가 쌓인다. */
    public synchronized void selfPractice() {
        fatigue = clamp(fatigue + 8);
        hunger = clamp(hunger + 3);
        mood = clamp(mood + 2);
        gainExp(3);
    }

    /** 기분 전환 (경기 영상 보기, 동료와 수다): 기분이 오르고 피로가 조금 쌓인다. */
    public synchronized void play() {
        mood = clamp(mood + 10);
        fatigue = clamp(fatigue + 4);
        hunger = clamp(hunger + 2);
    }

    /**
     * 상태와 성향을 보고 지금 할 행동을 정한다. (조건문 규칙)
     *  1. HP 30% 미만 / 피로가 성향 기준 이상 / 다쳤고 많이 지쳤으면 -> 휴식 (여기서 바로 휴식을 시작한다)
     *  2. 배고픔 60 이상 -> 식사
     *  3. 기분 30 미만 -> 기분 전환
     *  4. 기분이 성향 기준 이상이고 피로가 낮으면 50% 확률로 -> 섀도 펜싱
     * 훈련 중 / 휴식 중 / 시합 중이거나 자율 행동을 껐으면 아무것도 하지 않는다.
     */
    public synchronized AutoAction decideAutoAction(java.util.Random rnd) {
        if (autoDisabled || busy || activeTraining != null || resting) return AutoAction.NONE;
        boolean tired = fatigue >= personality.getRestThreshold();
        boolean weak = hp * 10 < maxHp * 3;
        boolean hurtAndWorn = injured && (fatigue > 30 || hp * 2 < maxHp);
        if ((tired || weak || hurtAndWorn) && startRest()) {
            autoRest = true;
            return AutoAction.REST;
        }
        if (hunger >= 60) return AutoAction.MEAL;
        if (injured) return AutoAction.NONE;
        if (mood < 30) return AutoAction.REFRESH;
        if (mood >= personality.getPracticeMood() && fatigue < 40 && hp * 2 >= maxHp && rnd.nextDouble() < 0.5) {
            return AutoAction.SHADOW;
        }
        return AutoAction.NONE;
    }

    public synchronized boolean isAutoEnabled() { return !autoDisabled; }
    public synchronized void setAutoEnabled(boolean enabled) { autoDisabled = !enabled; }
    public long getNextAutoAt() { return nextAutoAt; }
    public void setNextAutoAt(long t) { nextAutoAt = t; }
    public String getLastAuto() { return lastAuto == null ? "" : lastAuto; }
    public void setLastAuto(String s) { lastAuto = s; }

    // ---------- 전투 / 이벤트 ----------

    public synchronized void takeDamage(int damage) {
        hp = Math.max(0, hp - damage);
    }

    public synchronized void injure() {
        injured = true;
        mood = clamp(mood - 20);
    }

    public synchronized boolean isDown() {
        return hp <= 0;
    }

    /** 게임 시간이 하루 지날 때마다 호출 */
    public synchronized void tick() {
        hunger = clamp(hunger + 5);
        if (hunger >= 80) mood = clamp(mood - 3);
        if (fatigue >= 70) mood = clamp(mood - 2);
    }

    public synchronized boolean isBusy() { return busy; }
    public synchronized void setBusy(boolean busy) { this.busy = busy; }

    // ---------- Getter ----------

    public String getName() { return name; }
    public WeaponType getWeapon() { return weapon; }
    public Personality getPersonality() { return personality; }
    public synchronized int getLevel() { return level; }
    public synchronized int getExp() { return exp; }
    public synchronized Grade getGrade() { return Grade.fromLevel(level); }
    public synchronized int getHp() { return hp; }
    public synchronized int getMaxHp() { return maxHp; }
    // 능력치 = 기본 능력치 + 장착한 장비의 보너스
    public synchronized int getAttack() { return attack + gearSum(Equipment::getAtk); }
    public synchronized int getDefense() { return defense + gearSum(Equipment::getDef); }
    public synchronized int getAgility() { return agility + gearSum(Equipment::getAgi); }
    public synchronized int getTechnique() { return technique + gearSum(Equipment::getTec); }
    public synchronized int getHunger() { return hunger; }
    public synchronized int getFatigue() { return fatigue; }
    public synchronized int getMood() { return mood; }
    public synchronized boolean isInjured() { return injured; }

    // ---------- 장비 ----------

    /** 장비를 장착하고, 그 부위에 끼고 있던 이전 장비를 돌려준다. */
    public synchronized Equipment equip(Equipment e) {
        if (gear == null) gear = new EnumMap<>(EquipSlot.class);
        return gear.put(e.getSlot(), e);
    }

    public synchronized Equipment unequip(EquipSlot slot) {
        return gear == null ? null : gear.remove(slot);
    }

    public synchronized Equipment getGear(EquipSlot slot) {
        return gear == null ? null : gear.get(slot);
    }

    /** 장착한 장비 중에 해당 특수 효과가 있는지 */
    public synchronized boolean hasTag(GearTag tag) {
        if (gear == null) return false;
        for (Equipment e : gear.values()) if (e.getTag() == tag) return true;
        return false;
    }

    private int gearSum(ToIntFunction<Equipment> f) {
        if (gear == null) return 0;
        int sum = 0;
        for (Equipment e : gear.values()) sum += f.applyAsInt(e);
        return sum;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    // 저장 중에 값이 바뀌지 않도록 잠금
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (trainingLevels == null) trainingLevels = createTrainingLevels();
        for (TrainingType type : TrainingType.values()) trainingLevels.putIfAbsent(type, 1);
        if (!personalityBonusesApplied) {
            applyPersonalityBonuses();
            personalityBonusesApplied = true;
        }
    }

    /** 자세한 상태 문자열 (디버깅/테스트용) */
    public synchronized String describe() {
        return String.format("%s [%s/%s/%s] Lv.%d(%d/%d) HP %d/%d | 공%d 방%d 민%d 기%d | 배고픔%d 피로%d 기분%d%s",
                name, weapon.getDisplayName(), personality.getDisplayName(), getGrade().getDisplayName(),
                level, exp, expToNext(), hp, maxHp, attack, defense, agility, technique,
                hunger, fatigue, mood, injured ? " [부상]" : "");
    }

    @Override
    public String toString() {
        return name + (isResting() ? " (휴식 중)" : isTraining() ? " (훈련 중)" : "")
                + " (" + weapon.getDisplayName() + " Lv." + getLevel() + ")";
    }
}
