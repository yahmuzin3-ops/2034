import javax.swing.*;
import java.awt.*;

/** 훈련: 선수 선택 + 훈련 종류 버튼. 피로/배고픔/부상/날씨/시설이 효율에 영향 */
public class TrainingPanel extends ScreenPanel {
    private final JComboBox<Fencer> combo = UI.fencerCombo();
    private final JLabel status = new JLabel(" ");
    private final JTextArea result = UI.logArea();

    public TrainingPanel(MainFrame frame) {
        super(frame, "훈련");

        JPanel top = new JPanel(new BorderLayout(10, 6));
        top.setOpaque(false);
        JPanel pick = new JPanel(new BorderLayout(8, 0));
        pick.setOpaque(false);
        pick.add(new JLabel("훈련할 선수"), BorderLayout.WEST);
        pick.add(combo, BorderLayout.CENTER);
        top.add(pick, BorderLayout.NORTH);
        top.add(status, BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        for (TrainingType t : TrainingType.values()) {
            JButton b = UI.button("<html><div style='text-align:left'><b>" + t.getDisplayName() + "</b><br>"
                    + t.getDescription() + "<br>피로 +" + t.getFatigueCost() + "</div></html>");
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.addActionListener(e -> train(t));
            grid.add(b);
        }

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(grid, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JScrollPane sp = new JScrollPane(result);
        sp.setBorder(BorderFactory.createTitledBorder("훈련 결과"));
        sp.setPreferredSize(new Dimension(0, 150));
        add(sp, BorderLayout.SOUTH);
    }

    private void train(TrainingType t) {
        Fencer f = (Fencer) combo.getSelectedItem();
        if (f == null) return;
        String msg = state().train(f, t);
        UI.appendLog(result, msg);
        GameLog.post("[훈련] " + msg);
        updateStatus();
    }

    private void updateStatus() {
        Fencer f = (Fencer) combo.getSelectedItem();
        if (f == null) {
            status.setText(" ");
            return;
        }
        double eff = f.getTrainingEfficiency() * state().getTrainingMultiplier();
        status.setText("<html>현재 훈련 효율 <b>" + Math.round(eff * 100) + "%</b> &nbsp;(날씨 "
                + state().getWeather().getDisplayName() + ", 훈련장 Lv." + state().getTrainingHallLevel() + ")<br>"
                + "피로 " + f.getFatigue() + " / 배고픔 " + f.getHunger() + " / 기분 " + f.getMood()
                + (f.isInjured() ? " / <font color='red'>부상 중</font>" : "") + "</html>");
    }

    @Override
    public void onShow() {
        UI.reloadCombo(combo, state().getRoster());
        updateStatus();
    }

    @Override
    public void onTick() { updateStatus(); }
}
