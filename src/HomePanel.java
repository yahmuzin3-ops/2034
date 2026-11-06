import javax.swing.*;
import java.awt.*;

/** 홈: 현황 요약 + 게임 로그 + 하루 넘기기 */
public class HomePanel extends ScreenPanel {
    private final JLabel summary = new JLabel(" ");
    private final JTextArea logArea = UI.logArea();
    private final JCheckBox autoCheck = new JCheckBox("시간 자동 흐름");

    public HomePanel(MainFrame frame) {
        super(frame, "홈");

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(summary, BorderLayout.CENTER);
        JButton next = UI.button("하루 넘기기");
        next.addActionListener(e -> frame.getEngine().advanceDay());
        autoCheck.setOpaque(false);
        autoCheck.setSelected(true);
        autoCheck.addActionListener(e -> frame.getEngine().setAutoDay(autoCheck.isSelected()));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(autoCheck);
        right.add(next);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.CENTER);

        JPanel logBox = new JPanel(new BorderLayout());
        logBox.setOpaque(false);
        logBox.setBorder(BorderFactory.createTitledBorder("게임 로그 (자율 행동 · 이벤트 · 경기 결과)"));
        logBox.add(new JScrollPane(logArea), BorderLayout.CENTER);
        logBox.setPreferredSize(new Dimension(0, 380));
        add(logBox, BorderLayout.SOUTH);

        for (String s : GameLog.recent()) UI.appendLog(logArea, s);
        GameLog.addListener(s -> UI.appendLog(logArea, s));
        GameLog.post("[시작] Rise of Champions - 펜서를 키워 세계 챔피언에 도전하세요!");
    }

    private void update() {
        GameState s = state();
        summary.setText("<html>보유 선수 <b>" + s.getRoster().size() + "</b>명 &nbsp;|&nbsp; 승리 <b>"
                + s.getWins() + "</b>회 (" + s.getMatches() + "전) &nbsp;|&nbsp; 날씨 <b>"
                + s.getWeather().getDisplayName() + "</b><br><br>"
                + "왼쪽 메뉴에서 훈련, 상점, 경기를 이용해보세요.<br>"
                + "선수들은 스스로 쉬고, 먹고, 연습해요. "
                + (frame.getEngine().isAutoDay() ? "(" + GameEngine.DAY_MILLIS / 1000 + "초 = 하루)"
                : "(시간은 '하루 넘기기'를 눌러야 흘러요)") + "</html>");
    }

    @Override
    public void onShow() {
        autoCheck.setSelected(frame.getEngine().isAutoDay());
        update();
    }

    @Override
    public void onTick() { update(); }
}
