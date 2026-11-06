/** 훈련 종류. 종류마다 오르는 능력치와 피로 비용이 다르다. */
public enum TrainingType {
    //            이름                 HP  공격 방어 민첩 기술 피로
    FOOTWORK("풋워크 훈련",            0, 0, 0, 3, 1, 15),
    BLADEWORK("블레이드워크 훈련",     0, 1, 0, 0, 3, 15),
    STRENGTH("근력 훈련",              0, 3, 1, 0, 0, 20),
    PARRY("파라드(방어) 훈련",         0, 0, 3, 1, 1, 15),
    ENDURANCE("지구력 훈련",           8, 0, 1, 0, 0, 20);

    private final String displayName;
    private final int hp, atk, def, agi, tec, fatigueCost;

    TrainingType(String displayName, int hp, int atk, int def, int agi, int tec, int fatigueCost) {
        this.displayName = displayName;
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.agi = agi;
        this.tec = tec;
        this.fatigueCost = fatigueCost;
    }

    /** 오르는 능력치 설명 (자동 생성) */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if (hp != 0) sb.append("최대HP +").append(hp).append("  ");
        if (atk != 0) sb.append("공격 +").append(atk).append("  ");
        if (def != 0) sb.append("방어 +").append(def).append("  ");
        if (agi != 0) sb.append("민첩 +").append(agi).append("  ");
        if (tec != 0) sb.append("기술 +").append(tec).append("  ");
        return sb.toString().trim();
    }

    public String getDisplayName() { return displayName; }
    public int getHp() { return hp; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public int getAgi() { return agi; }
    public int getTec() { return tec; }
    public int getFatigueCost() { return fatigueCost; }

    @Override
    public String toString() { return displayName; }
}
