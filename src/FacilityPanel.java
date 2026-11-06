import javax.swing.*;
import java.awt.*;

/** 시설 확장: 훈련장(훈련 효율), 의무실(휴식 회복/부상 감소) */
public class FacilityPanel extends ScreenPanel {
    private final JLabel goldLabel = new JLabel(" ");
    private final JLabel hallLabel = new JLabel(" ");
    private final JLabel clinicLabel = new JLabel(" ");
    private final JLabel msg = new JLabel(" ");

    public FacilityPanel(MainFrame frame) {
        super(frame, "시설");

        JPanel box = new JPanel(new GridLayout(0, 1, 0, 14));
        box.setOpaque(false);
        goldLabel.setFont(goldLabel.getFont().deriveFont(Font.BOLD, 16f));
        box.add(goldLabel);
        box.add(card(hallLabel, "훈련장 업그레이드", () -> state().upgradeTrainingHall()));
        box.add(card(clinicLabel, "의무실 업그레이드", () -> state().upgradeClinic()));
        box.add(msg);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(box, BorderLayout.NORTH);
        add(wrap, BorderLayout.CENTER);
    }

    private JPanel card(JLabel label, String btnText, java.util.function.Supplier<String> action) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 220)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        p.add(label, BorderLayout.CENTER);
        JButton b = UI.button(btnText);
        b.addActionListener(e -> {
            msg.setText(action.get());
            update();
        });
        p.add(b, BorderLayout.EAST);
        return p;
    }

    private void update() {
        GameState s = state();
        goldLabel.setText("보유 재화: " + s.getGold() + "G");
        int h = s.getTrainingHallLevel();
        int c = s.getClinicLevel();
        hallLabel.setText("<html><b>훈련장</b> Lv." + h + "/" + GameState.MAX_FACILITY_LEVEL
                + "<br>훈련 효율 +" + (h * 5) + "%  |  "
                + (h >= GameState.MAX_FACILITY_LEVEL ? "최대 레벨" : "다음 레벨 " + s.upgradeCost(h) + "G") + "</html>");
        clinicLabel.setText("<html><b>의무실</b> Lv." + c + "/" + GameState.MAX_FACILITY_LEVEL
                + "<br>휴식 회복 +" + (c * 3) + ", 경기 후 부상 확률 -" + (c * 8) + "%p  |  "
                + (c >= GameState.MAX_FACILITY_LEVEL ? "최대 레벨" : "다음 레벨 " + s.upgradeCost(c) + "G") + "</html>");
    }

    @Override
    public void onShow() { update(); }

    @Override
    public void onTick() { update(); }
}
