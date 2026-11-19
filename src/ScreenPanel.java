import javax.swing.*;
import java.awt.*;

/**
 * 모든 화면(메뉴)의 부모 클래스.
 *  - onShow() : 화면이 열릴 때 (목록 새로고침)
 *  - onTick() : 0.5초마다 (상태 바 같은 값만 갱신)
 *  - onStateReplaced() : 불러오기로 게임 상태가 통째로 바뀌었을 때
 */
public class ScreenPanel extends JPanel {
    protected final MainFrame frame;

    protected ScreenPanel(MainFrame frame, String title) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        setBackground(UI.BG);
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 24f));
        JButton help = new JButton("? 도움말");   // 모든 화면에서 같은 자리에 있다
        help.setFocusPainted(false);
        help.setToolTipText("이 화면에 맞는 도움말을 열어요 (F1)");
        help.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        help.addActionListener(e -> HelpDialog.show(this, helpTopic()));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(t, BorderLayout.CENTER);
        head.add(help, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);
    }

    /** 도움말 버튼을 눌렀을 때 열 탭 (화면마다 바꿔 쓴다) */
    protected String helpTopic() {
        return HelpDialog.START;
    }

    protected GameState state() {
        return frame.getGameState();
    }

    public void onShow() {
    }

    public void onTick() {
    }

    public void onStateReplaced() {
    }
}
