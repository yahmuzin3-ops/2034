/** 목표(업적). 달성하면 보상 재화를 받는다. */
public enum Achievement {
    FIRST_WIN("첫 승리", "경기에서 처음으로 승리하기", 200),
    WINS_10("10승 달성", "경기에서 누적 10번 승리하기", 400),
    COLLECTOR("선수 수집가", "선수 3명 이상 보유하기", 300),
    PRO_GRADE("프로 등급", "선수를 프로 등급까지 성장시키기", 400),
    MASTER_GRADE("마스터 등급", "선수를 마스터 등급까지 성장시키기", 1000),
    RICH("부자 클럽", "재화 5000G 이상 보유하기", 500),
    BOSS_SLAYER("결승전 정복", "결승전(보스전)에서 승리하기", 600),
    GRAND_SLAM("3종목 석권", "플뢰레·에페·사브르 세계 선수권을 모두 우승하기", 3000),
    WORLD_CHAMPION("세계 챔피언", "세계 선수권에서 우승하기", 3000),
    KOREAN_CHAMPION("국내 챔피언", "자국 선수권(소속 국가의 선수권)에서 우승하기", 800),
    FIRST_MEDAL("메달리스트", "대회에서 메달 따기", 500),
    OLYMPIC_MEDAL("올림픽 메달", "올림픽에서 메달 따기", 3000),
    OLYMPIC_CHAMPION("올림픽 챔피언", "올림픽에서 금메달 따기", 6000),
    WORLD_NO1("세계 랭킹 1위", "한 종목 파워 랭킹 세계 1위에 오르기", 2000);

    private final String displayName;
    private final String description;
    private final int reward;

    Achievement(String displayName, String description, int reward) {
        this.displayName = displayName;
        this.description = description;
        this.reward = reward;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getReward() { return reward; }
}
