import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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

    private final ToastPane toastPane = new ToastPane();
    private final Map<String, JToggleButton> navButtons = new LinkedHashMap<>();
    private final Map<String, String> navLabels = new LinkedHashMap<>();
    private final List<String> menuKeys = new ArrayList<>();
    private final JLabel nationLabel = new JLabel();
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
        JScrollPane sideScroll = new JScrollPane(createSideMenu(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(null);
        sideScroll.getViewport().setBackground(SIDE_BG);
        sideScroll.getVerticalScrollBar().setUnitIncrement(16);
        sideScroll.setPreferredSize(new Dimension(206, 0));
        add(sideScroll, BorderLayout.WEST);
        add(cardPanel, BorderLayout.CENTER);

        setGlassPane(toastPane);
        GameLog.addListener(this::toastFromLog);   // 업적/의뢰 달성은 화면 위에 알림으로 띄운다
        registerScreens();
        showScreen("home");
        updateTopBar();
        engine.start();
        installShortcuts();
        new javax.swing.Timer(3 * 60 * 1000, e -> autosave()).start();   // 3분마다 자동 저장
        if (state.consumeShowGuide()) SwingUtilities.invokeLater(() -> HelpDialog.show(this, HelpDialog.START));   // 새 게임이면 시작 가이드

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

    /** 달성/메달 알림 팝업 */
    public void toast(String title, String msg, Color accent) {
        SwingUtilities.invokeLater(() -> toastPane.show(title, msg, accent));
    }

    private void toastFromLog(String msg) {
        int i = msg.indexOf("목표 달성: ");
        if (i >= 0) {
            toast("업적 달성!", msg.substring(i + "목표 달성: ".length()), GOLD);
        } else if (msg.startsWith("[시즌] ") && (msg.contains("개막") || msg.contains("3일 뒤"))) {
            toast("시즌 일정", msg.substring("[시즌] ".length()), new Color(110, 200, 255));
        } else if (msg.startsWith("[스태프] ") && msg.contains("떠났어요")) {
            toast("스태프 이탈", msg.substring("[스태프] ".length()), new Color(255, 150, 110));
        } else if (msg.startsWith("[의뢰] 완료!")) {
            toast("의뢰 완료!", msg.substring("[의뢰] 완료! ".length()), new Color(110, 220, 140));
        } else if (msg.startsWith("[대회] ") && (msg.endsWith("우승!") || msg.contains("(동메달)") || msg.endsWith("준우승"))) {
            toast("메달 획득!", msg.substring("[대회] ".length()), new Color(255, 205, 70));
        }
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
        for (JLabel l : new JLabel[]{nationLabel, goldLabel, dayLabel, weatherLabel}) {
            l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
            l.setForeground(Color.WHITE);
            info.add(l);
        }
        bar.add(info, BorderLayout.EAST);
        return bar;
    }

    private void updateTopBar() {
        String nat = state.getNation();
        if (!nat.equals(nationLabel.getName())) {   // 소속 국가가 바뀔 때만 국기를 다시 만든다
            nationLabel.setName(nat);
            nationLabel.setIcon(FlagArt.icon(nat, 27, 18));
            nationLabel.setText(" " + FencingWorld.shortName(nat));
        }
        updateBadges();
        goldLabel.setText("재화 " + state.getGold() + "G");
        dayLabel.setText(state.getDay() + "일차");
        weatherLabel.setText("날씨 " + state.getWeather().getDisplayName() + " (내일 " + state.getForecast().getDisplayName() + ")");
    }

    private JPanel createSideMenu() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDE_BG);
        side.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        String[][] menus = {
                {"home", "홈"}, {"athletes", "선수 관리"}, {"training", "훈련"}, {"staff", "코치 · 스태프"}, {"shop", "상점"},
                {"inventory", "인벤토리"}, {"match", "경기 · 대회"}, {"calendar", "시즌 캘린더"}, {"ranking", "파워 랭킹"},
                {"records", "기록실"}, {"facility", "시설"}, {"goals", "목표 · 의뢰"}, {"history", "경기 기록"},
        };
        for (int i = 0; i < menus.length; i++) {
            String[] m = menus[i];
            JToggleButton nb = createNavButton(m[0], m[1]);
            nb.setToolTipText(m[1] + (i < 9 ? "  (Alt+" + (i + 1) + ")" : ""));
            navButtons.put(m[0], nb);
            navLabels.put(m[0], m[1]);
            menuKeys.add(m[0]);
            side.add(nb);
            side.add(Box.createVerticalStrut(5));
        }
        side.add(Box.createVerticalGlue());
        side.add(createActionButton("도움말 (F1)", this::showHelp));
        side.add(Box.createVerticalStrut(8));
        side.add(createActionButton("저장 (Ctrl+S)", this::saveGame));
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
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
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
        addScreen("ranking", new RankingPanel(this));
        addScreen("records", new RecordsPanel(this));
        addScreen("calendar", new CalendarPanel(this));
        addScreen("staff", new StaffPanel(this));
        addScreen("facility", new FacilityPanel(this));
        addScreen("goals", new GoalPanel(this));
        addScreen("history", new HistoryPanel(this));
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

    // ---------- 도움말 / 단축키 / 메뉴 알림 ----------

    public void showHelp() {
        ScreenPanel p = screens.get(currentKey);
        HelpDialog.show(this, p == null ? HelpDialog.START : p.helpTopic());
    }

    private void installShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "help", this::showHelp);
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save", this::saveGame);
        for (int i = 0; i < 9 && i < menuKeys.size(); i++) {
            final String key = menuKeys.get(i);
            bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, InputEvent.ALT_DOWN_MASK), "nav" + i, () -> showScreen(key));
        }
    }

    private static void bind(InputMap im, ActionMap am, KeyStroke ks, String name, Runnable r) {
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { r.run(); }
        });
    }

    /** 대회가 열렸거나 급한 일이 있으면 메뉴 이름 옆에 ●, ! 를 붙인다. */
    private void updateBadges() {
        Map<String, String> badges = Advisor.cachedBadges(state);
        for (Map.Entry<String, JToggleButton> e : navButtons.entrySet()) {
            String badge = badges.get(e.getKey());
            String text = navLabels.get(e.getKey()) + (badge == null ? "" : "   " + badge);
            if (!text.equals(e.getValue().getText())) e.getValue().setText(text);
        }
    }

    /** 조용히 자동 저장한다. (실패해도 게임은 계속된다) */
    public void autosave() {
        try {
            synchronized (state) {
                SaveManager.autosave(state);
            }
            GameLog.post("[저장] 자동 저장했어요.");
        } catch (Exception ignored) {
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
        List<File> files = new ArrayList<>();
        if (SaveManager.FILE.exists()) files.add(SaveManager.FILE);
        if (SaveManager.AUTO_FILE.exists()) files.add(SaveManager.AUTO_FILE);
        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, "저장된 게임이 없어요.", "불러오기", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File pick = files.get(0);
        if (files.size() > 1) {   // 직접 저장과 자동 저장이 모두 있으면 고른다
            Object[] labels = {"직접 저장", "자동 저장", "취소"};
            int c = JOptionPane.showOptionDialog(this, "어느 저장을 불러올까요?\n\n" + SaveManager.describe(SaveManager.FILE) + "\n" + SaveManager.describe(SaveManager.AUTO_FILE)
                    + "\n\n(현재 진행 상황은 사라져요)", "불러오기", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, labels,
                    SaveManager.newest() == SaveManager.AUTO_FILE ? labels[1] : labels[0]);
            if (c != 0 && c != 1) return;
            pick = c == 0 ? SaveManager.FILE : SaveManager.AUTO_FILE;
        } else {
            int r = JOptionPane.showConfirmDialog(this, "저장된 게임을 불러올까요?\n\n" + SaveManager.describe(pick) + "\n\n(현재 진행 상황은 사라져요)",
                    "불러오기", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }
        try {
            GameState loaded = SaveManager.load(pick);
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
        Object[] labels = {"저장하고 종료", "저장 안 하고 종료", "취소"};
        int r = JOptionPane.showOptionDialog(this, "게임을 종료할까요?\n\n'저장 안 하고 종료'를 누르면 마지막 저장 이후의 진행은 사라져요.", "종료",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
        if (r != 0 && r != 1) return;
        if (r == 0) {
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
