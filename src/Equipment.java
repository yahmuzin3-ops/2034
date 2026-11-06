/**
 * 장비 아이템: 사용하면 능력치가 영구적으로 오른다.
 * (간단하게 만든 버전. 나중에 확장 기능으로 '장착 슬롯'을 만들 수도 있어요.)
 */
public class Equipment extends Item {
    private static final long serialVersionUID = 1L;

    private final int atk, def, agi, tec;

    public Equipment(String name, int price, String description, int atk, int def, int agi, int tec) {
        super(name, price, description);
        this.atk = atk;
        this.def = def;
        this.agi = agi;
        this.tec = tec;
    }

    @Override
    public String use(Fencer target) {
        target.addStats(atk, def, agi, tec);
        return target.getName() + "이(가) " + getName() + "을(를) 사용했어요. ("
                + statText() + ")";
    }

    private String statText() {
        StringBuilder sb = new StringBuilder();
        if (atk != 0) sb.append("공격 +").append(atk).append(" ");
        if (def != 0) sb.append("방어 +").append(def).append(" ");
        if (agi != 0) sb.append("민첩 +").append(agi).append(" ");
        if (tec != 0) sb.append("기술 +").append(tec).append(" ");
        return sb.toString().trim();
    }

    @Override
    public String getTypeName() {
        return "장비";
    }
}
