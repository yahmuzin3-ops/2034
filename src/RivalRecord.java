import java.io.Serializable;

/** 컴퓨터 선수 한 명과의 상대 전적 (개인전 기준) */
public class RivalRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String name, nation;
    public final WeaponType weapon;
    public int wins, losses;
    public int pointsFor, pointsAgainst;
    public int firstDay, lastDay;
    public boolean lastWon;
    public int lastMy, lastOpp;
    public int streak;     // 양수: 연승, 음수: 연패

    public RivalRecord(String name, String nation, WeaponType weapon, int day) {
        this.name = name;
        this.nation = nation;
        this.weapon = weapon;
        this.firstDay = day;
    }

    public static String key(String name, String nation, WeaponType weapon) {
        return weapon + "|" + name + "|" + nation;
    }

    public int games() { return wins + losses; }

    public void add(boolean won, int my, int opp, int day) {
        if (won) wins++;
        else losses++;
        pointsFor += my;
        pointsAgainst += opp;
        lastDay = day;
        lastWon = won;
        lastMy = my;
        lastOpp = opp;
        streak = won ? (streak > 0 ? streak + 1 : 1) : (streak < 0 ? streak - 1 : -1);
    }

    /** 관계를 한 단어로 */
    public String status() {
        if (games() >= 3 && losses >= wins + 2) return "천적";
        if (games() >= 3 && wins >= losses + 2) return "상대 우위";
        if (games() >= 3) return "라이벌";
        if (!lastWon) return "복수 대상";
        return games() == 1 ? "첫 만남" : "다시 만난 상대";
    }

    public String recordText() { return wins + "승 " + losses + "패"; }
}
