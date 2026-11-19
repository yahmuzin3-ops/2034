import java.io.*;
import java.util.*;

/** 국기 / 라이벌 / 트로피 룸 / 시즌 캘린더 / 코치·스태프 / 경기 통계가 제대로 동작하는지 확인한다. */
public class FeatureTest {
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

    static Battle autoBattle(GameState s, List<Fencer> a, List<Fencer> b, Battle.Options o) {
        Battle bt = new Battle(s, Region.CLUB, a, false, b, o);
        int guard = 0;
        while (!bt.isOver() && guard++ < 20000) {
            if (bt.isOnBreak()) bt.continueFromBreak();
            else bt.choose(bt.suggest());
            bt.drainEvents();
            bt.drainLog();
        }
        return bt;
    }

    public static void main(String[] args) throws Exception {
        GameState s = GameState.newGame();

        // ---------- 시즌 캘린더 ----------
        SeasonCalendar.Event n1 = SeasonCalendar.next(Region.NATIONAL, 1);
        check(n1 != null && n1.start == 20 && n1.end == 27, "첫 자국 선수권은 20~27일 (" + (n1 == null ? "-" : n1.start + "~" + n1.end) + ")");
        check(SeasonCalendar.current(Region.WORLD, 55) != null && SeasonCalendar.current(Region.WORLD, 60) == null, "세계 선수권은 50~59일에만 열린다");
        SeasonCalendar.Event oly = SeasonCalendar.next(Region.OLYMPIC, 1);
        check(oly != null && oly.start == 220, "첫 올림픽은 220일째 (" + (oly == null ? "-" : oly.start) + ")");
        SeasonCalendar.Event oly2 = SeasonCalendar.next(Region.OLYMPIC, 300);
        check(oly2 != null && oly2.start == 700, "다음 올림픽은 4시즌 뒤인 700일째 (" + (oly2 == null ? "-" : oly2.start) + ")");
        check("시즌 1 · 3월 1일".equals(SeasonCalendar.dateText(21)) && "시즌 2 · 1월 1일".equals(SeasonCalendar.dateText(121)), "날짜 표기: " + SeasonCalendar.dateText(21) + " / " + SeasonCalendar.dateText(121));

        List<Fencer> team = new ArrayList<>(List.of(mk(WeaponType.SABRE, 12, "시즌선수")));
        s.addFencer(team.get(0));
        for (int i = 0; i < 40; i++) s.recordResult(true, Region.CLUB, false, WeaponType.SABRE);
        String closed = s.canEnter(Region.NATIONAL, WeaponType.SABRE, team);
        check(closed != null && closed.contains("대회 기간"), "대회 기간 전에는 출전 불가: " + closed);
        for (int i = 0; i < 20; i++) s.nextDay();    // 21일째
        check(s.canEnter(Region.NATIONAL, WeaponType.SABRE, team) == null, "대회 기간(21일째)에는 출전 가능");
        s.markEntered(Region.NATIONAL, WeaponType.SABRE);
        String again = s.canEnter(Region.NATIONAL, WeaponType.SABRE, team);
        check(again != null && again.contains("이미 출전"), "같은 대회에는 같은 종목으로 두 번 출전할 수 없다: " + again);
        for (int i = 0; i < 62; i++) s.nextDay();    // 83일째: 두 번째 자국 선수권
        check(s.canEnter(Region.NATIONAL, WeaponType.SABRE, team) == null, "다음 대회(83일째)에는 다시 출전 가능");

        // ---------- 국기 ----------
        boolean flagsOk = true;
        for (String c : FencingWorld.nationCodes()) flagsOk &= FlagArt.icon(c, 30, 20).getIconWidth() == 30;
        FlagArt.icon("???", 30, 20);
        FlagArt.icon(null, 30, 20);
        check(flagsOk, "16개국 국기 아이콘 생성 (모르는 코드도 오류 없이 처리)");

        // ---------- 코치 · 스태프 ----------
        s.addGold(5000);
        double trainBase = s.getTrainingMultiplier();
        int restBase = s.getRestBonus();
        Staff coach = new Staff(Staff.Role.HEAD_COACH, "테스트코치", 3), physio = new Staff(Staff.Role.PHYSIO, "테스트트레이너", 2);
        Staff analyst = new Staff(Staff.Role.ANALYST, "테스트분석관", 4), nutri = new Staff(Staff.Role.NUTRITIONIST, "테스트영양사", 5);
        // 후보 목록에 넣어서 영입 (영입은 후보에서만 가능)
        s.refreshCandidates();
        List<Staff> cands = s.getCandidates();
        check(cands.size() == 8, "영입 후보는 역할마다 2명 (총 " + cands.size() + "명)");
        int gold0 = s.getGold();
        Staff pick = cands.get(0);
        String msg = s.hireStaff(pick);
        check(s.getGold() == gold0 - pick.getHireFee() && s.getStaff(pick.getRole()) != null, "영입하면 영입 비용만큼 골드가 줄고 스태프가 생긴다: " + msg);
        Staff dup = null;
        for (Staff c : s.getCandidates()) if (c.getRole() == pick.getRole()) dup = c;
        check(dup != null && s.hireStaff(dup).contains("이미"), "같은 역할은 두 명 고용할 수 없다");
        check(s.fireStaff(pick.getRole()).contains("종료") && s.getStaff(pick.getRole()) == null, "계약 종료");

        // 효과 확인을 위해 역할별 후보를 직접 고용
        for (Staff.Role role : Staff.Role.values()) {
            Staff c = null;
            for (Staff x : s.getCandidates()) if (x.getRole() == role) { c = x; break; }
            s.hireStaff(c);
        }
        double coachNow = s.getCoachBonus();
        check(s.getTrainingMultiplier() > trainBase && Math.abs(s.getTrainingMultiplier() - trainBase * (1 + coachNow)) < 1e-9, String.format("수석 코치: 훈련 배율 %.3f → %.3f", trainBase, s.getTrainingMultiplier()));
        check(s.getRestBonus() == restBase + 2 * s.staffLevel(Staff.Role.PHYSIO), "트레이너: 휴식 회복 " + restBase + " → " + s.getRestBonus());
        check(s.getPhysioMatchHeal() == 3 * s.staffLevel(Staff.Role.PHYSIO) && s.getPhysioInjuryCut() > 0, "트레이너: 대회 중 회복·부상 확률 감소");
        // 영양사 보너스
        Food lunchItem = new Food("테스트도시락", 10, "시험용", 40, 5);
        s.getInventory().add(lunchItem, 1);
        Fencer eater = mk(WeaponType.EPEE, 5, "먹는선수");
        eater.eat(-60, 0);   // 배고픔을 올린다
        int h0 = eater.getHunger();
        String eatMsg = s.useItem(eater, lunchItem);
        int expectDrop = 40 + (int) Math.round(40 * 0.1 * s.getNutritionLevel());
        check(h0 - eater.getHunger() >= 40 && eatMsg.contains("영양사"), "영양사: 식단 효과 증가 (배고픔 " + h0 + " → " + eater.getHunger() + ", 기대 -" + expectDrop + "): " + eatMsg);
        // 분석관: 정찰 보고서
        Battle sc = new Battle(s, Region.CLUB, List.of(mk(WeaponType.SABRE, 8, "나")), false, List.of(mk(WeaponType.SABRE, 8, "상대")), new Battle.Options());
        String report = sc.getScoutReport();
        check(s.getAnalystLevel() >= 1 && (s.getAnalystLevel() < 2 || report.contains("분석 Lv")), "분석관 Lv" + s.getAnalystLevel() + ": " + report);
        // 승급
        Staff.Role up = null;
        for (Staff.Role r : Staff.Role.values()) if (s.getStaff(r) != null && s.getStaff(r).canPromote()) { up = r; break; }
        if (up != null) {
            int before = s.getStaff(up).getLevel(), g1 = s.getGold();
            int cost = s.getStaff(up).getPromoteCost();
            s.promoteStaff(up);
            check(s.getStaff(up).getLevel() == before + 1 && s.getGold() == g1 - cost, "승급: Lv" + before + " → Lv" + s.getStaff(up).getLevel() + " (-" + cost + "G)");
        }
        // 주급
        int weekly = s.getStaffWeeklyCost();
        while (s.getDay() % Staff.PAY_DAYS != Staff.PAY_DAYS - 1) s.nextDay();
        int g2 = s.getGold();
        s.nextDay();   // 주급일
        check(s.getGold() == g2 - weekly, "주급일에 " + weekly + "G가 나간다 (" + g2 + " → " + s.getGold() + ")");
        // 주급을 못 주면 비싼 스태프부터 떠난다
        s.spendGold(s.getGold());
        int staffCount = s.getStaff().size();
        for (int i = 0; i < 7; i++) s.nextDay();
        check(s.getStaff().size() < staffCount, "골드가 없으면 스태프가 떠난다 (" + staffCount + "명 → " + s.getStaff().size() + "명)");

        // ---------- 경기 통계 ----------
        GameState s2 = GameState.newGame();
        Random rnd = new Random(3);
        boolean consistent = true;
        String why = "";
        for (int i = 0; i < 60; i++) {
            WeaponType w = WeaponType.values()[i % 3];
            boolean teamMatch = i % 6 == 5;
            int n = teamMatch ? 2 : 1;
            List<Fencer> a = new ArrayList<>(), b = new ArrayList<>();
            for (int k = 0; k < n; k++) {
                a.add(mk(w, 6 + rnd.nextInt(8), "내" + k));
                b.add(mk(w, 6 + rnd.nextInt(8), "적" + k));
            }
            Battle.Options o = new Battle.Options();
            o.rewards = false;
            o.rated = false;
            Battle bt = autoBattle(s2, a, b, o);
            bt.settle();
            MatchStat m = s2.getMatchStats().get(s2.getMatchStats().size() - 1);
            int usedSum = Arrays.stream(m.used).sum(), scoredSum = Arrays.stream(m.scored).sum(), oppSum = Arrays.stream(m.oppUsed).sum();
            int perTouches = m.perFencer.values().stream().mapToInt(x -> x[1]).sum(), perPhrases = m.perFencer.values().stream().mapToInt(x -> x[0]).sum();
            if (m.touchesFor != bt.getPlayerScore()) { consistent = false; why += "득점 합 불일치 "; }
            if (m.touchesAgainst != bt.getEnemyScore()) { consistent = false; why += "실점 합 불일치 "; }
            if (usedSum != m.phrases || oppSum != m.phrases) { consistent = false; why += "전술 합 불일치 "; }
            if (scoredSum != m.touchesFor - m.doubles) { consistent = false; why += "전술별 득점 합 불일치 "; }
            if (perTouches != m.touchesFor || perPhrases != m.phrases) { consistent = false; why += "선수별 합 불일치 "; }
            if (m.won != bt.isWon() || m.myScore != bt.getPlayerScore()) { consistent = false; why += "결과 불일치 "; }
            if (w != WeaponType.EPEE && m.doubles > 0) { consistent = false; why += "에페 외 동시타 "; }
        }
        check(consistent && s2.getMatchStats().size() == 60, "경기 통계 60판: 득점·전술·선수별 합계가 모두 일치 " + why);
        for (int i = 0; i < 400; i++) s2.addMatchStat(new MatchStat());
        check(s2.getMatchStats().size() == 400, "통계는 최근 400경기까지만 보관한다");

        // ---------- 라이벌 전적 / 복수전 ----------
        GameState s3 = GameState.newGame();
        Fencer me = mk(WeaponType.SABRE, 12, "라이벌선수");
        s3.addFencer(me);
        Tournament t1 = Tournament.create(s3, Region.NATIONAL, WeaponType.SABRE, new ArrayList<>(List.of(me)));
        int myMatches = 0;
        Map<String, Integer> faced = new HashMap<>();
        while (!t1.isFinished()) {
            Tournament.Match m = t1.nextPlayerMatch();
            faced.merge(m.other(t1.getMe()).name, 1, Integer::sum);
            t1.simulatePlayerMatch(m);
            myMatches++;
        }
        int recorded = s3.getRivalRecords().stream().mapToInt(RivalRecord::games).sum();
        check(recorded == myMatches && s3.getRivalRecords().size() == faced.size(), "개인전 내 경기 " + myMatches + "판이 모두 상대 전적에 기록 (상대 " + s3.getRivalRecords().size() + "명)");
        RivalRecord any = s3.getRivalRecords().get(0);
        check(any.wins + any.losses == any.games() && !any.status().isEmpty(), "상대 전적 상태: " + any.name + " " + any.recordText() + " (" + any.status() + ")");
        // 복수전: 진 기록이 있는 상대가 다시 나오면 알림이 생긴다
        boolean noteSeen = false;
        for (int attempt = 0; attempt < 60 && !noteSeen; attempt++) {
            GameState s4 = GameState.newGame();
            Fencer me4 = mk(WeaponType.SABRE, 12, "복수");
            s4.addFencer(me4);
            Tournament a = Tournament.create(s4, Region.NATIONAL, WeaponType.SABRE, new ArrayList<>(List.of(me4)));
            Rival victim = null;
            for (Tournament.Entry e : a.getEntries()) if (e != a.getMe() && e.rivals != null) { victim = e.rivals.get(0); break; }
            s4.recordRivalMatch(victim, false, 4, 5);
            for (int k = 0; k < 6 && !noteSeen; k++) {
                Tournament b = Tournament.create(s4, Region.NATIONAL, WeaponType.SABRE, new ArrayList<>(List.of(me4)));
                for (String line : b.getLog()) if (line.startsWith("복수전 상대")) noteSeen = true;
            }
        }
        check(noteSeen, "지난번에 진 상대가 대회에 나오면 복수전 알림이 로그에 남는다");
        Tournament.Entry opp0 = null;
        for (Tournament.Entry e : t1.getEntries()) if (e != t1.getMe() && e.rivals != null && s3.getRivalRecord(e.rivals.get(0)) != null) { opp0 = e; break; }
        check(opp0 != null && t1.headToHead(opp0).startsWith("상대 전적"), "상대와의 지난 전적이 다음 경기 안내에 표시된다: " + (opp0 == null ? "" : t1.headToHead(opp0)));

        // ---------- 트로피 룸 (메달 기록에 국가가 남는다) ----------
        GameState s5 = GameState.newGame();
        s5.setNation("ITA");
        Fencer me5 = mk(WeaponType.SABRE, 30, "메달선수");
        s5.addFencer(me5);
        Tournament tt = null;
        for (int i = 0; i < 12 && s5.getMedals().isEmpty(); i++) {   // 메달이 나올 때까지 (실력이 압도적이라 곧 나온다)
            tt = Tournament.create(s5, Region.WORLD, WeaponType.SABRE, new ArrayList<>(List.of(me5)));
            while (!tt.isFinished()) tt.simulatePlayerMatch(tt.nextPlayerMatch());
            s5.finishTournament(tt);
        }
        boolean nationKept = !s5.getMedals().isEmpty() && "ITA".equals(s5.getMedals().get(0).getNation());
        check(nationKept, "메달 기록에 그때의 소속 국가가 남는다 (메달 " + s5.getMedals().size() + "개, " + tt.getPlacementText() + ")");

        // ---------- 저장 / 불러오기 ----------
        s.setNation("JPN");
        File f = new File("feature-save.dat");
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(f))) { o.writeObject(s3); }
        GameState loaded;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) { loaded = (GameState) in.readObject(); }
        check(loaded.getRivalRecords().size() == s3.getRivalRecords().size() && loaded.getMatchStats().size() == s3.getMatchStats().size(), "저장/불러오기 후 상대 전적과 통계 유지");
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(f))) { o.writeObject(s); }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) { loaded = (GameState) in.readObject(); }
        check(loaded.getStaff().size() == s.getStaff().size() && loaded.getCandidates().size() == s.getCandidates().size() && loaded.getDay() == s.getDay(), "저장/불러오기 후 스태프와 후보 유지");
        f.delete();
        if (args.length > 0) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(args[0]))) {
                GameState old = (GameState) in.readObject();
                check(old.getStaff().isEmpty() && old.getRivalRecords().isEmpty() && old.getMatchStats().isEmpty() && old.canEnter(Region.NATIONAL, WeaponType.SABRE, old.getRoster()) != null,
                        "예전 저장 파일: 스태프·통계·전적은 비어 있고 정상적으로 불러온다");
                old.nextDay();
            }
        }
        System.out.println(fails == 0 ? "\nALL OK" : "\nFAILS=" + fails);
    }
}
