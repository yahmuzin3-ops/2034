/**
 * 파워 레이팅 계산.
 *  - 레이팅은 Elo 방식이다. 이길 확률 = 1 / (1 + 10^((상대 - 나) / 400))
 *  - 선수의 "환산 레벨"은 능력치(장비 포함)로 계산해서, 훈련만 해도 레이팅이 오르고 레벨 대신 실력을 반영한다.
 *  - 레벨 1칸의 레이팅 차이(POINTS_PER_LEVEL)는 실제 경기 엔진의 승률을 측정해서 맞췄다.
 */
public final class RatingSystem {
    public static final double BASE = 1000;
    public static final double POINTS_PER_LEVEL = 48;
    public static final double MAX_LEVEL = 30;

    private RatingSystem() {
    }

    /** a 가 b 를 이길 확률 */
    public static double expected(double ra, double rb) {
        return 1.0 / (1.0 + Math.pow(10, (rb - ra) / 400.0));
    }

    public static double ratingForLevel(double level) {
        return BASE + (level - 1) * POINTS_PER_LEVEL;
    }

    public static double levelForRating(double rating) {
        return 1 + (rating - BASE) / POINTS_PER_LEVEL;
    }

    /** 능력치로 계산한 환산 레벨 (장비 포함). 해당 레벨까지 성장한 선수는 정확히 그 레벨이 나온다. */
    public static double levelEquivalent(Fencer f) {
        WeaponType w = f.getWeapon();
        double baseSum = w.getBaseAttack() + w.getBaseDefense() + w.getBaseAgility() + w.getBaseTechnique();
        double growSum = w.getGrowAttack() + w.getGrowDefense() + w.getGrowAgility() + w.getGrowTechnique();
        double sum = f.getAttack() + f.getDefense() + f.getAgility() + f.getTechnique();
        double statLevel = 1 + (sum - baseSum) / growSum;
        double hpLevel = 1 + (f.getMaxHp() - w.getBaseHp()) / (double) w.getGrowHp();
        double lv = 0.8 * statLevel + 0.2 * hpLevel;
        return Math.max(1, Math.min(MAX_LEVEL + 10, lv));
    }

    /** 경기 결과가 반영되기 전의 기본 레이팅 (능력치만 반영) */
    public static double baseRating(Fencer f) {
        return ratingForLevel(levelEquivalent(f));
    }

    /** 등급 이름 */
    public static String tier(double rating) {
        double lv = levelForRating(rating);
        if (lv < 4) return "입문";
        if (lv < 8) return "아마추어";
        if (lv < 12) return "유망주";
        if (lv < 16) return "프로";
        if (lv < 20) return "엘리트";
        if (lv < 24) return "월드클래스";
        return "전설";
    }
}
