import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * 국기 그림. 이미지 파일 없이 코드로 그린다. (16개국, 단순화한 모양)
 * 랭킹 표, 대진표, 시상식, 경기 점수판 등 여러 화면이 함께 쓴다.
 */
public final class FlagArt {
    private static final Map<String, ImageIcon> CACHE = new HashMap<>();
    private static final Map<String, BufferedImage> BIG = new HashMap<>();

    private FlagArt() {
    }

    /** 표나 목록에 넣는 작은 국기 아이콘 */
    public static synchronized ImageIcon icon(String code, int w, int h) {
        String key = code + "/" + w + "x" + h;
        ImageIcon icon = CACHE.get(key);
        if (icon == null) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            draw(g, code, 0, 0, w, h);
            g.dispose();
            icon = new ImageIcon(img);
            CACHE.put(key, icon);
        }
        return icon;
    }

    /** 펄럭이는 국기: 시상식에서 국기가 게양될 때 쓴다. */
    public static void drawWaving(Graphics2D g, String code, double x, double y, double w, double h, long phaseMs) {
        BufferedImage img;
        synchronized (FlagArt.class) {
            String key = code + "/big";
            img = BIG.get(key);
            if (img == null) {
                img = new BufferedImage(240, 160, BufferedImage.TYPE_INT_ARGB);
                Graphics2D ig = img.createGraphics();
                draw(ig, code, 0, 0, 240, 160);
                ig.dispose();
                BIG.put(key, img);
            }
        }
        int slices = 24;
        double sw = w / slices;
        for (int i = 0; i < slices; i++) {
            double wave = Math.sin(phaseMs / 220.0 + i * 0.55) * (1.0 + i * 0.12);
            int sx = (int) (i * 240.0 / slices);
            g.drawImage(img, (int) Math.round(x + i * sw), (int) Math.round(y + wave), (int) Math.round(x + (i + 1) * sw + 1), (int) Math.round(y + wave + h),
                    sx, 0, sx + 240 / slices, 160, null);
        }
    }

    public static void draw(Graphics2D g0, String code, double x, double y, double w, double h) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.clip(new Rectangle2D.Double(x, y, w, h));
        g.translate(x, y);
        switch (code == null ? "" : code) {
            case "KOR": korea(g, w, h); break;
            case "FRA": vertical(g, w, h, rgb(0, 85, 164), Color.WHITE, rgb(239, 65, 53)); break;
            case "ITA": vertical(g, w, h, rgb(0, 146, 70), Color.WHITE, rgb(206, 43, 55)); break;
            case "ROU": vertical(g, w, h, rgb(0, 43, 127), rgb(252, 209, 22), rgb(206, 17, 38)); break;
            case "HUN": horizontal(g, w, h, rgb(205, 42, 62), Color.WHITE, rgb(67, 111, 77)); break;
            case "GER": horizontal(g, w, h, Color.BLACK, rgb(221, 0, 0), rgb(255, 206, 0)); break;
            case "UKR": horizontal(g, w, h, rgb(0, 87, 183), rgb(255, 215, 0)); break;
            case "POL": horizontal(g, w, h, Color.WHITE, rgb(220, 20, 60)); break;
            case "EGY": egypt(g, w, h); break;
            case "IRN": iran(g, w, h); break;
            case "ESP": spain(g, w, h); break;
            case "CAN": canada(g, w, h); break;
            case "JPN": japan(g, w, h); break;
            case "CHN": china(g, w, h); break;
            case "USA": usa(g, w, h); break;
            case "GBR": britain(g, w, h); break;
            default:
                g.setColor(new Color(200, 204, 214));
                g.fill(new Rectangle2D.Double(0, 0, w, h));
                g.setColor(new Color(120, 126, 150));
                g.setFont(g.getFont().deriveFont(Font.BOLD, (float) (h * 0.7)));
                g.drawString("?", (float) (w / 2 - h * 0.18), (float) (h * 0.76));
        }
        g.dispose();
        Graphics2D b = (Graphics2D) g0.create();   // 테두리
        b.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        b.setColor(new Color(0, 0, 0, 70));
        b.setStroke(new BasicStroke(1f));
        b.draw(new Rectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1));
        b.dispose();
    }

    private static Color rgb(int r, int g, int b) {
        return new Color(r, g, b);
    }

    private static void vertical(Graphics2D g, double w, double h, Color... cs) {
        for (int i = 0; i < cs.length; i++) {
            g.setColor(cs[i]);
            g.fill(new Rectangle2D.Double(w * i / cs.length - 0.5, 0, w / cs.length + 1, h));
        }
    }

    private static void horizontal(Graphics2D g, double w, double h, Color... cs) {
        for (int i = 0; i < cs.length; i++) {
            g.setColor(cs[i]);
            g.fill(new Rectangle2D.Double(0, h * i / cs.length - 0.5, w, h / cs.length + 1));
        }
    }

    private static Path2D star(double cx, double cy, double outer, double inner, int points, double rot) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double a = rot + i * Math.PI / points - Math.PI / 2;
            double px = cx + Math.cos(a) * r, py = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(px, py);
            else p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    // ---------------- 나라별 ----------------

    private static void korea(Graphics2D g, double w, double h) {
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(0, 0, w, h));
        double cx = w / 2, cy = h / 2, r = h * 0.25;
        Graphics2D t = (Graphics2D) g.create();
        t.translate(cx, cy);
        t.rotate(-Math.atan2(2, 3));
        t.setColor(rgb(205, 46, 58));
        t.fill(new Arc2D.Double(-r, -r, 2 * r, 2 * r, 0, 180, Arc2D.PIE));
        t.setColor(rgb(0, 71, 160));
        t.fill(new Arc2D.Double(-r, -r, 2 * r, 2 * r, 180, 180, Arc2D.PIE));
        t.setColor(rgb(205, 46, 58));
        t.fill(new Ellipse2D.Double(-r, -r / 2, r, r));
        t.setColor(rgb(0, 71, 160));
        t.fill(new Ellipse2D.Double(0, -r / 2, r, r));
        t.dispose();
        // 건곤감리: 모서리마다 막대 3개
        double bar = h * 0.045, len = h * 0.2, gap = h * 0.03;
        double[][] pos = {{0.17, 0.2, 0}, {0.83, 0.8, 0}, {0.83, 0.2, 1}, {0.17, 0.8, 2}};
        for (double[] p : pos) {
            Graphics2D b = (Graphics2D) g.create();
            b.translate(p[0] * w, p[1] * h);
            b.rotate(Math.atan2(cy - p[1] * h, cx - p[0] * w) + Math.PI / 2);
            b.setColor(Color.BLACK);
            for (int i = -1; i <= 1; i++) {
                double yy = i * (bar + gap) - bar / 2;
                boolean solid = p[2] == 0 ? (p[0] < 0.5) : p[2] == 1 ? i == 0 ? false : true : i == 0;
                if (p[2] == 0 && p[0] > 0.5) solid = false;   // 곤: 모두 끊어진 막대
                if (solid) b.fill(new Rectangle2D.Double(-len / 2, yy, len, bar));
                else {
                    b.fill(new Rectangle2D.Double(-len / 2, yy, len * 0.43, bar));
                    b.fill(new Rectangle2D.Double(len * 0.07, yy, len * 0.43, bar));
                }
            }
            b.dispose();
        }
    }

    private static void japan(Graphics2D g, double w, double h) {
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(0, 0, w, h));
        g.setColor(rgb(188, 0, 45));
        double r = h * 0.3;
        g.fill(new Ellipse2D.Double(w / 2 - r, h / 2 - r, 2 * r, 2 * r));
    }

    private static void china(Graphics2D g, double w, double h) {
        g.setColor(rgb(222, 41, 16));
        g.fill(new Rectangle2D.Double(0, 0, w, h));
        g.setColor(rgb(255, 222, 0));
        g.fill(star(w * 0.17, h * 0.27, h * 0.19, h * 0.075, 5, 0));
        double[][] small = {{0.34, 0.11, 0.3}, {0.41, 0.2, 0.0}, {0.41, 0.33, -0.3}, {0.34, 0.42, 0.2}};
        for (double[] s : small) g.fill(star(w * s[0], h * s[1], h * 0.06, h * 0.024, 5, s[2]));
    }

    private static void usa(Graphics2D g, double w, double h) {
        for (int i = 0; i < 13; i++) {
            g.setColor(i % 2 == 0 ? rgb(178, 34, 52) : Color.WHITE);
            g.fill(new Rectangle2D.Double(0, h * i / 13.0 - 0.3, w, h / 13.0 + 0.6));
        }
        double cw = w * 0.42, ch = h * 7 / 13.0;
        g.setColor(rgb(60, 59, 110));
        g.fill(new Rectangle2D.Double(0, 0, cw, ch));
        g.setColor(Color.WHITE);
        double r = Math.max(0.6, h * 0.028);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 5; col++) {
                double px = cw * (col + 0.7 + (row % 2) * 0.0) / 5.4, py = ch * (row + 0.6) / 4.2;
                g.fill(new Ellipse2D.Double(px - r, py - r, 2 * r, 2 * r));
            }
        }
    }

    private static void britain(Graphics2D g, double w, double h) {
        g.setColor(rgb(1, 33, 105));
        g.fill(new Rectangle2D.Double(0, 0, w, h));
        g.setStroke(new BasicStroke((float) (h * 0.2)));
        g.setColor(Color.WHITE);
        g.draw(new Line2D.Double(0, 0, w, h));
        g.draw(new Line2D.Double(0, h, w, 0));
        g.setStroke(new BasicStroke((float) (h * 0.08)));
        g.setColor(rgb(200, 16, 46));
        g.draw(new Line2D.Double(0, 0, w, h));
        g.draw(new Line2D.Double(0, h, w, 0));
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(w * 0.5 - h * 0.17, 0, h * 0.34, h));
        g.fill(new Rectangle2D.Double(0, h * 0.5 - h * 0.17, w, h * 0.34));
        g.setColor(rgb(200, 16, 46));
        g.fill(new Rectangle2D.Double(w * 0.5 - h * 0.1, 0, h * 0.2, h));
        g.fill(new Rectangle2D.Double(0, h * 0.5 - h * 0.1, w, h * 0.2));
    }

    private static void egypt(Graphics2D g, double w, double h) {
        horizontal(g, w, h, rgb(206, 17, 38), Color.WHITE, Color.BLACK);
        g.setColor(rgb(193, 154, 49));
        double r = h * 0.11;
        g.fill(new Ellipse2D.Double(w / 2 - r, h / 2 - r, 2 * r, 2 * r));
        g.fill(new Rectangle2D.Double(w / 2 - r * 0.5, h / 2 + r * 0.4, r, r * 0.9));
    }

    private static void iran(Graphics2D g, double w, double h) {
        horizontal(g, w, h, rgb(35, 159, 64), Color.WHITE, rgb(218, 0, 0));
        g.setColor(rgb(218, 0, 0));
        double r = h * 0.1;
        g.setStroke(new BasicStroke((float) Math.max(1, h * 0.035)));
        g.draw(new Ellipse2D.Double(w / 2 - r, h / 2 - r, 2 * r, 2 * r));
        g.draw(new Line2D.Double(w / 2, h / 2 - r * 1.15, w / 2, h / 2 + r * 1.15));
    }

    private static void spain(Graphics2D g, double w, double h) {
        g.setColor(rgb(170, 21, 27));
        g.fill(new Rectangle2D.Double(0, 0, w, h));
        g.setColor(rgb(241, 191, 0));
        g.fill(new Rectangle2D.Double(0, h * 0.25, w, h * 0.5));
        g.setColor(rgb(173, 21, 27));
        g.fill(new RoundRectangle2D.Double(w * 0.22, h * 0.36, w * 0.11, h * 0.28, 3, 3));
        g.setColor(rgb(150, 110, 40));
        g.draw(new RoundRectangle2D.Double(w * 0.22, h * 0.36, w * 0.11, h * 0.28, 3, 3));
    }

    private static void canada(Graphics2D g, double w, double h) {
        g.setColor(rgb(216, 6, 33));
        g.fill(new Rectangle2D.Double(0, 0, w, h));
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(w * 0.25, 0, w * 0.5, h));
        g.setColor(rgb(216, 6, 33));
        g.fill(star(w / 2, h * 0.5, h * 0.3, h * 0.14, 11, 0));
        g.fill(new Rectangle2D.Double(w / 2 - h * 0.025, h * 0.55, h * 0.05, h * 0.25));
    }
}
