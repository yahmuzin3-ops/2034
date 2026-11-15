import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * 장비 / 아이템 그림. 이미지 파일 없이 Java2D 로 직접 그린다.
 * 선수 화면(FencerFigure)과 상점/인벤토리 아이콘이 같은 그림 함수를 쓴다.
 *
 * 등급(tier): 0 = 장비 없음(기본 모습), 1 = 하급, 2 = 중급, 3 = 상급
 * 날씨 대응 장비(GearTag)는 중급 등급 + 전용 무늬(논슬립 밑창, 쿨링 통풍구)로 그린다.
 */
public final class GearArt {
    public static final Color SKIN = new Color(242, 208, 176);

    private GearArt() {
    }

    // ---------- 공통 도우미 ----------

    /** 장비 등급: 가격으로 하급/중급/상급을 나눈다. 장비가 없으면 0 */
    public static int tier(Equipment e) {
        if (e == null) return 0;
        if (e.getTag() != null) return 2;
        int p = e.getPrice();
        return p <= 200 ? 1 : p <= 600 ? 2 : 3;
    }

    /** 종목별 대표 색 (옷 포인트 색) */
    public static Color accent(WeaponType w) {
        if (w == null) return new Color(74, 120, 216);
        switch (w) {
            case FOIL: return new Color(74, 120, 216);
            case EPEE: return new Color(52, 160, 110);
            default:   return new Color(212, 72, 72);
        }
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ---------- 무기 (그립 + 칼날 + 가드) ----------

    /** 손 위치(x, y)에서 angleDeg 방향으로 뻗은 무기를 그린다. blade / grip 이 null 이면 기본 모양 */
    public static void sword(Graphics2D g, double x, double y, double angleDeg, double len,
                             WeaponType w, Equipment blade, Equipment grip) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.toRadians(angleDeg));
        gripShape(g, tier(grip), 1.0);
        bladeShape(g, len, tier(blade), w);
        guardShape(g, w, tier(blade));
        g.setTransform(old);
    }

    /** 그립: 로컬 좌표에서 x = -34 ~ 2 에 그린다 (손이 잡는 곳이 원점) */
    public static void gripShape(Graphics2D g, int tier, double s) {
        AffineTransform old = g.getTransform();
        g.scale(s, s);
        Color base, line;
        switch (tier) {
            case 1:  base = new Color(206, 162, 102); line = new Color(150, 108, 58); break;   // 코르크
            case 2:  base = new Color(38, 92, 104);   line = new Color(110, 190, 205); break;  // 컨트롤
            case 3:  base = new Color(30, 30, 36);    line = new Color(255, 140, 30); break;   // 프로
            default: base = new Color(52, 52, 58);    line = new Color(90, 90, 98);
        }
        g.setColor(base);
        g.fill(new RoundRectangle2D.Double(-34, -4.5, 36, 9, 6, 6));
        g.setColor(line);
        g.setStroke(new BasicStroke(1.3f));
        if (tier == 1) {
            for (int i = 0; i < 7; i++) {
                g.fill(new Ellipse2D.Double(-31 + i * 5, (i % 2 == 0) ? -2.6 : 0.8, 1.8, 1.8));
            }
        } else if (tier == 2) {
            for (int i = 0; i < 5; i++) g.draw(new Line2D.Double(-28 + i * 6, -4, -28 + i * 6, 4));
        } else if (tier == 3) {
            for (int i = 0; i < 6; i++) g.draw(new Line2D.Double(-31 + i * 6, 4.4, -27 + i * 6, -4.4));
        }
        g.setColor(tier == 3 ? new Color(245, 200, 70) : tier == 2 ? new Color(120, 200, 215) : new Color(150, 150, 158));
        g.fill(new Ellipse2D.Double(-39, -5.5, 11, 11));
        g.setTransform(old);
    }

    /** 칼날: 로컬 좌표에서 x = 2 ~ len */
    public static void bladeShape(Graphics2D g, double len, int tier, WeaponType w) {
        float width = (w == WeaponType.EPEE) ? 4.6f : 3.2f;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(2, 0);
        p.quadTo(len * 0.55, -len * 0.03, len, -len * 0.012);   // 살짝 휜 날
        Color c;
        switch (tier) {
            case 1:  c = new Color(205, 212, 224); break;
            case 2:  c = new Color(120, 152, 196); break;
            case 3:  c = new Color(46, 54, 68);    break;
            default: c = new Color(186, 192, 202);
        }
        if (tier == 3) {
            g.setColor(new Color(0, 225, 255, 70));
            g.setStroke(new BasicStroke(width + 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(p);
        }
        g.setColor(c);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(p);
        if (tier == 2) {
            g.setColor(new Color(225, 238, 252));
            g.setStroke(new BasicStroke(1f));
            g.draw(p);
        } else if (tier == 3) {
            g.setColor(new Color(0, 225, 255));
            g.setStroke(new BasicStroke(1.1f));
            g.draw(p);
        }
        g.setColor(tier == 3 ? new Color(0, 225, 255) : new Color(120, 124, 134));
        g.fill(new Ellipse2D.Double(len - 2.5, -len * 0.012 - 2.5, 5, 5));
    }

    /** 가드: 종목마다 모양이 다르다 (플뢰레 작은 종, 에페 큰 종, 사브르 손등 가드) */
    public static void guardShape(Graphics2D g, WeaponType w, int tier) {
        Color shell = tier == 3 ? new Color(235, 200, 90) : tier == 2 ? new Color(190, 205, 225) : new Color(168, 172, 182);
        Color dark = new Color(70, 74, 84);
        if (w == WeaponType.SABRE) {
            Path2D.Double k = new Path2D.Double();
            k.moveTo(4, -4);
            k.curveTo(6, -17, -20, -17, -30, -6);
            g.setColor(shell);
            g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(k);
            g.fill(new Ellipse2D.Double(0.5, -7, 6, 14));
            g.setColor(dark);
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Double(0.5, -7, 6, 14));
            return;
        }
        double r = w == WeaponType.EPEE ? 16 : 11;
        Arc2D.Double cup = new Arc2D.Double(-r * 0.5, -r, r * 1.2, r * 2, -90, 180, Arc2D.PIE);
        g.setColor(shell);
        g.fill(cup);
        g.setColor(dark);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(cup);
        g.draw(new Line2D.Double(-r * 0.5 + r * 0.6, -r * 0.8, -r * 0.5 + r * 0.6, r * 0.8));
    }

    // ---------- 마스크 ----------

    /** 마스크(투구). (cx, cy) 는 얼굴 중심, r 은 얼굴 반지름 */
    public static void mask(Graphics2D g, double cx, double cy, double r, int tier) {
        Color frame, mesh, trim;
        switch (tier) {
            case 1:  frame = new Color(110, 116, 128); mesh = new Color(140, 146, 158); trim = null; break;
            case 2:  frame = new Color(46, 56, 82);    mesh = new Color(100, 118, 158); trim = new Color(70, 120, 230); break;
            default: frame = new Color(196, 203, 215); mesh = new Color(238, 241, 247); trim = new Color(240, 190, 70);
        }
        // 턱받이(bib)
        Path2D.Double bib = new Path2D.Double();
        bib.moveTo(cx - r * 0.95, cy + r * 0.62);
        bib.lineTo(cx + r * 0.95, cy + r * 0.62);
        bib.lineTo(cx + r * 1.12, cy + r * 1.38);
        bib.lineTo(cx - r * 1.12, cy + r * 1.38);
        bib.closePath();
        g.setColor(new Color(240, 241, 245));
        g.fill(bib);
        g.setColor(frame);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(bib);
        // 머리 부분 (얼굴이 비쳐 보이도록 반투명)
        Ellipse2D.Double dome = new Ellipse2D.Double(cx - r * 1.14, cy - r * 1.16, r * 2.28, r * 2.3);
        g.setColor(new Color(frame.getRed(), frame.getGreen(), frame.getBlue(), tier == 3 ? 70 : 50));
        g.fill(dome);
        Shape oldClip = g.getClip();
        g.clip(dome);
        g.setColor(new Color(mesh.getRed(), mesh.getGreen(), mesh.getBlue(), 190));
        g.setStroke(new BasicStroke(tier == 1 ? 1f : 1.4f));
        for (int i = -5; i <= 5; i++) g.draw(new Line2D.Double(cx + i * r * 0.24, cy - r * 1.3, cx + i * r * 0.24, cy + r * 1.3));
        for (int j = -5; j <= 5; j++) g.draw(new Line2D.Double(cx - r * 1.3, cy + j * r * 0.26, cx + r * 1.3, cy + j * r * 0.26));
        if (trim != null) {
            g.setColor(trim);
            g.fill(new Rectangle2D.Double(cx - r * 1.2, cy - r * 0.66, r * 2.4, r * 0.15));
            if (tier == 3) {
                g.setStroke(new BasicStroke(2.6f));
                g.draw(new Line2D.Double(cx, cy - r * 1.2, cx, cy + r * 1.2));
            }
        }
        g.setClip(oldClip);
        g.setColor(frame);
        g.setStroke(new BasicStroke(tier == 1 ? 2f : 2.8f));
        g.draw(dome);
    }

    // ---------- 신발 ----------

    /** 신발 옆모습. (x, y) = 발목 위쪽 중심, 오른쪽을 보는 모양이 기본. 날씨 대응 장비면 tag 로 무늬를 바꾼다 */
    public static void shoe(Graphics2D g, double x, double y, double s, int tier, GearTag tag, boolean facingRight) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.scale(facingRight ? s : -s, s);
        Color upper, stripe, sole;
        switch (tier) {
            case 1:  upper = new Color(238, 240, 245); stripe = new Color(110, 170, 235); sole = new Color(190, 194, 204); break;
            case 2:  upper = new Color(244, 244, 247); stripe = new Color(246, 150, 50);  sole = new Color(80, 86, 100);   break;
            case 3:  upper = new Color(34, 36, 44);    stripe = new Color(230, 50, 60);   sole = new Color(18, 18, 22);    break;
            default: upper = new Color(122, 128, 140); stripe = new Color(170, 176, 188); sole = new Color(70, 74, 84);
        }
        if (tag == GearTag.RAIN_PROOF) {
            upper = new Color(52, 92, 70);
            stripe = new Color(176, 230, 80);
            sole = new Color(150, 220, 60);
        }
        Path2D.Double body = new Path2D.Double();
        body.moveTo(-10, -5);
        body.lineTo(8, -5);
        body.quadTo(16, 1, 26, 9);
        body.lineTo(26, 13);
        body.lineTo(-12, 13);
        body.lineTo(-12, 3);
        body.closePath();
        g.setColor(upper);
        g.fill(body);
        g.setColor(upper.darker());
        g.setStroke(new BasicStroke(1.2f));
        g.draw(body);
        // 옆선
        Path2D.Double st = new Path2D.Double();
        st.moveTo(-10, 6);
        st.quadTo(6, 0, 20, 9);
        st.lineTo(20, 12);
        st.quadTo(6, 6, -10, 11);
        st.closePath();
        g.setColor(stripe);
        g.fill(st);
        // 끈
        g.setColor(upper.darker());
        for (int i = 0; i < 3; i++) g.draw(new Line2D.Double(2 + i * 4.5, -3 + i * 2.4, 6 + i * 4.5, 0 + i * 2.4));
        // 밑창
        g.setColor(sole);
        g.fill(new RoundRectangle2D.Double(-13, 12, 40, 5.5, 4, 4));
        if (tag == GearTag.RAIN_PROOF) {
            g.setColor(new Color(40, 80, 30));
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 8; i++) g.draw(new Line2D.Double(-10 + i * 5, 13, -8 + i * 5, 17));
        } else if (tier == 3) {
            g.setColor(new Color(200, 200, 210));
            for (int i = 0; i < 5; i++) {
                double px = -8 + i * 8;
                g.fill(new Polygon(new int[]{(int) px, (int) px + 3, (int) px + 6}, new int[]{17, 21, 17}, 3));
            }
        }
        g.setTransform(old);
    }

    // ---------- 재킷 ----------

    public static Color jacketColor(Equipment e, Color accent) {
        if (e == null) return accent;
        if (e.getTag() == GearTag.HEAT_PROOF) return new Color(236, 248, 255);
        return new Color(246, 246, 248);
    }

    /** 상체: 장비가 없으면 종목 색 훈련복, 있으면 펜싱 재킷 */
    public static void jacket(Graphics2D g, double x, double y, double w, double h, Equipment e, Color accent) {
        Color base = jacketColor(e, accent);
        RoundRectangle2D.Double body = new RoundRectangle2D.Double(x, y, w, h, 22, 22);
        g.setColor(base);
        g.fill(body);
        g.setColor(base.darker());
        g.setStroke(new BasicStroke(1.6f));
        g.draw(body);
        // 목선 / 깃
        Path2D.Double v = new Path2D.Double();
        v.moveTo(x + w * 0.30, y);
        v.lineTo(x + w * 0.50, y + h * 0.17);
        v.lineTo(x + w * 0.70, y);
        v.closePath();
        g.setColor(e == null ? SKIN : new Color(222, 226, 234));
        g.fill(v);
        g.setColor(base.darker());
        g.draw(v);
        if (e != null) {
            g.setColor(new Color(150, 154, 166));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new Line2D.Double(x + w * 0.5, y + h * 0.17, x + w * 0.5, y + h - 8));
            Color ac = e.getTag() == GearTag.HEAT_PROOF ? new Color(70, 190, 230) : accent;
            g.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 200));
            g.setStroke(new BasicStroke(2.2f));
            g.draw(new Line2D.Double(x + 5, y + h * 0.2, x + 5, y + h - 8));
            g.draw(new Line2D.Double(x + w - 5, y + h * 0.2, x + w - 5, y + h - 8));
            if (e.getTag() == GearTag.HEAT_PROOF) {
                g.setColor(new Color(150, 214, 240));
                g.fill(new RoundRectangle2D.Double(x + 8, y + h * 0.3, w * 0.16, h * 0.5, 8, 8));
                g.fill(new RoundRectangle2D.Double(x + w - 8 - w * 0.16, y + h * 0.3, w * 0.16, h * 0.5, 8, 8));
                g.setColor(new Color(60, 150, 190));
                g.setStroke(new BasicStroke(1.4f));
                for (int i = 0; i < 3; i++) {
                    double vy = y + h * 0.40 + i * h * 0.12;
                    g.draw(new Line2D.Double(x + 12, vy, x + 8 + w * 0.16 - 2, vy));
                    g.draw(new Line2D.Double(x + w - 12, vy, x + w - 6 - w * 0.16 + 2, vy));
                }
                snowflake(g, x + w * 0.70, y + h * 0.32, 6, new Color(60, 150, 200));
            } else {
                g.setColor(accent);
                g.fill(new Ellipse2D.Double(x + w * 0.64, y + h * 0.26, 9, 9));
            }
        }
        g.setColor(e == null ? base.darker() : new Color(205, 209, 219));
        g.fill(new Rectangle2D.Double(x + 3, y + h - 7, w - 6, 7));
    }

    private static void snowflake(Graphics2D g, double cx, double cy, double r, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(i * 60);
            g.draw(new Line2D.Double(cx - Math.cos(a) * r, cy - Math.sin(a) * r, cx + Math.cos(a) * r, cy + Math.sin(a) * r));
        }
    }

    // ---------- 아이콘 (상점 / 인벤토리) ----------

    /** 아이템 아이콘. size x size 픽셀 */
    public static Icon icon(Item it, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        smooth(g);
        g.scale(size / 100.0, size / 100.0);
        paintItem(g, it);
        g.dispose();
        return new ImageIcon(img);
    }

    /** 100 x 100 칸 안에 아이템을 그린다 */
    public static void paintItem(Graphics2D g, Item it) {
        int tier = it instanceof Equipment ? tier((Equipment) it) : 0;
        Color top, bottom;
        switch (tier) {
            case 1:  top = new Color(240, 214, 186); bottom = new Color(214, 176, 140); break;   // 하급: 청동
            case 2:  top = new Color(226, 232, 242); bottom = new Color(184, 194, 212); break;   // 중급: 은
            case 3:  top = new Color(252, 236, 160); bottom = new Color(232, 192, 80);  break;   // 상급: 금
            default: top = new Color(240, 244, 250); bottom = new Color(208, 218, 234);
        }
        g.setPaint(new GradientPaint(0, 0, top, 0, 100, bottom));
        g.fill(new RoundRectangle2D.Double(2, 2, 96, 96, 20, 20));
        g.setColor(bottom.darker());
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new RoundRectangle2D.Double(2, 2, 96, 96, 20, 20));

        if (it instanceof Equipment) {
            paintEquipment(g, (Equipment) it, tier);
        } else if (it instanceof Food) {
            paintFood(g, it.getName());
        } else if (it instanceof Medicine) {
            paintMedicine(g, it.getName());
        }
        if (tier > 0) {   // 등급 별
            g.setColor(new Color(214, 150, 20));
            for (int i = 0; i < tier; i++) star(g, 14 + i * 13, 90, 5, 2.2);
        }
    }

    private static void paintEquipment(Graphics2D g, Equipment e, int tier) {
        Color accent = new Color(74, 120, 216);
        EquipSlot slot = e.getSlot();
        if (slot == null) {
            g.setColor(new Color(120, 130, 150));
            g.fill(new Ellipse2D.Double(30, 30, 40, 40));
            return;
        }
        switch (slot) {
            case BLADE:
                sword(g, 24, 76, -42, 78, WeaponType.FOIL, e, null);
                break;
            case GRIP: {
                AffineTransform old = g.getTransform();
                g.translate(54, 50);
                g.rotate(Math.toRadians(-28));
                gripShape(g, tier, 1.7);
                g.setTransform(old);
                break;
            }
            case MASK:
                mask(g, 50, 40, 26, tier);
                break;
            case SHOES:
                shoe(g, 50, 36, 1.55, tier, e.getTag(), true);
                break;
            default: {   // JACKET
                Color c = jacketColor(e, accent);
                g.setColor(c);
                g.fill(new RoundRectangle2D.Double(10, 24, 18, 44, 10, 10));
                g.fill(new RoundRectangle2D.Double(72, 24, 18, 44, 10, 10));
                g.setColor(c.darker());
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new RoundRectangle2D.Double(10, 24, 18, 44, 10, 10));
                g.draw(new RoundRectangle2D.Double(72, 24, 18, 44, 10, 10));
                jacket(g, 24, 14, 52, 66, e, accent);
            }
        }
    }

    private static void paintFood(Graphics2D g, String name) {
        if (name.contains("도시락")) {
            g.setColor(new Color(150, 98, 60));
            g.fill(new RoundRectangle2D.Double(18, 34, 64, 40, 10, 10));
            g.setColor(new Color(250, 250, 246));
            g.fill(new RoundRectangle2D.Double(23, 39, 25, 30, 6, 6));                      // 밥
            g.setColor(new Color(244, 200, 150));
            g.fill(new RoundRectangle2D.Double(52, 39, 25, 14, 6, 6));                      // 닭가슴살
            g.setColor(new Color(110, 190, 90));
            g.fill(new RoundRectangle2D.Double(52, 56, 25, 13, 6, 6));                      // 채소
            g.setColor(new Color(210, 96, 70));
            g.fill(new Ellipse2D.Double(32, 48, 7, 7));
        } else if (name.contains("쉐이크")) {
            g.setColor(new Color(244, 244, 248));
            g.fill(new RoundRectangle2D.Double(36, 28, 28, 50, 8, 8));
            g.setColor(new Color(90, 140, 220));
            g.fill(new Rectangle2D.Double(36, 46, 28, 16));
            g.setColor(new Color(60, 70, 90));
            g.fill(new RoundRectangle2D.Double(38, 18, 24, 12, 6, 6));
            g.setColor(new Color(150, 156, 170));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new RoundRectangle2D.Double(36, 28, 28, 50, 8, 8));
        } else {   // 에너지바
            g.setColor(new Color(236, 150, 50));
            g.fill(new RoundRectangle2D.Double(16, 38, 68, 26, 8, 8));
            g.setColor(new Color(255, 232, 190));
            g.fill(new Polygon(new int[]{46, 58, 52}, new int[]{44, 44, 58}, 3));
            g.setColor(new Color(160, 90, 20));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new RoundRectangle2D.Double(16, 38, 68, 26, 8, 8));
            for (int i = 0; i < 4; i++) g.draw(new Line2D.Double(20 + i * 3, 38, 20 + i * 3, 64));
        }
    }

    private static void paintMedicine(Graphics2D g, String name) {
        if (name.contains("구급")) {
            g.setColor(new Color(222, 62, 62));
            g.fill(new RoundRectangle2D.Double(18, 32, 64, 44, 10, 10));
            g.setColor(new Color(140, 30, 30));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new RoundRectangle2D.Double(18, 32, 64, 44, 10, 10));
            g.draw(new RoundRectangle2D.Double(38, 24, 24, 10, 5, 5));
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Double(46, 40, 8, 28));
            g.fill(new Rectangle2D.Double(36, 50, 28, 8));
        } else if (name.contains("쿠폰")) {
            g.setColor(new Color(250, 244, 220));
            g.fill(new RoundRectangle2D.Double(14, 34, 72, 36, 8, 8));
            g.setColor(new Color(200, 120, 40));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 3f}, 0f));
            g.draw(new RoundRectangle2D.Double(18, 38, 64, 28, 6, 6));
            g.setColor(new Color(60, 160, 120));
            g.fill(new Rectangle2D.Double(46, 44, 8, 16));
            g.fill(new Rectangle2D.Double(42, 48, 16, 8));
        } else {   // 파스
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(-18), 50, 50);
            g.setColor(new Color(236, 200, 160));
            g.fill(new RoundRectangle2D.Double(24, 34, 52, 32, 12, 12));
            g.setColor(new Color(190, 150, 110));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new RoundRectangle2D.Double(24, 34, 52, 32, 12, 12));
            g.setColor(new Color(120, 190, 150));
            g.fill(new RoundRectangle2D.Double(36, 42, 28, 16, 6, 6));
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Double(48, 44, 4, 12));
            g.fill(new Rectangle2D.Double(44, 48, 12, 4));
            g.setTransform(old);
        }
    }

    /** 별 모양 (등급 표시) */
    public static void star(Graphics2D g, double cx, double cy, double outer, double inner) {
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outer : inner;
            double a = Math.toRadians(-90 + i * 36);
            double px = cx + Math.cos(a) * r, py = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        g.fill(p);
    }
}
