import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 대회(전투 지역). 승리 횟수가 쌓이면 새 지역이 열린다.
 * 새 지역은 enum 에 한 줄만 추가하면 된다.
 */
public enum Region {
    //          이름              적Lv  적Lv  필요승 보상G 보상EXP
    CLUB("동네 클럽 대회",          1, 1,   0,   150,  60),
    PROVINCIAL("도 대회",           3, 6,   3,   400, 120),
    NATIONAL("전국 대회",           8, 13,  8,   900, 220),
    WORLD("세계 선수권",           14, 22, 20,  2500, 500);

    private static final Random R = new Random();
    private static final List<String> NAMES = List.of(
            "레온", "미샤", "카이", "루카", "엘레나", "빅토르", "소피", "안톤", "이자벨", "마르코");

    private final String displayName;
    private final int minLevel, maxLevel, requiredWins, rewardGold, rewardExp;

    Region(String displayName, int minLevel, int maxLevel, int requiredWins, int rewardGold, int rewardExp) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.requiredWins = requiredWins;
        this.rewardGold = rewardGold;
        this.rewardExp = rewardExp;
    }

    public boolean isUnlocked(int wins) {
        return wins >= requiredWins;
    }

    /** 적 팀 생성: 종목/성향은 랜덤, 레벨은 지역 범위 안에서 랜덤 */
    public List<Fencer> createEnemies(int size) {
        List<String> names = new ArrayList<>(NAMES);
        Collections.shuffle(names);
        List<Fencer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            WeaponType w = WeaponType.values()[R.nextInt(WeaponType.values().length)];
            Personality p = Personality.values()[R.nextInt(Personality.values().length)];
            Fencer f = new Fencer(names.get(i), w, p);
            int target = minLevel + R.nextInt(maxLevel - minLevel + 1);
            while (f.getLevel() < target) {
                f.gainExp(f.expToNext());
            }
            list.add(f);
        }
        return list;
    }

    public String getDisplayName() { return displayName; }
    public int getRequiredWins() { return requiredWins; }
    public int getRewardGold() { return rewardGold; }
    public int getRewardExp() { return rewardExp; }
    public String getLevelRange() { return "Lv." + minLevel + "~" + maxLevel; }

    @Override
    public String toString() { return displayName; }
}
