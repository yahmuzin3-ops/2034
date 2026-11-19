import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** 새 화면(기록실, 시즌 캘린더, 코치·스태프, 시상식)을 창 없이 그려서 이미지로 저장한다. */
public class RenderNew {
    static void layoutAll(Component c) {
        if (c instanceof Container) {
            c.doLayout();
            for (Component k : ((Container) c).getComponents()) layoutAll(k);
        }
    }

    static void shot(JComponent c, int w, int h, String file) throws Exception {
        c.setSize(w, h);
        layoutAll(c);
        layoutAll(c);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }

    static Fencer mk(WeaponType w, int lvl, String name) {
        Fencer f = new Fencer(name, w, Personality.CAREFUL);
        while (f.getLevel() < lvl) f.gainExp(f.expToNext());
        return f;
    }

    static void play(GameState s, Fencer a, Fencer b) {
        Battle.Options o = new Battle.Options();
        o.rewards = false;
        o.rated = false;
        Battle bt = new Battle(s, Region.CLUB, List.of(a), false, List.of(b), o);
        int guard = 0;
        while (!bt.isOver() && guard++ < 9000) {
            if (bt.isOnBreak()) bt.continueFromBreak();
            else bt.choose(bt.suggest());
            bt.drainEvents();
            bt.drainLog();
        }
        bt.settle();
    }

    public static void main(String[] a) throws Exception {
        GameState s = GameState.newGame();
        s.setNation("KOR");
        Fencer me = mk(WeaponType.SABRE, 22, "아델");
        s.addFencer(me);
        Fencer me2 = mk(WeaponType.EPEE, 14, "하준");
        s.addFencer(me2);
        for (int i = 0; i < 40; i++) s.recordResult(true, Region.CLUB, false, WeaponType.SABRE);
        for (int i = 0; i < 40; i++) s.recordResult(true, Region.CLUB, false, WeaponType.EPEE);
        // 경기 통계 데이터
        for (int i = 0; i < 24; i++) play(s, mk(WeaponType.SABRE, 14 + i % 5, "아델"), mk(WeaponType.SABRE, 12 + (i * 3) % 7, "상대"));
        for (int i = 0; i < 8; i++) play(s, mk(WeaponType.EPEE, 12, "하준"), mk(WeaponType.EPEE, 11 + i % 4, "상대"));
        // 대회 3개 (상대 전적, 메달)
        for (int round = 0; round < 6; round++) {
            Region r = round % 2 == 0 ? Region.NATIONAL : Region.WORLD;
            WeaponType w = round % 3 == 0 ? WeaponType.SABRE : round % 3 == 1 ? WeaponType.SABRE : WeaponType.SABRE;
            Tournament t = Tournament.create(s, r, w, new ArrayList<>(List.of(me)));
            while (!t.isFinished()) t.simulatePlayerMatch(t.nextPlayerMatch());
            s.finishTournament(t);
            for (int d = 0; d < 20; d++) s.nextDay();
        }
        while (s.getDay() < 58) s.nextDay();
        s.addGold(3000);
        s.refreshCandidates();
        for (Staff.Role role : Staff.Role.values()) {
            if (role == Staff.Role.NUTRITIONIST) continue;
            for (Staff c : s.getCandidates()) if (c.getRole() == role) { s.hireStaff(c); break; }
        }
        s.markEntered(Region.WORLD, WeaponType.SABRE);

        // MainFrame 을 만들지 않고 상태만 연결한 가짜 프레임 (화면 그리기 확인용)
        Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
        MainFrame frame = (MainFrame) unsafe.allocateInstance(MainFrame.class);
        Field sf = MainFrame.class.getDeclaredField("state");
        sf.setAccessible(true);
        sf.set(frame, s);

        CalendarPanel cal = new CalendarPanel(frame);
        cal.onShow();
        shot(cal, 1000, 640, "/home/claude/new_calendar.png");
        StaffPanel st = new StaffPanel(frame);
        st.onShow();
        shot(st, 1000, 640, "/home/claude/new_staff.png");
        RecordsPanel rec = new RecordsPanel(frame);
        rec.onShow();
        JTabbedPane tabs = null;
        for (Component c : rec.getComponents()) if (c instanceof JTabbedPane) tabs = (JTabbedPane) c;
        String[] names = {"trophy", "rivals", "stats"};
        for (int i = 0; i < 3; i++) {
            tabs.setSelectedIndex(i);
            shot(rec, 1000, 700, "/home/claude/new_records_" + names[i] + ".png");
        }
        // 시상식 (국기 게양)
        CeremonyDialog.CeremonyPanel cp = new CeremonyDialog.CeremonyPanel("세계 선수권 사브르부 개인전", 1, new String[]{"아델 (KOR)", "Luca Rossi (ITA)", "Wei Zhang (CHN)"},
                new String[]{"KOR", "ITA", "CHN"}, List.of(me), WeaponType.SABRE, false, false);
        cp.setStartTime(System.currentTimeMillis() - 3600);
        shot(cp, 720, 440, "/home/claude/new_ceremony.png");
        System.out.println("rendered; medals=" + s.getMedals().size() + " rivals=" + s.getRivalRecords().size() + " stats=" + s.getMatchStats().size());
    }
}
