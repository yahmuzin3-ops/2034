/** 랭킹 한 줄 (내 선수 또는 컴퓨터 선수) */
public class RankEntry {
    public int rank;
    public final String name;
    public final String nation;
    public final WeaponType weapon;
    public final double rating;
    public final double levelEquivalent;
    public final int wins, losses;
    public final double delta;
    public final Fencer fencer;   // 내 선수면 값이 있다
    public final Rival rival;     // 컴퓨터 선수면 값이 있다

    public RankEntry(Rival r) {
        this.name = r.getName();
        this.nation = r.getNation();
        this.weapon = r.getWeapon();
        this.rating = r.getRating();
        this.levelEquivalent = r.getLevel();
        this.wins = r.getWins();
        this.losses = r.getLosses();
        this.delta = r.getDelta();
        this.fencer = null;
        this.rival = r;
    }

    public RankEntry(Fencer f) {
        this.name = f.getName();
        this.nation = "KOR";
        this.weapon = f.getWeapon();
        this.rating = f.getRating();
        this.levelEquivalent = RatingSystem.levelEquivalent(f);
        this.wins = f.getRatedWins();
        this.losses = f.getRatedLosses();
        this.delta = f.getRatingDelta();
        this.fencer = f;
        this.rival = null;
    }

    public boolean isMine() { return fencer != null; }

    public String getTier() { return RatingSystem.tier(rating); }
}
