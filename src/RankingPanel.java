import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 파워 랭킹: 종목별 세계 랭킹, 내 선수, 메달 기록, 대회 출전 조건 */
public class RankingPanel extends ScreenPanel {
    private final JComboBox<WeaponType> weaponCombo = new JComboBox<>(WeaponType.values());
    private final JCheckBox koreanOnly = new JCheckBox("한국 선수만 보기");
    private final JLabel summary = new JLabel(" ");

    private final JTable rankTable = UI.table("세계", "한국", "선수", "국가", "파워 레이팅", "등급", "환산 Lv", "전적", "주간 변동");
    private final JTable mineTable = UI.table("선수", "종목", "파워 레이팅", "등급", "세계 순위", "한국 순위", "전적", "주간 변동");
    private final JTable medalTable = UI.table("일차", "대회", "종목", "구분", "메달", "선수");
    private final JTable entryTable = UI.table("대회", "종목", "출전 조건 / 상태");
    private final JLabel medalSummary = new JLabel(" ");

    private List<RankEntry> shown = new ArrayList<>();
    private int lastDay = -1;
    private String lastSig = "";

    public RankingPanel(MainFrame frame) {
        super(frame, "파워 랭킹");

        JPanel top = new JPanel(new BorderLayout(10, 6));
        top.setOpaque(false);
        JPanel pick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pick.setOpaque(false);
        pick.add(new JLabel("종목"));
        pick.add(weaponCombo);
        koreanOnly.setOpaque(false);
        pick.add(koreanOnly);
        top.add(pick, BorderLayout.NORTH);
        top.add(summary, BorderLayout.CENTER);

        weaponCombo.addActionListener(e -> refresh(true));
        koreanOnly.addActionListener(e -> refresh(true));

        // 랭킹 표: 내 선수 줄은 노란색으로 강조
        DefaultTableCellRenderer mine = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                boolean isMine = row >= 0 && row < shown.size() && shown.get(row).isMine();
                setHorizontalAlignment(col == 2 || col == 3 || col == 5 ? LEFT : CENTER);
                if (!sel) {
                    c.setBackground(isMine ? new Color(255, 238, 170) : row < 3 ? new Color(238, 244, 255) : Color.WHITE);
                    c.setFont(c.getFont().deriveFont(isMine ? Font.BOLD : Font.PLAIN));
                    c.setForeground(col == 8 && v != null && v.toString().startsWith("▲") ? new Color(40, 150, 80)
                            : col == 8 && v != null && v.toString().startsWith("▼") ? new Color(200, 70, 70) : Color.BLACK);
                }
                return c;
            }
        };
        for (int i = 0; i < rankTable.getColumnCount(); i++) rankTable.getColumnModel().getColumn(i).setCellRenderer(mine);
        rankTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        rankTable.getColumnModel().getColumn(5).setPreferredWidth(110);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("파워 랭킹", new JScrollPane(rankTable));
        tabs.addTab("내 선수", new JScrollPane(mineTable));
        JPanel medals = new JPanel(new BorderLayout(0, 8));
        medals.add(medalSummary, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(medalTable), new JScrollPane(entryTable));
        split.setResizeWeight(0.5);
        medals.add(split, BorderLayout.CENTER);
        tabs.addTab("메달 · 대회 출전", medals);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private WeaponType weapon() { return (WeaponType) weaponCombo.getSelectedItem(); }

    private static String delta(double d) {
        if (d >= 1) return "▲ " + Math.round(d);
        if (d <= -1) return "▼ " + Math.round(-d);
        return "-";
    }

    private void refresh(boolean force) {
        GameState s = state();
        WeaponType w = weapon();
        List<RankEntry> all = s.getRanking(w);
        StringBuilder sig = new StringBuilder().append(w).append(koreanOnly.isSelected()).append(s.getMedals().size());
        sig.append((int) all.get(0).rating).append(all.size());
        for (RankEntry e : all) if (e.isMine()) sig.append((int) e.rating);
        if (!force && s.getDay() == lastDay && sig.toString().equals(lastSig)) return;
        lastDay = s.getDay();
        lastSig = sig.toString();

        // 한국 순위
        int korRank = 0;
        int[] korRanks = new int[all.size()];
        for (int i = 0; i < all.size(); i++) {
            if ("KOR".equals(all.get(i).nation)) korRank++;
            korRanks[i] = "KOR".equals(all.get(i).nation) ? korRank : 0;
        }

        shown = new ArrayList<>();
        UI.model(rankTable).setRowCount(0);
        for (int i = 0; i < all.size(); i++) {
            RankEntry e = all.get(i);
            if (koreanOnly.isSelected() && !"KOR".equals(e.nation)) continue;
            shown.add(e);
            UI.model(rankTable).addRow(new Object[]{e.rank, korRanks[i] == 0 ? "-" : korRanks[i], e.name + (e.isMine() ? "  (내 선수)" : ""),
                    e.nation, Math.round(e.rating), e.getTier(), String.format("%.1f", e.levelEquivalent),
                    e.wins + "승 " + e.losses + "패", delta(e.delta)});
        }

        // 요약
        RankEntry best = null;
        for (RankEntry e : all) if (e.isMine()) { best = e; break; }
        if (best == null) {
            summary.setText("<html>" + w + "부 선수가 없어요. 선수 관리에서 영입하면 랭킹에 올라요.<br>레이팅이 높을수록 센 선수이고, 이기면 오르고 지면 내려가요. 자기보다 센 상대를 이기면 더 많이 올라요.</html>");
        } else {
            summary.setText("<html><b>" + w + "부</b> 내 최고 선수: <b>" + best.name + "</b> - 세계 <b>" + best.rank + "위</b> / 파워 레이팅 <b>"
                    + Math.round(best.rating) + "</b> (" + best.getTier() + ")<br>올림픽 출전 조건: 파워 레이팅 8위 이내 "
                    + (best.rank <= 8 ? "<font color='green'>(충족!)</font>" : "<font color='gray'>(" + (best.rank - 8) + "계단 남음)</font>") + "</html>");
        }

        // 내 선수 (전 종목)
        UI.model(mineTable).setRowCount(0);
        for (WeaponType ww : WeaponType.values()) {
            List<RankEntry> list = s.getRanking(ww);
            int kr = 0;
            for (RankEntry e : list) {
                if ("KOR".equals(e.nation)) kr++;
                if (e.isMine()) {
                    UI.model(mineTable).addRow(new Object[]{e.name, ww.getDisplayName(), Math.round(e.rating), e.getTier(),
                            e.rank + "위", kr + "위", e.wins + "승 " + e.losses + "패", delta(e.delta)});
                }
            }
        }

        // 메달
        List<MedalRecord> medals = s.getMedals();
        int g = 0, si = 0, br = 0;
        UI.model(medalTable).setRowCount(0);
        for (int i = medals.size() - 1; i >= 0; i--) {
            MedalRecord m = medals.get(i);
            UI.model(medalTable).addRow(new Object[]{m.getDay() + "일차", m.getRegion().getDisplayName(), m.getWeapon().getDisplayName(),
                    m.isTeam() ? "단체전" : "개인전", m.getMedalName(), m.getWho()});
        }
        for (MedalRecord m : medals) {
            if (m.getRank() == 1) g++;
            else if (m.getRank() == 2) si++;
            else br++;
        }
        medalSummary.setText("<html>메달 합계: <b>금 " + g + " / 은 " + si + " / 동 " + br + "</b></html>");

        // 대회별 출전 상태
        UI.model(entryTable).setRowCount(0);
        for (Region r : Region.values()) {
            if (!r.isTournament()) continue;
            for (WeaponType ww : WeaponType.values()) {
                List<Fencer> team = new ArrayList<>();
                for (Fencer f : s.getRoster()) if (f.getWeapon() == ww) team.add(f);
                String status;
                if (team.isEmpty()) status = "이 종목 선수가 없어요";
                else {
                    String why = s.canEnter(r, ww, team);
                    status = why == null ? "출전 가능!" : why;
                }
                UI.model(entryTable).addRow(new Object[]{r.getDisplayName(), ww.getDisplayName(), status});
            }
        }
    }

    @Override
    public void onShow() { refresh(true); }

    @Override
    public void onTick() { refresh(false); }
}
