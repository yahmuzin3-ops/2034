import java.io.Serializable;

/** 대회에서 딴 메달 기록 */
public class MedalRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int day;
    private final Region region;
    private final WeaponType weapon;
    private final int rank;       // 1 금, 2 은, 3 동
    private final boolean team;
    private final String who;     // 선수/팀 이름
    private String nation;        // 그때의 소속 국가 (예전 저장 파일에는 없다)

    public MedalRecord(int day, Region region, WeaponType weapon, int rank, boolean team, String who, String nation) {
        this(day, region, weapon, rank, team, who);
        this.nation = nation;
    }

    public MedalRecord(int day, Region region, WeaponType weapon, int rank, boolean team, String who) {
        this.day = day;
        this.region = region;
        this.weapon = weapon;
        this.rank = rank;
        this.team = team;
        this.who = who;
    }

    public int getDay() { return day; }
    public Region getRegion() { return region; }
    public WeaponType getWeapon() { return weapon; }
    public int getRank() { return rank; }
    public boolean isTeam() { return team; }
    public String getWho() { return who; }
    public String getNation() { return nation == null ? "KOR" : nation; }

    public String getMedalName() { return rank == 1 ? "금메달" : rank == 2 ? "은메달" : "동메달"; }
}
