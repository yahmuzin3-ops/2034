import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * 펜서(선수) 모델.
 * - 능력치: HP, 공격, 방어, 민첩, 기술 (성장에 따라 증가)
 * - 상태  : 배고픔, 피로도, 기분, 부상 (0~100, 시간에 따라 변화)
 * - 멀티스레딩: 값을 바꾸는 모든 메서드는 synchronized
 */
public class Fencer implements Serializable {
    private static final long serialVersionUID = 2L;

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

    // hunger: 높을수록 배고픔 / fatigue: 높을수록 지침 / mood: 높을수록 좋음
    private int hunger = 30;
    private int fatigue = 0;
    private int mood = 70;
    private boolean injured = false;

    // 시합 중에는 자율 행동을 하지 않는다 (저장하지 않음)
    private transient volatile boolean busy = false;

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
    }

    // ---------- 성장 ----------

    public synchronized int expToNext() {
        return level * 50;
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

    /** 훈련 효율. 피로 / 배고픔 / 부상 / 기분 / 성향이 영향을 준다. */
    public synchronized double getTrainingEfficiency() {
        double eff = 1.0;
        if (fatigue >= 70) eff *= 0.5;
        else if (fatigue >= 40) eff *= 0.8;
        if (hunger >= 80) eff *= 0.8;
        if (injured) eff *= 0.3;
        if (mood >= 70) eff *= 1.1;
        else if (mood < 30) eff *= 0.9;
        eff *= personality.getTrainingBonus();
        return eff;
    }

    /**
     * 훈련 적용. extra 는 날씨/시설 같은 외부 배율.
     * @return 실제로 적용된 효율(상태 효율 x extra)
     */
    public synchronized double applyTraining(int hpGain, int atk, int def, int agi, int tec,
                                             int fatigueCost, double extra) {
        double eff = getTrainingEfficiency() * extra;
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

    public synchronized void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
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

    public synchronized void addFatigue(int amount) {
        fatigue = clamp(fatigue + amount);
    }

    public synchronized void addMood(int amount) {
        mood = clamp(mood + amount);
    }

    // ---------- 자율 행동 ----------

    /** 개인 연습: 경험치를 조금 얻고 피로가 쌓인다. */
    public synchronized void selfPractice() {
        fatigue = clamp(fatigue + 8);
        hunger = clamp(hunger + 3);
        mood = clamp(mood + 2);
        gainExp(3);
    }

    /** 놀이: 기분 전환 */
    public synchronized void play() {
        mood = clamp(mood + 10);
        fatigue = clamp(fatigue + 4);
        hunger = clamp(hunger + 2);
    }

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

    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }

    // ---------- Getter ----------

    public String getName() { return name; }
    public WeaponType getWeapon() { return weapon; }
    public Personality getPersonality() { return personality; }
    public synchronized int getLevel() { return level; }
    public synchronized int getExp() { return exp; }
    public synchronized Grade getGrade() { return Grade.fromLevel(level); }
    public synchronized int getHp() { return hp; }
    public synchronized int getMaxHp() { return maxHp; }
    public synchronized int getAttack() { return attack; }
    public synchronized int getDefense() { return defense; }
    public synchronized int getAgility() { return agility; }
    public synchronized int getTechnique() { return technique; }
    public synchronized int getHunger() { return hunger; }
    public synchronized int getFatigue() { return fatigue; }
    public synchronized int getMood() { return mood; }
    public synchronized boolean isInjured() { return injured; }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    // 저장 중에 값이 바뀌지 않도록 잠금
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
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
        return name + " (" + weapon.getDisplayName() + " Lv." + getLevel() + ")";
    }
}
