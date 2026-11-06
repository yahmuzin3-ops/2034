import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rise of Champions (Fencing) - 메인 화면
 * 상단 정보바 + 좌측 메뉴 + 중앙 화면 전환(CardLayout)
 */
public class MainFrame extends JFrame {

    private static final Color NAVY = new Color(28, 37, 65);
    private static final Color GOLD = new Color(240, 196, 25);
    private static final Color SIDE_BG = new Color(38, 50, 84);
    private static final Color SELECTED = new Color(70, 90, 150);

    private GameState state;
    private GameEngine engine;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final Map<String, ScreenPanel> screens = new LinkedHashMap<>();
    private final ButtonGroup navGroup = new ButtonGroup();
    private String currentKey = "home";

    private final JLabel goldLabel = new JLabel();
    private final JLabel dayLabel = new JLabel();
    private final JLabel weatherLabel = new JLabel();

    public MainFrame(GameState state) {
        this.state = state;
        this.engine = new GameEngine(state);

        setTitle("Rise of Champions - Fencing");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(createTopBar(), BorderLayout.NORTH);
        add(createSideMenu(), BorderLayout.WEST);
        add(cardPanel, BorderLayout.CENTER);

        registerScreens();
        showScreen("home");
        updateTopBar();
        engine.start();

        // 0.5초마다 상단바와 현재 화면의 상태 값을 갱신
        new javax.swing.Timer(500, e -> {
            updateTopBar();
            ScreenPanel p = screens.get(currentKey);
            if (p != null) p.onTick();
        }).start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exitGame();
            }
        });
    }

    public GameState getGameState() { return state; }

    public GameEngine getEngine() { return engine; }

    // ---------- 화면 구성 ----------

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(NAVY);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("Rise of Champions");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(GOLD);
        bar.add(title, BorderLayout.WEST);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        info.setOpaque(false);
        for (JLabel l : new JLabel[]{goldLabel, dayLabel, weatherLabel}) {
            l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
            l.setForeground(Color.WHITE);
            info.add(l);
        }
        bar.add(info, BorderLayout.EAST);
        return bar;
    }

    private void updateTopBar() {
        goldLabel.setText("재화 " + state.getGold() + "G");
        dayLabel.setText(state.getDay() + "일차");
        weatherLabel.setText("날씨 " + state.getWeather().getDisplayName());
    }

    private JPanel createSideMenu() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDE_BG);
        side.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        side.setPreferredSize(new Dimension(190, 0));

        String[][] menus = {
                {"home", "홈"}, {"athletes", "선수 관리"}, {"training", "훈련"}, {"shop", "상점"},
                {"inventory", "인벤토리"}, {"match", "경기 · 대회"}, {"facility", "시설"}, {"goals", "목표 · 업적"},
        };
        for (String[] m : menus) {
            side.add(createNavButton(m[0], m[1]));
            side.add(Box.createVerticalStrut(8));
        }
        side.add(Box.createVerticalGlue());
        side.add(createActionButton("저장", this::saveGame));
        side.add(Box.createVerticalStrut(8));
        side.add(createActionButton("불러오기", this::loadGame));
        side.add(Box.createVerticalStrut(8));
        side.add(createActionButton("종료", this::exitGame));
        return side;
    }

    private JToggleButton createNavButton(String key, String text) {
        JToggleButton btn = new JToggleButton(text);
        styleButton(btn, SIDE_BG);
        btn.setActionCommand(key);
        btn.addActionListener(e -> showScreen(key));
        btn.addChangeListener(e -> btn.setBackground(btn.isSelected() ? SELECTED : SIDE_BG));
        navGroup.add(btn);
        return btn;
    }

    private JButton createActionButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        styleButton(btn, new Color(55, 70, 115));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void styleButton(AbstractButton btn, Color bg) {
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ---------- 화면 등록 / 전환 ----------

    private void registerScreens() {
        addScreen("home", new HomePanel(this));
        addScreen("athletes", new AthletePanel(this));
        addScreen("training", new TrainingPanel(this));
        addScreen("shop", new ShopPanel(this));
        addScreen("inventory", new InventoryPanel(this));
        addScreen("match", new MatchPanel(this));
        addScreen("facility", new FacilityPanel(this));
        addScreen("goals", new GoalPanel(this));
    }

    private void addScreen(String key, ScreenPanel panel) {
        screens.put(key, panel);
        cardPanel.add(panel, key);
    }

    public void showScreen(String key) {
        ScreenPanel panel = screens.get(key);
        if (panel == null) return;
        currentKey = key;
        panel.onShow();
        cardLayout.show(cardPanel, key);
        for (Enumeration<AbstractButton> e = navGroup.getElements(); e.hasMoreElements(); ) {
            AbstractButton b = e.nextElement();
            if (key.equals(b.getActionCommand())) b.setSelected(true);
        }
    }

    // ---------- 저장 / 불러오기 / 종료 ----------

    private void saveGame() {
        try {
            SaveManager.save(state);
            GameLog.post("[저장] 게임을 저장했어요. (" + SaveManager.FILE.getAbsolutePath() + ")");
            JOptionPane.showMessageDialog(this, "저장했어요!", "저장", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "저장에 실패했어요: " + ex.getMessage(), "저장", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadGame() {
        if (!SaveManager.exists()) {
            JOptionPane.showMessageDialog(this, "저장된 게임이 없어요.", "불러오기", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "저장된 게임을 불러올까요?\n(현재 진행 상황은 사라져요)",
                "불러오기", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            GameState loaded = SaveManager.load();
            engine.stop();
            state = loaded;
            engine = new GameEngine(state);
            engine.start();
            for (ScreenPanel p : screens.values()) p.onStateReplaced();
            showScreen("home");
            updateTopBar();
            GameLog.post("[불러오기] 저장된 게임을 불러왔어요.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "불러오기에 실패했어요: " + ex.getMessage(), "불러오기", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exitGame() {
        int r = JOptionPane.showConfirmDialog(this, "저장하고 종료할까요?\n(아니오를 누르면 저장하지 않고 종료해요)",
                "종료", JOptionPane.YES_NO_CANCEL_OPTION);
        if (r == JOptionPane.CANCEL_OPTION || r == JOptionPane.CLOSED_OPTION) return;
        if (r == JOptionPane.YES_OPTION) {
            try {
                SaveManager.save(state);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "저장에 실패했어요: " + ex.getMessage());
                return;
            }
        }
        engine.stop();
        dispose();
        System.exit(0);
    }
}
