import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 인벤토리: 보유 아이템 확인, 선수에게 사용(먹이 / 치료 / 장비) */
public class InventoryPanel extends ScreenPanel {
    private final JTable table = UI.table("종류", "이름", "보유", "설명");
    private final JComboBox<Fencer> combo = UI.fencerCombo();
    private final JLabel msg = new JLabel(" ");
    private final List<Item> rowItems = new ArrayList<>();

    public InventoryPanel(MainFrame frame) {
        super(frame, "인벤토리");

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 8));
        bottom.setOpaque(false);
        JPanel use = new JPanel(new BorderLayout(8, 0));
        use.setOpaque(false);
        use.add(new JLabel("사용할 선수"), BorderLayout.WEST);
        use.add(combo, BorderLayout.CENTER);
        JButton b = UI.button("아이템 사용");
        b.addActionListener(e -> useItem());
        use.add(b, BorderLayout.EAST);
        bottom.add(use, BorderLayout.NORTH);
        bottom.add(msg, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void useItem() {
        int row = table.getSelectedRow();
        Fencer f = (Fencer) combo.getSelectedItem();
        if (row < 0 || row >= rowItems.size()) {
            msg.setText("사용할 아이템을 선택해주세요.");
            return;
        }
        if (f == null) {
            msg.setText("대상 선수를 선택해주세요.");
            return;
        }
        String result = state().useItem(f, rowItems.get(row));
        msg.setText(result);
        GameLog.post("[아이템] " + result);
        refresh();
    }

    private void refresh() {
        int sel = table.getSelectedRow();
        UI.model(table).setRowCount(0);
        rowItems.clear();
        for (Map.Entry<Item, Integer> e : state().getInventory().snapshot().entrySet()) {
            Item it = e.getKey();
            rowItems.add(it);
            UI.model(table).addRow(new Object[]{it.getTypeName(), it.getName(), e.getValue(), it.getDescription()});
        }
        if (sel >= 0 && sel < table.getRowCount()) table.setRowSelectionInterval(sel, sel);
    }

    @Override
    public void onShow() {
        UI.reloadCombo(combo, state().getRoster());
        refresh();
    }
}
