import java.io.*;
import java.util.*;

public class FlowTest {
    static Fencer mk(WeaponType w, int lvl, String name) {
        Fencer f = new Fencer(name, w, Personality.CAREFUL);
        while (f.getLevel() < lvl) f.gainExp(f.expToNext());
        return f;
    }

    static void autoBattle(Battle b) {
        int guard = 0;
        while (!b.isOver() && guard++ < 8000) {
            if (b.isOnBreak()) b.continueFromBreak();
            else b.choose(b.suggest());
            b.drainEvents();
            b.drainLog();
        }
        if (!b.isOver()) throw new IllegalStateException("battle did not end");
    }

    static void check(boolean c, String msg) {
        if (!c) throw new IllegalStateException("FAIL: " + msg);
        System.out.println("OK   " + msg);
    }

    public static void main(String[] a) throws Exception {
        GameState s = GameState.newGame();
        // 선수 보강: 사브르 3명 (Lv14), 플뢰레 1명
        for (Fencer f : new ArrayList<>(s.getRoster())) s.getRoster().remove(f);
        List<Fencer> team = new ArrayList<>();
        for (int i = 0; i < 3; i++) { Fencer f = mk(WeaponType.SABRE, 14, "사브르" + i); s.addFencer(f); team.add(f); }
        Fencer solo = mk(WeaponType.FOIL, 13, "플뢰레솔로"); s.addFencer(solo);

        // 1. 잠금 / 랭킹 / 출전 조건
        check(s.canEnter(Region.NATIONAL, WeaponType.SABRE, team) != null, "한국 선수권은 승리 수가 모자라면 잠겨 있다: " + s.canEnter(Region.NATIONAL, WeaponType.SABRE, team));
        for (int i = 0; i < 40; i++) s.recordResult(true, Region.CLUB, false, WeaponType.SABRE);
        check(s.canEnter(Region.NATIONAL, WeaponType.SABRE, team) != null, "승리 수가 충분해도 대회 기간이 아니면 출전할 수 없다: " + s.canEnter(Region.NATIONAL, WeaponType.SABRE, team));
        for (int i = 0; i < 25; i++) s.nextDay();   // 자국 선수권 기간(20~27일)으로
        check(s.canEnter(Region.NATIONAL, WeaponType.SABRE, team) == null, "40승 + 대회 기간이면 자국 선수권 출전 가능");
        System.out.println("     올림픽 사브르: " + s.canEnter(Region.OLYMPIC, WeaponType.SABRE, team));
        List<RankEntry> rk = s.getRanking(WeaponType.SABRE);
        System.out.println("     사브르 랭킹 1위 " + rk.get(0).name + " " + Math.round(rk.get(0).rating) + ", 8위 " + Math.round(rk.get(7).rating) + ", 내 최고 순위 " + s.bestRank(WeaponType.SABRE, team));
        check(rk.size() == 32 + 15 * 7 + 3, "랭킹 인원 수 " + rk.size());

        // 2. 한국 선수권 개인전 (플뢰레는 승리 수가 없으므로 사브르 1명으로)
        List<Fencer> one = new ArrayList<>(team.subList(0, 1));
        Tournament t = Tournament.create(s, Region.NATIONAL, WeaponType.SABRE, one);
        int matches = 0;
        while (!t.isFinished()) {
            Tournament.Match m = t.nextPlayerMatch();
            if (m == null) throw new IllegalStateException("stuck");
            Battle b = new Battle(s, t.getRegion(), t.getMyFencers(), false, t.enemyFencers(m), t.optionsFor(m));
            autoBattle(b);
            boolean won = b.isWon();
            int ms = b.getPlayerScore(), os = b.getEnemyScore();
            b.settle();
            t.recordPlayerMatch(m, ms, os, won);
            matches++;
        }
        System.out.println("     한국 선수권 결과: " + t.getPlacementText() + " (내 경기 " + matches + "판)");
        int goldBefore = s.getGold();
        String sum = s.finishTournament(t);
        System.out.println(sum.replace("\n", "\n       "));
        check(s.getGold() >= goldBefore, "정산 후 골드 증가/유지");
        check(!one.get(0).isBusy(), "대회 후 선수는 시합 중이 아니다");

        // 3. 세계 선수권 단체전 (실제 경기, 3명)
        for (int i = 0; i < 30; i++) s.nextDay();   // 세계 선수권 기간(50~59일)으로
        t = Tournament.create(s, Region.WORLD, WeaponType.SABRE, team);
        while (!t.isFinished()) {
            Tournament.Match m = t.nextPlayerMatch();
            Battle b = new Battle(s, t.getRegion(), t.getMyFencers(), false, t.enemyFencers(m), t.optionsFor(m));
            autoBattle(b);
            boolean won = b.isWon(); int ms = b.getPlayerScore(), os = b.getEnemyScore();
            b.settle();
            t.recordPlayerMatch(m, ms, os, won);
        }
        System.out.println("     세계 선수권 단체전: " + t.getPlacementText());
        System.out.println("       " + s.finishTournament(t).replace("\n", "\n       "));

        // 4. 올림픽 (시뮬레이션) + 시상식 데이터
        Tournament oly = Tournament.create(s, Region.OLYMPIC, WeaponType.SABRE, team);
        while (!oly.isFinished()) oly.simulatePlayerMatch(oly.nextPlayerMatch());
        System.out.println("     올림픽 단체전: " + oly.getPlacementText() + " 금 " + oly.getChampion() + " 은 " + oly.getRunnerUp() + " 동 " + oly.getBronzeEntries());
        s.finishTournament(oly);

        // 5. 레이팅: 내 선수 기록, 일일 진행, 메달
        for (int d = 0; d < 30; d++) s.nextDay();
        for (RankEntry e : s.getRanking(WeaponType.SABRE)) if (e.isMine()) System.out.println("     내 선수 " + e.name + " 순위 " + e.rank + " 레이팅 " + Math.round(e.rating) + " (" + e.getTier() + ") 변동 " + String.format("%.1f", e.delta) + " " + e.wins + "승 " + e.losses + "패");
        System.out.println("     메달 " + s.getMedals().size() + "개: " + s.getMedals().stream().map(MedalRecord::getMedalName).reduce("", (x, y) -> x + y + " "));

        // 6. 저장/불러오기
        File f = new File("test-save.dat");
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(f))) { o.writeObject(s); }
        GameState loaded;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) { loaded = (GameState) in.readObject(); }
        check(loaded.getMedals().size() == s.getMedals().size(), "저장/불러오기 후 메달 기록 유지");
        check(loaded.getRanking(WeaponType.EPEE).size() == s.getRanking(WeaponType.EPEE).size(), "저장/불러오기 후 세계 랭킹 유지");
        f.delete();
        System.out.println("ALL DONE");
    }
}
