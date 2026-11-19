import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 기록실: 트로피 룸(메달·우승컵·업적), 라이벌(상대 전적), 경기 통계 */
public class RecordsPanel extends ScreenPanel {
    private final TrophyView trophy = new TrophyView();
    private final RivalsView rivals = new RivalsView();
    private final StatsView stats = new StatsView();
    private String lastSig = "";

    public RecordsPanel(MainFrame frame) {
        super(frame, "기록실");
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("트로피 룸", trophy);
        tabs.addTab("라이벌", rivals);
        tabs.addTab("경기 통계", stats);
        add(tabs, BorderLayout.CENTER);
    }

    private String signature(GameState s) {
        return s.getMedals().size() + "/" + s.getRivalRecords().size() + "/" + s.getMatchStats().size() + "/" + s.getNation()
                + "/" + s.getRivalRecords().stream().mapToInt(RivalRecord::games).sum();
    }

    private void refresh(boolean force) {
        GameState s = state();
        String sig = signature(s);
        if (!force && sig.equals(lastSig)) return;
        lastSig = sig;
        trophy.refresh(s);
        rivals.refresh(s);
        stats.refresh(s);
    }

    @Override
    public void onShow() { refresh(true); }

    @Override
    public void onTick() { refresh(false); }

    @Override
    public void onStateReplaced() { lastSig = ""; }

    // =================================================================== 트로피 룸

    private static class Item {
        final Rectangle2D.Double box = new Rectangle2D.Double();
        MedalRecord medal;
        boolean cup;
        String tip;
    }

    private static class TrophyView extends JPanel {
        private final JLabel summary = new JLabel(" ");
        private final Case cabinet = new Case();

