import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

/** 새 홈 화면과 도움말을 창 없이 그려서 이미지로 저장한다. */
public class RenderUsability {
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

    static Fencer mk(WeaponType w, int lvl, String name, Personality p) {
        Fencer f = new Fencer(name, w, p);
        while (f.getLevel() < lvl) f.gainExp(f.expToNext());
        return f;
    }

    public static void main(String[] a) throws Exception {
        GameState s = GameState.newGame();
        s.setNation("KOR");
        Fencer adel = s.getRoster().get(0);
        while (adel.getLevel() < 11) adel.gainExp(adel.expToNext());
        Fencer ha = mk(WeaponType.EPEE, 9, "하준", Personality.CAREFUL);
        s.addFencer(ha);
        Fencer yu = mk(WeaponType.FOIL, 7, "유나", Personality.PASSIONATE);
        s.addFencer(yu);
        for (int i = 0; i < 9; i++) s.recordResult(true, Region.CLUB, false, WeaponType.SABRE);
        for (int i = 0; i < 4; i++) s.recordResult(true, Region.CLUB, false, WeaponType.EPEE);
        adel.addFatigue(92);
        ha.injure();
        yu.addFatigue(35);
        s.addGold(2500);
        while (s.getDay() < 15) s.nextDay();

        Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
        MainFrame frame = (MainFrame) unsafe.allocateInstance(MainFrame.class);
        Field sf = MainFrame.class.getDeclaredField("state");
        sf.setAccessible(true);
        sf.set(frame, s);
        Field ef = MainFrame.class.getDeclaredField("engine");
        ef.setAccessible(true);
        ef.set(frame, new GameEngine(s));

        HomePanel home = new HomePanel(frame);
        home.onShow();
        shot(home, 1000, 700, "/home/claude/ux_home.png");
        shot(HelpDialog.content(HelpDialog.START), 780, 540, "/home/claude/ux_help_start.png");
        shot(HelpDialog.content(HelpDialog.MATCH), 780, 640, "/home/claude/ux_help_match.png");
        System.out.println("rendered");
    }
}
