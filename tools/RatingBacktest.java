import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 파워 레이팅 백테스트.
 * 실제 경기 엔진(Battle)으로 많은 경기를 치르고, 레이팅이 결과를 얼마나 잘 예측했는지 채점한다.
 *
 *  1부. 예측 정확도: 경기 전 레이팅으로 이길 확률을 계산하고 실제 결과와 비교한다.
 *      - 정확도(레이팅이 높은 쪽이 이긴 비율), Brier 점수, 로그 손실, 구간별 신뢰도 표
 *      - 우도가 가장 높은 "레벨 1칸당 점수"를 찾아 현재 설정(POINTS_PER_LEVEL)과 비교한다.
 *  2부. 갱신 방식(K값) 비교: 모든 선수의 레이팅을 같은 값에서 시작해 경기를 치르며 갱신하고,
 *      K값마다 진짜 실력으로 수렴하는 속도와 예측 손실을 비교한다.
 *
 * 실행 (프로젝트를 컴파일한 폴더에서):
 *   java -cp out RatingBacktest [경기수=2500] [시드=1] [--csv 경기기록.csv]
 */
public class RatingBacktest {
    static final Battle.Options OPT = new Battle.Options();

    static {
        OPT.rewards = false;
        OPT.rated = false;
    }

    /** 실제 엔진으로 한 판. a 가 이기면 true */
    static boolean play(GameState s, Fencer a, Fencer b) {
        Battle bt = new Battle(s, Region.CLUB, List.of(a), false, List.of(b), OPT);
        int guard = 0;
        while (!bt.isOver() && guard++ < 6000) {
            if (bt.isOnBreak()) bt.continueFromBreak();
            else bt.choose(bt.suggest());
            bt.drainEvents();
            bt.drainLog();
        }
        return bt.isWon();
    }

    static double expected(double d, double scale) {
        return 1.0 / (1.0 + Math.pow(10, -scale * d / 400.0));
    }

    public static void main(String[] args) throws Exception {
        int n = 2500;
        long seed = 1;
        String csv = null;
        List<String> pos = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--csv") && i + 1 < args.length) csv = args[++i];
            else pos.add(args[i]);
        }
        if (pos.size() > 0) n = Integer.parseInt(pos.get(0));
        if (pos.size() > 1) seed = Long.parseLong(pos.get(1));

        Random rnd = new Random(seed);
        GameState state = GameState.newGame();
        FencingWorld world = new FencingWorld(new Random(seed));

        // ---------------- 1부: 예측 정확도 ----------------
        double[] rA = new double[n], rB = new double[n], pwrA = new double[n], pwrB = new double[n];
        boolean[] win = new boolean[n];
        WeaponType[] wp = new WeaponType[n];
        for (int i = 0; i < n; i++) {
            WeaponType w = WeaponType.values()[rnd.nextInt(3)];
            List<Rival> pool = world.getRivals(w);
            Rival a = pool.get(rnd.nextInt(pool.size()));
            Rival b;
            do {   // 실력이 비슷한 상대와 먼 상대가 모두 나오도록 무작위로 뽑는다.
                b = pool.get(rnd.nextInt(pool.size()));
            } while (b == a);
            Fencer fa = a.toFencer(), fb = b.toFencer();
            rA[i] = a.getRating();
            rB[i] = b.getRating();
            pwrA[i] = RatingSystem.baseRating(fa);   // 실제로 만들어진 선수의 능력치 기준 레이팅
            pwrB[i] = RatingSystem.baseRating(fb);
            win[i] = play(state, fa, fb);
            wp[i] = w;
        }

        System.out.println("=== 1부. 예측 정확도 (경기 " + n + "판, 시드 " + seed + ") ===");
        score("라이벌 레이팅 기준", rA, rB, win, 1.0);
        score("능력치 기준(실제 선수)", pwrA, pwrB, win, 1.0);

