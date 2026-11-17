import java.io.Serializable;

/**
 * 컴퓨터 선수(라이벌). 세계 랭킹에 올라 있고, 대회에서 만나는 상대가 된다.
 * 경기 결과(Elo)와 꾸준한 훈련으로 레이팅이 조금씩 변한다.
 */
public class Rival implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String nation;
    private final WeaponType weapon;
    private final Personality personality;
    private double rating;
    private int wins, losses;
    private double weekMark, prevMark;

    public Rival(String name, String nation, WeaponType weapon, Personality personality, double rating) {
        this.name = name;
        this.nation = nation;
        this.weapon = weapon;
        this.personality = personality;
        this.rating = rating;
        this.weekMark = rating;
        this.prevMark = rating;
    }

    public synchronized void applyElo(double oppRating, boolean won, double k) {
        double e = RatingSystem.expected(rating, oppRating);
        rating += k * ((won ? 1 : 0) - e);
        rating = Math.max(RatingSystem.BASE, Math.min(RatingSystem.ratingForLevel(RatingSystem.MAX_LEVEL + 4), rating));
        if (won) wins++;
        else losses++;
    }

    /** 꾸준히 훈련해서 실력이 조금씩 늘어난다. */
    public synchronized void train(double points) {
        rating = Math.min(RatingSystem.ratingForLevel(RatingSystem.MAX_LEVEL + 4), rating + points);
    }

    public synchronized void snapshot() {
        prevMark = weekMark;
        weekMark = rating;
    }

    /**
     * 이 선수의 실력에 맞는 실제 경기용 선수를 만든다.
     * 성향 보너스(능력치 가산)가 있어서, 환산 레벨이 레이팅에 가장 가까운 레벨로 고른다.
     */
    public synchronized Fencer toFencer() {
        double target = RatingSystem.levelForRating(rating);
        int center = (int) Math.max(1, Math.round(target));
        Fencer best = null;
        double bestGap = Double.MAX_VALUE;
        for (int lv = Math.max(1, center - 2); lv <= center + 1; lv++) {
            Fencer f = new Fencer(name, weapon, personality);
            while (f.getLevel() < lv) f.gainExp(f.expToNext());
            double gap = Math.abs(RatingSystem.levelEquivalent(f) - target);
            if (gap < bestGap) {
                bestGap = gap;
                best = f;
            }
        }
        return best;
    }

    public String getName() { return name; }
    public String getNation() { return nation; }
    public String getDisplayName() { return name + " (" + nation + ")"; }
    public WeaponType getWeapon() { return weapon; }
    public Personality getPersonality() { return personality; }
    public synchronized double getRating() { return rating; }
    public synchronized double getLevel() { return RatingSystem.levelForRating(rating); }
    public synchronized int getWins() { return wins; }
    public synchronized int getLosses() { return losses; }
    public synchronized double getDelta() { return rating - prevMark; }

    @Override
    public String toString() { return getDisplayName(); }
}
