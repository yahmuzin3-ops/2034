import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/** 사용하기 편하게 바꾼 부분(추천, 도움말, 저장, 단축키, 도움말 버튼)이 제대로 동작하는지 확인한다. */
public class UsabilityTest {
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

    static boolean has(List<Advisor.Tip> tips, String text) {
        for (Advisor.Tip t : tips) if (t.title.contains(text)) return true;
        return false;
    }

    static void advanceTo(GameState s, int day) {
        while (s.getDay() < day) s.nextDay();
    }

    static MainFrame fake(GameState s) throws Exception {
        Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
        MainFrame frame = (MainFrame) unsafe.allocateInstance(MainFrame.class);
        Field sf = MainFrame.class.getDeclaredField("state");
        sf.setAccessible(true);
        sf.set(frame, s);
        return frame;
    }

    static JButton findButton(Container c, String text) {
        for (Component k : c.getComponents()) {
            if (k instanceof JButton && text.equals(((JButton) k).getText())) return (JButton) k;
            if (k instanceof Container) {
                JButton b = findButton((Container) k, text);
                if (b != null) return b;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        // ---------- 오늘의 추천 ----------
        GameState s = GameState.newGame();
        List<Advisor.Tip> tips = Advisor.tips(s);
        check(!tips.isEmpty() && tips.size() <= 6, "추천은 1~6개 (지금 " + tips.size() + "개)");
        check(tips.get(0).title.contains("첫 경기") && has(tips, "도움말"), "새 게임에서는 '첫 경기에 도전'이 맨 위, 도움말 안내도 있다");
        for (Advisor.Tip t : tips) if (t.screen != null && !t.screen.equals("help")) {
            // 이동할 화면 이름이 실제 메뉴 이름인지는 MainFrame 메뉴 목록과 비교한다
            check(Arrays.asList("home", "athletes", "training", "staff", "shop", "inventory", "match", "calendar", "ranking", "records", "facility", "goals", "history").contains(t.screen), "추천 '" + t.title + "' → 화면 '" + t.screen + "'은 실제 메뉴");
        }

        // 급한 일: 피로
        Fencer tired = s.getRoster().get(0);
        tired.addFatigue(95);
        tips = Advisor.tips(s);
        check(tips.get(0).level == Advisor.URGENT && tips.get(0).title.contains("피로"), "피로가 높으면 가장 먼저 알려준다: " + tips.get(0).title);
        check("!".equals(Advisor.badges(s).get("athletes")), "메뉴 '선수 관리' 옆에 ! 표시");
        // 부상
        Fencer hurt = mk(WeaponType.EPEE, 5, "다친선수");
        s.addFencer(hurt);
        hurt.injure();
        tips = Advisor.tips(s);
        check(has(tips, "다쳤어요") && "!".equals(Advisor.badges(s).get("inventory")), "부상 선수가 있으면 급한 일로 알리고 인벤토리 메뉴에 ! 표시");

        // 대회가 열려 있고 출전 가능
        GameState s2 = GameState.newGame();
        s2.addFencer(mk(WeaponType.SABRE, 12, "출전선수"));
        for (int i = 0; i < 40; i++) s2.recordResult(true, Region.CLUB, false, WeaponType.SABRE);
        advanceTo(s2, 14);   // 자국 선수권(20일째) 6일 전
        tips = Advisor.tips(s2);
        check(has(tips, "열려요"), "대회 개막이 가까우면 미리 알려준다: " + tips.stream().filter(t -> t.title.contains("열려요")).findFirst().map(t -> t.title).orElse("-"));
        check("●".equals(Advisor.badges(s2).get("calendar")) == false, "개막 3일 전보다 이르면 캘린더 메뉴에는 아직 표시하지 않는다");
        advanceTo(s2, 18);
        check("●".equals(Advisor.badges(s2).get("calendar")), "개막 3일 전부터 '시즌 캘린더' 옆에 ● 표시");
        advanceTo(s2, 21);
        tips = Advisor.tips(s2);
        check(has(tips, "개최 중") && "●".equals(Advisor.badges(s2).get("match")), "대회가 열려 있고 나갈 수 있으면 추천 + '경기 · 대회' 옆에 ● 표시");
        check(tips.get(0).level <= Advisor.IMPORTANT, "대회 추천은 참고(TIP)보다 위에 있다");
        s2.markEntered(Region.NATIONAL, WeaponType.SABRE);
        check(!has(Advisor.tips(s2), "개최 중"), "이미 출전한 대회는 더 이상 추천하지 않는다");

        // 해금까지 남은 승리
        GameState s3 = GameState.newGame();
        s3.addFencer(mk(WeaponType.SABRE, 3, "초보"));
        for (int i = 0; i < 5; i++) s3.recordResult(true, Region.CLUB, false, WeaponType.SABRE);
        tips = Advisor.tips(s3);
        check(has(tips, "해금까지"), "다음 대회가 잠겨 있으면 필요한 승리 수를 알려준다: " + tips.stream().filter(t -> t.title.contains("해금")).findFirst().map(t -> t.title).orElse("-"));

        // 주급 걱정
        GameState s4 = GameState.newGame();
        s4.addGold(3000);
        s4.refreshCandidates();
        for (Staff c : s4.getCandidates()) { s4.hireStaff(c); if (s4.getStaff().size() >= 2) break; }
        s4.spendGold(s4.getGold());
        while ((Staff.PAY_DAYS - s4.getDay() % Staff.PAY_DAYS) % Staff.PAY_DAYS > 2) s4.nextDay();
        if (s4.getStaff().size() > 0) {
            tips = Advisor.tips(s4);
            check(has(tips, "주급") && tips.get(0).level == Advisor.URGENT && "!".equals(Advisor.badges(s4).get("match")), "주급을 낼 돈이 모자라면 급한 일로 알린다: " + tips.get(0).title);
        } else check(true, "주급 시나리오: 이미 주급 정산으로 스태프가 떠난 뒤라 건너뜀");

        // ---------- 도움말 ----------
        Map<String, String> topics = HelpDialog.topics();
        check(topics.size() == 6, "도움말 탭 6개: " + topics.values());
        String match = HelpDialog.html(HelpDialog.MATCH);
        boolean tacticsOk = true;
        for (Battle.Tactic t : Battle.Tactic.values()) tacticsOk &= match.contains(t.getDisplayName()) && match.contains(t.getHint());
        check(tacticsOk, "경기 방법에 전술 6개의 이름과 설명이 모두 들어 있다");
        String start = HelpDialog.html(HelpDialog.START);
        check(start.contains(Region.NATIONAL.getRequiredWins() + "승") && start.contains(Region.WORLD.getRequiredWins() + "승") && start.contains(Region.OLYMPIC.getRequiredWins() + "승"), "해금 승리 수가 게임 설정과 같다");
        String staff = HelpDialog.html(HelpDialog.STAFF);
        boolean rolesOk = true;
        for (Staff.Role r : Staff.Role.values()) rolesOk &= staff.contains(r.getDisplayName()) && staff.contains(Staff.effectText(r, 1));
        check(rolesOk && staff.contains(Staff.weeklySalary(Staff.MAX_LEVEL) + "G"), "스태프 도움말이 역할·효과·주급과 같다");
        check(HelpDialog.html(HelpDialog.SEASON).contains("시즌마다 2번") && HelpDialog.html(HelpDialog.RATING).contains("Elo") && HelpDialog.html(HelpDialog.KEYS).contains("Ctrl + S"), "대회/랭킹/단축키 도움말 내용");
        JTabbedPane tabs = HelpDialog.content(HelpDialog.SEASON);
        check(tabs.getTabCount() == 6 && tabs.getSelectedIndex() == 2, "주제를 고르면 그 탭이 열린다 (선택 탭 " + tabs.getSelectedIndex() + ")");

        // ---------- 시작 가이드 ----------
        GameState g = GameState.newGame();
        check(g.consumeShowGuide() && !g.consumeShowGuide(), "새 게임에서는 시작 가이드가 한 번만 나온다");
        if (args.length > 0) {
            try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream(args[0]))) {
                check(!((GameState) in.readObject()).consumeShowGuide(), "예전 저장 파일을 불러오면 시작 가이드를 다시 띄우지 않는다");
            }
        }

        // ---------- 저장 ----------
        File auto = SaveManager.AUTO_FILE, manual = SaveManager.FILE;
        auto.delete();
        manual.delete();
        check(SaveManager.newest() == null && !SaveManager.exists(), "저장 파일이 없으면 이어하기 없음");
        GameState a = GameState.newGame();
        SaveManager.save(a);
        Thread.sleep(1100);
        a.addGold(1234);
        SaveManager.autosave(a);
        check(SaveManager.newest() == auto && SaveManager.exists(), "더 최근에 저장된 자동 저장이 이어하기 대상이 된다");
        check(!new File("autosave.dat.tmp").exists() && !new File("savegame.dat.tmp").exists(), "저장이 끝나면 임시 파일이 남지 않는다");
        check(SaveManager.describe(auto).startsWith("자동 저장") && SaveManager.describe(manual).startsWith("직접 저장") && SaveManager.describe(auto).contains("선수"), "저장 설명: " + SaveManager.describe(auto));
        check(SaveManager.load(auto).getGold() == a.getGold() && SaveManager.load(manual).getGold() == a.getGold() - 1234, "직접 저장과 자동 저장을 따로 불러올 수 있다");
        auto.delete();
        check(SaveManager.newest() == manual, "자동 저장이 없으면 직접 저장을 이어한다");
        // 망가진 파일
        try (java.io.FileWriter w = new java.io.FileWriter(auto)) { w.write("깨진 파일"); }
        check(SaveManager.describe(auto).contains("읽을 수 없는 파일"), "망가진 저장 파일도 오류 없이 설명한다");
        auto.delete();
        manual.delete();

        // ---------- 화면: 도움말 버튼, 키보드 ----------
        GameState ps = GameState.newGame();
        MainFrame frame = fake(ps);
        Field ef = MainFrame.class.getDeclaredField("engine");
        ef.setAccessible(true);
        ef.set(frame, new GameEngine(ps));
        Map<ScreenPanel, String> expect = new LinkedHashMap<>();
        expect.put(new MatchPanel(frame), HelpDialog.MATCH);
        expect.put(new CalendarPanel(frame), HelpDialog.SEASON);
        expect.put(new RankingPanel(frame), HelpDialog.RATING);
        expect.put(new StaffPanel(frame), HelpDialog.STAFF);
        expect.put(new HomePanel(frame), HelpDialog.START);
        expect.put(new RecordsPanel(frame), HelpDialog.START);
        boolean helpOk = true;
        String bad = "";
        for (Map.Entry<ScreenPanel, String> e : expect.entrySet()) {
            Method m = ScreenPanel.class.getDeclaredMethod("helpTopic");
            m.setAccessible(true);
            String topic = (String) m.invoke(e.getKey());
            if (findButton(e.getKey(), "? 도움말") == null || !topic.equals(e.getValue())) {
                helpOk = false;
                bad += e.getKey().getClass().getSimpleName() + " ";
            }
        }
        check(helpOk, "모든 화면 제목 줄에 '? 도움말' 버튼이 있고 화면에 맞는 탭이 열린다 " + bad);
        MatchPanel mp = (MatchPanel) expect.keySet().iterator().next();
        InputMap im = mp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        boolean keysOk = true;
        for (int i = 0; i < 6; i++) keysOk &= ("tactic" + i).equals(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, 0)));
        keysOk &= "continue".equals(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0))) && "continue".equals(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)));
        check(keysOk, "경기 화면 키보드: 1~6 전술, Space/Enter 계속하기");
        // 전술 버튼 이름에 번호
        boolean numbered = true;
        int idx = 0;
        for (Battle.Tactic t : Battle.Tactic.values()) {
            idx++;
            numbered &= findButton(mp, idx + " " + t.getDisplayName() + " (체력 " + t.getCost() + ")") != null;
        }
        check(numbered, "전술 버튼에 단축키 번호가 붙어 있다");
        // 홈 화면: 추천 카드와 컨디션 목록이 오류 없이 만들어진다
        HomePanel home = (HomePanel) expect.keySet().toArray()[4];
        home.onShow();
        home.onTick();
        check(true, "홈 화면 갱신 (추천 카드, 선수 컨디션) 오류 없음");

        System.out.println(fails == 0 ? "\nALL OK" : "\nFAILS=" + fails);
    }
}
