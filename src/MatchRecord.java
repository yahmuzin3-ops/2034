import java.io.Serializable;

/** 경기 한 판의 기록 (경기 기록 화면에 표시) */
public class MatchRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int day;
    private final WeaponType weapon;   // 혼성 경기(테스트용)이면 null
    private final Region region;
    private final boolean boss;
    private final Weather weather;
    private final int playerScore;
    private final int enemyScore;
    private final boolean won;

    public MatchRecord(int day, WeaponType weapon, Region region, boolean boss, Weather weather,
                       int playerScore, int enemyScore, boolean won) {
        this.day = day;
        this.weapon = weapon;
        this.region = region;
        this.boss = boss;
        this.weather = weather;
        this.playerScore = playerScore;
        this.enemyScore = enemyScore;
        this.won = won;
    }

    public int getDay() { return day; }
    public WeaponType getWeapon() { return weapon; }
    public Region getRegion() { return region; }
    public boolean isBoss() { return boss; }
    public Weather getWeather() { return weather; }
    public int getPlayerScore() { return playerScore; }
    public int getEnemyScore() { return enemyScore; }
    public boolean isWon() { return won; }
}
