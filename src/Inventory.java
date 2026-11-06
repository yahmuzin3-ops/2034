import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 보유 아이템 목록. (아이템 -> 개수)
 * 여러 스레드가 접근할 수 있으니 synchronized 로 보호한다.
 */
public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Item, Integer> items = new LinkedHashMap<>();

    public synchronized void add(Item item, int qty) {
        if (qty <= 0) return;
        items.merge(item, qty, Integer::sum);
    }

    /** 개수가 모자라면 아무것도 하지 않고 false */
    public synchronized boolean remove(Item item, int qty) {
        int have = count(item);
        if (qty <= 0 || have < qty) return false;
        if (have == qty) items.remove(item);
        else items.put(item, have - qty);
        return true;
    }

    public synchronized int count(Item item) {
        return items.getOrDefault(item, 0);
    }

    /** 화면 표시용 복사본 (원본을 직접 건드리지 못하게) */
    public synchronized Map<Item, Integer> snapshot() {
        return new LinkedHashMap<>(items);
    }

    // 저장 중에 값이 바뀌지 않도록 잠금
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }
}