        TrophyView() {
            setLayout(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            add(summary, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(cabinet);
            sp.setBorder(BorderFactory.createLineBorder(new Color(90, 60, 30), 3));
            sp.getVerticalScrollBar().setUnitIncrement(18);
            add(sp, BorderLayout.CENTER);
        }

        void refresh(GameState s) {
            List<MedalRecord> medals = s.getMedals();
            int g = 0, si = 0, br = 0;
            for (MedalRecord m : medals) {
                if (m.getRank() == 1) g++;
                else if (m.getRank() == 2) si++;
                else br++;
            }
            int year = SeasonCalendar.yearOf(s.getDay());
            summary.setText("<html><b>금 " + g + " · 은 " + si + " · 동 " + br + "</b>   |   이번 시즌 메달 포인트 <b>" + s.seasonPoints(year) + "</b>   |   업적 <b>"
                    + unlocked(s).size() + "/" + Achievement.values().length + "</b></html>");
            cabinet.setData(medals, unlocked(s));
        }

        private static List<Achievement> unlocked(GameState s) {
            List<Achievement> list = new ArrayList<>();
            for (Achievement a : Achievement.values()) if (s.hasAchievement(a)) list.add(a);
            return list;
        }
    }

    /** 진열장: 위 칸에는 우승컵, 아래 칸에는 메달, 맨 아래에는 업적 명패 */
    private static class Case extends JPanel {
        private static final int CW = 112, CH = 150, PAD = 24;
        private List<MedalRecord> medals = new ArrayList<>();
        private List<Achievement> badges = new ArrayList<>();
        private final List<Item> items = new ArrayList<>();

        Case() {
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        void setData(List<MedalRecord> medals, List<Achievement> badges) {
            this.medals = new ArrayList<>(medals);
            java.util.Collections.reverse(this.medals);   // 최근 것이 앞에
            this.badges = badges;
            layoutItems(Math.max(500, getWidth()));
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int cols = Math.max(1, (Math.max(500, getWidth()) - PAD * 2) / CW);
            int cups = 0, rest = medals.size();
            for (MedalRecord m : medals) if (m.getRank() == 1) cups++;
            int rows = (cups == 0 ? 0 : (cups + cols - 1) / cols) + (rest == 0 ? 0 : (rest + cols - 1) / cols);
            int badgeRows = badges.isEmpty() ? 0 : (badges.size() + 3) / 4;
            return new Dimension(500, 90 + Math.max(1, rows) * (CH + 18) + badgeRows * 42 + 70);
        }

        private void layoutItems(int width) {
            items.clear();
            int cols = Math.max(1, (width - PAD * 2) / CW);
            int y = 70, col = 0;
            // 1단: 우승컵 (금메달)
            List<MedalRecord> golds = new ArrayList<>();
            for (MedalRecord m : medals) if (m.getRank() == 1) golds.add(m);
            for (MedalRecord m : golds) {
                Item it = new Item();
                it.medal = m;
                it.cup = true;
                it.box.setRect(PAD + col * CW, y, CW - 8, CH);
                it.tip = tip(m, true);
                items.add(it);
                if (++col >= cols) {
                    col = 0;
                    y += CH + 18;
                }
            }
            if (!golds.isEmpty() && col != 0) y += CH + 18;
            col = 0;
            // 2단: 모든 메달
            for (MedalRecord m : medals) {
                Item it = new Item();
                it.medal = m;
                it.box.setRect(PAD + col * CW, y, CW - 8, CH);
                it.tip = tip(m, false);
                items.add(it);
                if (++col >= cols) {
                    col = 0;
                    y += CH + 18;
                }
            }
        }

        private static String tip(MedalRecord m, boolean cup) {
            return "<html><b>" + m.getRegion().getDisplayName() + " " + m.getWeapon().getDisplayName() + "부 " + (m.isTeam() ? "단체전" : "개인전") + "</b><br>"
                    + (cup ? "우승컵 · " : "") + m.getMedalName() + "<br>" + SeasonCalendar.dateText(m.getDay()) + " (" + m.getDay() + "일차)<br>"
                    + m.getWho() + "</html>";
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            for (Item it : items) if (it.box.contains(e.getPoint())) return it.tip;
            return null;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            layoutItems(w);
            // 진열장 배경 (어두운 나무 + 유리)
            g.setPaint(new GradientPaint(0, 0, new Color(60, 40, 24), 0, h, new Color(38, 26, 18)));
            g.fillRect(0, 0, w, h);
            g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 26), w, h, new Color(255, 255, 255, 4)));
            g.fillRect(0, 0, w, h);

            g.setFont(getFont().deriveFont(Font.BOLD, 20f));
            g.setColor(new Color(245, 210, 120));
            g.drawString("트로피 룸", PAD, 36);
            g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g.setColor(new Color(210, 190, 150));
            g.drawString("우승컵과 메달에 마우스를 올려 보세요.", PAD, 56);

            if (medals.isEmpty()) {
                g.setFont(getFont().deriveFont(Font.PLAIN, 15f));
                g.setColor(new Color(220, 205, 175));
                g.drawString("아직 딴 메달이 없어요. 자국 선수권, 세계 선수권, 올림픽에서 메달에 도전해 보세요!", PAD, 120);
            }
            // 선반
            int lastRowY = 0;
            for (Item it : items) lastRowY = Math.max(lastRowY, (int) it.box.y);
            Map<Integer, Boolean> shelves = new LinkedHashMap<>();
            for (Item it : items) shelves.put((int) it.box.y, true);
            for (int sy : shelves.keySet()) {
                g.setPaint(new GradientPaint(0, sy + CH, new Color(120, 82, 46), 0, sy + CH + 10, new Color(78, 52, 30)));
                g.fillRoundRect(10, sy + CH, w - 20, 10, 4, 4);
                g.setColor(new Color(0, 0, 0, 50));
                g.fillRect(14, sy + CH + 10, w - 28, 4);
            }
            for (Item it : items) {
                if (it.cup) drawCup(g, it);
                else drawMedal(g, it);
            }
            // 업적 명패
            int by = (items.isEmpty() ? 150 : lastRowY + CH + 56);
            g.setFont(getFont().deriveFont(Font.BOLD, 16f));
            g.setColor(new Color(245, 210, 120));
            g.drawString("업적", PAD, by);
            int bx = PAD, bw = Math.max(180, (w - PAD * 2 - 30) / 4), bi = 0;
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            for (Achievement a : badges) {
                int px = PAD + (bi % 4) * (bw + 10), py = by + 12 + (bi / 4) * 40;
                g.setPaint(new GradientPaint(px, py, new Color(214, 170, 70), px, py + 32, new Color(150, 108, 36)));
                g.fill(new RoundRectangle2D.Double(px, py, bw, 32, 10, 10));
                g.setColor(new Color(255, 236, 170));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Double(px + 2, py + 2, bw - 4, 28, 8, 8));
                g.setColor(new Color(50, 32, 8));
                String t = "★ " + a.getDisplayName();
                FontMetrics fm = g.getFontMetrics();
                while (fm.stringWidth(t) > bw - 14 && t.length() > 3) t = t.substring(0, t.length() - 1);
                g.drawString(t, px + 8, py + 21);
                bi++;
            }
            if (badges.isEmpty()) {
                g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
                g.setColor(new Color(210, 190, 150));
                g.drawString("달성한 업적이 여기에 걸려요.", PAD, by + 30);
            }
            g.dispose();
        }

