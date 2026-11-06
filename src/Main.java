import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            // 어떤 OS에서도 색상이 똑같이 보이도록 Metal 룩앤필 사용
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
