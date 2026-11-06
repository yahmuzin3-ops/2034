import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** 화면에서 공통으로 쓰는 색상/컴포넌트 생성 도우미 */
public final class UI {
    public static final Color BG = new Color(245, 246, 250);
    public static final Color NAVY = new Color(28, 37, 65);
    public static final Color GOLD = new Color(240, 196, 25);
    public static final Color ACCENT = new Color(70, 90, 150);

    private UI() {
    }

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JProgressBar bar(Color color) {
        JProgressBar p = new JProgressBar(0, 100);
        p.setStringPainted(true);
        p.setForeground(color);
        p.setPreferredSize(new Dimension(200, 22));
        return p;
    }

    public static void setBar(JProgressBar p, int value, int max) {
        p.setMaximum(max);
        p.setValue(value);
        p.setString(value + " / " + max);
    }

    public static String fencerLabel(Fencer f) {
        return f.getName() + "  (" + f.getWeapon().getDisplayName() + " · "
                + f.getGrade().getDisplayName() + " Lv." + f.getLevel() + ")  HP "
                + f.getHp() + "/" + f.getMaxHp();
    }

    private static ListCellRenderer<Object> fencerRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Fencer) setText(fencerLabel((Fencer) value));
                return this;
            }
        };
    }

    public static JComboBox<Fencer> fencerCombo() {
        JComboBox<Fencer> c = new JComboBox<>();
        c.setRenderer(fencerRenderer());
        return c;
    }

    public static JList<Fencer> fencerList(DefaultListModel<Fencer> model) {
        JList<Fencer> l = new JList<>(model);
        l.setCellRenderer(fencerRenderer());
        return l;
    }

    /** 선택된 선수를 유지하면서 콤보박스 내용을 다시 채운다. */
    public static void reloadCombo(JComboBox<Fencer> combo, List<Fencer> roster) {
        Fencer sel = (Fencer) combo.getSelectedItem();
        combo.removeAllItems();
        for (Fencer f : roster) combo.addItem(f);
        if (sel != null && roster.contains(sel)) combo.setSelectedItem(sel);
    }

    public static JTable table(String... cols) {
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable t = new JTable(m);
        t.setRowHeight(26);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.setFillsViewportHeight(true);
        return t;
    }

    public static DefaultTableModel model(JTable t) {
        return (DefaultTableModel) t.getModel();
    }

    public static JTextArea logArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    public static void appendLog(JTextArea a, String s) {
        a.append(s + "\n");
        a.setCaretPosition(a.getDocument().getLength());
    }
}
