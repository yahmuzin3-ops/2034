import java.io.Serializable;
import java.util.Objects;

/**
 * 아이템 공통 부모 클래스.
 * 종류별(Food / Medicine / Equipment)로 use()를 다르게 구현한다.
 * 이름이 같으면 같은 아이템으로 취급한다. (인벤토리 Map의 key로 사용)
 */
public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int price;
    private final String description;

    protected Item(String name, int price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
    }

    /** 대상 선수에게 아이템 효과 적용. 결과 메시지를 반환 */
    public abstract String use(Fencer target);

    /** 종류 표시용 이름 (상점/인벤토리 화면에서 사용) */
    public abstract String getTypeName();

    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getSellPrice() { return price / 2; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        return name.equals(((Item) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " (" + getTypeName() + ", " + price + "G)";
    }
}