        // 종목별
        for (WeaponType w : WeaponType.values()) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < n; i++) if (wp[i] == w) idx.add(i);
            double[] a = new double[idx.size()], b = new double[idx.size()];
            boolean[] y = new boolean[idx.size()];
            for (int k = 0; k < idx.size(); k++) {
                a[k] = pwrA[idx.get(k)];
                b[k] = pwrB[idx.get(k)];
                y[k] = win[idx.get(k)];
            }
            score(w.getDisplayName() + " (" + idx.size() + "판)", a, b, y, 1.0);
        }

        // 신뢰도 표: 예측한 확률대로 이겼는가
        System.out.println("\n예측 확률 구간별 실제 승률 (능력치 기준)");
        System.out.println(String.format("%-12s %6s %8s %8s", "구간", "판수", "예측", "실제"));
        for (int lo = 0; lo < 100; lo += 10) {
            double sp = 0;
            int cnt = 0, wins = 0;
            for (int i = 0; i < n; i++) {
                double p = expected(pwrA[i] - pwrB[i], 1.0);
                if (p * 100 >= lo && p * 100 < lo + 10) {
                    cnt++;
                    sp += p;
                    if (win[i]) wins++;
                }
            }
            if (cnt >= 8) System.out.println(String.format("%2d~%3d%%    %6d %7.1f%% %7.1f%%", lo, lo + 10, cnt, 100 * sp / cnt, 100.0 * wins / cnt));
        }

        // 가장 잘 맞는 배율 찾기 (우도 최대): p = 1 / (1 + 10^(-scale * 차이 / 400))
        double bestScale = 1, bestLl = -1e18;
        for (double sc = 0.4; sc <= 2.0001; sc += 0.02) {
            double ll = 0;
            for (int i = 0; i < n; i++) {
                double p = Math.min(0.999, Math.max(0.001, expected(pwrA[i] - pwrB[i], sc)));
                ll += Math.log(win[i] ? p : 1 - p);
            }
            if (ll > bestLl) {
                bestLl = ll;
                bestScale = sc;
            }
        }
        System.out.println(String.format("\n가장 잘 맞는 배율: %.2f  →  레벨 1칸당 약 %.0f점 (현재 설정 %.0f점)",
                bestScale, RatingSystem.POINTS_PER_LEVEL / bestScale, RatingSystem.POINTS_PER_LEVEL));
        System.out.println("  ※ 배율이 1.00에 가까울수록 현재 레이팅 환산이 실제 엔진과 잘 맞아요.");
        System.out.println("     배율이 1보다 크면 실제로는 레벨 차이가 더 크게 작용하고, 작으면 더 작게 작용해요.");
        // 선수 쪽(왼쪽) 유리함: 같은 실력일 때 a 가 이기는 비율
        double adv = 0;
        int near = 0, nearWins = 0;
        for (int i = 0; i < n; i++) {
            if (Math.abs(pwrA[i] - pwrB[i]) < 40) {
                near++;
                if (win[i]) nearWins++;
            }
        }
        if (near > 0) System.out.println(String.format("  실력이 비슷한 경기(차이 40점 미만) %d판에서 선수 쪽 승률 %.1f%% (50%%에서 벗어난 만큼이 진행 순서 유불리)", near, 100.0 * nearWins / near));

        if (csv != null) {
            try (PrintWriter w = new PrintWriter(csv, "UTF-8")) {
                w.println("weapon,rating_a,rating_b,power_a,power_b,a_won");
                for (int i = 0; i < n; i++) w.println(wp[i] + "," + Math.round(rA[i]) + "," + Math.round(rB[i]) + "," + Math.round(pwrA[i]) + "," + Math.round(pwrB[i]) + "," + (win[i] ? 1 : 0));
            }
            System.out.println("경기 기록 저장: " + csv);
        }

        // ---------------- 2부: K값 비교 ----------------
        int m = Math.max(1500, n);
        System.out.println("\n=== 2부. 갱신 방식(K값) 비교: 모두 같은 레이팅으로 시작해 " + m + "판 치렀을 때 ===");
        System.out.println(String.format("%-6s %12s %14s %16s", "K", "순위 상관", "평균 오차(점)", "최근 500판 손실"));
        WeaponType w = WeaponType.SABRE;
        List<Rival> pool = world.getRivals(w);
        int size = pool.size();
        double[] truth = new double[size];
        for (int i = 0; i < size; i++) truth[i] = pool.get(i).getRating();
        // 같은 대진·같은 결과를 모든 K 에 쓰기 위해, 먼저 경기를 한 번만 치러 결과를 저장한다.
        int[] ia = new int[m], ib = new int[m];
        boolean[] res = new boolean[m];
        Random r2 = new Random(seed + 99);
        for (int i = 0; i < m; i++) {
            ia[i] = r2.nextInt(size);
            do {
                ib[i] = r2.nextInt(size);
            } while (ib[i] == ia[i]);
            res[i] = play(state, pool.get(ia[i]).toFencer(), pool.get(ib[i]).toFencer());
        }
        double mean = Arrays.stream(truth).average().orElse(1400);
        for (double k : new double[]{4, 8, 16, 24, 32, 48}) {
            double[] est = new double[size];
            Arrays.fill(est, mean);
            double tail = 0;
            int tailN = 0;
            for (int i = 0; i < m; i++) {
                double p = RatingSystem.expected(est[ia[i]], est[ib[i]]);
                if (i >= m - 500) {
                    double pc = Math.min(0.999, Math.max(0.001, p));
                    tail += -Math.log(res[i] ? pc : 1 - pc);
                    tailN++;
                }
                double d = k * ((res[i] ? 1 : 0) - p);
                est[ia[i]] += d;
                est[ib[i]] -= d;
            }
            System.out.println(String.format("%-6.0f %12.3f %14.1f %16.4f", k, spearman(est, truth), meanAbsErr(est, truth), tail / tailN));
        }
        System.out.println("  ※ 순위 상관은 1에 가까울수록, 평균 오차와 손실은 작을수록 좋아요. (현재 게임: 경기 16~24, 대회 12~20)");
    }

    static void score(String label, double[] a, double[] b, boolean[] y, double scale) {
        int n = y.length, correct = 0, decided = 0;
        double brier = 0, ll = 0;
        for (int i = 0; i < n; i++) {
            double p = Math.min(0.999, Math.max(0.001, expected(a[i] - b[i], scale)));
            brier += (p - (y[i] ? 1 : 0)) * (p - (y[i] ? 1 : 0));
            ll += -Math.log(y[i] ? p : 1 - p);
            if (Math.abs(a[i] - b[i]) > 1e-9) {
                decided++;
                if ((a[i] > b[i]) == y[i]) correct++;
            }
        }
        System.out.println(String.format("%-22s 정확도 %5.1f%% | Brier %.4f (무작위 0.2500) | 로그손실 %.4f (무작위 0.6931)",
                label, 100.0 * correct / Math.max(1, decided), brier / n, ll / n));
    }

    static double meanAbsErr(double[] est, double[] truth) {
        // 평균이 같아지도록 맞춘 뒤 오차를 잰다 (레이팅은 상대값이기 때문)
        double me = Arrays.stream(est).average().orElse(0), mt = Arrays.stream(truth).average().orElse(0);
        double s = 0;
        for (int i = 0; i < est.length; i++) s += Math.abs((est[i] - me) - (truth[i] - mt));
        return s / est.length;
    }

    static double spearman(double[] x, double[] y) {
        double[] rx = ranks(x), ry = ranks(y);
        double mx = Arrays.stream(rx).average().orElse(0), my = Arrays.stream(ry).average().orElse(0);
        double sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < x.length; i++) {
            sxy += (rx[i] - mx) * (ry[i] - my);
            sxx += (rx[i] - mx) * (rx[i] - mx);
            syy += (ry[i] - my) * (ry[i] - my);
        }
        return sxy / Math.sqrt(sxx * syy);
    }

    static double[] ranks(double[] v) {
        Integer[] idx = new Integer[v.length];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (p, q) -> Double.compare(v[p], v[q]));
        double[] r = new double[v.length];
        for (int i = 0; i < idx.length; i++) r[idx[i]] = i;
        return r;
    }
}
