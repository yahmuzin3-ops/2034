import java.util.Random;

/**
 * 날씨. trainingModifier 는 훈련 효율에 곱해서 사용할 수 있다. (이벤트 단계에서 연결)
 */
public enum Weather {
    SUNNY("맑음", 1.0),
    CLOUDY("흐림", 1.0),
    RAINY("비", 0.9),
    HOT("무더위", 0.85);

    private static final Random RANDOM = new Random();

    private final String displayName;
    private final double trainingModifier;

    Weather(String displayName, double trainingModifier) {
        this.displayName = displayName;
        this.trainingModifier = trainingModifier;
    }

    public static Weather random() {
        Weather[] all = values();
        return all[RANDOM.nextInt(all.length)];
    }

    public String getDisplayName() { return displayName; }
    public double getTrainingModifier() { return trainingModifier; }
}
