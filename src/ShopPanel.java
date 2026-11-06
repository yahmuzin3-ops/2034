import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 상점: 구매 / 판매 탭 */
public class ShopPanel extends ScreenPanel {
    private final JLabel goldLabel = new JLabel(" ");
    private final JLabel msg = new JLabel(" ");
    private final JTable buyTable = UI.table("종류", "이름", "가격", "설명");
    private final JTable sellTable = UI.table("종류", "이름", "보유", "판매가", "설명");
    private final JSpinner buyQty = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JSpinner sellQty = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final List<Item> sellItems = new ArrayList<>();

    public ShopPanel(MainFrame frame) {
        super(frame, "상점");

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(goldLabel, BorderLayout.WEST);
        top.add(msg, BorderLayout.CENTER);
        goldLabel.setFont(goldLabel.getFont().deriveFont(Font.BOLD, 16f));
        msg.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        for (Item it : ItemCatalog.ALL) {
            UI.model(buyTable).addRow(new Object[]{it.getTypeName(), it.getName(), it.getPrice() + "G", it.getDescription()});
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("구매", tab(buyTable, buyQty, "구매", this::buy));
        tabs.addTab("판매", tab(sellTable, sellQty, "판매", this::sell));

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JPanel tab(JTable table, JSpinner qty, String btnText, Runnable action) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bottom.add(new JLabel("수량"));
        bottom.add(qty);
        JButton b = UI.button(btnText);
        b.addActionListener(e -> action.run());
        bottom.add(b);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void buy() {
        int row = buyTable.getSelectedRow();
        if (row < 0) {
            msg.setText("구매할 아이템을 선택해주세요.");
            return;
        }
        Item it = ItemCatalog.ALL.get(row);
        int q = (Integer) buyQty.getValue();
        if (Shop.buy(state(), it, q)) {
            msg.setText(it.getName() + " x" + q + " 구매 완료!");
        } else {
            msg.setText("재화가 모자라요.");
        }
        refreshSell();
        updateGold();
    }

    private void sell() {
        int row = sellTable.getSelectedRow();
        if (row < 0 || row >= sellItems.size()) {
            msg.setText("판매할 아이템을 선택해주세요.");
            return;
        }
        Item it = sellItems.get(row);
        int q = (Integer) sellQty.getValue();
        if (Shop.sell(state(), it, q)) {
            msg.setText(it.getName() + " x" + q + " 판매 완료! (+" + it.getSellPrice() * q + "G)");
        } else {
            msg.setText("보유 수량이 모자라요.");
        }
        refreshSell();
        updateGold();
    }

    private void refreshSell() {
        int sel = sellTable.getSelectedRow();
        UI.model(sellTable).setRowCount(0);
        sellItems.clear();
        for (Map.Entry<Item, Integer> e : state().getInventory().snapshot().entrySet()) {
            Item it = e.getKey();
            sellItems.add(it);
            UI.model(sellTable).addRow(new Object[]{it.getTypeName(), it.getName(), e.getValue(),
                    it.getSellPrice() + "G", it.getDescription()});
        }
        if (sel >= 0 && sel < sellTable.getRowCount()) sellTable.setRowSelectionInterval(sel, sel);
    }

    private void updateGold() {
        goldLabel.setText("보유 재화: " + state().getGold() + "G");
    }

    @Override
    public void onShow() {
        refreshSell();
        updateGold();
    }

    @Override
    public void onTick() { updateGold(); }
}
