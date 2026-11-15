import java.io.Serializable;

/** 하루짜리 의뢰(미션). 달성하면 보상 재화를 바로 받는다. */
public class Mission implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { TRAIN, WIN_MATCH, WEAPON_WIN, USE_ITEM }

    private final Type type;
    private final WeaponType weapon;   // WEAPON_WIN 일 때만 사용
    private final int target;
    private final int reward;
    private int progress = 0;
    private boolean done = false;

    public Mission(Type type, WeaponType weapon, int target, int reward) {
        this.type = type;
        this.weapon = weapon;
        this.target = target;
        this.reward = reward;
    }

    public String describe() {
        switch (type) {
            case TRAIN:      return "오늘 훈련을 " + target + "번 하기";
            case WIN_MATCH:  return "경기에서 " + target + "번 승리하기";
            case WEAPON_WIN: return weapon + "부에서 " + target + "번 승리하기";
            default:         return "아이템을 " + target + "번 사용하기";
        }
    }

    /** 진행도를 올린다. 이번에 처음 달성했으면 true */
    public boolean advance(Type t, WeaponType w, int amount) {
        if (done || t != type) return false;
        if (type == Type.WEAPON_WIN && weapon != w) return false;
        progress = Math.min(target, progress + amount);
        if (progress >= target) {
            done = true;
            return true;
        }
        return false;
    }

    public Type getType() { return type; }
    public WeaponType getWeapon() { return weapon; }
    public int getTarget() { return target; }
    public int getReward() { return reward; }
    public int getProgress() { return progress; }
    public boolean isDone() { return done; }
}
