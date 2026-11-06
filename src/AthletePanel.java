import javax.swing.*;
import java.awt.*;

/** 선수 관리: 목록 + 능력치/상태 + 휴식 + 영입 */
public class AthletePanel extends ScreenPanel {
    private final DefaultListModel<Fencer> model = new DefaultListModel<>();
    private final JList<Fencer> list = UI.fencerList(model);

    private final JLabel nameLabel = new JLabel("-");
    private final JLabel infoLabel = new JLabel(" ");
    private final JLabel statLabel = new JLabel(" ");
    private final JLabel skillLabel = new JLabel(" ");
    private final JLabel stateLabel = new JLabel(" ");
    private final JProgressBar expBar = UI.bar(new Color(90, 140, 220));
    private final JProgressBar hpBar = UI.bar(new Color(220, 80, 80));
    private final JProgressBar hungerBar = UI.bar(new Color(230, 150, 50));
    private final JProgressBar fatigueBar = UI.bar(new Color(150, 110, 200));
    private final JProgressBar moodBar = UI.bar(new Color(80, 180, 120));

    public AthletePanel(MainFrame frame) {
        super(frame, "선수 관리");

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> updateDetail());
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(300, 0));
        listScroll.setBorder(BorderFactory.createTitledBorder("보유 선수"));
        add(listScroll, BorderLayout.WEST);

        JPanel detail = new JPanel();
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setOpaque(false);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 22f));
        for (JLabel l : new JLabel[]{nameLabel, infoLabel, statLabel, skillLabel, stateLabel}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            detail.add(l);
            detail.add(Box.createVerticalStrut(6));
        }
        detail.add(row("경험치", expBar));
        detail.add(row("체력", hpBar));
        detail.add(row("배고픔", hungerBar));
        detail.add(row("피로도", fatigueBar));
        detail.add(row("기분", moodBar));

        JButton rest = UI.button("휴식시키기");
        rest.addActionListener(e -> rest());
        JButton recruit = UI.button("선수 영입");
        recruit.addActionListener(e -> recruit());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(rest);
        btns.add(recruit);
        detail.add(Box.createVerticalStrut(14));
        detail.add(btns);
        add(detail, BorderLayout.CENTER);
    }

    private JPanel row(String label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(70, 22));
        p.add(l, BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private void rest() {
        Fencer f = list.getSelectedValue();
        if (f == null) return;
        if (f.isBusy()) {
            JOptionPane.showMessageDialog(this, f.getName() + "은(는) 시합 중이에요.");
            return;
        }
        f.rest(state().getRestBonus());
        GameLog.post("[관리] " + f.getName() + "을(를) 쉬게 했어요. (피로 감소, HP 회복)");
        updateDetail();
    }

    private void recruit() {
        GameState s = state();
        int cost = s.recruitCost();
        if (s.getRoster().size() >= GameState.MAX_ROSTER) {
            JOptionPane.showMessageDialog(this, "선수는 최대 " + GameState.MAX_ROSTER + "명까지 보유할 수 있어요.");
            return;
        }
        if (s.getGold() < cost) {
            JOptionPane.showMessageDialog(this, "재화가 모자라요. (영입 비용 " + cost + "G)");
            return;
        }
        String name = JOptionPane.showInputDialog(this, "새 선수의 이름을 입력하세요. (영입 비용 " + cost + "G)");
        if (name == null || name.isBlank()) return;
        WeaponType w = (WeaponType) JOptionPane.showInputDialog(this, "종목(무기)을 선택하세요.", "선수 영입",
                JOptionPane.QUESTION_MESSAGE, null, WeaponType.values(), WeaponType.FOIL);
        if (w == null) return;
        Fencer f = s.recruit(name.trim(), w);
        if (f == null) {
            JOptionPane.showMessageDialog(this, "영입에 실패했어요.");
            return;
        }
        frame.getEngine().syncWorkers();
        GameLog.post("[영입] " + f.getName() + " (" + w.getDisplayName() + " / "
                + f.getPersonality().getDisplayName() + ") 선수가 합류했어요!");
        refreshList();
    }

    private void refreshList() {
        Fencer sel = list.getSelectedValue();
        model.clear();
        for (Fencer f : state().getRoster()) model.addElement(f);
        if (sel != null && model.contains(sel)) list.setSelectedValue(sel, true);
        else if (!model.isEmpty()) list.setSelectedIndex(0);
        updateDetail();
    }

    private void updateDetail() {
        Fencer f = list.getSelectedValue();
        if (f == null) return;
        nameLabel.setText(f.getName());
        infoLabel.setText(f.getWeapon().getDisplayName() + " · " + f.getPersonality().getDisplayName()
                + " · " + f.getGrade().getDisplayName() + " · Lv." + f.getLevel());
        statLabel.setText("공격 " + f.getAttack() + "   방어 " + f.getDefense()
                + "   민첩 " + f.getAgility() + "   기술 " + f.getTechnique());
        skillLabel.setText("특수 기술: " + f.getWeapon().getSpecialSkill());
        stateLabel.setText(f.isBusy() ? "시합 중" : f.isInjured() ? "부상 중 (치료 아이템이나 휴식이 필요해요)" : "컨디션 양호");
        UI.setBar(expBar, f.getExp(), f.expToNext());
        UI.setBar(hpBar, f.getHp(), f.getMaxHp());
        UI.setBar(hungerBar, f.getHunger(), 100);
        UI.setBar(fatigueBar, f.getFatigue(), 100);
        UI.setBar(moodBar, f.getMood(), 100);
        list.repaint();
    }

    @Override
    public void onShow() { refreshList(); }

    @Override
    public void onTick() {
        if (model.size() != state().getRoster().size()) refreshList();
        else updateDetail();
    }

    @Override
    public void onStateReplaced() { model.clear(); }
}
