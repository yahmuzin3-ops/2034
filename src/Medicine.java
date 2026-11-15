/** 회복 아이템: HP를 회복하고, 종류에 따라 부상도 치료한다. */
public class Medicine extends Item {
    private static final long serialVersionUID = 1L;

    private final int healAmount;
    private final boolean curesInjury;

    public Medicine(String name, int price, String description, int healAmount, boolean curesInjury) {
        super(name, price, description);
        this.healAmount = healAmount;
        this.curesInjury = curesInjury;
    }

    @Override
    public String use(Fencer target) {
        String failure = cannotUse(target);
        if (failure != null) return failure;
        int healed = target.heal(healAmount);
        boolean cured = curesInjury && target.isInjured();
        if (cured) {
            target.cure();
        }
        StringBuilder effects = new StringBuilder();
        if (healed > 0) effects.append("HP +").append(healed);
        if (cured) {
            if (effects.length() > 0) effects.append(", ");
            effects.append("부상 치료");
        }
        return target.getName() + "에게 " + getName() + "을(를) 사용했어요. (" + effects + ")";
    }

    @Override
    public String cannotUse(Fencer target) {
        if (target == null) return "선수를 선택해주세요.";
        boolean canHeal = healAmount > 0 && target.getHp() < target.getMaxHp();
        boolean canCure = curesInjury && target.isInjured();
        if (!canHeal && !canCure) return target.getName() + "은(는) 치료할 필요가 없어요.";
        return null;
    }

    @Override
    public String getTypeName() {
        return "회복";
    }
}
