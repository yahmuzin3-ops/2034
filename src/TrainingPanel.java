import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/** 훈련: 선수 선택, 훈련별 숙련도, 시간 진행, 재화 즉시 완료 */
public class TrainingPanel extends ScreenPanel {
    private final JComboBox<Fencer> combo = UI.fencerCombo();
    private final JLabel status = new JLabel(" ");
    private final JTextArea result = UI.logArea();
    private final Map<TrainingType, JButton> trainingButtons = new EnumMap<>(TrainingType.class);
    private final JButton finishNowButton = UI.button("재화로 훈련 즉시 완료");

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
        for (TrainingType type : TrainingType.values()) {
            JButton button = UI.button(trainingButtonLabel(type, 1));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.addActionListener(e -> train(type));
            trainingButtons.put(type, button);
            grid.add(button);
        }

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(grid, BorderLayout.CENTER);
        finishNowButton.addActionListener(e -> finishTrainingNow());
        center.add(finishNowButton, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(result);
        scroll.setBorder(BorderFactory.createTitledBorder("훈련 결과"));
        scroll.setPreferredSize(new Dimension(0, 150));
        add(scroll, BorderLayout.SOUTH);
        GameLog.addListener(message -> {
            if (message.startsWith("[훈련 완료]")) {
                UI.appendLog(result, message);
                updateStatus();
            }
        });
    }

    private void train(TrainingType type) {
        Fencer f = (Fencer) combo.getSelectedItem();
        if (f == null) return;
        String message = state().train(f, type);
        UI.appendLog(result, message);
        GameLog.post("[훈련] " + message);
        updateStatus();
    }

    private void finishTrainingNow() {
        Fencer f = (Fencer) combo.getSelectedItem();
        if (f == null) return;
        String message = state().finishTrainingImmediately(f);
        if (message.startsWith("즉시 완료 비용:") || message.startsWith("훈련 시간이 끝나")) {
            GameLog.post("[훈련 완료] " + message);
        } else {
            UI.appendLog(result, message);
            GameLog.post("[훈련] " + message);
        }
        updateStatus();
    }

    private void updateStatus() {
        Fencer f = (Fencer) combo.getSelectedItem();
        boolean canTrain = f != null && !f.isBusy() && !f.isTraining() && !f.isResting()
                && !f.isDown() && f.getFatigue() < 95;
        for (TrainingType type : TrainingType.values()) {
            JButton button = trainingButtons.get(type);
            int level = f == null ? 1 : f.getTrainingLevel(type);
            button.setText(trainingButtonLabel(type, level));
            button.setEnabled(canTrain);
        }
        if (f == null) {
            status.setText(" ");
            finishNowButton.setText("재화로 훈련 즉시 완료");
            finishNowButton.setEnabled(false);
            return;
        }
        int cost = state().getInstantTrainingCost(f);
        finishNowButton.setText(f.isTraining()
                ? "재화로 즉시 완료 (" + cost + "G · 보유 " + state().getGold() + "G)"
                : "재화로 훈련 즉시 완료");
        finishNowButton.setEnabled(f.isTraining() && !f.isBusy());
        double efficiency = f.getTrainingEfficiency() * state().getTrainingMultiplier();
        String conditions = "피로 " + f.getFatigue() + " / 배고픔 " + f.getHunger() + " / 기분 " + f.getMood()
                + (f.isInjured() ? " / <font color='red'>부상 중</font>" : "");
        if (f.isTraining()) {
            status.setText("<html><b>" + f.getActiveTraining().getDisplayName() + " 훈련 중</b> · 남은 시간 "
                    + formatDuration(f.getTrainingRemainingMillis()) + "<br>" + conditions + "</html>");
        } else {
            status.setText("<html>현재 훈련 효율 <b>" + Math.round(efficiency * 100) + "%</b> &nbsp;(날씨 "
                    + state().getWeather().getDisplayName() + ", 훈련장 Lv." + state().getTrainingHallLevel() + ")<br>"
                    + "예상 훈련 시간 " + state().getTrainingDurationMinutes(f) + "분 · " + conditions + "</html>");
        }
    }

    private static String trainingButtonLabel(TrainingType type, int level) {
        return "<html><div style='text-align:left'><b>" + type.getDisplayName() + " · Lv." + level + "/"
                + TrainingType.MAX_LEVEL + "</b><br>" + type.getDescription() + "<br>피로 +"
                + type.getFatigueCost() + "</div></html>";
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void onShow() {
        UI.reloadCombo(combo, state().getRoster());
        updateStatus();
    }

    @Override
    public void onTick() { updateStatus(); }
}
