import javax.swing.*;
import java.awt.*;

/** 목표/업적 + 대회(지역) 해금 현황 */
public class GoalPanel extends ScreenPanel {
    private final JTextArea area = UI.logArea();
    private String last = "";

    public GoalPanel(MainFrame frame) {
        super(frame, "목표 · 업적");
        area.setFont(area.getFont().deriveFont(15f));
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    private void update() {
        GameState s = state();
        StringBuilder sb = new StringBuilder();
        sb.append("[ 업적 ]\n");
        for (Achievement a : Achievement.values()) {
            sb.append(s.hasAchievement(a) ? " [완료] " : " [    ] ")
                    .append(a.getDisplayName()).append(" - ").append(a.getDescription())
                    .append(" (보상 ").append(a.getReward()).append("G)\n");
        }
        sb.append("\n[ 대회 ]  현재 ").append(s.getWins()).append("승\n");
        for (Region r : Region.values()) {
            sb.append(" ").append(r.getDisplayName()).append(" (적 ").append(r.getLevelRange()).append(")  ");
            if (s.isCleared(r)) sb.append("우승 경험 있음");
            else if (r.isUnlocked(s.getWins())) sb.append("도전 가능");
            else sb.append("잠김 (").append(r.getRequiredWins()).append("승 필요)");
            sb.append("\n");
        }
        String text = sb.toString();
        if (!text.equals(last)) {
            last = text;
            area.setText(text);
            area.setCaretPosition(0);
        }
    }

    @Override
    public void onShow() { update(); }

    @Override
    public void onTick() { update(); }
}
