/** 장비를 끼는 부위. 부위마다 장비를 하나씩 장착할 수 있다. */
public enum EquipSlot {
    BLADE("검"), MASK("마스크"), SHOES("신발"), JACKET("재킷");

    private final String displayName;

    EquipSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