        private void drawLabel(Graphics2D g, Item it) {
            MedalRecord m = it.medal;
            double cx = it.box.x + it.box.width / 2;
            FlagArt.draw(g, m.getNation(), cx - 14, it.box.y + 96, 28, 19);   // 메달/컵 아래, 글자 위
            g.setFont(getFont().deriveFont(Font.BOLD, 11f));
            FontMetrics fm = g.getFontMetrics();
            String l1 = m.getRegion().getDisplayName();
            while (fm.stringWidth(l1) > it.box.width && l1.length() > 2) l1 = l1.substring(0, l1.length() - 1);
            g.setColor(new Color(245, 232, 200));
            g.drawString(l1, (float) (cx - fm.stringWidth(l1) / 2.0), (float) (it.box.y + CH - 24));
            g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            fm = g.getFontMetrics();
            String l2 = m.getWeapon().getDisplayName() + " " + (m.isTeam() ? "단체" : "개인");
            g.setColor(new Color(205, 188, 150));
            g.drawString(l2, (float) (cx - fm.stringWidth(l2) / 2.0), (float) (it.box.y + CH - 8));
        }

        private static Color medalColor(int rank) {
            return rank == 1 ? new Color(245, 200, 50) : rank == 2 ? new Color(205, 210, 225) : new Color(200, 130, 70);
        }

