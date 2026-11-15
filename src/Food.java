/** 식단 아이템: 배고픔을 줄이고 기분을 올린다. */
public class Food extends Item {
    private static final long serialVersionUID = 1L;

    private final int nutrition;
    private final int moodBonus;

    public Food(String name, int price, String description, int nutrition, int moodBonus) {
        super(name, price, description);
        this.nutrition = nutrition;
        this.moodBonus = moodBonus;
    }

    @Override
    public String use(Fencer target) {
        String failure = cannotUse(target);
        if (failure != null) return failure;
        target.eat(nutrition, moodBonus);
        return target.getName() + "이(가) " + getName() + "을(를) 먹었어요. (배고픔 -" + nutrition + ", 기분 +" + moodBonus + ")";
    }

    @Override
    public String cannotUse(Fencer target) {
        if (target == null) return "선수를 선택해주세요.";
        if (target.getHunger() <= 0) return target.getName() + "은(는) 배고프지 않아 지금은 음식을 먹을 필요가 없어요.";
        return null;
    }

    @Override
    public String getTypeName() {
        return "식단";
    }
}
