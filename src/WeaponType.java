/**
 * 펜싱 종목(무기). 초기 능력치 / 성장률 / 특수 기술 / 상성을 결정한다.
 * 상성: 플뢰레 > 사브르 > 에페 > 플뢰레
 */
public enum WeaponType {
    //          이름     HP   공격 방어 민첩 기술 | 성장: HP 공격 방어 민첩 기술
    FOIL("플뢰레", 100, 10, 10, 12, 16, 8, 2, 2, 2, 3, "정확한 리포스트"),
    EPEE("에페", 120, 12, 14, 8, 12, 10, 3, 3, 1, 2, "카운터 스톱히트"),
    SABRE("사브르", 90, 15, 8, 16, 10, 7, 3, 1, 3, 2, "플레슈 돌격");

    private final String displayName;
    private final int baseHp, baseAttack, baseDefense, baseAgility, baseTechnique;
    private final int growHp, growAttack, growDefense, growAgility, growTechnique;
    private final String specialSkill;

    WeaponType(String displayName,
               int baseHp, int baseAttack, int baseDefense, int baseAgility, int baseTechnique,
               int growHp, int growAttack, int growDefense, int growAgility, int growTechnique,
               String specialSkill) {
        this.displayName = displayName;
        this.baseHp = baseHp;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseAgility = baseAgility;
        this.baseTechnique = baseTechnique;
        this.growHp = growHp;
        this.growAttack = growAttack;
        this.growDefense = growDefense;
        this.growAgility = growAgility;
        this.growTechnique = growTechnique;
        this.specialSkill = specialSkill;
    }

    /** 상성 배율: 유리 1.25 / 불리 0.8 / 동일 1.0 */
    public double matchupAgainst(WeaponType other) {
        if (this == other) return 1.0;
        boolean win = (this == FOIL && other == SABRE)
                || (this == SABRE && other == EPEE)
                || (this == EPEE && other == FOIL);
        return win ? 1.25 : 0.8;
    }

    @Override
    public String toString() { return displayName; }

    public String getDisplayName() { return displayName; }
    public String getSpecialSkill() { return specialSkill; }
    public int getBaseHp() { return baseHp; }
    public int getBaseAttack() { return baseAttack; }
    public int getBaseDefense() { return baseDefense; }
    public int getBaseAgility() { return baseAgility; }
    public int getBaseTechnique() { return baseTechnique; }
    public int getGrowHp() { return growHp; }
    public int getGrowAttack() { return growAttack; }
    public int getGrowDefense() { return growDefense; }
    public int getGrowAgility() { return growAgility; }
    public int getGrowTechnique() { return growTechnique; }
}
