/** 상점: 구매/판매 로직. 화면(GUI)과 분리해서 콘솔에서도 테스트할 수 있어요. */
public final class Shop {
    private Shop() {
    }

    /** 구매 성공 시 true. 돈이 모자라면 false */
    public static boolean buy(GameState state, Item item, int qty) {
        if (qty <= 0) return false;
        synchronized (state) {
            if (!state.spendGold(item.getPrice() * qty)) return false;
            state.getInventory().add(item, qty);
            return true;
        }
    }

    /** 판매 성공 시 true. 개수가 모자라면 false (판매가는 구매가의 절반) */
    public static boolean sell(GameState state, Item item, int qty) {
        if (qty <= 0) return false;
        synchronized (state) {
            if (!state.getInventory().remove(item, qty)) return false;
            state.addGold(item.getSellPrice() * qty);
            return true;
        }
    }
}
