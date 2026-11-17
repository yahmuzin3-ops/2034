import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 펜서 그림. 경기 장면(FencingStage)과 시상식(CeremonyDialog)이 함께 쓴다.
 * 종목마다 검 모양과 자세가 다르고, 상황에 따라 자세(Pose)를 바꾼다.
 *  - NORMAL    : 평소 자세
 *  - TIRED     : 체력이 바닥나서 뒤로 기운 자세
 *  - CELEBRATE : 득점/승리 세리머니 (검을 높이 들고 다른 팔을 흔든다)
 *  - SLUMP     : 졌을 때 고개를 떨군 자세
 */
public final class FencerArt {
    public enum Pose { NORMAL, TIRED, CELEBRATE, SLUMP }

    private FencerArt() {
    }

    public static Color weaponColor(WeaponType w) {
        switch (w) {
            case FOIL: return new Color(70, 130, 230);
            case EPEE: return new Color(60, 175, 105);
            default: return new Color(225, 75, 75);
        }
    }

    /**
     * @param x,y   발 위치
     * @param sc    크기 배율
     * @param facing 1: 오른쪽을 봄, -1: 왼쪽을 봄
     * @param lunge 찌르기 정도 0~1
     * @param phase 애니메이션 시간(ms) - 흔드는 팔 등에 쓴다
     */
    public static void draw(Graphics2D g, WeaponType wt, double x, double y, double sc, int facing,
                            double lunge, boolean flash, Pose pose, long phase) {
        Color wc = weaponColor(wt);
        Graphics2D gg = (Graphics2D) g.create();
        gg.translate(x, y);
        gg.setColor(new Color(0, 0, 0, 70));
        gg.fill(new Ellipse2D.Double(-34 * sc, -5 * sc, 68 * sc, 12 * sc));
        gg.scale(facing * sc, sc);
        double lean = pose == Pose.TIRED ? -0.12 : pose == Pose.SLUMP ? -0.16
                : pose == Pose.CELEBRATE ? 0.0 : wt == WeaponType.EPEE ? 0.10 : wt == WeaponType.SABRE ? -0.04 : 0.0;
        gg.rotate(lean);
        double l = pose == Pose.CELEBRATE ? 0 : lunge;
        BasicStroke thick = new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke armStroke = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        boolean up = pose == Pose.CELEBRATE;

        // 다리
        gg.setStroke(thick);
        gg.setColor(new Color(50, 56, 82));
        gg.draw(new Line2D.Double(-4, -30, up ? -10 : -12 - 4 * l, 0));
        gg.draw(new Line2D.Double(4, -30, up ? 10 : 10 + 22 * l, 0));

        // 뒤쪽 팔: 세리머니 때는 높이 들어 흔든다
        gg.setStroke(armStroke);
        gg.setColor(new Color(235, 238, 246));
        if (up) {
            double wave = Math.sin(phase / 90.0) * 7;
            gg.draw(new Line2D.Double(-8, -56, -20 + wave, -98));
        } else {
            gg.draw(wt == WeaponType.SABRE ? new Line2D.Double(-8, -54, -18, -76) : new Line2D.Double(-8, -54, -16, -64));
        }

        // 몸통
        RoundRectangle2D torso = new RoundRectangle2D.Double(-12, -64, 24, 36, 10, 10);
        gg.setColor(flash ? new Color(255, 125, 125) : new Color(240, 243, 250));
        gg.fill(torso);
        if (wt != WeaponType.EPEE && !flash) {   // 플뢰레/사브르: 전기 채점용 금속 재킷
            gg.setPaint(new GradientPaint(-12, -64, new Color(225, 232, 245), 12, -28, new Color(150, 162, 190)));
            gg.fill(torso);
        }
        gg.setColor(wc);
        gg.fill(new Rectangle2D.Double(-12, -48, 24, 5));
        gg.setColor(new Color(60, 66, 94));
        gg.setStroke(new BasicStroke(1.5f));
        gg.draw(torso);

        // 머리와 마스크 (졌을 때는 고개를 떨군다)
        double headDrop = pose == Pose.SLUMP ? 7 : 0;
        Ellipse2D head = new Ellipse2D.Double(-11, -86 + headDrop, 22, 22);
        gg.setColor(flash ? new Color(255, 160, 160) : Color.WHITE);
        gg.fill(head);
        gg.setColor(new Color(110, 116, 142));
        gg.setStroke(new BasicStroke(1f));
        gg.draw(head);
        for (int i = -6; i <= 6; i += 4) gg.draw(new Line2D.Double(i, -84 + headDrop, i, -66 + headDrop));
        for (int j = -80; j <= -68; j += 4) gg.draw(new Line2D.Double(-9, j + headDrop, 9, j + headDrop));

        if (up) drawRaisedWeapon(gg, wt, wc, phase);
        else drawWeaponArm(gg, wt, l, wc);
        gg.dispose();
    }

