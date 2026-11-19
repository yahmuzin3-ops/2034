import java.io.*;
import java.util.*;

/** 소속 국가 선택이 대회·랭킹·저장에 제대로 반영되는지 확인한다. */
public class NationTest {
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

    public static void main(String[] a) throws Exception {
        GameState s = GameState.newGame();
        check("KOR".equals(s.getNation()) && Region.NATIONAL.getDisplayName().equals("한국 선수권"), "기본 소속은 한국, 자국 선수권 이름은 '" + Region.NATIONAL.getDisplayName() + "'");

        for (String code : FencingWorld.nationCodes()) {
            s.setNation(code);
            boolean ok = true;
            StringBuilder why = new StringBuilder();
            for (WeaponType w : WeaponType.values()) {
                int domestic = 0;
                for (Rival r : s.getWorld().getRivals(w)) if (code.equals(r.getNation())) domestic++;
                if (domestic < 32) { ok = false; why.append(w + " 국내 선수 " + domestic + "명 "); }
                for (int teamSize : new int[]{1, 3}) {
                    List<Fencer> mine = new ArrayList<>();
                    for (int i = 0; i < teamSize; i++) mine.add(mk(w, 10, "내" + i));
                    for (Region r : new Region[]{Region.NATIONAL, Region.WORLD, Region.OLYMPIC}) {
                        Tournament t = Tournament.create(s, r, w, mine);
                        int guard = 0;
                        while (!t.isFinished() && guard++ < 100) {
                            Tournament.Match m = t.nextPlayerMatch();
                            if (m == null) { ok = false; why.append("멈춤 " + r + " "); break; }
                            t.simulatePlayerMatch(m);
                        }
                        if (!t.isFinished()) { ok = false; continue; }
                        // 자국 선수권: 상대가 모두 자국 소속 / 세계·올림픽 단체전: 상대 대표팀에 자국이 없다
                        for (List<Tournament.Match> round : t.getRounds()) for (Tournament.Match m : round) for (Tournament.Entry e : new Tournament.Entry[]{m.a, m.b}) {
                            if (e == null || e == t.getMe()) continue;
                            if (r == Region.NATIONAL && !code.equals(e.nation)) { ok = false; why.append("자국 선수권에 외국 선수 " + e.name + " "); }
                            if (r != Region.NATIONAL && teamSize > 1 && code.equals(e.nation)) { ok = false; why.append("단체전 상대에 자국 대표팀 "); }
                        }
                        if (!t.getTitle().contains(r == Region.NATIONAL ? FencingWorld.shortName(code) + " 선수권" : r.getDisplayName())) { ok = false; why.append("제목 " + t.getTitle() + " "); }
                    }
                }
            }
            if (!ok) check(false, code + ": " + why);
        }
        check(fails == 0, "16개 국가 모두 대회(자국/세계/올림픽, 개인/단체) 정상 진행");

        // 랭킹: 내 선수의 소속이 바뀐다
        s.setNation("FRA");
        s.addFencer(mk(WeaponType.SABRE, 12, "소속테스트"));
        boolean mineFra = false;
        for (RankEntry e : s.getRanking(WeaponType.SABRE)) if (e.isMine()) mineFra = "FRA".equals(e.nation);
        check(mineFra && Region.NATIONAL.getDisplayName().equals("프랑스 선수권"), "랭킹의 내 선수 소속이 프랑스, 대회 이름 '" + Region.NATIONAL.getDisplayName() + "'");

        // 저장/불러오기
        File f = new File("nation-save.dat");
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(f))) { o.writeObject(s); }
        Region.setHomeNation("KOR");
        GameState loaded;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) { loaded = (GameState) in.readObject(); }
        f.delete();
        check("FRA".equals(loaded.getNation()) && Region.NATIONAL.getDisplayName().equals("프랑스 선수권"), "저장/불러오기 후 소속 국가와 대회 이름 유지");

        // 예전 저장 파일(소속 국가 없음)은 한국으로
        if (a.length > 0) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(a[0]))) {
                GameState old = (GameState) in.readObject();
                check("KOR".equals(old.getNation()) && Region.NATIONAL.getDisplayName().equals("한국 선수권"), "예전 저장 파일은 한국 소속으로 불러온다");
            }
        }
        System.out.println(fails == 0 ? "\nALL OK" : "\nFAILS=" + fails);
    }
}
