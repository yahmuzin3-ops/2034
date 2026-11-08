import java.util.List;

/** 상점에서 파는 아이템 목록. 아이템을 추가하고 싶으면 여기에만 한 줄 추가하면 돼요. */
public final class ItemCatalog {
    private ItemCatalog() {
    }

    public static final List<Item> ALL = List.of(
            // 식단 (이름, 가격, 설명, 영양, 기분)
            new Food("에너지바", 30, "간단히 허기를 달래요", 20, 2),
            new Food("프로틴 쉐이크", 50, "운동 후 든든하게", 30, 3),
            new Food("닭가슴살 도시락", 60, "배도 채우고 기분도 좋아져요", 40, 5),

            // 회복 (이름, 가격, 설명, 회복량, 부상 치료)
            new Medicine("파스", 40, "HP를 조금 회복해요", 30, false),
            new Medicine("구급 키트", 120, "HP 회복 + 부상 치료", 60, true),
            new Medicine("물리치료 쿠폰", 100, "부상을 치료해요", 0, true),

            // 장비 (이름, 가격, 설명, 부위, 전용 종목, 특수 효과, 공격, 방어, 민첩, 기술)
            new Equipment("카본 블레이드", 250, "가볍고 단단한 범용 검",
                    EquipSlot.BLADE, null, null, 2, 0, 0, 1),
            new Equipment("정밀 플뢰레 블레이드", 320, "칼끝이 정확해요",
                    EquipSlot.BLADE, WeaponType.FOIL, null, 0, 0, 0, 4),
            new Equipment("강화 에페 벨가드", 320, "막아내기 좋아요",
                    EquipSlot.BLADE, WeaponType.EPEE, null, 0, 4, 0, 0),
            new Equipment("경량 사브르 블레이드", 320, "휘두르기 가벼워요",
                    EquipSlot.BLADE, WeaponType.SABRE, null, 0, 0, 4, 0),
            new Equipment("강화 마스크", 200, "방어력이 올라가요",
                    EquipSlot.MASK, null, null, 0, 2, 0, 0),
            new Equipment("집중 마스크", 260, "시야가 넓어져 기술이 올라요",
                    EquipSlot.MASK, null, null, 0, 0, 0, 3),
            new Equipment("가벼운 펜싱화", 200, "발이 한결 가벼워져요",
                    EquipSlot.SHOES, null, null, 0, 0, 2, 0),
            new Equipment("논슬립 펜싱화", 340, "비 오는 날에도 미끄럽지 않아요",
                    EquipSlot.SHOES, null, GearTag.RAIN_PROOF, 0, 0, 1, 0),
            new Equipment("보호 재킷", 220, "방어력이 올라가요",
                    EquipSlot.JACKET, null, null, 0, 2, 0, 0),
            new Equipment("쿨링 재킷", 340, "무더위에도 체력이 줄지 않아요",
                    EquipSlot.JACKET, null, GearTag.HEAT_PROOF, 0, 1, 0, 0)
    );
}
