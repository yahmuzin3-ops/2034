public class GameStateTest {
    public static void main(String[] args) {
        GameState state = new GameState();
        Fencer f = new Fencer("아델", WeaponType.SABRE, Personality.PASSIONATE);
        state.addFencer(f);
        System.out.println("시작: " + f + " | 재화 " + state.getGold() + "G");

        // 카탈로그에서 아이템 찾기
        Item bento = find("닭가슴살 도시락");
        Item kit = find("구급 키트");
        Item blade = find("카본 블레이드");

        // 구매
        System.out.println("도시락 3개 구매: " + Shop.buy(state, bento, 3));
        System.out.println("구급 키트 1개 구매: " + Shop.buy(state, kit, 1));
        System.out.println("블레이드 5개 구매(돈 부족): " + Shop.buy(state, blade, 5));
        System.out.println("블레이드 1개 구매: " + Shop.buy(state, blade, 1));
        System.out.println("재화: " + state.getGold() + "G");

        // 사용
        System.out.println(state.useItem(f, blade));
        System.out.println(state.useItem(f, bento));
        f.injure();
        f.takeDamage(50);
        System.out.println("부상 상태: " + f);
        System.out.println(state.useItem(f, kit));
        System.out.println("치료 후: " + f);
        System.out.println(state.useItem(f, kit)); // 이미 썼으니 없음

        // 판매
        System.out.println("도시락 1개 판매: " + Shop.sell(state, bento, 1) + " -> 재화 " + state.getGold() + "G");

        // 하루 경과
        state.nextDay();
        System.out.println(state.getDay() + "일차, 날씨: " + state.getWeather().getDisplayName());
        System.out.println("인벤토리: " + state.getInventory().snapshot());
    }

    private static Item find(String name) {
        for (Item i : ItemCatalog.ALL) {
            if (i.getName().equals(name)) return i;
        }
        throw new IllegalArgumentException("없는 아이템: " + name);
    }
}
