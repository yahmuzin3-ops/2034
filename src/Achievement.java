/** 목표(업적). 달성하면 보상 재화를 받는다. */
public enum Achievement {
    FIRST_WIN("첫 승리", "경기에서 처음으로 승리하기", 200),
    WINS_10("10승 달성", "경기에서 누적 10번 승리하기", 400),
    COLLECTOR("선수 수집가", "선수 3명 이상 보유하기", 300),
    PRO_GRADE("프로 등급", "선수를 프로 등급까지 성장시키기", 400),
    MASTER_GRADE("마스터 등급", "선수를 마스터 등급까지 성장시키기", 1000),
    RICH("부자 클럽", "재화 5000G 이상 보유하기", 500),
    WORLD_CHAMPION("세계 챔피언", "세계 선수권에서 우승하기", 3000);

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
