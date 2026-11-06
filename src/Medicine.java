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
        target.heal(healAmount);
        String msg = target.getName() + "의 HP가 " + healAmount + " 회복됐어요.";
        if (curesInjury && target.isInjured()) {
            target.cure();
            msg += " 부상도 치료됐어요!";
        }
        return msg;
    }

    @Override
    public String getTypeName() {
        return "회복";
    }
}
