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
            java.io.File saved = SaveManager.newest();
            if (saved != null) {   // 가장 최근 저장(직접 저장 또는 자동 저장)을 이어서 할 수 있다
                int r = JOptionPane.showOptionDialog(null, "저장된 게임이 있어요.\n\n" + SaveManager.describe(saved) + "\n\n이어서 할까요?",
                        "이어하기", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"이어하기", "새 게임"}, "이어하기");
                if (r == 0) {
                    try {
                        state = SaveManager.load(saved);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "불러오기에 실패해서 새 게임으로 시작해요.");
                    }
                }
            }
            if (state == null) {   // 새 게임: 소속 국가를 고른다 (창을 닫으면 한국)
                state = GameState.newGame();
                String nation = NationChooser.choose(null, "KOR");
                if (nation != null) state.setNation(nation);
            }
            new MainFrame(state).setVisible(true);
        });
    }
}
