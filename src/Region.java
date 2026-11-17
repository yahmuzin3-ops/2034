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
    NATIONAL("한국 선수권",         8, 13,  8,   900, 220),
    WORLD("세계 선수권",           14, 22, 20,  2500, 500),
    OLYMPIC("올림픽",              20, 28, 30,  6000, 900);

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

    /** 토너먼트(예선 풀 + 본선 대진표)로 치르는 대회인지 */
    public boolean isTournament() {
        return this == NATIONAL || this == WORLD || this == OLYMPIC;
    }

    public boolean isUnlocked(int wins) {
        return wins >= requiredWins;
    }

    /** 적 팀 생성: 종목을 지정하면 모두 그 종목 (종목별 대회), null 이면 종목은 랜덤 */
    public List<Fencer> createEnemies(int size, WeaponType weapon) {
        List<String> names = new ArrayList<>(NAMES);
        Collections.shuffle(names);
        List<Fencer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            WeaponType w = weapon != null ? weapon : WeaponType.values()[R.nextInt(WeaponType.values().length)];
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

    public List<Fencer> createEnemies(int size) {
        return createEnemies(size, null);
    }

    /** 결승전(보스전) 상대: 최대 레벨 +8, 최대 HP(체력) 4배의 챔피언 1명 (종목 지정 가능) */
    public List<Fencer> createBoss(WeaponType weapon) {
        WeaponType w = weapon != null ? weapon : WeaponType.values()[R.nextInt(WeaponType.values().length)];
        Personality p = Personality.values()[R.nextInt(Personality.values().length)];
        Fencer f = new Fencer("챔피언 " + NAMES.get(R.nextInt(NAMES.size())), w, p);
        int target = maxLevel + 8;
        while (f.getLevel() < target) {
            f.gainExp(f.expToNext());
        }
        f.boostMaxHp(4.0);
        List<Fencer> list = new ArrayList<>();
        list.add(f);
        return list;
    }

    public List<Fencer> createBoss() {
        return createBoss(null);
    }

    public String getDisplayName() { return displayName; }
    public int getRequiredWins() { return requiredWins; }
    public int getRewardGold() { return rewardGold; }
    public int getRewardExp() { return rewardExp; }
    public String getLevelRange() { return "Lv." + minLevel + "~" + maxLevel; }

    @Override
    public String toString() { return displayName; }
}
