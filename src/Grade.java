/**
 * 성장(진화) 단계. 레벨에 따라 등급이 올라간다.
 * 등급별 보너스 배율은 경기 계산에 곱해서 사용할 수 있다.
 */
public enum Grade {
    ROOKIE("신인", 1, 1.00),
    JUNIOR("주니어", 5, 1.10),
    PRO("프로", 10, 1.20),
    MASTER("마스터", 15, 1.35);

    private final String displayName;
    private final int minLevel;
    private final double bonus;

    Grade(String displayName, int minLevel, double bonus) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.bonus = bonus;
    }

    public static Grade fromLevel(int level) {
        Grade result = ROOKIE;
        for (Grade g : values()) {
            if (level >= g.minLevel) result = g;
        }
        return result;
    }

    public String getDisplayName() { return displayName; }
    public int getMinLevel() { return minLevel; }
    public double getBonus() { return bonus; }
}
