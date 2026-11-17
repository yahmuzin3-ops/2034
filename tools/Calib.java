import java.util.*;

public class Calib {
    static Fencer mk(WeaponType w, int lvl) {
        Fencer f = new Fencer("t", w, Personality.CAREFUL);
        while (f.getLevel() < lvl) f.gainExp(f.expToNext());
        return f;
    }

    static boolean play(GameState s, WeaponType w1, int l1, WeaponType w2, int l2, Battle.Options o) {
        Fencer a = mk(w1, l1), b = mk(w2, l2);
        Battle bt = new Battle(s, Region.CLUB, List.of(a), false, List.of(b), o);
        int guard = 0;
        while (!bt.isOver() && guard++ < 5000) {
            if (bt.isOnBreak()) bt.continueFromBreak();
            else bt.choose(bt.suggest());
            bt.drainEvents();
            bt.drainLog();
        }
        return bt.isWon();
    }

    public static void main(String[] args) {
        GameState s = GameState.newGame();
        Battle.Options o = new Battle.Options();
        o.rewards = false; o.rated = false;
        int N = Integer.parseInt(args.length > 0 ? args[0] : "400");
        int[] deltas = {0, 1, 2, 3, 4, 6, 8};
        for (WeaponType w : WeaponType.values()) {
            for (int base : new int[]{6, 14}) {
                StringBuilder sb = new StringBuilder(w + " base Lv" + base + ": ");
                for (int d : deltas) {
                    int win = 0;
                    for (int i = 0; i < N; i++) if (play(s, w, base + d, w, base, o)) win++;
                    double p = win / (double) N;
                    double pc = Math.min(0.995, Math.max(0.005, p));
                    double elo = 400 * Math.log10(pc / (1 - pc));
                    sb.append(String.format("d%d=%.2f(elo %.0f) ", d, p, elo));
                }
                System.out.println(sb);
            }
        }
    }
}
