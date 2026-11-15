/** 장비를 끼우는 부위. */
public enum EquipSlot {
    BLADE("블레이드"), GRIP("그립"), MASK("마스크"), SHOES("펜싱화"), JACKET("재킷");

    private final String displayName;

    EquipSlot(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    public static EquipSlot fromDisplayName(String name) {
        for (EquipSlot slot : values()) {
            if (slot.displayName.equals(name) || slot.name().equalsIgnoreCase(name)) return slot;
        }
        throw new IllegalArgumentException("알 수 없는 장비 종류: " + name);
    }

    @Override
    public String toString() { return displayName; }
}
