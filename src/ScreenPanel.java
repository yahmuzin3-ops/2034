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
        add(t, BorderLayout.NORTH);
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
