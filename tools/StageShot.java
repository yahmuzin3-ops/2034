import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** 경기 장면에서 득점 세리머니와 승리 포즈가 나오는지 확인하고 화면을 저장한다. */
public class StageShot {
    static Fencer mk(WeaponType w, int lvl, String name) {
        Fencer f = new Fencer(name, w, Personality.CAREFUL);
        while (f.getLevel() < lvl) f.gainExp(f.expToNext());
        return f;
    }

    static long get(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getLong(o);
    }

    static boolean getB(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getBoolean(o);
    }

    static Object fx(FencingStage s, String side) throws Exception {
        Field f = FencingStage.class.getDeclaredField(side);
        f.setAccessible(true);
        return f.get(s);
    }

    static void shot(FencingStage st, String file) throws Exception {
        BufferedImage img = new BufferedImage(st.getWidth(), st.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        st.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }

    public static void main(String[] a) throws Exception {
        GameState s = GameState.newGame();
        Battle.Options o = new Battle.Options();
        o.rewards = false;
        o.rated = false;
        Battle bt = new Battle(s, Region.CLUB, java.util.List.of(mk(WeaponType.SABRE, 10, "아델")), false, java.util.List.of(mk(WeaponType.SABRE, 8, "상대")), o);
        final FencingStage[] stage = new FencingStage[1];
        SwingUtilities.invokeAndWait(() -> {
            stage[0] = new FencingStage();
            stage[0].setSize(900, 400);
            stage[0].doLayout();
            stage[0].setup(bt);
        });
        boolean touchShot = false, winShot = false, loseShot = false;
        int celebrationsSeen = 0;
        int guard = 0;
        while (!bt.isOver() && guard++ < 400) {
            if (bt.isOnBreak()) bt.continueFromBreak();
            else bt.choose(bt.suggest());
            CountDownLatch done = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> stage[0].play(bt.drainEvents(), done::countDown));
            // 재생 중 0.1초마다 확인
            long t0 = System.currentTimeMillis();
            while (done.getCount() > 0 && System.currentTimeMillis() - t0 < 20000) {
                Thread.sleep(80);
                Object l = fx(stage[0], "L"), r = fx(stage[0], "R");
                long now = System.currentTimeMillis();
                boolean cel = (l != null && get(l, "celebrateUntil") > now && get(l, "celebrateUntil") < Long.MAX_VALUE / 2)
                        || (r != null && get(r, "celebrateUntil") > now && get(r, "celebrateUntil") < Long.MAX_VALUE / 2);
                if (cel) {
                    celebrationsSeen++;
                    if (!touchShot) {
                        final FencingStage st = stage[0];
                        SwingUtilities.invokeAndWait(() -> { try { shot(st, "/home/claude/stage_score.png"); } catch (Exception e) { e.printStackTrace(); } });
                        touchShot = true;
                    }
                }
            }
        }
        Thread.sleep(500);
        Object l = fx(stage[0], "L"), r = fx(stage[0], "R");
        boolean lWin = l != null && get(l, "celebrateUntil") > Long.MAX_VALUE / 2;
        boolean rWin = r != null && get(r, "celebrateUntil") > Long.MAX_VALUE / 2;
        boolean lSlump = l != null && getB(l, "slump"), rSlump = r != null && getB(r, "slump");
        final FencingStage st = stage[0];
        SwingUtilities.invokeAndWait(() -> { try { shot(st, "/home/claude/stage_end.png"); } catch (Exception e) { e.printStackTrace(); } });
        System.out.println("경기 결과: " + (bt.isWon() ? "승리" : "패배") + " " + bt.getPlayerScore() + ":" + bt.getEnemyScore());
        System.out.println("득점 세리머니가 감지된 횟수(프레임): " + celebrationsSeen);
        System.out.println("끝난 뒤 이긴 쪽 승리 포즈: " + (bt.isWon() ? lWin : rWin) + " / 진 쪽 낙담 포즈: " + (bt.isWon() ? rSlump : lSlump));
        System.exit(0);
    }
}
