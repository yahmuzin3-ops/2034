import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/** 선수 관리: 목록 + 능력치/상태 + 휴식 + 영입 */
public class AthletePanel extends ScreenPanel {
    private final DefaultListModel<Fencer> model = new DefaultListModel<>();
    private final JList<Fencer> list = UI.fencerList(model);

    private final JLabel nameLabel = new JLabel("-");
    private final JLabel infoLabel = new JLabel(" ");
    private final JLabel statLabel = new JLabel(" ");
    private final JLabel skillLabel = new JLabel(" ");
    private final JLabel ruleLabel = new JLabel(" ");
    private final JLabel gearLabel = new JLabel(" ");
    private final JLabel stateLabel = new JLabel(" ");
    private final JLabel autoLabel = new JLabel(" ");
    private final JCheckBox autoCheck = new JCheckBox("자율 행동 켜기", true);
    private final FencerFigure figure = new FencerFigure();
    private final AbilityRadarChart abilityChart = new AbilityRadarChart();
    private final JButton restButton = UI.button("휴식시키기");
    private final JButton releaseButton = UI.button("방출하기");
    private final JProgressBar expBar = UI.bar(new Color(90, 140, 220));
    private final JProgressBar hpBar = UI.bar(new Color(220, 80, 80));
    private final JProgressBar hungerBar = UI.bar(new Color(230, 150, 50));
    private final JProgressBar fatigueBar = UI.bar(new Color(150, 110, 200));
    private final JProgressBar moodBar = UI.bar(new Color(80, 180, 120));

