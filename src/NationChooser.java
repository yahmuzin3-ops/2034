import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 소속 국가를 고르는 창. 국가마다 그 나라의 펜싱 수준(국내 대회와 세계 무대 난이도)이 다르다. */
public final class NationChooser {
    private NationChooser() {
    }

    /** 고른 국가 코드를 돌려준다. 취소하면 null. */
    public static String choose(Component parent, String current) {
        List<String> codes = FencingWorld.nationCodes();
        List<String> labels = new ArrayList<>();
        for (String c : codes) labels.add(FencingWorld.nationName(c) + " (" + c + ")  -  펜싱 수준: " + FencingWorld.levelLabel(c));
        JComboBox<String> box = new JComboBox<>(labels.toArray(new String[0]));
        box.setRenderer(new DefaultListCellRenderer() {   // 목록에 국기를 함께 보여준다
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                Component c = super.getListCellRendererComponent(list, value, index, sel, focus);
                int i = labels.indexOf(String.valueOf(value));
                setIcon(i >= 0 ? FlagArt.icon(codes.get(i), 30, 20) : null);
                setIconTextGap(10);
                return c;
            }
        });
        int idx = codes.indexOf(current);
        box.setSelectedIndex(Math.max(0, idx));
        JLabel hint = new JLabel("<html>소속 국가는 <b>자국 선수권</b>에 나오는 국내 선수와 대표팀, 랭킹에 영향을 줘요.<br>"
                + "펜싱 강국일수록 국내 대회가 어려워요. (한국이 가장 쉬운 편이에요)</html>");
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.add(hint, BorderLayout.NORTH);
        p.add(box, BorderLayout.CENTER);
        int r = JOptionPane.showConfirmDialog(parent, p, "소속 국가 선택", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return null;
        return codes.get(box.getSelectedIndex());
    }
}
