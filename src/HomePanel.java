import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/** 홈: 오늘의 추천(지금 할 일), 선수 컨디션, 하루 넘기기, 게임 로그 */
public class HomePanel extends ScreenPanel {
    private final JLabel summary = new JLabel(" ");
    private final JTextArea logArea = UI.logArea();
    private final JCheckBox autoCheck = new JCheckBox("시간 자동 흐름");
    private final JCheckBox soundCheck = new JCheckBox("효과음");
    private final JPanel tipBox = new TipBox();
    private JScrollPane tipScroll;
    private final ConditionList conditions = new ConditionList();
    private String tipSig = "";
    private int textWidth = 280;

    public HomePanel(MainFrame frame) {
        super(frame, "홈");

        // 위: 날짜·날씨 요약과 시간 조작
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(summary, BorderLayout.CENTER);
        JButton next = UI.button("하루 넘기기");
        next.setToolTipText("시간이 자동으로 흐르지 않을 때 직접 하루를 넘겨요.");
        next.addActionListener(e -> frame.getEngine().advanceDay());
        autoCheck.setOpaque(false);
        autoCheck.setSelected(true);
        autoCheck.setToolTipText("끄면 '하루 넘기기'를 눌러야만 시간이 흘러요.");
        autoCheck.addActionListener(e -> frame.getEngine().setAutoDay(autoCheck.isSelected()));
        soundCheck.setOpaque(false);
        soundCheck.setSelected(Sfx.isEnabled());
        soundCheck.addActionListener(e -> Sfx.setEnabled(soundCheck.isSelected()));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(soundCheck);
        right.add(autoCheck);
        right.add(next);
        top.add(right, BorderLayout.EAST);

        // 가운데: 왼쪽 오늘의 추천, 오른쪽 선수 컨디션
        tipBox.setOpaque(false);
        tipBox.setLayout(new BoxLayout(tipBox, BoxLayout.Y_AXIS));
        JScrollPane tipScroll = new JScrollPane(tipBox, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.tipScroll = tipScroll;
        tipScroll.setBorder(BorderFactory.createTitledBorder("오늘의 추천 - 지금 하면 좋은 일"));
        tipScroll.getViewport().setOpaque(false);
        tipScroll.setOpaque(false);
        tipScroll.getVerticalScrollBar().setUnitIncrement(16);

        conditions.setOnClick(() -> frame.showScreen("athletes"));
        JScrollPane condScroll = new JScrollPane(conditions);
        condScroll.setBorder(BorderFactory.createTitledBorder("선수 컨디션 (누르면 선수 관리로 이동)"));
        condScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setOpaque(false);
        center.add(tipScroll);
        center.add(condScroll);

        // 아래: 게임 로그
        JPanel logBox = new JPanel(new BorderLayout());
        logBox.setOpaque(false);
        logBox.setBorder(BorderFactory.createTitledBorder("게임 로그 (자율 행동 · 이벤트 · 경기 결과)"));
        logBox.add(new JScrollPane(logArea), BorderLayout.CENTER);
        logBox.setPreferredSize(new Dimension(0, 170));

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(center, BorderLayout.CENTER);
        body.add(logBox, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        for (String s : GameLog.recent()) UI.appendLog(logArea, s);
        GameLog.addListener(s -> UI.appendLog(logArea, s));
        GameLog.post("[시작] Rise of Champions - 펜서를 키워 세계 챔피언에 도전하세요!");
    }

    private void update() {
        GameState s = state();
        summary.setText("<html><b>" + SeasonCalendar.dateText(s.getDay()) + "</b> (" + s.getDay() + "일차) &nbsp;|&nbsp; 소속 <b>" + FencingWorld.nationName(s.getNation())
                + "</b> &nbsp;|&nbsp; 선수 <b>" + s.getRoster().size() + "</b>명 &nbsp;|&nbsp; 승리 <b>" + s.getWins() + "</b>회 (" + s.getMatches() + "전)<br>"
                + "날씨 <b>" + s.getWeather().getDisplayName() + "</b> - " + s.getWeather().getDayNote() + "  /  내일 예보 <b>" + s.getForecast().getDisplayName() + "</b><br>"
                + "<font color='gray'>경기 영향: " + s.getWeather().getBattleNote() + "<br>선수들은 스스로 쉬고, 먹고, 연습해요. "
                + (frame.getEngine().isAutoDay() ? GameEngine.DAY_MILLIS / 1000 + "초 = 하루" : "시간은 '하루 넘기기'를 눌러야 흘러요") + "</font></html>");
        updateTips(s);
        conditions.setFencers(s.getRoster());
    }

    private void updateTips(GameState s) {
        List<Advisor.Tip> tips = Advisor.cachedTips(s);
        int vw = tipScroll == null ? 0 : tipScroll.getViewport().getWidth();
        textWidth = vw > 0 ? Math.max(150, Math.min(560, vw - 280)) : 280;   // 카드 글자 폭: 창 너비에 맞춘다 (버튼이 잘리지 않게)
        StringBuilder sig = new StringBuilder().append(textWidth / 10).append('\n');
        for (Advisor.Tip t : tips) sig.append(t.title).append('|').append(t.level).append('\n');
        if (sig.toString().equals(tipSig)) return;   // 달라진 게 없으면 다시 만들지 않는다 (버튼 깜박임 방지)
        tipSig = sig.toString();
        tipBox.removeAll();
        for (Advisor.Tip t : tips) {
            tipBox.add(card(t));
            tipBox.add(Box.createVerticalStrut(6));
        }
        tipBox.revalidate();
        tipBox.repaint();
    }

    private static Color levelColor(int level) {
        return level == Advisor.URGENT ? new Color(220, 80, 80) : level == Advisor.IMPORTANT ? new Color(230, 160, 40) : new Color(90, 130, 210);
    }

    private JComponent card(Advisor.Tip t) {
        JPanel p = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g.setColor(levelColor(t.level));
                g.fill(new RoundRectangle2D.Double(0, 0, 8, getHeight() - 1, 8, 8));
                g.setColor(new Color(205, 210, 228));
                g.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 10));
        String tag = t.level == Advisor.URGENT ? "급해요" : t.level == Advisor.IMPORTANT ? "추천" : "참고";
        JLabel text = new JLabel("<html><div style='width:" + textWidth + "px'><font color='" + (t.level == Advisor.URGENT ? "#c03c3c" : t.level == Advisor.IMPORTANT ? "#b87812" : "#3f62b8") + "'><b>["
                + tag + "]</b></font> <b>" + t.title + "</b><br><font color='gray'>" + t.detail + "</font></div></html>");
        p.add(text, BorderLayout.CENTER);
        if (t.screen != null) {
            JButton go = UI.button(t.button == null ? "이동" : t.button);
            go.addActionListener(e -> {
                if ("help".equals(t.screen)) HelpDialog.show(this, HelpDialog.START);
                else frame.showScreen(t.screen);
            });
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setOpaque(false);
            wrap.add(go);
            p.add(wrap, BorderLayout.EAST);
        }
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 6));
        return p;
    }

    @Override
    public void onShow() {
        autoCheck.setSelected(frame.getEngine().isAutoDay());
        tipSig = "";
        update();
    }

    @Override
    public void onTick() { update(); }

    @Override
    public void onStateReplaced() { tipSig = ""; }

    /** 가로로는 항상 보이는 영역 폭에 맞춰지는 목록 */
    private static class TipBox extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }

        @Override
        public boolean getScrollableTracksViewportWidth() { return true; }

        @Override
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ----------------------------------------------------------- 선수 컨디션

    /** 선수마다 한 줄: 이름, 상태, 체력·피로·배고픔·기분 막대 */
    static class ConditionList extends JPanel {
        private static final int ROW = 74;
        private List<Fencer> fencers = new java.util.ArrayList<>();
        private Runnable onClick;

        ConditionList() {
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onClick != null) onClick.run();
                }
            });
        }

        void setOnClick(Runnable r) { onClick = r; }

        void setFencers(List<Fencer> list) {
            fencers = new java.util.ArrayList<>(list);
            setPreferredSize(new Dimension(300, Math.max(ROW, fencers.size() * ROW + 6)));
            revalidate();
            repaint();
        }

        private static Color bad(int v) {   // 높을수록 나쁜 값 (피로, 배고픔)
            return v >= 70 ? new Color(220, 80, 80) : v >= 40 ? new Color(235, 160, 50) : new Color(80, 175, 110);
        }

        private static Color good(int v) {   // 높을수록 좋은 값 (기분)
            return v < 30 ? new Color(220, 80, 80) : v < 60 ? new Color(235, 160, 50) : new Color(80, 175, 110);
        }

        private void bar(Graphics2D g, int x, int y, int w, String label, double ratio, Color c, String value) {
            g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            g.setColor(new Color(90, 96, 120));
            g.drawString(label, x, y + 10);
            g.setColor(new Color(228, 231, 242));
            g.fillRoundRect(x, y + 14, w, 8, 8, 8);
            g.setColor(c);
            g.fillRoundRect(x, y + 14, (int) Math.max(3, Math.min(1.0, ratio) * w), 8, 8, 8);
            g.setColor(new Color(60, 66, 94));
            g.drawString(value, x + w - g.getFontMetrics().stringWidth(value), y + 10);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (fencers.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("선수가 없어요. 선수 관리에서 영입해 보세요.", 14, 28);
                g.dispose();
                return;
            }
            int w = getWidth();
            for (int i = 0; i < fencers.size(); i++) {
                Fencer f = fencers.get(i);
                int y = 4 + i * ROW;
                if (i > 0) {
                    g.setColor(new Color(232, 235, 245));
                    g.drawLine(10, y - 2, w - 10, y - 2);
                }
                g.setColor(FencerArt.weaponColor(f.getWeapon()));
                g.fillRoundRect(10, y + 6, 6, ROW - 18, 6, 6);
                g.setFont(getFont().deriveFont(Font.BOLD, 14f));
                g.setColor(new Color(30, 36, 70));
                g.drawString(f.getName() + "  " + f.getWeapon().getDisplayName() + " Lv." + f.getLevel(), 24, y + 20);
                String tag = f.isInjured() ? "부상" : f.isBusy() ? "시합 중" : f.isTraining() ? "훈련 중" : f.isResting() ? "휴식 중" : "대기";
                Color tc = f.isInjured() ? new Color(220, 80, 80) : f.isBusy() ? new Color(140, 90, 200) : f.isTraining() ? new Color(70, 110, 215) : f.isResting() ? new Color(80, 175, 110) : new Color(130, 136, 160);
                g.setFont(getFont().deriveFont(Font.BOLD, 12f));
                int tw = g.getFontMetrics().stringWidth(tag);
                g.setColor(tc);
                g.fillRoundRect(w - tw - 28, y + 6, tw + 14, 18, 10, 10);
                g.setColor(Color.WHITE);
                g.drawString(tag, w - tw - 21, y + 19);
                int bw = (w - 40 - 30) / 4, bx = 24;
                bar(g, bx, y + 30, bw, "체력", f.getHp() / (double) Math.max(1, f.getMaxHp()), new Color(70, 130, 220), f.getHp() + "/" + f.getMaxHp());
                bar(g, bx + (bw + 10), y + 30, bw, "피로", f.getFatigue() / 100.0, bad(f.getFatigue()), String.valueOf(f.getFatigue()));
                bar(g, bx + 2 * (bw + 10), y + 30, bw, "배고픔", f.getHunger() / 100.0, bad(f.getHunger()), String.valueOf(f.getHunger()));
                bar(g, bx + 3 * (bw + 10), y + 30, bw, "기분", f.getMood() / 100.0, good(f.getMood()), String.valueOf(f.getMood()));
            }
            g.dispose();
        }
    }
}
