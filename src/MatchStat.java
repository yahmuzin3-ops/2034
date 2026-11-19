import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** 한 경기의 통계. 경기가 끝나면 기록실의 "경기 통계"에 쌓인다. */
public class MatchStat implements Serializable {
    private static final long serialVersionUID = 1L;

    public int day;
    public WeaponType weapon;
    public String title = "";          // 대회 이름 (예: 세계 선수권 사브르부 개인전 16강)
    public String opponent = "";
    public String oppNation;           // 상대 국가 (대회 경기에서만)
    public boolean individual, tournament, boss, sudden, won;
    public Region region;
    public int myScore, oppScore;
    public int phrases;                // 치른 구간 수
    public int[] used = new int[6];        // 내가 쓴 전술 (Battle.Tactic 순서)
    public int[] scored = new int[6];      // 그 전술로 낸 득점
    public int[] oppUsed = new int[6];     // 상대가 쓴 전술
    public int touchesFor, touchesAgainst;
    public int doubles, offTargets;
    public int exitsFor, exitsAgainst;     // 후방 라인 이탈로 얻은/내준 점수
    public int ripostesFor, ripostesAgainst;
    /** 내 선수별 {구간 수, 득점, 실점} */
    public Map<String, int[]> perFencer = new HashMap<>();

    public int margin() { return myScore - oppScore; }
}
