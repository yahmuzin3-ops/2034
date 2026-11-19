import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** 시즌 캘린더: 1시즌은 120일. 자국 선수권, 세계 선수권, 올림픽이 열리는 시기를 보여준다. */
public class CalendarPanel extends ScreenPanel {
    private final JLabel head = new JLabel(" ");
    private final YearBar bar = new YearBar();
    private final JTable upcoming = UI.table("대회", "기간", "상태", "내 선수 출전");
    private final JTable seasons = UI.table("시즌", "금", "은", "동", "메달 포인트");
    private int lastDay = -1, lastMedals = -1;
    private String lastRoster = "";

    public CalendarPanel(MainFrame frame) {
        super(frame, "시즌 캘린더");
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        top.add(bar, BorderLayout.CENTER);

        upcoming.getColumnModel().getColumn(0).setPreferredWidth(110);
        upcoming.getColumnModel().getColumn(1).setPreferredWidth(240);
        upcoming.getColumnModel().getColumn(2).setPreferredWidth(110);
        upcoming.getColumnModel().getColumn(3).setPreferredWidth(300);
        seasons.getColumnModel().getColumn(0).setPreferredWidth(110);
        for (int i = 1; i <= 3; i++) seasons.getColumnModel().getColumn(i).setPreferredWidth(34);
        seasons.getColumnModel().getColumn(4).setPreferredWidth(80);
        JScrollPane up = new JScrollPane(upcoming);
        up.setBorder(BorderFactory.createTitledBorder("다가오는 대회"));
        JScrollPane se = new JScrollPane(seasons);
        se.setBorder(BorderFactory.createTitledBorder("시즌별 메달 성적"));
        se.setPreferredSize(new Dimension(330, 0));
        JPanel low = new JPanel(new BorderLayout(10, 0));
        low.setOpaque(false);
        low.add(up, BorderLayout.CENTER);
        low.add(se, BorderLayout.EAST);

        JLabel note = new JLabel("<html><div style='width:760px'><font color='gray'>동네 클럽·도 대회는 언제든 열려 있어요. 자국 선수권·세계 선수권·올림픽은 <b>대회 기간</b>에만 출전할 수 있고, "
                + "한 대회에는 종목마다 한 번만 나갈 수 있어요. 올림픽은 4시즌에 한 번 열려요.</font></div></html>");

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(low, BorderLayout.CENTER);
        center.add(note, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private void refresh(boolean force) {
        GameState s = state();
        StringBuilder rs = new StringBuilder();
        for (Fencer f : s.getRoster()) rs.append(f.getWeapon());
        if (!force && s.getDay() == lastDay && s.getMedals().size() == lastMedals && rs.toString().equals(lastRoster)) return;
        lastDay = s.getDay();
        lastMedals = s.getMedals().size();
        lastRoster = rs.toString();

        int day = s.getDay(), year = SeasonCalendar.yearOf(day);
        head.setText("<html><span style='font-size:15px'><b>" + SeasonCalendar.dateText(day) + "</b></span>  ·  " + day + "일차  ·  이번 시즌 메달 포인트 <b>" + s.seasonPoints(year) + "</b></html>");
        bar.setDay(day);

        UI.model(upcoming).setRowCount(0);
        List<SeasonCalendar.Event> evs = SeasonCalendar.between(day, day + SeasonCalendar.YEAR * 2);
        int shown = 0;
        for (SeasonCalendar.Event e : evs) {
            if (shown++ >= 9) break;
            String status = e.isOpen(day) ? "개최 중 (" + (e.end - day + 1) + "일 남음)" : (e.start - day) + "일 뒤";
            UI.model(upcoming).addRow(new Object[]{e.getName(), e.getPeriodText(), status, entryText(s, e)});
        }

        UI.model(seasons).setRowCount(0);
        for (int y = year; y >= Math.max(0, year - 5); y--) {
            int g = 0, si = 0, br = 0;
            for (MedalRecord m : s.getMedals()) {
                if (SeasonCalendar.yearOf(m.getDay()) != y) continue;
                if (m.getRank() == 1) g++;
                else if (m.getRank() == 2) si++;
                else br++;
            }
            UI.model(seasons).addRow(new Object[]{"시즌 " + (y + 1) + (y == year ? " (이번)" : ""), g, si, br, s.seasonPoints(y)});
        }
    }

    /** 열려 있는 대회라면 내 종목별로 출전 가능한지, 아니면 안내 문구 */
    private String entryText(GameState s, SeasonCalendar.Event e) {
        if (!e.isOpen(s.getDay())) return "대회 기간이 되면 출전할 수 있어요";
        List<String> ok = new ArrayList<>(), no = new ArrayList<>();
        for (WeaponType w : WeaponType.values()) {
            List<Fencer> team = new ArrayList<>();
            for (Fencer f : s.getRoster()) if (f.getWeapon() == w) team.add(f);
            if (team.isEmpty()) continue;
            String why = s.canEnter(e.region, w, team);
            if (why == null) ok.add(w.getDisplayName());
            else no.add(w.getDisplayName() + ": " + why);
        }
        if (!ok.isEmpty()) return "출전 가능: " + String.join(", ", ok) + (no.isEmpty() ? "" : "  /  " + no.get(0));
        return no.isEmpty() ? "내 선수가 없어요" : no.get(0);
    }

    @Override
    protected String helpTopic() { return HelpDialog.SEASON; }

    @Override
    public void onShow() { refresh(true); }

    @Override
    public void onTick() { refresh(false); }

    @Override
    public void onStateReplaced() { lastDay = -1; }

    // ----------------------------------------------------------- 한 해 막대

    private static class YearBar extends JPanel {
        private int day = 1;

        YearBar() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(200, 205, 220)));
            setPreferredSize(new Dimension(600, 210));
        }

