import java.io.Serializable;
import java.util.Random;

/** 코치·스태프. 역할마다 한 명씩 고용할 수 있고, 레벨이 높을수록 효과가 크고 주급도 높다. */
public class Staff implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Role {
        HEAD_COACH("수석 코치", "훈련 효율"),
        PHYSIO("트레이너", "회복"),
        ANALYST("전력 분석관", "상대 분석"),
        NUTRITIONIST("영양사", "식사 효과");

        private final String displayName, effectName;

        Role(String displayName, String effectName) {
            this.displayName = displayName;
            this.effectName = effectName;
        }

        public String getDisplayName() { return displayName; }
        public String getEffectName() { return effectName; }

        @Override
        public String toString() { return displayName; }
    }

    public static final int MAX_LEVEL = 5;
    public static final int PAY_DAYS = 7;   // 주급은 7일마다 나간다.
    private static final int[] SALARY = {0, 60, 120, 200, 300, 450};
    private static final String[] FAMILY = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오"};
    private static final String[] GIVEN = {"도현", "서윤", "지훈", "하은", "민석", "수빈", "태오", "예린", "준혁", "소연", "건우", "나연", "시온", "유진"};

    private final Role role;
    private final String name;
    private int level;
    private int hiredDay;

    public Staff(Role role, String name, int level) {
        this.role = role;
        this.name = name;
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
    }

    public static Staff random(Role role, int level, Random rnd) {
        return new Staff(role, FAMILY[rnd.nextInt(FAMILY.length)] + GIVEN[rnd.nextInt(GIVEN.length)], level);
    }

    public static int weeklySalary(int level) { return SALARY[Math.max(1, Math.min(MAX_LEVEL, level))]; }

    public Role getRole() { return role; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getHiredDay() { return hiredDay; }
    public void setHiredDay(int d) { hiredDay = d; }
    public int getWeeklySalary() { return weeklySalary(level); }
    public int getHireFee() { return 3 * weeklySalary(level); }
    public boolean canPromote() { return level < MAX_LEVEL; }
    /** 승급 비용: 다음 레벨 스태프를 새로 영입하는 값의 절반 */
    public int getPromoteCost() { return level >= MAX_LEVEL ? 0 : 3 * weeklySalary(level + 1) / 2; }
    public void promote() { if (level < MAX_LEVEL) level++; }

    public String stars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= MAX_LEVEL; i++) sb.append(i <= level ? "★" : "☆");
        return sb.toString();
    }

    /** 레벨에 따른 효과 설명 */
    public static String effectText(Role role, int level) {
        switch (role) {
            case HEAD_COACH: return "훈련 효율 +" + 4 * level + "%";
            case PHYSIO: return "휴식 회복 +" + 2 * level + ", 대회 중 회복 +" + 3 * level + ", 무리한 부상 확률 -" + 2 * level + "%p";
            case ANALYST: return level >= 4 ? "상대의 전술 비율과 다음 선택 예상" : level >= 2 ? "상대의 전술 비율 공개" : "상대가 즐겨 쓰는 전술 안내(기본)";
            default: return "식단 효과 +" + 10 * level + "%";
        }
    }

    public String getEffectText() { return effectText(role, level); }
}