        private void drawMedal(Graphics2D g, Item it) {
            double cx = it.box.x + it.box.width / 2, top = it.box.y + 6;
            Color c = medalColor(it.medal.getRank());
            g.setColor(new Color(200, 50, 60));
            Path2D rib = new Path2D.Double();
            rib.moveTo(cx - 14, top);
            rib.lineTo(cx - 3, top + 34);
            rib.lineTo(cx + 3, top + 34);
            rib.lineTo(cx - 6, top);
            rib.closePath();
            g.fill(rib);
            g.setColor(new Color(40, 80, 190));
            Path2D rib2 = new Path2D.Double();
            rib2.moveTo(cx + 14, top);
            rib2.lineTo(cx + 3, top + 34);
            rib2.lineTo(cx - 3, top + 34);
            rib2.lineTo(cx + 6, top);
            rib2.closePath();
            g.fill(rib2);
            double r = 24, my = top + 34;
            g.setPaint(new GradientPaint((float) (cx - r), (float) my, c.brighter(), (float) (cx + r), (float) (my + 2 * r), c.darker()));
            g.fill(new Ellipse2D.Double(cx - r, my, 2 * r, 2 * r));
            g.setColor(c.darker().darker());
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(cx - r, my, 2 * r, 2 * r));
            g.draw(new Ellipse2D.Double(cx - r + 6, my + 6, 2 * r - 12, 2 * r - 12));
            g.setFont(getFont().deriveFont(Font.BOLD, 22f));
            FontMetrics fm = g.getFontMetrics();
            String n = String.valueOf(it.medal.getRank());
            g.drawString(n, (float) (cx - fm.stringWidth(n) / 2.0), (float) (my + r + 8));
            drawLabel(g, it);
        }

        private void drawCup(Graphics2D g, Item it) {
            double cx = it.box.x + it.box.width / 2, top = it.box.y + 6;
            Color gold = new Color(245, 200, 50);
            g.setPaint(new GradientPaint((float) (cx - 28), (float) top, new Color(255, 236, 140), (float) (cx + 28), (float) (top + 50), new Color(190, 140, 20)));
            Path2D bowl = new Path2D.Double();   // 컵
            bowl.moveTo(cx - 28, top);
            bowl.lineTo(cx + 28, top);
            bowl.curveTo(cx + 28, top + 40, cx + 12, top + 48, cx + 5, top + 52);
            bowl.lineTo(cx - 5, top + 52);
            bowl.curveTo(cx - 12, top + 48, cx - 28, top + 40, cx - 28, top);
            bowl.closePath();
            g.fill(bowl);
            g.setColor(new Color(215, 165, 30));
            g.setStroke(new BasicStroke(3.5f));   // 손잡이
            g.draw(new Arc2D.Double(cx - 42, top + 4, 22, 26, 90, 180, Arc2D.OPEN));
            g.draw(new Arc2D.Double(cx + 20, top + 4, 22, 26, -90, 180, Arc2D.OPEN));
            g.setPaint(new GradientPaint((float) cx, (float) top + 52, gold, (float) cx, (float) top + 64, new Color(170, 120, 20)));
            g.fill(new Rectangle2D.Double(cx - 5, top + 52, 10, 14));    // 기둥
            g.fill(new RoundRectangle2D.Double(cx - 24, top + 64, 48, 12, 6, 6));   // 받침
            g.setColor(new Color(70, 40, 10));
            g.fill(new Rectangle2D.Double(cx - 18, top + 70, 36, 6));
            g.setColor(new Color(255, 250, 220, 200));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(cx - 18, top + 8, cx - 16, top + 30));   // 반짝임
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(110, 70, 10));
            g.drawString("★", (float) (cx - 6), (float) (top + 24));
            drawLabel(g, it);
        }
    }

    // =================================================================== 라이벌

    private static class RivalsView extends JPanel {
        private final JLabel head = new JLabel(" ");
        private final JTable table = UI.table("", "상대", "종목", "전적", "득점 : 실점", "연속", "관계", "마지막 경기", "상대 레이팅");
        private final JTextArea detail = UI.logArea();
        private List<RivalRecord> rows = new ArrayList<>();
        private Map<RivalRecord, Rival> rivalOf = new LinkedHashMap<>();

        RivalsView() {
            setLayout(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            add(head, BorderLayout.NORTH);
            table.getColumnModel().getColumn(0).setMaxWidth(44);
            table.getColumnModel().getColumn(0).setMinWidth(44);
            int[] widths = {44, 170, 60, 70, 90, 60, 100, 250, 90};
            for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                    super.getTableCellRendererComponent(t, col == 0 ? null : v, sel, focus, row, col);
                    setIcon(col == 0 && v instanceof Icon ? (Icon) v : null);
                    setHorizontalAlignment(col == 1 || col == 7 ? LEFT : CENTER);
                    if (!sel && row >= 0 && row < rows.size()) {
                        String st = rows.get(row).status();
                        setBackground("천적".equals(st) ? new Color(255, 226, 226) : "라이벌".equals(st) ? new Color(255, 244, 205)
                                : "상대 우위".equals(st) ? new Color(226, 244, 230) : "복수 대상".equals(st) ? new Color(235, 228, 252) : Color.WHITE);
                    }
                    return this;
                }
            });
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), new JScrollPane(detail));
            split.setResizeWeight(0.72);
            add(split, BorderLayout.CENTER);
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) showDetail();
            });
        }

        void refresh(GameState s) {
            rows = s.getRivalRecords();
            rivalOf.clear();
            for (RivalRecord r : rows) {
                for (Rival rv : s.getWorld().getRivals(r.weapon)) {
                    if (rv.getName().equals(r.name) && rv.getNation().equals(r.nation)) {
                        rivalOf.put(r, rv);
                        break;
                    }
                }
            }
            int rival = 0, nemesis = 0, revenge = 0;
            for (RivalRecord r : rows) {
                String st = r.status();
                if ("라이벌".equals(st)) rival++;
                else if ("천적".equals(st)) nemesis++;
                else if ("복수 대상".equals(st)) revenge++;
            }
            head.setText(rows.isEmpty()
                    ? "<html>아직 상대 전적이 없어요. 대회 <b>개인전</b>에서 컴퓨터 선수를 만나면 전적이 쌓여요.</html>"
                    : "<html>만난 상대 <b>" + rows.size() + "명</b> · 라이벌 " + rival + " · 천적 " + nemesis + " · 복수 대상 " + revenge
                    + "<br><font color='gray'>3경기 이상 겨루면 라이벌, 3패 이상 앞서 지면 천적이에요. 복수 대상이 다시 대회에 나오면 알려 줘요.</font></html>");
            UI.model(table).setRowCount(0);
            for (RivalRecord r : rows) {
                Rival rv = rivalOf.get(r);
                UI.model(table).addRow(new Object[]{FlagArt.icon(r.nation, 28, 19), r.name + " (" + r.nation + ")", r.weapon.getDisplayName(), r.recordText(),
                        r.pointsFor + " : " + r.pointsAgainst, r.streak > 0 ? r.streak + "연승" : r.streak < 0 ? (-r.streak) + "연패" : "-", r.status(),
                        SeasonCalendar.dateText(r.lastDay) + "  " + (r.lastWon ? "승 " : "패 ") + r.lastMy + ":" + r.lastOpp,
                        rv == null ? "-" : Math.round(rv.getRating())});
            }
            showDetail();
        }

        private void showDetail() {
            int row = table.getSelectedRow();
            if (row < 0 || row >= rows.size()) {
                detail.setText(rows.isEmpty() ? "" : "표에서 상대를 선택하면 자세한 전적이 나와요.");
                return;
            }
            RivalRecord r = rows.get(row);
            Rival rv = rivalOf.get(r);
            StringBuilder sb = new StringBuilder();
            sb.append(r.name).append(" (").append(FencingWorld.nationName(r.nation)).append(") - ").append(r.weapon.getDisplayName()).append("부\n");
            sb.append("전적 ").append(r.recordText()).append(" (승률 ").append(r.games() == 0 ? 0 : Math.round(100.0 * r.wins / r.games())).append("%), 평균 ")
                    .append(String.format("%.1f : %.1f", r.pointsFor / (double) Math.max(1, r.games()), r.pointsAgainst / (double) Math.max(1, r.games()))).append("점\n");
            sb.append("처음 만난 날: ").append(SeasonCalendar.dateText(r.firstDay)).append("\n");
            sb.append("마지막 경기: ").append(SeasonCalendar.dateText(r.lastDay)).append(" - ").append(r.lastWon ? "승리" : "패배").append(" ").append(r.lastMy).append(" : ").append(r.lastOpp).append("\n");
            if (rv != null) sb.append("지금 레이팅 ").append(Math.round(rv.getRating())).append(" (").append(RatingSystem.tier(rv.getRating())).append(", ").append(rv.getPersonality().getDisplayName()).append(")\n");
            sb.append("관계: ").append(r.status());
            detail.setText(sb.toString());
            detail.setCaretPosition(0);
        }
    }

    // =================================================================== 경기 통계

    private static class StatsView extends JPanel {
        private final JLabel summary = new JLabel(" ");
        private final FormChart form = new FormChart();
        private final TacticChart tactics = new TacticChart();
        private final JTable weaponTable = UI.table("종목", "경기", "전적", "승률", "평균 득점", "평균 실점");
        private final JTable kindTable = UI.table("구분", "경기", "전적", "승률", "평균 득점", "평균 실점");
        private final JTable fencerTable = UI.table("선수", "구간", "득점", "실점", "득실률");
        private final JTextArea ways = UI.logArea();

        StatsView() {
            setLayout(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            add(summary, BorderLayout.NORTH);
            JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
            grid.setOpaque(false);
            grid.add(form);
            grid.add(tactics);
            JTabbedPane low = new JTabbedPane();
            low.addTab("종목별", new JScrollPane(weaponTable));
            low.addTab("대회별", new JScrollPane(kindTable));
            JTabbedPane low2 = new JTabbedPane();
            low2.addTab("득점 방식", new JScrollPane(ways));
            low2.addTab("선수별", new JScrollPane(fencerTable));
            grid.add(low);
            grid.add(low2);
            add(grid, BorderLayout.CENTER);
        }

        private static String pct(int a, int b) { return b == 0 ? "-" : Math.round(100.0 * a / b) + "%"; }

        private static String avg(int sum, int n) { return n == 0 ? "-" : String.format("%.1f", sum / (double) n); }

        void refresh(GameState s) {
            List<MatchStat> list = s.getMatchStats();
            int n = list.size(), wins = 0, pf = 0, pa = 0, best = 0, run = 0, sdW = 0, sdL = 0;
            int rip = 0, dbl = 0, ex = 0, off = 0, touches = 0, ripAg = 0, exAg = 0;
            for (MatchStat m : list) {
                if (m.won) {
                    wins++;
                    run++;
                    best = Math.max(best, run);
                } else run = 0;
                pf += m.myScore;
                pa += m.oppScore;
                if (m.sudden) {
                    if (m.won) sdW++;
                    else sdL++;
                }
                rip += m.ripostesFor;
                ripAg += m.ripostesAgainst;
                dbl += m.doubles;
                ex += m.exitsFor;
                exAg += m.exitsAgainst;
                off += m.offTargets;
                touches += m.touchesFor;
            }
            if (n == 0) {
                summary.setText("<html>아직 경기 통계가 없어요. 경기를 치르면 이곳에 쌓여요. (최근 400경기까지 기록해요)</html>");
            } else {
                summary.setText("<html>최근 <b>" + n + "경기</b> · 승률 <b>" + pct(wins, n) + "</b> (" + wins + "승 " + (n - wins) + "패) · 평균 <b>" + avg(pf, n) + " : " + avg(pa, n)
                        + "</b> · 현재 " + (run > 0 ? run + "연승" : "연승 없음") + " (최고 " + best + "연승)" + (sdW + sdL > 0 ? " · 서든데스 " + sdW + "승 " + sdL + "패" : "") + "</html>");
            }
            form.setData(list);
            tactics.setData(list);

            // 종목별 / 대회별
            fill(weaponTable, list, 0);
            fill(kindTable, list, 1);

            // 득점 방식
            StringBuilder sb = new StringBuilder();
            if (n == 0) sb.append("경기를 치르면 득점 방식이 정리돼요.");
            else {
                sb.append("내 득점 ").append(touches).append("점 중\n");
                sb.append("  · 방어 후 리포스트 득점 ").append(rip).append("점 (").append(pct(rip, touches)).append(")\n");
                sb.append("  · 동시타(에페) ").append(dbl).append("점\n");
                sb.append("  · 상대 후방 라인 이탈로 얻은 점수 ").append(ex).append("점\n\n");
                sb.append("내준 점수 중\n");
                sb.append("  · 상대 리포스트에 당한 점수 ").append(ripAg).append("점\n");
                sb.append("  · 내가 후방 라인을 벗어나 내준 점수 ").append(exAg).append("점\n\n");
                sb.append("무효 타격(백색등) ").append(off).append("번");
            }
            ways.setText(sb.toString());
            ways.setCaretPosition(0);

            // 선수별
            Map<String, int[]> per = new LinkedHashMap<>();
            for (MatchStat m : list) {
                for (Map.Entry<String, int[]> e : m.perFencer.entrySet()) {
                    int[] a = per.computeIfAbsent(e.getKey(), k -> new int[3]);
                    for (int i = 0; i < 3; i++) a[i] += e.getValue()[i];
                }
            }
            UI.model(fencerTable).setRowCount(0);
            for (Map.Entry<String, int[]> e : per.entrySet()) {
                int[] a = e.getValue();
                UI.model(fencerTable).addRow(new Object[]{e.getKey(), a[0], a[1], a[2], a[1] + a[2] == 0 ? "-" : Math.round(100.0 * a[1] / (a[1] + a[2])) + "%"});
            }
        }

        private void fill(JTable table, List<MatchStat> list, int mode) {
            Map<String, int[]> g = new LinkedHashMap<>();   // {경기, 승, 득점, 실점}
            if (mode == 0) for (WeaponType w : WeaponType.values()) g.put(w.getDisplayName(), new int[4]);
            else {
                g.put("일반 경기", new int[4]);
                g.put("자국 선수권", new int[4]);
                g.put("세계 선수권", new int[4]);
                g.put("올림픽", new int[4]);
            }
            for (MatchStat m : list) {
                String key;
                if (mode == 0) key = m.weapon == null ? null : m.weapon.getDisplayName();
                else key = m.region == Region.NATIONAL ? "자국 선수권" : m.region == Region.WORLD ? "세계 선수권" : m.region == Region.OLYMPIC ? "올림픽" : "일반 경기";
                if (key == null) continue;
                int[] a = g.computeIfAbsent(key, k -> new int[4]);
                a[0]++;
                if (m.won) a[1]++;
                a[2] += m.myScore;
                a[3] += m.oppScore;
            }
            UI.model(table).setRowCount(0);
            for (Map.Entry<String, int[]> e : g.entrySet()) {
                int[] a = e.getValue();
                UI.model(table).addRow(new Object[]{e.getKey(), a[0], a[1] + "승 " + (a[0] - a[1]) + "패", pct(a[1], a[0]), avg(a[2], a[0]), avg(a[3], a[0])});
            }
        }
    }

    /** 최근 경기의 점수 차 막대: 이기면 위쪽(초록), 지면 아래쪽(빨강) */
    private static class FormChart extends JPanel {
        private List<MatchStat> data = new ArrayList<>();

        FormChart() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(200, 205, 220)));
            setPreferredSize(new Dimension(300, 200));
        }

        void setData(List<MatchStat> list) {
            data = list.size() > 20 ? new ArrayList<>(list.subList(list.size() - 20, list.size())) : list;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(60, 66, 94));
            g.drawString("최근 " + data.size() + "경기 점수 차 (위쪽 승리 / 아래쪽 패배)", 12, 20);
            if (data.isEmpty()) {
                g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
                g.setColor(Color.GRAY);
                g.drawString("경기 기록이 없어요.", 12, h / 2);
                g.dispose();
                return;
            }
            int top = 34, bottom = h - 22, mid = (top + bottom) / 2;
            int maxAbs = 3;
            for (MatchStat m : data) maxAbs = Math.max(maxAbs, Math.abs(m.margin()));
            double unit = (mid - top - 4) / (double) maxAbs;
            g.setColor(new Color(215, 220, 235));
            g.drawLine(10, mid, w - 10, mid);
            int slot = (w - 24) / 20, bw = Math.max(6, slot - 4);
            for (int i = 0; i < data.size(); i++) {
                MatchStat m = data.get(i);
                int x = 12 + i * slot;
                int len = (int) Math.round(Math.abs(m.margin()) * unit);
                len = Math.max(len, 3);
                g.setColor(m.won ? new Color(70, 175, 105) : new Color(220, 85, 85));
                if (m.margin() >= 0) g.fillRoundRect(x, mid - len, bw, len, 4, 4);
                else g.fillRoundRect(x, mid, bw, len, 4, 4);
            }
            g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            g.setColor(Color.GRAY);
            g.drawString("오래된 경기", 12, h - 6);
            String r = "최근";
            g.drawString(r, w - 12 - g.getFontMetrics().stringWidth(r), h - 6);
            g.dispose();
        }
    }

    /** 전술별 사용 비율과 득점 성공률 */
    private static class TacticChart extends JPanel {
        private int[] used = new int[6], scored = new int[6], opp = new int[6];

        TacticChart() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(200, 205, 220)));
            setPreferredSize(new Dimension(300, 200));
        }

        void setData(List<MatchStat> list) {
            used = new int[6];
            scored = new int[6];
            opp = new int[6];
            for (MatchStat m : list) {
                for (int i = 0; i < 6; i++) {
                    used[i] += m.used[i];
                    scored[i] += m.scored[i];
                    opp[i] += m.oppUsed[i];
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(60, 66, 94));
            g.drawString("전술 사용 비율 (파랑: 나, 회색: 상대) · 득점 성공률", 12, 20);
            int total = 0, otot = 0;
            for (int i = 0; i < 6; i++) {
                total += used[i];
                otot += opp[i];
            }
            if (total == 0) {
                g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
                g.setColor(Color.GRAY);
                g.drawString("경기 기록이 없어요.", 12, h / 2);
                g.dispose();
                return;
            }
            Battle.Tactic[] ts = Battle.Tactic.values();
            int rowH = (h - 40) / 6, labelW = 92, barMax = w - labelW - 130;
            for (int i = 0; i < 6; i++) {
                int y = 32 + i * rowH;
                g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
                g.setColor(Color.DARK_GRAY);
                g.drawString(ts[i].getDisplayName(), 12, y + rowH / 2 + 2);
                double mine = used[i] / (double) total, theirs = otot == 0 ? 0 : opp[i] / (double) otot;
                g.setColor(new Color(70, 110, 215));
                g.fillRoundRect(labelW, y + 3, (int) Math.max(2, mine * barMax * 2), Math.max(6, rowH / 2 - 4), 5, 5);
                g.setColor(new Color(170, 176, 195));
                g.fillRoundRect(labelW, y + rowH / 2 + 1, (int) Math.max(2, theirs * barMax * 2), Math.max(4, rowH / 2 - 6), 5, 5);
                String t = Math.round(mine * 100) + "%  득점 " + scored[i] + (used[i] > 0 ? " (" + Math.round(100.0 * scored[i] / used[i]) + "%)" : "");
                g.setColor(new Color(60, 66, 94));
                g.drawString(t, w - 126, y + rowH / 2 + 2);
            }
            g.dispose();
        }
    }
}
