/**
 * 장비 아이템. 부위(EquipSlot)마다 하나씩 장착하고, 장착하는 동안 능력치가 오른다.
 * - 종목 전용 장비: 해당 종목 선수만 장착할 수 있다.
 * - 날씨 대응 장비(GearTag): 비/무더위의 불리한 효과를 막아준다.
 * (slot 이 null 인 장비는 예전 저장 파일의 소모형 장비: 사용하면 능력치가 영구 상승)
 */
public class Equipment extends Item {
    private static final long serialVersionUID = 1L;

    private final int atk, def, agi, tec;
    private final EquipSlot slot;
    private final WeaponType only;
    private final GearTag tag;

    public Equipment(String name, int price, String description, EquipSlot slot, WeaponType only, GearTag tag,
                     int atk, int def, int agi, int tec) {
        super(name, price, description);
        this.slot = slot;
        this.only = only;
        this.tag = tag;
        this.atk = atk;
        this.def = def;
        this.agi = agi;
        this.tec = tec;
    }

    /** 예전 방식(소모형)의 장비 */
    public Equipment(String name, int price, String description, int atk, int def, int agi, int tec) {
        this(name, price, description, null, null, null, atk, def, agi, tec);
    }

    public EquipSlot getSlot() { return slot; }
    public WeaponType getOnly() { return only; }
    public GearTag getTag() { return tag; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public int getAgi() { return agi; }
    public int getTec() { return tec; }

    @Override
    public String cannotUse(Fencer target) {
        if (only != null && target.getWeapon() != only) {
            return getName() + "은(는) " + only + " 전용이라 " + target.getName() + "이(가) 장착할 수 없어요.";
        }
        return null;
    }

    /** 장착하고, 이전에 끼고 있던 장비를 돌려준다. (GameState 가 인벤토리로 되돌린다) */
    public Equipment equipOn(Fencer target) {
        return target.equip(this);
    }

    public String equipMessage(Fencer target, Equipment old) {
        return target.getName() + "이(가) " + getName() + "을(를) 장착했어요. (" + statText() + ")"
                + (old != null ? " " + old.getName() + "은(는) 인벤토리로 돌아갔어요." : "");
    }

    @Override
    public String use(Fencer target) {
        if (slot == null) {   // 예전 저장 파일의 소모형 장비
            target.addStats(atk, def, agi, tec);
            return target.getName() + "이(가) " + getName() + "을(를) 사용했어요. (" + statText() + ")";
        }
        Equipment old = equipOn(target);
        return equipMessage(target, old);
    }

    public String statText() {
        StringBuilder sb = new StringBuilder();
        if (atk != 0) sb.append("공격 +").append(atk).append(" ");
        if (def != 0) sb.append("방어 +").append(def).append(" ");
        if (agi != 0) sb.append("민첩 +").append(agi).append(" ");
        if (tec != 0) sb.append("기술 +").append(tec).append(" ");
        if (tag != null) sb.append(tag).append(" ");
        return sb.toString().trim();
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " [" + statText() + (only != null ? ", " + only + " 전용" : "") + "]";
    }

    @Override
    public String getTypeName() {
        return slot == null ? "장비" : "장비·" + slot;
    }
}
