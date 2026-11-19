import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 코치와 스태프: 수석 코치, 트레이너, 전력 분석관, 영양사를 고용해서 육성과 경기를 돕는다. */
public class StaffPanel extends ScreenPanel {
    private final JLabel info = new JLabel(" ");
    private final JLabel message = new JLabel(" ");
    private final JTable mine = UI.table("역할", "이름", "레벨", "효과", "주급", "영입일");
    private final JTable cand = UI.table("역할", "이름", "레벨", "효과", "주급", "영입 비용");
    private final JButton promoteBtn = UI.button("승급");
    private final JButton fireBtn = UI.button("계약 종료");
    private final JButton hireBtn = UI.button("영입");
    private final Staff.Role[] roles = Staff.Role.values();
    private List<Staff> candidates = new ArrayList<>();
    private int lastDay = -1, lastGold = -1;

    public StaffPanel(MainFrame frame) {
        super(frame, "코치 · 스태프");
        DefaultTableCellRenderer mineRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                setHorizontalAlignment(col == 3 ? LEFT : CENTER);
                if (!sel) c.setBackground("공석".equals(String.valueOf(t.getValueAt(row, 1))) ? new Color(240, 240, 246) : Color.WHITE);
                return c;
            }
        };
        mine.setDefaultRenderer(Object.class, mineRenderer);
        int[] widths = {150, 90, 110, 330, 70, 90};
        for (int i = 0; i < widths.length; i++) {
            mine.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            cand.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        DefaultTableCellRenderer cr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                setHorizontalAlignment(col == 3 ? LEFT : CENTER);
                return c;
            }
        };
        cand.setDefaultRenderer(Object.class, cr);

        JPanel mineBox = new JPanel(new BorderLayout(0, 6));
        mineBox.setOpaque(false);
        JScrollPane ms = new JScrollPane(mine);
        ms.setBorder(BorderFactory.createTitledBorder("내 스태프 (역할마다 한 명)"));
        ms.setPreferredSize(new Dimension(0, 150));
        mineBox.add(ms, BorderLayout.CENTER);
        JPanel mb = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        mb.setOpaque(false);
        mb.add(promoteBtn);
        mb.add(fireBtn);
        mineBox.add(mb, BorderLayout.SOUTH);

        JPanel candBox = new JPanel(new BorderLayout(0, 6));
        candBox.setOpaque(false);
        JScrollPane cs = new JScrollPane(cand);
        cs.setBorder(BorderFactory.createTitledBorder("영입 후보 (14일마다 새로 바뀌어요)"));
        candBox.add(cs, BorderLayout.CENTER);
        JPanel cb = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cb.setOpaque(false);
        cb.add(hireBtn);
        candBox.add(cb, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 10));
        center.setOpaque(false);
        center.add(mineBox);
        center.add(candBox);

        JPanel north = new JPanel(new GridLayout(2, 1, 0, 2));
        north.setOpaque(false);
        north.add(info);
        north.add(message);
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(north, BorderLayout.NORTH);
        wrap.add(center, BorderLayout.CENTER);
        add(wrap, BorderLayout.CENTER);

        promoteBtn.addActionListener(e -> {
            int row = mine.getSelectedRow();
            if (row < 0) {
                message.setText("승급할 스태프를 선택해주세요.");
                return;
            }
            message.setText(state().promoteStaff(roles[row]));
            refresh(true, row);
        });
        fireBtn.addActionListener(e -> {
            int row = mine.getSelectedRow();
            if (row < 0 || state().getStaff(roles[row]) == null) {
                message.setText("계약을 종료할 스태프를 선택해주세요.");
                return;
            }
            if (JOptionPane.showConfirmDialog(this, roles[row] + "와(과) 계약을 종료할까요? (영입 비용은 돌려받지 못해요)", "계약 종료", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            message.setText(state().fireStaff(roles[row]));
            refresh(true, -1);
        });
        hireBtn.addActionListener(e -> {
            int row = cand.getSelectedRow();
            if (row < 0 || row >= candidates.size()) {
                message.setText("영입할 후보를 선택해주세요.");
                return;
            }
            message.setText(state().hireStaff(candidates.get(row)));
            refresh(true, -1);
        });
    }

    private void refresh(boolean force, int keepRow) {
        GameState s = state();
        if (s.getCandidates().isEmpty()) s.refreshCandidates();
        if (!force && s.getDay() == lastDay && s.getGold() == lastGold) return;
        lastDay = s.getDay();
        lastGold = s.getGold();
        int untilPay = (Staff.PAY_DAYS - s.getDay() % Staff.PAY_DAYS) % Staff.PAY_DAYS;
        info.setText("<html>보유 골드 <b>" + s.getGold() + "G</b>  ·  주급 합계 <b>" + s.getStaffWeeklyCost() + "G</b> (" + Staff.PAY_DAYS + "일마다 지급, "
                + (untilPay == 0 ? "오늘 지급했어요" : untilPay + "일 뒤") + ")  ·  골드가 모자라면 주급이 높은 스태프부터 떠나요</html>");

        UI.model(mine).setRowCount(0);
        for (Staff.Role role : roles) {
            Staff st = s.getStaff(role);
            if (st == null) UI.model(mine).addRow(new Object[]{role.getDisplayName(), "공석", "-", role.getEffectName() + ": " + Staff.effectText(role, 1) + " (Lv.1) ~ " + Staff.effectText(role, 5) + " (Lv.5)", "-", "-"});
            else UI.model(mine).addRow(new Object[]{role.getDisplayName(), st.getName(), "Lv." + st.getLevel() + " " + st.stars(), st.getEffectText(),
                    st.getWeeklySalary() + "G", st.getHiredDay() + "일차"});
        }
        if (keepRow >= 0 && keepRow < roles.length) mine.setRowSelectionInterval(keepRow, keepRow);

        candidates = s.getCandidates();
        UI.model(cand).setRowCount(0);
        for (Staff c : candidates) {
            boolean filled = s.getStaff(c.getRole()) != null;
            UI.model(cand).addRow(new Object[]{c.getRole().getDisplayName() + (filled ? " (자리 있음)" : ""), c.getName(), "Lv." + c.getLevel() + " " + c.stars(), c.getEffectText(),
                    c.getWeeklySalary() + "G", c.getHireFee() + "G"});
        }
    }

    @Override
    protected String helpTopic() { return HelpDialog.STAFF; }

    @Override
    public void onShow() { refresh(true, -1); }

    @Override
    public void onTick() { refresh(false, mine.getSelectedRow()); }

    @Override
    public void onStateReplaced() {
        lastDay = -1;
        message.setText(" ");
    }
}
