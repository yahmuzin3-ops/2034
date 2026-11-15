import javax.swing.*;
import java.awt.*;
import java.util.List;

/** 경기 기록: 종목별 승률 그래프 + 최근 경기 목록 */
public class HistoryPanel extends ScreenPanel {
    private final JTable table = UI.table("일차", "종목", "대회", "날씨", "점수", "결과");
    private final JLabel summary = new JLabel(" ");
    private final WinRateChart chart = new WinRateChart();
    private int lastCount = -1;

    public HistoryPanel(MainFrame frame) {
        super(frame, "경기 기록");

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(summary, BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(0, 150));
        top.add(chart, BorderLayout.CENTER);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder("최근 경기 (최신순)"));

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(sp, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void update() {
        List<MatchRecord> records = state().getHistory();
        if (records.size() == lastCount) return;   // 바뀐 게 없으면 다시 그리지 않는다
        lastCount = records.size();

        UI.model(table).setRowCount(0);
        for (int i = records.size() - 1; i >= 0 && i >= records.size() - 40; i--) {
            MatchRecord r = records.get(i);
            UI.model(table).addRow(new Object[]{r.getDay() + "일차",
                    r.getWeapon() == null ? "혼성" : r.getWeapon().getDisplayName(),
                    r.getRegion().getDisplayName() + (r.isBoss() ? " 결승" : ""),
                    r.getWeather() == null ? "-" : r.getWeather().getDisplayName(),
                    r.getPlayerScore() + " : " + r.getEnemyScore(),
                    r.isWon() ? "승리" : "패배"});
        }

        int wins = 0;
        double scored = 0, lost = 0;
        for (MatchRecord r : records) {
            if (r.isWon()) wins++;
            scored += r.getPlayerScore();
            lost += r.getEnemyScore();
        }
        int n = records.size();
        summary.setText(n == 0 ? "아직 기록이 없어요. 경기에 나가보세요!"
                : "<html>총 <b>" + n + "</b>경기 <b>" + wins + "</b>승 <b>" + (n - wins) + "</b>패 (승률 <b>"
                + Math.round(wins * 100.0 / n) + "%</b>) &nbsp;|&nbsp; 경기당 평균 득점 <b>"
                + String.format("%.1f", scored / n) + "</b> / 실점 <b>" + String.format("%.1f", lost / n) + "</b></html>");
        chart.setRecords(records);
    }

    @Override
    public void onShow() {
        lastCount = -1;
        update();
    }

    @Override
    public void onTick() { update(); }

    @Override
    public void onStateReplaced() { lastCount = -1; }

    /** 종목별 승률 막대 + 최근 12경기 결과 */
    private static class WinRateChart extends JPanel {
        private List<MatchRecord> records = new java.util.ArrayList<>();

        void setRecords(List<MatchRecord> r) {
            records = r;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            Color[] colors = {new Color(70, 130, 230), new Color(60, 175, 105), new Color(225, 75, 75)};
            int y = 8;
            int barX = 190, barW = Math.max(100, w / 2 - 80);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            WeaponType[] ws = WeaponType.values();
            for (int i = 0; i < ws.length; i++) {
                int wins = 0, total = 0;
                for (MatchRecord r : records) {
                    if (r.getWeapon() == ws[i]) {
                        total++;
                        if (r.isWon()) wins++;
                    }
                }
                double rate = total == 0 ? 0 : wins / (double) total;
                g.setColor(new Color(50, 56, 80));
                g.drawString(ws[i].getDisplayName() + "부  " + wins + "승 " + (total - wins) + "패", 8, y + 16);
                g.setColor(new Color(215, 219, 232));
                g.fillRoundRect(barX, y + 3, barW, 18, 10, 10);
                g.setColor(colors[i]);
                g.fillRoundRect(barX, y + 3, (int) (barW * rate), 18, 10, 10);
                g.setColor(new Color(50, 56, 80));
                g.drawString(total == 0 ? "-" : Math.round(rate * 100) + "%", barX + barW + 10, y + 17);
                y += 30;
            }
            // 최근 12경기: 초록(승) / 빨강(패)
            int x = w / 2 + 120;
            g.setColor(new Color(50, 56, 80));
            g.drawString("최근 경기", x, 24);
            int start = Math.max(0, records.size() - 12);
            for (int i = start; i < records.size(); i++) {
                boolean won = records.get(i).isWon();
                g.setColor(won ? new Color(70, 180, 110) : new Color(220, 85, 85));
                int bx = x + (i - start) * 24;
                g.fillRoundRect(bx, 34, 20, 30, 8, 8);
                g.setColor(Color.WHITE);
                g.drawString(won ? "승" : "패", bx + 3, 54);
            }
            g.dispose();
        }
    }
}
