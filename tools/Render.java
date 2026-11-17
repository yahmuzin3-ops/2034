import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Render {
    static void shot(JComponent c, int w, int h, String file) throws Exception {
        c.setSize(w, h);
        c.doLayout();
        layoutAll(c);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }
    static void layoutAll(Component c) {
        if (c instanceof Container) {
            c.doLayout();
            for (Component k : ((Container) c).getComponents()) layoutAll(k);
        }
    }
    static Fencer mk(WeaponType w, int lvl, String name) {
        Fencer f = new Fencer(name, w, Personality.CAREFUL);
        while (f.getLevel() < lvl) f.gainExp(f.expToNext());
        return f;
    }
    public static void main(String[] a) throws Exception {
        GameState s = GameState.newGame();
        Fencer me = mk(WeaponType.SABRE, 16, "아델");
        Tournament t = Tournament.create(s, Region.OLYMPIC, WeaponType.SABRE, new ArrayList<>(List.of(me)));
        // 몇 라운드 진행
        for (int i = 0; i < 2 && !t.isFinished(); i++) { Tournament.Match m = t.nextPlayerMatch(); if (m == null) break; t.simulatePlayerMatch(m); }
        TournamentPanel tp = new TournamentPanel(new TournamentPanel.Actions() { public void play() {} public void simulate() {} public void giveUp() {} });
        tp.refresh(t);
        shot(tp, 1000, 560, "/home/claude/t_olympic.png");
        Tournament n = Tournament.create(s, Region.NATIONAL, WeaponType.SABRE, new ArrayList<>(List.of(me)));
        TournamentPanel tp2 = new TournamentPanel(new TournamentPanel.Actions() { public void play() {} public void simulate() {} public void giveUp() {} });
        tp2.refresh(n);
        shot(tp2, 1000, 480, "/home/claude/t_pool.png");
        CeremonyDialog.CeremonyPanel cp = new CeremonyDialog.CeremonyPanel("올림픽 사브르부 개인전", 1, new String[]{"아델 (KOR)", "Luca Rossi (ITA)", "Wei Zhang (CHN)"}, List.of(me), WeaponType.SABRE, false, true);
        cp.setStartTime(System.currentTimeMillis() - 2500);
        shot(cp, 720, 440, "/home/claude/ceremony.png");
        System.out.println("rendered");
    }
}
