/** 선수 성향은 선수 생성 때 능력치에 한 번 적용된다. */
public enum Personality {
    PASSIONATE("열정형", 80, 40, 0, 3, 0, 0, 0),
    CAREFUL("신중형", 60, 50, 0, 0, 0, 0, 3),
    LAZY("느긋형", 50, 70, 20, 0, 0, 0, 0),
    COMPETITIVE("승부욕형", 75, 45, 0, 0, 0, 3, 0);

    private final String displayName;
    // 이전 저장 데이터와 코드 호환을 위해 기준값을 유지한다.
    private final int restThreshold;
    private final int practiceMood;
    private final int hpBonus, attackBonus, defenseBonus, agilityBonus, techniqueBonus;

    Personality(String displayName, int restThreshold, int practiceMood, int hpBonus,
                int attackBonus, int defenseBonus, int agilityBonus, int techniqueBonus) {
        this.displayName = displayName;
        this.restThreshold = restThreshold;
        this.practiceMood = practiceMood;
        this.hpBonus = hpBonus;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.agilityBonus = agilityBonus;
        this.techniqueBonus = techniqueBonus;
    }

    public String getDisplayName() { return displayName; }
    public int getRestThreshold() { return restThreshold; }
    public int getPracticeMood() { return practiceMood; }
    public int getHpBonus() { return hpBonus; }
    public int getAttackBonus() { return attackBonus; }
    public int getDefenseBonus() { return defenseBonus; }
    public int getAgilityBonus() { return agilityBonus; }
    public int getTechniqueBonus() { return techniqueBonus; }

    /** 성향은 훈련 효율을 바꾸지 않는다. */
    public double getTrainingBonus() { return 1.0; }

    public String getBonusDescription() {
        if (hpBonus != 0) return "최대 HP +" + hpBonus;
        if (attackBonus != 0) return "공격 +" + attackBonus;
        if (defenseBonus != 0) return "방어 +" + defenseBonus;
        if (agilityBonus != 0) return "민첩 +" + agilityBonus;
        if (techniqueBonus != 0) return "기술 +" + techniqueBonus;
        return "능력치 보너스 없음";
    }
}
