import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 토너먼트 진행 화면.
 *  - 예선 풀: 내 풀의 순위표와 내 경기 결과
 *  - 본선: 시드가 배정된 대진표 (내 경로를 강조하고, 올림픽은 동메달 결정전도 표시)
 *  - 다음 경기를 직접 하거나 시뮬레이션하거나, 대회를 포기할 수 있다.
 */
public class TournamentPanel extends JPanel {

    /** 버튼이 눌렸을 때 MatchPanel 이 처리한다. */
    public interface Actions {
        void play();

        void simulate();

        void simulateAll();

        void giveUp();
    }

    private static final String POOL = "pool", DE = "de";

    private final JLabel titleLabel = new JLabel(" ");
    private final JLabel stageLabel = new JLabel(" ");
    private final JLabel nextLabel = new JLabel(" ");
    private final JTable poolTable = UI.table("순위", "선수(팀)", "승", "패", "득점", "실점", "득실");
    private final JTextArea poolInfo = UI.logArea();
    private final BracketView bracket = new BracketView();
    private final JTextArea logArea = UI.logArea();
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JButton playBtn = UI.button("다음 경기 직접 하기");
    private final JButton simBtn = UI.button("다음 경기 시뮬레이션");
    private final JButton simAllBtn = UI.button("끝까지 시뮬레이션");
    private final JButton giveUpBtn = UI.button("대회 포기");
    private List<Tournament.Row> rows = new ArrayList<>();
    private Tournament.Entry meEntry;

