import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 경기 · 대회: 종목/대회/출전 선수 선택 화면 + 실제 펜싱 규칙의 경기 화면.
 * 매 구간마다 전술을 고르면 상대도 동시에 전술을 고르고, 규칙에 따라 결과가 정해진다.
 */
public class MatchPanel extends ScreenPanel {
    private static final String SETUP = "setup";
    private static final String BATTLE = "battle";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    // 편성 화면
    private final DefaultListModel<Fencer> teamModel = new DefaultListModel<>();
    private final JList<Fencer> teamList = UI.fencerList(teamModel);
    private final JComboBox<WeaponType> weaponCombo = new JComboBox<>(WeaponType.values());
    private final JComboBox<Region> regionCombo = new JComboBox<>(Region.values());
    private final JLabel setupMsg = new JLabel(" ");
    private final JCheckBox bossCheck = new JCheckBox("결승전(보스전) 도전 - 보상 2배");

    // 경기 화면
    private Battle battle;
    private boolean boutAuto = false;   // "이 경기 자동"
    private final FencingStage stage = new FencingStage();
    private final JLabel infoLabel = new JLabel(" ");
    private final JLabel scoutLabel = new JLabel(" ");
    private final JTextArea logArea = UI.logArea();
    private final Map<Battle.Tactic, JButton> tacticButtons = new EnumMap<>(Battle.Tactic.class);
    private final JButton continueBtn = UI.button("다음 경기 시작");
    private final JButton itemBtn = UI.button("아이템 (휴식 때)");
    private final JButton boutAutoBtn = UI.button("이 경기 자동");
    private final JButton giveUpBtn = UI.button("기권");
    private final JCheckBox autoCheck = new JCheckBox("자동 진행");
    private final JCheckBox fastCheck = new JCheckBox("빠른 진행");

