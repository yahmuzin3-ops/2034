import javax.swing.*;
import java.awt.*;

/** 홈: 현황 요약 + 게임 로그 + 하루 넘기기 */
public class HomePanel extends ScreenPanel {
    private final JLabel summary = new JLabel(" ");
    private final JTextArea logArea = UI.logArea();

    public HomePanel(MainFrame frame) {
        super(frame, "홈");

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(summary, BorderLayout.CENTER);
        JButton next = UI.button("하루 넘기기");
        next.addActionListener(e -> frame.getEngine().advanceDay());
        top.add(next, BorderLayout.EAST);
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
                + "선수들은 스스로 쉬고, 먹고, 연습해요. (" + GameEngine.DAY_MILLIS / 1000 + "초 = 하루)</html>");
    }

    @Override
    public void onShow() { update(); }

    @Override
    public void onTick() { update(); }
}