        void setDay(int d) {
            day = d;
            repaint();
        }

        private static Color colorOf(Region r) {
            return r == Region.OLYMPIC ? new Color(235, 180, 40) : r == Region.WORLD ? new Color(140, 90, 200) : new Color(60, 120, 215);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int year = SeasonCalendar.yearOf(day);
            drawYear(g, year, 14, w, true);
            drawYear(g, year + 1, 112, w, false);
            // 범례
            int lx = w - 330;
            String[] names = {"자국 선수권", "세계 선수권", "올림픽"};
            Region[] regs = {Region.NATIONAL, Region.WORLD, Region.OLYMPIC};
            g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            for (int i = 0; i < 3; i++) {
                g.setColor(colorOf(regs[i]));
                g.fillRoundRect(lx + i * 108, 194, 14, 10, 4, 4);
                g.setColor(Color.DARK_GRAY);
                g.drawString(names[i], lx + i * 108 + 19, 204);
            }
            g.setColor(new Color(220, 70, 70));
            g.drawString("▼ 오늘", 14, 204);
            g.dispose();
        }

        private void drawYear(Graphics2D g, int year, int y, int w, boolean current) {
            int x0 = 20, x1 = w - 20, bw = x1 - x0, h = 34, ty = y + 26;
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(60, 66, 94));
            g.drawString("시즌 " + (year + 1) + (current ? " (이번 시즌)" : " (다음 시즌)"), x0, y + 12);
            // 12달 칸
            for (int m = 0; m < 12; m++) {
                int cx = x0 + bw * m / 12, cw = bw / 12;
                g.setColor(m % 2 == 0 ? new Color(238, 241, 250) : new Color(228, 232, 245));
                g.fillRect(cx, ty, cw + 1, h);
                g.setColor(new Color(130, 136, 160));
                g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
                g.drawString((m + 1) + "월", cx + 4, ty + h + 13);
            }
            g.setColor(new Color(190, 196, 215));
            g.drawRect(x0, ty, bw, h);
            int base = year * SeasonCalendar.YEAR;
            for (SeasonCalendar.Event e : SeasonCalendar.between(base + 1, base + SeasonCalendar.YEAR)) {
                if (e.year != year) continue;
                double s = (e.start - base - 1) / (double) SeasonCalendar.YEAR, f = (e.end - base) / (double) SeasonCalendar.YEAR;
                int ex = x0 + (int) Math.round(bw * s), ew = Math.max(8, (int) Math.round(bw * (f - s)));
                boolean open = e.isOpen(day);
                g.setColor(colorOf(e.region));
                g.fill(new RoundRectangle2D.Double(ex, ty + 3, ew, h - 6, 8, 8));
                if (open) {
                    g.setColor(new Color(255, 255, 255, 190));
                    g.setStroke(new BasicStroke(2.4f));
                    g.draw(new RoundRectangle2D.Double(ex + 1, ty + 4, ew - 2, h - 8, 7, 7));
                    g.setStroke(new BasicStroke(1f));
                }
                g.setFont(getFont().deriveFont(Font.BOLD, 11f));
                String label = e.region == Region.OLYMPIC ? "올림픽" : e.region == Region.WORLD ? "세계" : "자국";
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                if (fm.stringWidth(label) < ew - 4) g.drawString(label, ex + (ew - fm.stringWidth(label)) / 2, ty + h / 2 + 4);
                g.setColor(colorOf(e.region).darker());
                String d = ((e.start - base - 1) / SeasonCalendar.MONTH + 1) + "/" + ((e.start - base - 1) % SeasonCalendar.MONTH + 1);
                g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
                g.drawString(d, ex, ty - 3);
            }
            if (current) {   // 오늘 표시
                int doy = SeasonCalendar.dayOfYear(day);
                int tx = x0 + (int) Math.round(bw * (doy - 0.5) / SeasonCalendar.YEAR);
                g.setColor(new Color(220, 70, 70));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(tx, ty - 2, tx, ty + h + 2);
                Path2D tri = new Path2D.Double();
                tri.moveTo(tx - 6, ty - 12);
                tri.lineTo(tx + 6, ty - 12);
                tri.lineTo(tx, ty - 3);
                tri.closePath();
                g.fill(tri);
                g.setStroke(new BasicStroke(1f));
            }
        }
    }
}
