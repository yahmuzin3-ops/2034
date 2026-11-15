/** 장비의 특수 효과. 날씨의 불리한 효과를 막아준다. */
public enum GearTag {
    RAIN_PROOF("비에 강함"),   // 비 오는 날의 미끄러움 페널티를 받지 않는다
    HEAT_PROOF("더위에 강함"); // 무더위의 체력 소모를 받지 않는다

    private final String displayName;

    GearTag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
