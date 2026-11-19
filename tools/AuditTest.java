import java.util.*;

/** 의도한 대로 동작하는지 확인하는 점검 모음 */
public class AuditTest {
    static int fails = 0;

    static void check(boolean ok, String msg) {
        System.out.println((ok ? "OK    " : "FAIL  ") + msg);
        if (!ok) fails++;
    }

    static Fencer mk(WeaponType w, int lvl, String name) {
        Fencer f = new Fencer(name, w, Personality.CAREFUL);
        while (f.getLevel() < lvl) f.gainExp(f.expToNext());
        return f;
    }

    static StringBuilder lastLog = new StringBuilder();

    static Battle run(GameState s, List<Fencer> a, List<Fencer> b, Battle.Options o) {
        Battle bt = new Battle(s, Region.CLUB, a, false, b, o);
        lastLog = new StringBuilder();
        int guard = 0;
        while (!bt.isOver() && guard++ < 20000) {
            if (bt.isOnBreak()) bt.continueFromBreak();
            else bt.choose(bt.suggest());
            bt.drainEvents();
            for (Object line : bt.drainLog()) lastLog.append(line).append('\n');
        }
        return bt;
    }

    public static void main(String[] args) {
        GameState s = GameState.newGame();
        Random rnd = new Random(5);

        // 1. 본선 시드 배치: 1~2번 시드는 결승에서만, 1~4번 시드는 준결승 이후에만 만난다.
        for (int m : new int[]{4, 8, 16, 32}) {
            int[] ord = Tournament.seedOrder(m);
            boolean ok = true;
            for (int top = 2; top <= m; top *= 2) {
                int block = m / top;   // 상위 top 명이 서로 다른 블록에 들어가야 한다
                Set<Integer> used = new HashSet<>();
                for (int pos = 0; pos < m; pos++) if (ord[pos] <= top && !used.add(pos / block)) ok = false;
            }
            check(ok, m + "강 시드 배치가 표준 대진이다 (1·2번 시드는 결승에서만 만남)");
        }

        // 2. 경기 길이: 개인전 15점, 단체전 2명 20점 / 3명 45점
        Battle b1 = run(s, List.of(mk(WeaponType.SABRE, 8, "a")), List.of(mk(WeaponType.SABRE, 8, "b")), new Battle.Options());
        check(b1.getFinalScore() == 15, "개인전 목표 점수 15점 (" + b1.getFinalScore() + ")");
        Battle b2 = run(s, List.of(mk(WeaponType.EPEE, 8, "a"), mk(WeaponType.EPEE, 8, "b")),
                List.of(mk(WeaponType.EPEE, 8, "c"), mk(WeaponType.EPEE, 8, "d")), new Battle.Options());
        check(b2.getFinalScore() == 20 && b2.getBoutCount() == 4, "2인 단체전 4경기 20점 (" + b2.getBoutCount() + "경기, " + b2.getFinalScore() + "점)");
        Battle b3 = run(s, List.of(mk(WeaponType.FOIL, 8, "a"), mk(WeaponType.FOIL, 8, "b"), mk(WeaponType.FOIL, 8, "c")),
                List.of(mk(WeaponType.FOIL, 8, "d"), mk(WeaponType.FOIL, 8, "e"), mk(WeaponType.FOIL, 8, "f")), new Battle.Options());
        check(b3.getFinalScore() == 45 && b3.getBoutCount() == 9, "3인 단체전 9경기 45점 (" + b3.getBoutCount() + "경기, " + b3.getFinalScore() + "점)");

        // 3. 예선 풀 경기(5점 1피리어드): 끝까지 진행되고 점수가 규칙에 맞는다
        Battle.Options pool = new Battle.Options();
        pool.target = 5;
        pool.periods = 1;
        pool.rewards = false;
        pool.rated = false;
        int ended = 0, bad = 0, sudden = 0;
        for (int i = 0; i < 300; i++) {
            WeaponType w = WeaponType.values()[i % 3];
            Battle b = run(s, List.of(mk(w, 6 + rnd.nextInt(8), "a")), List.of(mk(w, 6 + rnd.nextInt(8), "b")), pool);
            if (!b.isOver()) continue;
            ended++;
            int ps = b.getPlayerScore(), es = b.getEnemyScore();
            int win = Math.max(ps, es);
            // 승자는 5점에 도달했거나(동시타 포함 6점까지 가능), 시간 종료 시 앞서 있어야 한다.
            if (b.isWon() != (ps > es) && ps != es) bad++;
            if (win < 1 || win > 6) bad++;
        }
        check(ended == 300 && bad == 0, "5점 1피리어드 풀 경기 300판 모두 정상 종료 (종료 " + ended + ", 이상 " + bad + ")");

        // 4. 종목별 규칙: 에페는 동시타, 플뢰레는 우선권 규칙(동시타 불가)
        int[] doubles = new int[3];
        for (WeaponType w : WeaponType.values()) {
            for (int i = 0; i < 60; i++) {
                Battle b = run(s, List.of(mk(w, 8, "a")), List.of(mk(w, 8, "b")), new Battle.Options());
                if (lastLog.toString().contains("동시타!")) doubles[w.ordinal()]++;
            }
        }
        check(doubles[WeaponType.EPEE.ordinal()] > 0 && doubles[WeaponType.FOIL.ordinal()] == 0 && doubles[WeaponType.SABRE.ordinal()] == 0,
                "동시타는 에페에서만 나온다 (60판 중 에페 " + doubles[WeaponType.EPEE.ordinal()] + ", 플뢰레 " + doubles[WeaponType.FOIL.ordinal()] + ", 사브르 " + doubles[WeaponType.SABRE.ordinal()] + ")");

        // 5. 올림픽 출전선: 파워 랭킹 8위가 어느 정도 실력인가
        FencingWorld w0 = new FencingWorld(new Random(11));
        for (WeaponType w : WeaponType.values()) {
            List<Rival> list = w0.getRivals(w);
            list.sort((x, y) -> Double.compare(y.getRating(), x.getRating()));
            System.out.println(String.format("      %s: 1위 Lv%.1f / 8위 Lv%.1f / 한국 최고 Lv%.1f", w.getDisplayName(), list.get(0).getLevel(), list.get(7).getLevel(),
                    list.stream().filter(r -> "KOR".equals(r.getNation())).mapToDouble(Rival::getLevel).max().orElse(0)));
        }
        Random tickRnd = new Random(3);
        for (int d = 1; d <= 200; d++) w0.tick(d, tickRnd);
        List<Rival> sab = w0.getRivals(WeaponType.SABRE);
        sab.sort((x, y) -> Double.compare(y.getRating(), x.getRating()));
        System.out.println(String.format("      200일 뒤 사브르: 1위 Lv%.1f / 8위 Lv%.1f", sab.get(0).getLevel(), sab.get(7).getLevel()));
        check(sab.get(7).getLevel() < RatingSystem.MAX_LEVEL - 2, "200일 뒤에도 올림픽 출전선(8위)이 도달 가능한 레벨이다");

        // 6. 레이팅 반영: 이기면 오르고 지면 내려간다. 강한 상대를 이기면 더 오른다.
        Fencer me = mk(WeaponType.SABRE, 10, "me");
        double r0 = me.getRating();
        me.applyRating(r0 + 200, true, 20);
        double up = me.getRating() - r0;
        Fencer me2 = mk(WeaponType.SABRE, 10, "me2");
        me2.applyRating(me2.getRating() - 200, true, 20);
        double smallUp = me2.getRating() - r0;
        Fencer me3 = mk(WeaponType.SABRE, 10, "me3");
        me3.applyRating(me3.getRating(), false, 20);
        check(up > smallUp && smallUp > 0 && me3.getRating() < r0, String.format("레이팅 반영: 강한 상대 승 +%.1f > 약한 상대 승 +%.1f, 패배 %.1f", up, smallUp, me3.getRating() - r0));

        // 7. 훈련하면 레이팅이 오른다 (장비/훈련 모두 능력치에 반영)
        Fencer t = mk(WeaponType.SABRE, 10, "t");
        double before = t.getRating();
        for (int i = 0; i < 5; i++) t.gainExp(t.expToNext());
        check(t.getRating() > before + 100, String.format("성장하면 레이팅이 오른다 (%.0f → %.0f)", before, t.getRating()));

        // 8. 기권 기록: 앞서다 기권해도 패배로 정리된다
        List<Fencer> mine = new ArrayList<>(List.of(mk(WeaponType.SABRE, 12, "기권")));
        Tournament tour = Tournament.create(s, Region.OLYMPIC, WeaponType.SABRE, mine);
        Tournament.Match m = tour.nextPlayerMatch();
        tour.recordPlayerMatch(m, 9, 4, false);
        check(m.winner != null && m.winner != tour.getMe() && m.sa + m.sb > 0 && (m.sa == 15 || m.sb == 15), "점수가 앞서다 기권하면 몰수패로 기록 (" + m.sa + ":" + m.sb + ")");

        System.out.println(fails == 0 ? "\nALL OK" : "\nFAILS=" + fails);
    }
}