    public MatchPanel(MainFrame frame) {
        super(frame, "경기 · 대회");
        root.setOpaque(false);
        stage.setLogSink(s -> UI.appendLog(logArea, s));
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
                    String lock = r.isUnlocked(state().getWins(selectedWeapon())) ? "" : "  [잠김: " + r.getRequiredWins() + "승 필요]";
                    setText(r.getDisplayName() + "  (적 " + r.getLevelRange() + ", 보상 " + r.getRewardGold() + "G)" + lock);
                }
                return this;
            }
        });
        JPanel top = new JPanel(new GridLayout(3, 1, 0, 6));
        top.setOpaque(false);
        JPanel weaponRow = new JPanel(new BorderLayout(8, 0));
        weaponRow.setOpaque(false);
        weaponRow.add(new JLabel("출전 종목"), BorderLayout.WEST);
        weaponRow.add(weaponCombo, BorderLayout.CENTER);
        JPanel regionRow = new JPanel(new BorderLayout(8, 0));
        regionRow.setOpaque(false);
        regionRow.add(new JLabel("도전할 대회"), BorderLayout.WEST);
        regionRow.add(regionCombo, BorderLayout.CENTER);
        JLabel format = new JLabel("<html>출전 선수 <b>1명 = 개인전</b> (" + Battle.TARGET_SCORE + "점 선취, 3분 x 3피리어드, 동점이면 서든데스)  /  "
                + "<b>2~3명 = 단체전 릴레이</b> (경기당 3분, 5점씩 누적, 3명이면 9경기 45점)</html>");
        format.setForeground(Color.GRAY);
        top.add(weaponRow);
        top.add(regionRow);
        top.add(format);
        p.add(top, BorderLayout.NORTH);
        weaponCombo.addActionListener(e -> {
            teamList.clearSelection();
            refreshSetup();
        });

        teamList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane sp = new JScrollPane(teamList);
        sp.setBorder(BorderFactory.createTitledBorder("출전 선수 선택 - 선택한 종목의 선수만 보여요 (Ctrl/Shift 로 최대 3명)"));
        p.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bossCheck.setOpaque(false);
        bottom.add(bossCheck, BorderLayout.WEST);
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
            setupMsg.setText("출전 선수를 1~3명 선택해주세요. (1명=개인전, 2~3명=단체전)");
            return;
        }
        Region r = (Region) regionCombo.getSelectedItem();
        WeaponType weapon = selectedWeapon();
        if (r == null || weapon == null || !r.isUnlocked(state().getWins(weapon))) {
            setupMsg.setText("이 종목에서는 아직 잠긴 대회예요. " + weapon + "부에서 더 승리해보세요!");
            return;
        }
        for (Fencer f : sel) {
            if (f.isBusy()) {
                setupMsg.setText(f.getName() + "은(는) 이미 시합 중이에요.");
                return;
            }
        }
        boolean boss = bossCheck.isSelected();
        if (boss && !state().isCleared(r, weapon)) {
            setupMsg.setText("결승전(보스전)은 " + weapon + "부 해당 대회에서 한 번 이상 우승해야 열려요.");
            return;
        }
        battle = new Battle(state(), r, sel, boss);
        frame.getEngine().setPaused(true);   // 경기 중에는 게임 시간이 멈춘다
        bossCheck.setSelected(false);
        boutAuto = false;
        logArea.setText("");
        stage.setup(battle);
        cards.show(root, BATTLE);
        playPending();
    }

    // ---------- 경기 화면 ----------

    private JPanel buildBattle() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);

        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD, 15f));
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(infoLabel);
        head.add(scoutLabel);
        p.add(head, BorderLayout.NORTH);
        p.add(stage, BorderLayout.CENTER);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("경기 로그"));
        logScroll.setPreferredSize(new Dimension(0, 100));

        // 전술 선택 버튼 6개
        JPanel tactics = new JPanel(new GridLayout(1, 6, 6, 0));
        tactics.setOpaque(false);
        for (Battle.Tactic t : Battle.Tactic.values()) {
            JButton b = UI.button(t.getDisplayName() + " (체력 " + t.getCost() + ")");
            b.setToolTipText("<html><b>" + t.getDisplayName() + "</b><br>" + t.getHint() + "</html>");
            b.addActionListener(e -> choose(t));
            tacticButtons.put(t, b);
            tactics.add(b);
        }

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        controls.setOpaque(false);
        controls.add(continueBtn);
        controls.add(itemBtn);
        controls.add(boutAutoBtn);
        autoCheck.setOpaque(false);
        fastCheck.setOpaque(false);
        fastCheck.addActionListener(e -> stage.setFast(fastCheck.isSelected()));
        controls.add(autoCheck);
        controls.add(fastCheck);
        controls.add(giveUpBtn);

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setOpaque(false);
        south.add(logScroll, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new BorderLayout(0, 2));
        buttons.setOpaque(false);
        buttons.add(tactics, BorderLayout.NORTH);
        buttons.add(controls, BorderLayout.SOUTH);
        south.add(buttons, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);

        continueBtn.addActionListener(e -> {
            if (battle == null || stage.isBusy() || !battle.isOnBreak()) return;
            battle.continueFromBreak();
            playPending();
        });
        itemBtn.addActionListener(e -> useItem());
        boutAutoBtn.addActionListener(e -> {
            if (battle == null || stage.isBusy() || battle.isOnBreak() || battle.isOver()) return;
            boutAuto = true;
            autoStep();
        });
        giveUpBtn.addActionListener(e -> {
            if (battle == null || stage.isBusy()) return;
            int r = JOptionPane.showConfirmDialog(this, "정말 기권할까요?", "기권", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                boutAuto = false;
                battle.surrender();
                playPending();
            }
        });
        setControls(false, false);
        return p;
    }

    /** tactics: 전술 버튼, breakTime: 휴식 시간 버튼(계속/아이템) */
    private void setControls(boolean tactics, boolean breakTime) {
        for (JButton b : tacticButtons.values()) b.setEnabled(tactics);
        continueBtn.setEnabled(breakTime);
        itemBtn.setEnabled(breakTime);
        boutAutoBtn.setEnabled(tactics);
        giveUpBtn.setEnabled(tactics || breakTime);
    }

    private void choose(Battle.Tactic t) {
        if (battle == null || stage.isBusy() || battle.isOnBreak() || battle.isOver()) return;
        battle.choose(t);
        playPending();
    }

    private void autoStep() {
        battle.choose(battle.suggest());
        playPending();
    }

    private void useItem() {
        if (battle == null || stage.isBusy() || !battle.isOnBreak()) return;
        List<Item> items = new ArrayList<>(state().getInventory().snapshot().keySet());
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "사용할 수 있는 아이템이 없어요.");
            return;
        }
        Item it = (Item) JOptionPane.showInputDialog(this, "사용할 아이템을 선택하세요.", "휴식 시간 - 아이템",
                JOptionPane.QUESTION_MESSAGE, null, items.toArray(), items.get(0));
        if (it == null) return;
        Fencer ally = (Fencer) JOptionPane.showInputDialog(this, "대상 선수를 선택하세요.", "휴식 시간 - 아이템",
                JOptionPane.QUESTION_MESSAGE, null, battle.getPlayers().toArray(), battle.getPlayers().get(0));
        if (ally == null) return;
        battle.useItem(it, ally);
        playPending();
    }

    /** 방금 쌓인 이벤트를 장면에서 재생하고, 끝나면 다음 입력을 받는다. */
    private void playPending() {
        setControls(false, false);
        stage.play(battle.drainEvents(), this::afterPlay);
    }

    private void afterPlay() {
        if (battle == null) return;
        if (battle.isOver()) {
            finishBattle();
            return;
        }
        updateLabels();
        if (battle.isOnBreak()) {   // 피리어드/경기 사이 휴식
            boutAuto = false;
            if (autoCheck.isSelected()) {
                battle.continueFromBreak();
                playPending();
                return;
            }
            setControls(false, true);
            return;
        }
        if (autoCheck.isSelected() || boutAuto) {
            autoStep();
            return;
        }
        setControls(true, false);
    }

    private void updateLabels() {
        Fencer l = battle.getLeft(), r = battle.getRight();
        if (l == null || r == null) return;
        int sec = Math.max(0, battle.getClock());
        String where = battle.isSuddenDeath() ? "서든데스" : battle.isIndividual() ? battle.getPeriod() + "피리어드"
                : "제 " + battle.getBoutNumber() + "/" + battle.getBoutCount() + "경기";
        String pri;
        if (battle.isSuddenDeath()) pri = "우선권(추첨): " + battle.getSuddenPriority().getName();
        else if (l.getWeapon() == WeaponType.EPEE) pri = "에페: 우선권 없음";
        else pri = battle.getPriority() == null ? "우선권 없음" : "우선권: " + battle.getPriority().getName();
        infoLabel.setText(where + "   " + l.getName() + " vs " + r.getName() + "   |   남은 시간 "
                + String.format("%d:%02d", sec / 60, sec % 60) + "   |   " + pri);

        StringBuilder hist = new StringBuilder();
        List<Battle.Tactic> h = battle.getRightHistory();
        for (int i = Math.max(0, h.size() - 4); i < h.size(); i++) {
            if (hist.length() > 0) hist.append(" → ");
            hist.append(h.get(i).getDisplayName());
        }
        scoutLabel.setText(battle.getScoutReport() + (hist.length() > 0 ? "    [상대의 최근 선택: " + hist + "]" : ""));
        scoutLabel.setForeground(Color.GRAY);
        if (battle.isOnBreak()) {
            continueBtn.setText("다음 진행 (휴식 끝)");
        }
    }

    private void finishBattle() {
        stage.stop();
        String summary = battle.settle();
        battle = null;
        boutAuto = false;
        frame.getEngine().setPaused(false);
        JOptionPane.showMessageDialog(this, summary, "경기 결과", JOptionPane.INFORMATION_MESSAGE);
        for (String line : summary.split("\n")) GameLog.post("[경기] " + line);
        cards.show(root, SETUP);
        refreshSetup();
    }

    // ---------- 편성 ----------

    private WeaponType selectedWeapon() {
        return (WeaponType) weaponCombo.getSelectedItem();
    }

    private boolean hasFencer(WeaponType w) {
        for (Fencer f : state().getRoster()) if (f.getWeapon() == w) return true;
        return false;
    }

    private void refreshSetup() {
        List<Fencer> selected = teamList.getSelectedValuesList();
        teamModel.clear();
        WeaponType w = selectedWeapon();
        for (Fencer f : state().getRoster()) {
            if (f.getWeapon() == w) teamModel.addElement(f);   // 선택한 종목의 선수만 출전할 수 있다
        }
        for (Fencer f : selected) {
            int i = teamModel.indexOf(f);
            if (i >= 0) teamList.addSelectionInterval(i, i);
        }
        setupMsg.setText(teamModel.isEmpty() ? "이 종목 선수가 없어요. 선수 관리에서 " + w + " 선수를 영입해보세요." : " ");
        regionCombo.repaint();
    }

    @Override
    public void onShow() {
        if (battle == null) {
            if (!hasFencer(selectedWeapon())) {   // 선수가 있는 종목을 기본으로 보여준다
                for (WeaponType w : WeaponType.values()) {
                    if (hasFencer(w)) {
                        weaponCombo.setSelectedItem(w);
                        break;
                    }
                }
            }
            cards.show(root, SETUP);
            refreshSetup();
        }
    }

    @Override
    public void onStateReplaced() {
        stage.stop();
        battle = null;
        boutAuto = false;
        cards.show(root, SETUP);
        teamModel.clear();
    }
}
