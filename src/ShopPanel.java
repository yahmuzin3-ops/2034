import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 상점: 구매 / 판매 탭 */
public class ShopPanel extends ScreenPanel {
    private final JLabel goldLabel = new JLabel(" ");
    private final JLabel msg = new JLabel(" ");
    private final JTable buyTable = UI.table("모양", "종류", "이름", "가격", "설명");
    private final JTable sellTable = UI.table("모양", "종류", "이름", "보유", "판매가", "설명");
    private final JSpinner buyQty = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JSpinner sellQty = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final List<Item> sellItems = new ArrayList<>();
    private final FencerFigure previewFigure = new FencerFigure();
    private final JLabel previewName = new JLabel(" ");
    private final JLabel previewInfo = new JLabel(" ");

    public ShopPanel(MainFrame frame) {
        super(frame, "상점");

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(goldLabel, BorderLayout.WEST);
        top.add(msg, BorderLayout.CENTER);
        goldLabel.setFont(goldLabel.getFont().deriveFont(Font.BOLD, 16f));
        msg.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        for (Item it : ItemCatalog.ALL) {
            UI.model(buyTable).addRow(new Object[]{GearArt.icon(it, 44), it.getTypeName(), it.getName(), it.getPrice() + "G", it.getDescription()});
        }

        UI.iconColumn(buyTable, 0);
        UI.iconColumn(sellTable, 0);
        buyTable.getSelectionModel().addListSelectionListener(e -> {
            int row = buyTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < ItemCatalog.ALL.size()) showPreview(ItemCatalog.ALL.get(row));
        });
        sellTable.getSelectionModel().addListSelectionListener(e -> {
            int row = sellTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < sellItems.size()) showPreview(sellItems.get(row));
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("구매", tab(buyTable, buyQty, "구매", this::buy));
        tabs.addTab("판매", tab(sellTable, sellQty, "판매", this::sell));

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // 오른쪽: 선택한 장비를 선수에게 입혀 보는 미리보기
        JPanel preview = new JPanel(new BorderLayout(0, 6));
        preview.setOpaque(false);
        preview.setBorder(BorderFactory.createTitledBorder("착용 미리보기"));
        preview.setPreferredSize(new Dimension(290, 0));
        previewName.setFont(previewName.getFont().deriveFont(Font.BOLD, 14f));
        previewName.setHorizontalAlignment(SwingConstants.CENTER);
        previewInfo.setHorizontalAlignment(SwingConstants.CENTER);
        preview.add(previewName, BorderLayout.NORTH);
        preview.add(previewFigure, BorderLayout.CENTER);
        preview.add(previewInfo, BorderLayout.SOUTH);
        add(preview, BorderLayout.EAST);
    }

    /** 선택한 아이템이 장비면 첫 번째 선수에게 입혀서 보여준다. */
    private void showPreview(Item it) {
        java.util.List<Fencer> roster = state().getRoster();
        Fencer f = roster.isEmpty() ? null : roster.get(0);
        Equipment eq = it instanceof Equipment && ((Equipment) it).getSlot() != null ? (Equipment) it : null;
        previewFigure.setTryOn(f, eq);
        previewName.setText(it.getName());
        String who = f == null ? "" : f.getName() + " 선수에게 입혀 본 모습";
        previewInfo.setText("<html><div style='text-align:center;width:250px'>"
                + (eq != null ? eq.statText() + "<br>" + who : it.getTypeName() + " 아이템") + "</div></html>");
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
            UI.model(sellTable).addRow(new Object[]{GearArt.icon(it, 44), it.getTypeName(), it.getName(), e.getValue(),
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
        java.util.List<Fencer> roster = state().getRoster();
        previewFigure.setTryOn(roster.isEmpty() ? null : roster.get(0), null);
        previewName.setText("장비를 선택해 보세요");
        previewInfo.setText(" ");
    }

    @Override
    public void onTick() { updateGold(); }
}
