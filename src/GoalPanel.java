import javax.swing.*;
import java.awt.*;

/** 목표/업적 + 대회(지역) 해금 현황 */
public class GoalPanel extends ScreenPanel {
    private final JTextArea area = UI.logArea();
    private String last = "";

    public GoalPanel(MainFrame frame) {
        super(frame, "목표 · 의뢰");
        area.setFont(area.getFont().deriveFont(15f));
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    private void update() {
        GameState s = state();
        StringBuilder sb = new StringBuilder();
        sb.append("[ 오늘의 의뢰 ]  (달성하면 보상을 바로 받아요, 하루가 지나면 새 의뢰로 바뀌어요)\n");
        for (Mission m : s.getMissions()) {
            sb.append(m.isDone() ? " [완료] " : " [    ] ").append(m.describe())
                    .append("  (").append(m.getProgress()).append("/").append(m.getTarget()).append(")  보상 ")
                    .append(m.getReward()).append("G\n");
        }
        sb.append("\n[ 업적 ]\n");
        for (Achievement a : Achievement.values()) {
            sb.append(s.hasAchievement(a) ? " [완료] " : " [    ] ")
                    .append(a.getDisplayName()).append(" - ").append(a.getDescription())
                    .append(" (보상 ").append(a.getReward()).append("G)\n");
        }
        sb.append("\n[ 종목별 대회 ]  전체 ").append(s.getWins()).append("승\n");
        for (WeaponType w : WeaponType.values()) {
            sb.append("\n ").append(w.getDisplayName()).append("부  (").append(s.getWins(w)).append("승)\n");
            for (Region r : Region.values()) {
                sb.append("   ").append(r.getDisplayName()).append(r.isTournament() ? " (토너먼트)  " : " (적 " + r.getLevelRange() + ")  ");
                if (s.isCleared(r, w)) sb.append("우승 경험 있음");
                else if (r.isUnlocked(s.getWins(w))) sb.append("도전 가능");
                else sb.append("잠김 (").append(r.getRequiredWins()).append("승 필요)");
                sb.append("\n");
            }
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
