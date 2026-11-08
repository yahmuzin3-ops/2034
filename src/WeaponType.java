/**
 * 펜싱 종목(무기). 초기 능력치 / 성장률 / 특수 기술 / 상성을 결정한다.
 * 상성: 플뢰레 > 사브르 > 에페 > 플뢰레
 */
public enum WeaponType {
    //          이름     HP   공격 방어 민첩 기술 | 성장: HP 공격 방어 민첩 기술
    FOIL("플뢰레", 100, 11, 10, 12, 16, 8, 3, 2, 2, 3, "파라드-리포스트"),
    EPEE("에페", 120, 12, 14, 9, 12, 10, 3, 3, 2, 2, "스톱히트"),
    SABRE("사브르", 90, 14, 8, 14, 10, 7, 3, 1, 2, 2, "속공 런지");

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

    /** 종목별 전문 훈련: 해당 훈련의 효과가 20% 커진다. */
    public boolean isFavored(TrainingType t) {
        switch (this) {
            case FOIL:  return t == TrainingType.BLADEWORK;
            case EPEE:  return t == TrainingType.PARRY || t == TrainingType.ENDURANCE;
            default:    return t == TrainingType.FOOTWORK || t == TrainingType.STRENGTH;
        }
    }

    public String getStyleName() {
        switch (this) {
            case FOIL:  return "정밀 · 우선권형";
            case EPEE:  return "전신 · 동시타형";
            default:    return "속공 · 베기형";
        }
    }

    /** 종목별 규칙 요약 (선수 관리 화면에 표시) */
    public String getRuleSummary() {
        switch (this) {
            case FOIL:
                return "몸통만 유효(밖을 맞히면 경기 중단) · 우선권 있음 · 찌르기 전용. 방어 후 리포스트와 페인트가 강력해요.";
            case EPEE:
                return "전신 유효 · 우선권 없음 · 동시에 맞으면 동시타(양쪽 득점). 반격(스톱히트)이 강력해요.";
            default:
                return "허리 위 유효 · 우선권 있음 · 베기와 찌르기. 속공이 빠르지만 방어와 후퇴가 약해요.";
        }
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
