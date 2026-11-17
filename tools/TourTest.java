import java.util.*;

public class TourTest {
    static Fencer mk(WeaponType w, int lvl, String name) {
        Fencer f = new Fencer(name, w, Personality.CAREFUL);
        while (f.getLevel() < lvl) f.gainExp(f.expToNext());
        return f;
    }

    static void checkBracket(Tournament t) {
        List<List<Tournament.Match>> rounds = t.getRounds();
        for (int i = 0; i < rounds.size(); i++) {
            int expect = (i == 0) ? rounds.get(0).size() : rounds.get(i - 1).size() / 2;
            if (rounds.get(i).size() != expect) throw new IllegalStateException("round size " + i);
            for (Tournament.Match m : rounds.get(i)) {
                if (m.a == null || m.b == null || m.a == m.b) throw new IllegalStateException("bad match");
            }
        }
        // 같은 선수가 한 라운드에 두 번 나오지 않는다
        for (List<Tournament.Match> r : rounds) {
            Set<Tournament.Entry> seen = new HashSet<>();
            for (Tournament.Match m : r) {
                if (!seen.add(m.a) || !seen.add(m.b)) throw new IllegalStateException("dup entry in round");
            }
        }
    }

    public static void main(String[] args) {
        GameState s = GameState.newGame();
        int runs = 60;
        int[] levels = {6, 12, 18, 24};
        int problems = 0;
        for (Region r : new Region[]{Region.NATIONAL, Region.WORLD, Region.OLYMPIC}) {
            for (WeaponType w : WeaponType.values()) {
                for (int teamSize : new int[]{1, 3}) {
                    StringBuilder sb = new StringBuilder(String.format("%-6s %-4s %s: ", r.getDisplayName(), w, teamSize == 1 ? "개인" : "단체"));
                    for (int lv : levels) {
                        int gold = 0, medal = 0, out = 0;
                        double avgPlace = 0;
                        for (int i = 0; i < runs; i++) {
                            List<Fencer> mine = new ArrayList<>();
                            for (int k = 0; k < teamSize; k++) mine.add(mk(w, lv, "내선수" + k));
                            Tournament t = Tournament.create(s, r, w, mine);
                            int guard = 0;
                            while (!t.isFinished() && guard++ < 100) {
                                Tournament.Match m = t.nextPlayerMatch();
                                if (m == null) { problems++; System.out.println("STUCK " + r + " " + w + " stage " + t.getStage() + " out " + t.isMeOut()); break; }
                                t.simulatePlayerMatch(m);
                            }
                            if (!t.isFinished()) { problems++; continue; }
                            try { checkBracket(t); } catch (RuntimeException e) { problems++; System.out.println("BRACKET " + e.getMessage() + " " + r + " " + w); }
                            if (t.getMedal() == 1) gold++;
                            if (t.getMedal() > 0) medal++;
                            if (t.getChampion() == null) { problems++; System.out.println("NO CHAMPION"); }
                            if (r == Region.OLYMPIC && t.getBronze() == null) { problems++; System.out.println("NO BRONZE"); }
                            if (t.getBronzeEntries().isEmpty() && t.getMedal() == 3) { problems++; System.out.println("BRONZE LIST"); }
                        }
                        sb.append(String.format("Lv%d 금%2d%% 메달%3d%% | ", lv, gold * 100 / runs, medal * 100 / runs));
                    }
                    System.out.println(sb);
                }
            }
        }
        System.out.println("problems=" + problems);
    }
}
