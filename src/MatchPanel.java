import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 경기 · 대회: 팀 편성/대회 선택 화면 + 턴제 전투 화면 */
public class MatchPanel extends ScreenPanel {
    private static final String SETUP = "setup";
    private static final String BATTLE = "battle";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    // 편성 화면
    private final DefaultListModel<Fencer> teamModel = new DefaultListModel<>();
    private final JList<Fencer> teamList = UI.fencerList(teamModel);
    private final JComboBox<Region> regionCombo = new JComboBox<>(Region.values());
    private final JLabel setupMsg = new JLabel(" ");

    // 전투 화면
    private Battle battle;
    private final JPanel enemyBox = new JPanel();
    private final JPanel playerBox = new JPanel();
    private final Map<Fencer, JProgressBar> bars = new HashMap<>();
    private final JLabel turnLabel = new JLabel(" ");
    private final JTextArea logArea = UI.logArea();
    private final JComboBox<Fencer> targetCombo = UI.fencerCombo();
    private final JButton atkBtn = UI.button("공격 (찌르기)");
    private final JButton defBtn = UI.button("방어 (파라드)");
    private final JButton skillBtn = UI.button("특수 기술");
    private final JButton itemBtn = UI.button("아이템");
    private final JButton giveUpBtn = UI.button("기권");

    public MatchPanel(MainFrame frame) {
        super(frame, "경기 · 대회");
        root.setOpaque(false);
        root.add(buildSetup(), SETUP);
        root.add(buildBattle(), BATTLE);
        add(root, BorderLayout.CENTER);
    }

    // ---------- 편성 화면 ----------