    public AthletePanel(MainFrame frame) {
        super(frame, "선수 관리");

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> source, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(source, value, index, selected, focus);
                if (value instanceof Fencer) {
                    Fencer f = (Fencer) value;
                    String text = UI.fencerLabel(f);
                    String activity = f.isResting() ? "휴식 중" : f.isTraining() ? "훈련 중" : null;
                    if (activity != null) text = f.getName() + " (" + activity + ")" + text.substring(f.getName().length());
                    label.setText(text);
                    if (!selected && f.isResting()) label.setForeground(new Color(175, 30, 30));
                    else if (!selected && f.isTraining()) label.setForeground(new Color(45, 85, 165));
                    label.setFont(source.getFont().deriveFont(activity == null ? Font.PLAIN : Font.BOLD));
                }
                return label;
            }
        });
        list.addListSelectionListener(e -> updateDetail());
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(210, 0));
        listScroll.setBorder(BorderFactory.createTitledBorder("보유 선수"));
        add(listScroll, BorderLayout.WEST);

        JPanel detail = new JPanel();
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setOpaque(false);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 22f));
        for (JLabel l : new JLabel[]{nameLabel, infoLabel, statLabel}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            detail.add(l);
            detail.add(Box.createVerticalStrut(6));
        }
        abilityChart.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.add(abilityChart);
        detail.add(Box.createVerticalStrut(6));
        for (JLabel l : new JLabel[]{skillLabel, ruleLabel, gearLabel, stateLabel, autoLabel}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            detail.add(l);
            detail.add(Box.createVerticalStrut(6));
        }
        detail.add(row("경험치", expBar));
        detail.add(row("체력", hpBar));
        detail.add(row("배고픔", hungerBar));
        detail.add(row("피로도", fatigueBar));
        detail.add(row("기분", moodBar));

        autoCheck.setOpaque(false);
        autoCheck.setToolTipText("선수가 스스로 휴식 · 식사 · 기분 전환 · 섀도 펜싱을 합니다.");
        autoCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoCheck.addActionListener(e -> {
            Fencer sel = list.getSelectedValue();
            if (sel == null) return;
            sel.setAutoEnabled(autoCheck.isSelected());
            GameLog.post("[관리] " + sel.getName() + "의 자율 행동을 " + (autoCheck.isSelected() ? "켰어요." : "껐어요."));
            updateDetail();
        });
        detail.add(autoCheck);
        detail.add(Box.createVerticalStrut(6));

        restButton.addActionListener(e -> toggleRest());
        releaseButton.addActionListener(e -> release());
        JButton unequip = UI.button("장비 해제");
        unequip.addActionListener(e -> unequip());
        JButton recruit = UI.button("선수 영입");
        recruit.addActionListener(e -> recruit());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(restButton);
        btns.add(releaseButton);
        btns.add(unequip);
        btns.add(recruit);
        detail.add(Box.createVerticalStrut(14));
        detail.add(btns);

        // 오른쪽: 장비를 끼고 서 있는 선수 모습
        JPanel figurePanel = new JPanel(new BorderLayout());
        figurePanel.setOpaque(false);
        figurePanel.setBorder(BorderFactory.createTitledBorder("착용 장비 (마우스를 올려 보세요)"));
        figurePanel.setPreferredSize(new Dimension(250, 0));
        figurePanel.add(figure, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        JScrollPane detailScroll = new JScrollPane(detail, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.setOpaque(false);
        detailScroll.getViewport().setOpaque(false);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);
        center.add(detailScroll, BorderLayout.CENTER);
        center.add(figurePanel, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
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

    private void toggleRest() {
        Fencer f = list.getSelectedValue();
        if (f == null) return;
        if (f.isBusy()) {
            JOptionPane.showMessageDialog(this, f.getName() + "은(는) 시합 중이에요.");
            return;
        }
        if (f.isTraining()) {
            JOptionPane.showMessageDialog(this, f.getName() + "은(는) 훈련 중이라 쉴 수 없어요.");
            return;
        }
        if (f.isResting()) {
            f.stopRest();
            GameLog.post("[관리] " + f.getName() + "의 휴식을 중단했어요.");
        } else if (f.startRest()) {
            GameLog.post("[관리] " + f.getName() + "이(가) 휴식을 시작했어요.");
        } else {
            JOptionPane.showMessageDialog(this, f.getName() + "은(는) 이미 충분히 회복했어요.");
        }
        updateDetail();
    }

    private void release() {
        Fencer f = list.getSelectedValue();
        if (f == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                f.getName() + " 선수를 방출하겠습니까?", "선수 방출", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        String message = state().releaseFencer(f);
        if (!state().getRoster().contains(f)) frame.getEngine().removeWorker(f);
        else JOptionPane.showMessageDialog(this, message);
        GameLog.post("[방출] " + message);
        refreshList();
    }

    private void unequip() {
        Fencer f = list.getSelectedValue();
        if (f == null) return;
        java.util.List<EquipSlot> worn = new java.util.ArrayList<>();
        for (EquipSlot s : EquipSlot.values()) if (f.getGear(s) != null) worn.add(s);
        if (worn.isEmpty()) {
            JOptionPane.showMessageDialog(this, f.getName() + "은(는) 장착한 장비가 없어요.");
            return;
        }
        EquipSlot slot = (EquipSlot) JOptionPane.showInputDialog(this, "벗을 장비의 부위를 선택하세요.", "장비 해제",
                JOptionPane.QUESTION_MESSAGE, null, worn.toArray(), worn.get(0));
        if (slot == null) return;
        String msg = state().unequip(f, slot);
        GameLog.post("[장비] " + msg);
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
        boolean resting = f.isResting();
        boolean training = f.isTraining();
        nameLabel.setText(f.getName() + (resting ? " (휴식 중)" : training ? " (훈련 중)" : ""));
        restButton.setText(resting ? "휴식 중지" : training ? "훈련 중" : "휴식시키기");
        restButton.setEnabled(!f.isBusy() && !training);
        releaseButton.setEnabled(!f.isBusy() && !training && state().getRoster().size() > 1);
        infoLabel.setText("<html><body style='width:300px'>" + f.getWeapon().getDisplayName() + " · "
                + f.getPersonality().getDisplayName() + " (" + f.getPersonality().getBonusDescription() + ") · "
                + f.getGrade().getDisplayName() + " · Lv." + f.getLevel() + "</body></html>");
        statLabel.setText("<html><body style='width:300px'>최대 HP " + f.getMaxHp() + " &nbsp; 공격 " + f.getAttack()
                + " &nbsp; 방어 " + f.getDefense() + " &nbsp; 민첩 " + f.getAgility() + " &nbsp; 기술 " + f.getTechnique()
                + "</body></html>");
        abilityChart.setFencer(f);
        figure.setFencer(f);
        skillLabel.setText("주특기: " + f.getWeapon().getSpecialSkill());
        StringBuilder gear = new StringBuilder("<html><body style='width:300px'><b>장비</b>: ");
        for (EquipSlot s : EquipSlot.values()) {
            Equipment g = f.getGear(s);
            gear.append(s).append(" ").append(g == null ? "-" : g.getName()).append(s == EquipSlot.JACKET ? "" : " / ");
        }
        gearLabel.setText(gear.append("</body></html>").toString());
        ruleLabel.setText("<html><body style='width:300px'><b>" + f.getWeapon().getDisplayName() + " ("
                + f.getWeapon().getStyleName() + ")</b><br>" + f.getWeapon().getRuleSummary() + "</body></html>");
        stateLabel.setText(f.isBusy() ? "시합 중" : training ? "훈련 중"
                : resting ? "휴식 중" : f.isInjured() ? "부상 중 (치료 아이템이 필요해요)" : "컨디션 양호");
        autoCheck.setSelected(f.isAutoEnabled());
        autoLabel.setText("<html><body style='width:300px'>" + (!f.isAutoEnabled() ? "자율 행동: 꺼짐"
                : f.getLastAuto().isEmpty() ? "자율 행동: 켜짐 (아직 한 행동이 없어요)" : "최근 자율 행동: " + f.getLastAuto())
                + "</body></html>");
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

    /** 최대 HP, 공격, 민첩, 방어, 기술을 오각형으로 비교한다. */
    private static final class AbilityRadarChart extends JPanel {
        private static final String[] LABELS = {"최대HP", "공격", "민첩", "방어", "기술"};
        private static final int[] REFERENCES = {200, 50, 50, 50, 50};
        private Fencer fencer;

        AbilityRadarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 180));
            setMinimumSize(new Dimension(280, 180));
            setMaximumSize(new Dimension(420, 180));
        }

        void setFencer(Fencer fencer) { this.fencer = fencer; repaint(); }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2, cy = getHeight() / 2 + 8;
            int radius = Math.max(40, Math.min((getWidth() - 120) / 2, (getHeight() - 76) / 2));
            double[] angles = {-90, -18, 54, 126, 198};
            g.setColor(new Color(35, 45, 70));
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.drawString("능력치 레이더 (HP 200 / 기타 50 기준)", 4, 16);
            for (int ring = 1; ring <= 4; ring++) {
                Path2D path = new Path2D.Double();
                for (int i = 0; i < angles.length; i++) {
                    double radians = Math.toRadians(angles[i]);
                    double x = cx + Math.cos(radians) * radius * ring / 4.0;
                    double y = cy + Math.sin(radians) * radius * ring / 4.0;
                    if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                path.closePath();
                g.setColor(ring == 4 ? new Color(125, 140, 165) : new Color(205, 211, 222));
                g.draw(path);
            }
            for (double angle : angles) {
                double radians = Math.toRadians(angle);
                g.setColor(new Color(205, 211, 222));
                g.drawLine(cx, cy, (int) (cx + Math.cos(radians) * radius),
                        (int) (cy + Math.sin(radians) * radius));
            }
            if (fencer != null) {
                int[] values = {fencer.getMaxHp(), fencer.getAttack(), fencer.getAgility(),
                        fencer.getDefense(), fencer.getTechnique()};
                int[] px = new int[5], py = new int[5];
                Path2D polygon = new Path2D.Double();
                for (int i = 0; i < angles.length; i++) {
                    double scale = Math.max(0, Math.min(1.0, values[i] / (double) REFERENCES[i]));
                    double radians = Math.toRadians(angles[i]);
                    px[i] = (int) Math.round(cx + Math.cos(radians) * radius * scale);
                    py[i] = (int) Math.round(cy + Math.sin(radians) * radius * scale);
                    if (i == 0) polygon.moveTo(px[i], py[i]); else polygon.lineTo(px[i], py[i]);
                }
                polygon.closePath();
                g.setColor(new Color(70, 125, 220, 65));
                g.fill(polygon);
                g.setColor(new Color(55, 105, 190));
                g.setStroke(new BasicStroke(2f));
                g.draw(polygon);
                FontMetrics metrics = g.getFontMetrics();
                for (int i = 0; i < values.length; i++) {
                    g.fillOval(px[i] - 4, py[i] - 4, 8, 8);
                    String text = LABELS[i] + " " + values[i];
                    double radians = Math.toRadians(angles[i]);
                    int x = (int) Math.round(cx + Math.cos(radians) * (radius + 18));
                    int y = (int) Math.round(cy + Math.sin(radians) * (radius + 18));
                    if (Math.cos(radians) < -0.2) x -= metrics.stringWidth(text);
                    else if (Math.abs(Math.cos(radians)) <= 0.2) x -= metrics.stringWidth(text) / 2;
                    if (Math.sin(radians) < -0.2) y -= 2;
                    else if (Math.abs(Math.sin(radians)) <= 0.2) y += metrics.getAscent() / 2;
                    g.setColor(new Color(40, 48, 65));
                    g.drawString(text, x, y);
                }
            }
            g.dispose();
        }
    }
}
