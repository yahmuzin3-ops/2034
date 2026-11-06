/**
 * 선수 성향. 자율 행동의 기준값과 훈련 효율에 영향을 준다.
 *  - restThreshold : 피로도가 이 값 이상이면 휴식을 선택
 *  - practiceMood  : 기분이 이 값 이상이면 개인 연습을 선택
 *  - trainingBonus : 훈련 효율 배율
 */
public enum Personality {
    PASSIONATE("열정형", 80, 40, 1.10),
    CAREFUL("신중형", 60, 50, 1.00),
    LAZY("느긋형", 50, 70, 0.90),
    COMPETITIVE("승부욕형", 75, 45, 1.15);

    private final String displayName;
    private final int restThreshold;
    private final int practiceMood;
    private final double trainingBonus;

    Personality(String displayName, int restThreshold, int practiceMood, double trainingBonus) {
        this.displayName = displayName;
        this.restThreshold = restThreshold;
        this.practiceMood = practiceMood;
        this.trainingBonus = trainingBonus;
    }

    public String getDisplayName() { return displayName; }
    public int getRestThreshold() { return restThreshold; }
    public int getPracticeMood() { return practiceMood; }
    public double getTrainingBonus() { return trainingBonus; }
}