    public TournamentPanel(Actions actions) {
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(titleLabel);
        head.add(stageLabel);
        add(head, BorderLayout.NORTH);

        // 예선 화면
        poolTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                boolean mine = row >= 0 && row < rows.size() && rows.get(row).entry == meEntry;
                setHorizontalAlignment(col == 1 ? LEFT : CENTER);
                setIcon(col == 1 && row >= 0 && row < rows.size() ? FlagArt.icon(rows.get(row).entry.nation, 22, 15) : null);
                if (!sel) {
                    boolean pass = row < 2;   // 상위 2명이 본선 진출
                    c.setBackground(mine ? new Color(255, 238, 170) : pass ? new Color(226, 244, 230) : Color.WHITE);
                    c.setFont(c.getFont().deriveFont(mine ? Font.BOLD : Font.PLAIN));
                }
                return c;
            }
        });
        JPanel poolView = new JPanel(new BorderLayout(0, 8));
        poolView.setOpaque(false);
        JScrollPane ps = new JScrollPane(poolTable);
        ps.setBorder(BorderFactory.createTitledBorder("내 풀 순위 (상위 2명이 본선 진출, 초록색)"));
        ps.setPreferredSize(new Dimension(0, 170));
        poolView.add(ps, BorderLayout.NORTH);
        JScrollPane pi = new JScrollPane(poolInfo);
        pi.setBorder(BorderFactory.createTitledBorder("내 풀 경기"));
        poolView.add(pi, BorderLayout.CENTER);

        center.setOpaque(false);
        center.add(poolView, POOL);
        JScrollPane bs = new JScrollPane(bracket);
        bs.setBorder(BorderFactory.createTitledBorder("본선 대진표 (노란색이 내 경로)"));
        center.add(bs, DE);

        JScrollPane ls = new JScrollPane(logArea);
        ls.setBorder(BorderFactory.createTitledBorder("대회 진행 기록"));
        ls.setPreferredSize(new Dimension(260, 0));
        JPanel mid = new JPanel(new BorderLayout(10, 0));
        mid.setOpaque(false);
        mid.add(center, BorderLayout.CENTER);
        mid.add(ls, BorderLayout.EAST);
        add(mid, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(10, 0));
        south.setOpaque(false);
        south.add(nextLabel, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(playBtn);
        btns.add(simBtn);
        btns.add(simAllBtn);
        btns.add(giveUpBtn);
        playBtn.setToolTipText("직접 전술을 골라 이번 경기를 치러요.");
        simBtn.setToolTipText("이번 경기만 레이팅으로 결과를 정해요.");
        simAllBtn.setToolTipText("남은 내 경기를 모두 레이팅으로 정하고 대회를 끝내요.");
        giveUpBtn.setToolTipText("보상 없이 대회를 끝내요.");
        south.add(btns, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        playBtn.addActionListener(e -> actions.play());
        simBtn.addActionListener(e -> actions.simulate());
        simAllBtn.addActionListener(e -> actions.simulateAll());
        giveUpBtn.addActionListener(e -> actions.giveUp());
    }

    public void refresh(Tournament t) {
        meEntry = t.getMe();
        titleLabel.setText(t.getTitle());
        stageLabel.setText(t.getStageText());

        if (t.getStage() == Tournament.Stage.POOL && t.getMyPool() != null) {
            rows = t.standings(t.getMyPool());
            UI.model(poolTable).setRowCount(0);
            for (int i = 0; i < rows.size(); i++) {
                Tournament.Row r = rows.get(i);
                UI.model(poolTable).addRow(new Object[]{i + 1, r.entry.name, r.wins, r.losses, r.ts, r.tr,
                        (r.indicator() > 0 ? "+" : "") + r.indicator()});
            }
            StringBuilder sb = new StringBuilder();
            sb.append("풀 경기는 5점 먼저 따면 이기는 경기예요. 풀 안에서 모두와 한 번씩 겨뤄요.\n"
                    + "순위는 승수, 득실 차, 득점 순이고, 이 성적이 본선 시드가 돼요.\n\n");
            int[] prog = t.getMyPoolProgress();
            sb.append("내 풀 경기 ").append(prog[0]).append("/").append(prog[1]).append(" 완료\n");
            cards.show(center, POOL);
            poolInfo.setText(sb.toString());
        } else {
            bracket.setTournament(t);
            cards.show(center, DE);
        }

        StringBuilder lg = new StringBuilder();
        for (String s : t.getLog()) lg.append(s).append("\n");
        logArea.setText(lg.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());

        Tournament.Match next = t.nextPlayerMatch();
        boolean can = next != null && !t.isFinished();
        if (can) {
            Tournament.Entry opp = next.other(t.getMe());
            nextLabel.setText("<html><b>다음 경기: " + next.round + (next.bronze ? " (동메달 결정전)" : "") + "</b>  vs " + opp.name
                    + "  (파워 레이팅 " + Math.round(opp.rating()) + ", 내 " + Math.round(t.getMe().rating()) + ")"
                    + (t.headToHead(opp).isEmpty() ? "" : "  [" + t.headToHead(opp) + "]") + "  -  "
                    + (t.isTeam() ? "단체전 릴레이" : next.target + "점 경기") + "</html>");
        } else {
            nextLabel.setText(t.isFinished() ? "대회가 끝났어요." : "내 경기는 모두 끝났어요. 결과를 정리하는 중...");
        }
        playBtn.setEnabled(can);
        simBtn.setEnabled(can);
        simAllBtn.setEnabled(can);
        giveUpBtn.setEnabled(!t.isFinished());
    }

    // ---------- 본선 대진표 ----------

    /** 토너먼트 대진표를 그린다. 라운드마다 한 열이고, 내 경로는 노란색이다. */
    private static class BracketView extends JPanel {
        private static final int BW = 188, BH = 44, GAP = 26, TOP = 40;
        private Tournament t;

        BracketView() {
            setBackground(Color.WHITE);
        }

        void setTournament(Tournament t) {
            this.t = t;
            int first = t.getRounds().isEmpty() ? 8 : t.getRounds().get(0).size();
            int cols = Integer.numberOfTrailingZeros(Integer.highestOneBit(Math.max(1, first * 2)));
            setPreferredSize(new Dimension(cols * (BW + GAP) + 30, first * (BH + 12) + TOP + (t.getBronze() != null ? 90 : 30)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (t == null || t.getRounds().isEmpty()) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            List<List<Tournament.Match>> rounds = t.getRounds();
            int firstSize = rounds.get(0).size();
            int cols = Integer.numberOfTrailingZeros(Integer.highestOneBit(Math.max(1, firstSize * 2)));
            double[][] cy = new double[cols][];
            for (int c = 0; c < cols; c++) {
                int n = firstSize >> c;
                cy[c] = new double[n];
                for (int i = 0; i < n; i++) {
                    cy[c][i] = c == 0 ? TOP + i * (BH + 12) + BH / 2.0 : (cy[c - 1][2 * i] + cy[c - 1][2 * i + 1]) / 2;
                }
            }

            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            for (int c = 0; c < cols; c++) {
                int x = 12 + c * (BW + GAP);
                String name = c < rounds.size() ? rounds.get(c).get(0).round : futureName(firstSize >> c);
                g.setColor(new Color(60, 66, 94));
                g.drawString(name, x, 22);
                for (int i = 0; i < cy[c].length; i++) {
                    Tournament.Match m = c < rounds.size() ? rounds.get(c).get(i) : null;
                    drawMatch(g, m, x, (int) (cy[c][i] - BH / 2.0));
                    if (c + 1 < cols) {   // 다음 라운드로 이어지는 선
                        g.setColor(new Color(150, 156, 180));
                        g.setStroke(new BasicStroke(1.5f));
                        double x1 = x + BW, y1 = cy[c][i], x2 = x + BW + GAP, y2 = cy[c + 1][i / 2];
                        g.draw(new Line2D.Double(x1, y1, x1 + GAP / 2.0, y1));
                        g.draw(new Line2D.Double(x1 + GAP / 2.0, y1, x1 + GAP / 2.0, y2));
                        g.draw(new Line2D.Double(x1 + GAP / 2.0, y2, x2, y2));
                    }
                }
            }
            if (t.getBronze() != null) {   // 올림픽 동메달 결정전: 결승 아래
                int x = 12 + (cols - 1) * (BW + GAP);
                int y = (int) (cy[cols - 1][0] + BH + 34);
                g.setColor(new Color(60, 66, 94));
                g.setFont(getFont().deriveFont(Font.BOLD, 13f));
                g.drawString("동메달 결정전", x, y - 8);
                drawMatch(g, t.getBronze(), x, y);
            }
            g.dispose();
        }

        private String futureName(int participants) {
            switch (participants) {
                case 4: return "준결승";
                case 2: return "결승";
                case 8: return "8강";
                default: return participants + "강";
            }
        }

        private void drawMatch(Graphics2D g, Tournament.Match m, int x, int y) {
            boolean mine = m != null && t != null && m.involves(t.getMe());
            g.setColor(mine ? new Color(255, 244, 196) : new Color(244, 246, 252));
            g.fillRoundRect(x, y, BW, BH, 10, 10);
            g.setColor(mine ? new Color(235, 180, 40) : new Color(180, 186, 210));
            g.setStroke(new BasicStroke(mine ? 2.4f : 1.4f));
            g.drawRoundRect(x, y, BW, BH, 10, 10);
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(215, 220, 235));
            g.drawLine(x + 4, y + BH / 2, x + BW - 4, y + BH / 2);
            if (m == null || m.a == null || m.b == null) {
                g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
                g.setColor(new Color(150, 156, 180));
                g.drawString("?", x + 10, y + 17);
                g.drawString("?", x + 10, y + 38);
                return;
            }
            drawLine(g, m, m.a, m.sa, x, y + 16);
            drawLine(g, m, m.b, m.sb, x, y + 38);
        }

        private void drawLine(Graphics2D g, Tournament.Match m, Tournament.Entry e, int score, int x, int baseline) {
            boolean won = m.played && m.winner == e;
            boolean me = e == t.getMe();
            g.setFont(getFont().deriveFont(won ? Font.BOLD : Font.PLAIN, 12f));
            g.setColor(me ? new Color(180, 110, 0) : m.played && !won ? new Color(150, 156, 175) : new Color(40, 44, 70));
            String name = e.name;
            FontMetrics fm = g.getFontMetrics();
            while (fm.stringWidth(name) > BW - 68 && name.length() > 3) name = name.substring(0, name.length() - 1);
            FlagArt.draw(g, e.nation, x + 8, baseline - 11, 18, 12);   // 선수 이름 앞에 국기
            g.drawString(name, x + 32, baseline);
            if (m.played) {
                String s = String.valueOf(score);
                g.drawString(s, x + BW - 10 - fm.stringWidth(s), baseline);
            }
        }
    }
}