    /** 세리머니: 검을 머리 위로 번쩍 든다. */
    private static void drawRaisedWeapon(Graphics2D gg, WeaponType wt, Color wc, long phase) {
        double sway = Math.sin(phase / 140.0) * 0.12;
        double hx = 14, hy = -94;
        gg.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gg.setColor(new Color(235, 238, 246));
        gg.draw(new Line2D.Double(6, -58, hx, hy));
        double ang = -Math.PI / 2 + sway;
        double len = 46;
        double tx = hx + Math.cos(ang) * len, ty = hy + Math.sin(ang) * len;
        gg.setColor(new Color(222, 229, 242));
        if (wt == WeaponType.SABRE) {
            gg.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.draw(new QuadCurve2D.Double(hx, hy, hx + 8, (hy + ty) / 2, tx, ty));
        } else {
            gg.setStroke(new BasicStroke(wt == WeaponType.EPEE ? 3.4f : 1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.draw(new Line2D.Double(hx, hy, tx, ty));
        }
        gg.setColor(wc);
        gg.fill(new Ellipse2D.Double(hx - 4, hy - 4, 9, 9));
    }

    /** 종목별 검과 팔: 플뢰레=가는 검 찌르기, 에페=굵은 검+큰 벨 가드, 사브르=휘어진 날 베기 */
    private static void drawWeaponArm(Graphics2D gg, WeaponType wt, double l, Color wc) {
        Color skin = new Color(235, 238, 246);
        BasicStroke arm = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        switch (wt) {
            case SABRE: {
                double hx = 10 + 14 * l, hy = -60 + 6 * l;
                gg.setStroke(arm);
                gg.setColor(skin);
                gg.draw(new Line2D.Double(6, -58, hx, hy));
                double ang = -1.0 + 1.45 * l;
                double len = 44 + 8 * l;
                double tx = hx + Math.cos(ang) * len, ty = hy + Math.sin(ang) * len;
                double mx = (hx + tx) / 2 + Math.sin(ang) * 7, my = (hy + ty) / 2 - Math.cos(ang) * 7;
                gg.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gg.setColor(new Color(218, 225, 238));
                gg.draw(new QuadCurve2D.Double(hx, hy, mx, my, tx, ty));
                gg.setColor(wc);
                gg.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gg.draw(new Arc2D.Double(hx - 7, hy - 7, 14, 14, 80, 200, Arc2D.OPEN));
                if (l > 0.15) {
                    gg.setColor(new Color(255, 255, 255, (int) (120 * l)));
                    gg.setStroke(new BasicStroke(2.2f));
                    gg.draw(new Arc2D.Double(hx - 30, hy - 56, 84, 84, 50, -110 * l, Arc2D.OPEN));
                }
                break;
            }
            case EPEE: {
                double hx = 8 + 20 * l, hy = -54 - (1 - l) * 2;
                gg.setStroke(arm);
                gg.setColor(skin);
                gg.draw(new Line2D.Double(6, -58, hx, hy));
                double ang = -0.12 * (1 - l);
                double len = 52 + 14 * l;
                gg.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gg.setColor(new Color(140, 150, 172));
                gg.draw(new Line2D.Double(hx, hy, hx + Math.cos(ang) * len, hy + Math.sin(ang) * len));
                gg.setColor(wc.darker());
                gg.fill(new Arc2D.Double(hx - 6, hy - 9, 16, 18, 90, 180, Arc2D.PIE));
                gg.setColor(wc);
                gg.setStroke(new BasicStroke(1.5f));
                gg.draw(new Arc2D.Double(hx - 6, hy - 9, 16, 18, 90, 180, Arc2D.OPEN));
                break;
            }
            default: {
                double hx = 8 + 18 * l, hy = -56 - (1 - l) * 4;
                gg.setStroke(arm);
                gg.setColor(skin);
                gg.draw(new Line2D.Double(6, -58, hx, hy));
                double ang = -0.35 * (1 - l);
                double len = 46 + 14 * l;
                double tx = hx + Math.cos(ang) * len, ty = hy + Math.sin(ang) * len;
                gg.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gg.setColor(new Color(222, 229, 242));
                gg.draw(new Line2D.Double(hx, hy, tx, ty));
                gg.setColor(wc);
                gg.fill(new Ellipse2D.Double(hx - 1, hy - 4, 8, 8));
                gg.fill(new Ellipse2D.Double(tx - 2, ty - 2, 4, 4));
                break;
            }
        }
    }
}
