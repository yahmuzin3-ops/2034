import java.util.Random;

/**
 * 날씨. 훈련, 선수 컨디션, 자율 행동, 경기(종목별로 다르게)에 영향을 준다.
 *
 *  맑음   : 기분 +, 훈련 효율 x1.05, 경기에서 체력 소모 -1 (구간마다)
 *  흐림   : 평범, 경기에서 무효 타격 -2%p (정밀 타격이 쉬움)
 *  비     : 기분 -, 훈련 효율 x0.9, 개인 연습 감소, 경기에서 미끄러운 바닥
 *           (후퇴 회피·전진 압박 약화, 사브르 속공 약화, 플뢰레·사브르 무효 타격 증가)
 *  무더위 : 배고픔 증가, 훈련 효율 x0.85, 쉽게 지침, 경기에서 체력 소모 +1 (구간마다)
 */
public enum Weather {
    SUNNY("맑음", 1.05,
            "기분이 좋아지고 훈련 효율이 조금 올라요",
            "관중의 열기가 선수를 북돋아 체력 소모가 줄어요 (구간마다 -1)",
            "체력 소모 감소"),
    CLOUDY("흐림", 1.0,
            "평범한 날씨예요",
            "차분한 분위기라 정밀 타격이 쉬워져요 (무효 타격 -2%p)",
            "무효 타격 감소"),
    RAINY("비", 0.9,
            "기분이 가라앉고 훈련 효율이 떨어져요 (개인 연습도 줄어요)",
            "바닥이 미끄러워요: 후퇴로 공격을 흘리기 어렵고 전진 압박이 약해지며, 사브르 속공이 약해지고 플뢰레·사브르 무효 타격이 늘어요",
            "미끄러운 바닥"),
    HOT("무더위", 0.85,
            "배고픔이 빨리 늘고 훈련 효율이 떨어져요 (열사병 주의)",
            "무더위로 구간마다 체력 소모가 +1 늘어요",
            "체력 소모 증가");

    private static final Random RANDOM = new Random();

    private final String displayName;
    private final double trainingModifier;
    private final String dayNote;
    private final String battleNote;
    private final String chipText;

    Weather(String displayName, double trainingModifier, String dayNote, String battleNote, String chipText) {
        this.displayName = displayName;
        this.trainingModifier = trainingModifier;
        this.dayNote = dayNote;
        this.battleNote = battleNote;
        this.chipText = chipText;
    }

    /** 무작위 날씨 (균등) */
    public static Weather random() {
        Weather[] all = values();
        return all[RANDOM.nextInt(all.length)];
    }

    /** 어제 날씨를 바탕으로 이어지는 날씨 (맑은 날 다음엔 맑을 확률이 높고, 비는 흐림 뒤에 잘 온다) */
    public static Weather next(Weather current) {
        int[] w;   // 맑음, 흐림, 비, 무더위
        switch (current) {
            case SUNNY:  w = new int[]{50, 30, 10, 10}; break;
            case CLOUDY: w = new int[]{30, 30, 30, 10}; break;
            case RAINY:  w = new int[]{20, 40, 35, 5};  break;
            default:     w = new int[]{40, 20, 10, 30}; break;
        }
        int r = RANDOM.nextInt(100), acc = 0;
        for (int i = 0; i < w.length; i++) {
            acc += w[i];
            if (r < acc) return values()[i];
        }
        return SUNNY;
    }

    public String getDisplayName() { return displayName; }
    public double getTrainingModifier() { return trainingModifier; }
    /** 하루 생활(훈련, 컨디션)에 주는 영향 설명 */
    public String getDayNote() { return dayNote; }
    /** 경기에 주는 영향 설명 */
    public String getBattleNote() { return battleNote; }
    /** 경기 화면에 띄우는 짧은 문구 */
    public String getChipText() { return chipText; }

    @Override
    public String toString() { return displayName; }
}