    private JPanel buildSetup() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        regionCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Region) {
                    Region r = (Region) value;
                    String lock = r.isUnlocked(state().getWins()) ? "" : "  [잠김: " + r.getRequiredWins() + "승 필요]";
                    setText(r.getDisplayName() + "  (적 " + r.getLevelRange() + ", 보상 " + r.getRewardGold() + "G)" + lock);
                }
                return this;
            }
        });
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(new JLabel("도전할 대회"), BorderLayout.WEST);
        top.add(regionCombo, BorderLayout.CENTER);
        p.add(top, BorderLayout.NORTH);

        teamList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane sp = new JScrollPane(teamList);
        sp.setBorder(BorderFactory.createTitledBorder("출전 선수 선택 (Ctrl/Shift 로 최대 3명)"));
        p.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bottom.add(setupMsg, BorderLayout.CENTER);
        JButton start = UI.button("경기 시작");
        start.addActionListener(e -> startBattle());
        bottom.add(start, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void startBattle() {
        List<Fencer> sel = teamList.getSelectedValuesList();
        if (sel.isEmpty() || sel.size() > 3) {
            setupMsg.setText("출전 선수를 1~3명 선택해주세요.");
            return;
        }
        Region r = (Region) regionCombo.getSelectedItem();
        if (r == null || !r.isUnlocked(state().getWins())) {
            setupMsg.setText("아직 잠긴 대회예요. 더 많이 승리해보세요!");
            return;
        }
        for (Fencer f : sel) {
            if (f.isBusy()) {
                setupMsg.setText(f.getName() + "은(는) 이미 시합 중이에요.");
                return;
            }
        }
        battle = new Battle(state(), r, sel);
        logArea.setText("");
        bars.clear();
        enemyBox.removeAll();
        playerBox.removeAll();
        for (Fencer e : battle.getEnemies()) addRow(enemyBox, e, new Color(210, 80, 80));
        for (Fencer p : battle.getPlayers()) addRow(playerBox, p, new Color(70, 150, 220));
        enemyBox.revalidate();
        playerBox.revalidate();
        cards.show(root, BATTLE);
        refreshBattle();
    }

    // ---------- 전투 화면 ----------

    private JPanel buildBattle() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 16f));
        p.add(turnLabel, BorderLayout.NORTH);

        enemyBox.setLayout(new BoxLayout(enemyBox, BoxLayout.Y_AXIS));
        playerBox.setLayout(new BoxLayout(playerBox, BoxLayout.Y_AXIS));
        enemyBox.setBorder(BorderFactory.createTitledBorder("상대 팀"));
        playerBox.setBorder(BorderFactory.createTitledBorder("우리 팀"));
        JPanel left = new JPanel(new GridLayout(2, 1, 0, 8));
        left.setOpaque(false);
        left.add(enemyBox);
        left.add(playerBox);
        left.setPreferredSize(new Dimension(380, 0));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("경기 로그"));
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.add(left, BorderLayout.WEST);
        center.add(logScroll, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(new JLabel("대상:"));
        targetCombo.setPreferredSize(new Dimension(230, 30));
        actions.add(targetCombo);
        actions.add(atkBtn);
        actions.add(defBtn);
        actions.add(skillBtn);
        actions.add(itemBtn);
        actions.add(giveUpBtn);
        p.add(actions, BorderLayout.SOUTH);

        atkBtn.addActionListener(e -> doAction(Battle.Action.ATTACK));
        defBtn.addActionListener(e -> doAction(Battle.Action.DEFEND));
        skillBtn.addActionListener(e -> doAction(Battle.Action.SKILL));
        itemBtn.addActionListener(e -> useItem());
        giveUpBtn.addActionListener(e -> {
            if (battle == null) return;
            int r = JOptionPane.showConfirmDialog(this, "정말 기권할까요?", "기권", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                battle.surrender();
                refreshBattle();
            }
        });
        return p;
    }

    private void addRow(JPanel box, Fencer f, Color color) {
        JProgressBar b = new JProgressBar(0, f.getMaxHp());
        b.setStringPainted(true);
        b.setForeground(color);
        b.setPreferredSize(new Dimension(340, 26));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        box.add(b);
        box.add(Box.createVerticalStrut(6));
        bars.put(f, b);
    }

    private void doAction(Battle.Action action) {
        if (battle == null) return;
        Fencer target = (Fencer) targetCombo.getSelectedItem();
        if ((action == Battle.Action.ATTACK || action == Battle.Action.SKILL) && target == null) return;
        battle.act(action, target, null);
        refreshBattle();
    }

    private void useItem() {
        if (battle == null || battle.currentActor() == null) return;
        List<Item> items = new ArrayList<>(state().getInventory().snapshot().keySet());
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "사용할 수 있는 아이템이 없어요.");
            return;
        }
        Item it = (Item) JOptionPane.showInputDialog(this, "사용할 아이템을 선택하세요.", "아이템",
                JOptionPane.QUESTION_MESSAGE, null, items.toArray(), items.get(0));
        if (it == null) return;
        List<Fencer> allies = new ArrayList<>();
        for (Fencer p : battle.getPlayers()) if (!p.isDown()) allies.add(p);
        Fencer ally = (Fencer) JOptionPane.showInputDialog(this, "대상 선수를 선택하세요.", "아이템",
                JOptionPane.QUESTION_MESSAGE, null, allies.toArray(), allies.get(0));
        if (ally == null) return;
        battle.act(Battle.Action.ITEM, ally, it);
        refreshBattle();
    }

    private void refreshBattle() {
        if (battle == null) return;
        for (Map.Entry<Fencer, JProgressBar> e : bars.entrySet()) {
            Fencer f = e.getKey();
            JProgressBar b = e.getValue();
            b.setMaximum(f.getMaxHp());
            b.setValue(f.getHp());
            b.setString(f.getName() + " (" + f.getWeapon().getDisplayName() + " Lv." + f.getLevel() + ")  HP "
                    + f.getHp() + "/" + f.getMaxHp() + (f.isDown() ? " [전투불능]" : ""));
        }
        for (String s : battle.drainLog()) UI.appendLog(logArea, s);

        if (battle.isOver()) {
            finishBattle();
            return;
        }
        Fencer actor = battle.currentActor();
        if (actor == null) return;
        turnLabel.setText("라운드 " + battle.getRound() + " - " + actor.getName() + "의 차례");

        Fencer sel = (Fencer) targetCombo.getSelectedItem();
        targetCombo.removeAllItems();
        for (Fencer en : battle.getEnemies()) if (!en.isDown()) targetCombo.addItem(en);
        if (sel != null && !sel.isDown()) targetCombo.setSelectedItem(sel);

        boolean ready = battle.skillReady(actor);
        skillBtn.setText(actor.getWeapon().getSpecialSkill() + (ready ? "" : " (쿨타임 " + battle.getCooldown(actor) + ")"));
        skillBtn.setEnabled(ready);
    }

    private void finishBattle() {
        String summary = battle.settle();
        battle = null;
        JOptionPane.showMessageDialog(this, summary, "경기 결과", JOptionPane.INFORMATION_MESSAGE);
        for (String line : summary.split("\n")) GameLog.post("[경기] " + line);
        cards.show(root, SETUP);
        refreshSetup();
    }

    private void refreshSetup() {
        List<Fencer> selected = teamList.getSelectedValuesList();
        teamModel.clear();
        for (Fencer f : state().getRoster()) teamModel.addElement(f);
        for (Fencer f : selected) {
            int i = teamModel.indexOf(f);
            if (i >= 0) teamList.addSelectionInterval(i, i);
        }
        setupMsg.setText(" ");
        regionCombo.repaint();
    }

    @Override
    public void onShow() {
        if (battle == null) {
            cards.show(root, SETUP);
            refreshSetup();
        }
    }

    @Override
    public void onStateReplaced() {
        battle = null;
        cards.show(root, SETUP);
        teamModel.clear();
    }
}
