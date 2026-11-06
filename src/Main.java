import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        try {
            // 어떤 OS에서도 색상이 똑같이 보이도록 Metal 룩앤필 사용
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        // 모든 컴포넌트(팝업 창 포함)에 한글 폰트 적용
        FontUIResource font = new FontUIResource(new Font("Malgun Gothic", Font.PLAIN, 14));
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource) UIManager.put(key, font);
        }

        SwingUtilities.invokeLater(() -> {
            GameState state = null;
            if (SaveManager.exists()) {
                int r = JOptionPane.showConfirmDialog(null, "저장된 게임이 있어요. 이어서 할까요?",
                        "이어하기", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    try {
                        state = SaveManager.load();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "불러오기에 실패해서 새 게임으로 시작해요.");
                    }
                }
            }
            if (state == null) state = GameState.newGame();
            new MainFrame(state).setVisible(true);
        });
    }
}
