import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

/**
 * 펜싱 자세(앙가르드)로 서 있는 선수 그림.
 *  - 끼고 있는 장비(검 / 그립 / 마스크 / 신발 / 재킷)가 그대로 그림에 나타난다.
 *  - 성장 등급(신인~마스터)에 따라 배경색과 별 개수가 바뀐다.
 *  - 상태(훈련 / 휴식 / 부상 / 배고픔 등)는 말풍선으로 보여준다.
 *  - 장비 부위에 마우스를 올리면 장비 이름과 능력치가 툴팁으로 나온다.
 *  - 상점 미리보기: setTryOn() 으로 아직 안 산 장비를 입혀 볼 수 있다.
 */
public class FencerFigure extends JPanel {
    private static final int CW = 270, CH = 340;   // 그림을 그리는 가상 캔버스 크기

    private Fencer fencer;
    private Equipment tryOn;
    private double phase = 0;
    private double scale = 1, offX = 0, offY = 0;
    private final Timer timer;

    public FencerFigure() {
        setOpaque(false);
        setPreferredSize(new Dimension(260, 330));
        timer = new Timer(60, e -> {
            if (isShowing()) {
                phase += 0.12;
                repaint();
            }
        });
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    public void setFencer(Fencer f) {
        this.fencer = f;
        repaint();
    }

    /** 아직 사지 않은 장비를 입혀 본다. item 이 null 이면 현재 장비 그대로 */
    public void setTryOn(Fencer f, Equipment item) {
        this.fencer = f;
        this.tryOn = item;
        repaint();
    }

    private Equipment gearOf(EquipSlot slot) {
        if (tryOn != null && tryOn.getSlot() == slot) return tryOn;
        return fencer == null ? null : fencer.getGear(slot);
    }

    // ---------- 그리기 ----------

    @Override
    protected void paintComponent(Graphics gr) {
        Graphics2D g = (Graphics2D) gr.create();
        GearArt.smooth(g);
        int w = getWidth(), h = getHeight();
        Grade grade = fencer == null ? Grade.ROOKIE : fencer.getGrade();

        drawBackground(g, w, h, grade);

        scale = Math.min(w / (double) CW, h / (double) CH);
        offX = (w - CW * scale) / 2;
        offY = (h - CH * scale) / 2;
        Graphics2D f = (Graphics2D) g.create();
        f.translate(offX, offY);
        f.scale(scale, scale);
        drawFigure(f, grade);
        f.dispose();

        // 등급 별
        g.setColor(new Color(224, 160, 20));
        for (int i = 0; i <= grade.ordinal(); i++) GearArt.star(g, 18 + i * 17, 20, 7.5, 3.2);
        // 상태 말풍선
        String status = statusText();
        if (status != null) drawBubble(g, status, w);
        g.dispose();
    }

    private void drawBackground(Graphics2D g, int w, int h, Grade grade) {
        Color top, bottom;
        switch (grade) {
            case JUNIOR: top = new Color(226, 242, 230); bottom = new Color(190, 222, 200); break;
            case PRO:    top = new Color(222, 234, 252); bottom = new Color(172, 196, 240); break;
            case MASTER: top = new Color(254, 242, 204); bottom = new Color(236, 204, 120); break;
            default:     top = new Color(234, 238, 246); bottom = new Color(204, 213, 232);
        }
        g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 22, 22));
        g.setColor(bottom.darker());
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 22, 22));
    }

    private void drawFigure(Graphics2D g, Grade grade) {
        WeaponType weapon = fencer == null ? WeaponType.FOIL : fencer.getWeapon();
        Personality personality = fencer == null ? Personality.CAREFUL : fencer.getPersonality();
        Color accent = GearArt.accent(weapon);
        Equipment blade = gearOf(EquipSlot.BLADE), grip = gearOf(EquipSlot.GRIP), mask = gearOf(EquipSlot.MASK);
        Equipment shoes = gearOf(EquipSlot.SHOES), jacket = gearOf(EquipSlot.JACKET);
        double bob = Math.sin(phase) * 1.6;

        // 높은 등급은 몸 뒤에 은은한 빛
        if (grade.ordinal() >= Grade.PRO.ordinal()) {
            Color glow = grade == Grade.MASTER ? new Color(255, 214, 90) : new Color(110, 160, 255);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(112, 170), 150,
                    new float[]{0f, 1f}, new Color[]{new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 120),
                    new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)}));
            g.fill(new Rectangle2D.Double(-40, 0, CW + 80, CH));
        }

        // 바닥 (피스트)
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(28, 300, 170, 18));
        g.setColor(new Color(112, 120, 138));
        g.fill(new RoundRectangle2D.Double(6, 308, 258, 16, 8, 8));
        g.setColor(new Color(160, 168, 184));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Line2D.Double(14, 312, 256, 312));

        Color sleeve = GearArt.jacketColor(jacket, accent);
        Color pants = jacket != null ? new Color(232, 235, 242) : new Color(62, 70, 90);

        // 다리
        g.setColor(pants);
        g.setStroke(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double rear = new Path2D.Double();
        rear.moveTo(92, 198); rear.lineTo(68, 250); rear.lineTo(52, 288);
        g.draw(rear);
        Path2D.Double front = new Path2D.Double();
        front.moveTo(112, 198); front.lineTo(146, 244); front.lineTo(150, 288);
        g.draw(front);
        GearTag tag = shoes == null ? null : shoes.getTag();
        GearArt.shoe(g, 50, 288, 1.0, GearArt.tier(shoes), tag, false);
        GearArt.shoe(g, 152, 288, 1.0, GearArt.tier(shoes), tag, true);

        // 상체 (숨 쉬듯 살짝 움직임)
        AffineTransform keep = g.getTransform();
        g.translate(0, bob);

        // 뒤쪽 팔 (위로 접어 올림)
        g.setColor(sleeve.darker());
        g.setStroke(new BasicStroke(13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double rearArm = new Path2D.Double();
        rearArm.moveTo(80, 114); rearArm.lineTo(52, 104); rearArm.lineTo(48, 78);
        g.draw(rearArm);
        g.setColor(GearArt.SKIN);
        g.fill(new Ellipse2D.Double(41, 65, 14, 14));

        // 몸통
        GearArt.jacket(g, 72, 96, 58, 106, jacket, accent);

        // 머리
        g.setColor(GearArt.SKIN);
        g.fill(new Rectangle2D.Double(94, 84, 12, 14));
        g.fill(new Ellipse2D.Double(78, 42, 44, 44));
        g.setColor(hairColor(personality));
        g.fill(new Arc2D.Double(77, 40, 46, 42, 8, 164, Arc2D.CHORD));
        g.fill(new RoundRectangle2D.Double(100, 42, 24, 12, 10, 10));
        g.setColor(new Color(60, 50, 50));
        g.fill(new Ellipse2D.Double(90, 63, 4, 4));
        g.fill(new Ellipse2D.Double(106, 63, 4, 4));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new Arc2D.Double(94, 68, 12, 8, 200, 140, Arc2D.OPEN));
        if (fencer != null && fencer.isInjured()) {   // 반창고
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(106, 70, 14, 7, 3, 3));
            g.setColor(new Color(220, 70, 70));
            g.fill(new Rectangle2D.Double(112, 70, 2, 7));
        }
        if (mask != null) GearArt.mask(g, 100, 64, 22, GearArt.tier(mask));

        // 앞쪽 팔 + 무기
        g.setColor(sleeve);
        g.setStroke(new BasicStroke(13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double frontArm = new Path2D.Double();
        frontArm.moveTo(122, 112); frontArm.lineTo(148, 126); frontArm.lineTo(170, 118);
        g.draw(frontArm);
        double wobble = Math.sin(phase * 1.3) * 1.2;
        GearArt.sword(g, 176, 118, -6 + wobble, 86, weapon, blade, grip);
        g.setColor(GearArt.SKIN);
        g.fill(new Ellipse2D.Double(168, 110, 16, 16));
        g.setColor(GearArt.SKIN.darker());
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(168, 110, 16, 16));
        g.setTransform(keep);
    }

    private static Color hairColor(Personality p) {
        switch (p) {
            case PASSIONATE:  return new Color(170, 74, 44);
            case LAZY:        return new Color(160, 124, 76);
            case COMPETITIVE: return new Color(34, 56, 118);
            default:          return new Color(40, 38, 46);
        }
    }

    private String statusText() {
        if (fencer == null) return null;
        if (fencer.isBusy()) return "시합 중";
        if (fencer.isTraining()) return "훈련 중";
        if (fencer.isResting()) return "Zzz 휴식 중";
        if (fencer.isInjured()) return "부상...";
        if (fencer.getHunger() >= 80) return "배고파요";
        if (fencer.getFatigue() >= 80) return "지쳤어요";
        return null;
    }

    private void drawBubble(Graphics2D g, String text, int w) {
        g.setFont(getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics fm = g.getFontMetrics();
        int bw = fm.stringWidth(text) + 18, bh = 26;
        int bx = w - bw - 10, by = 12;
        g.setColor(new Color(255, 255, 255, 235));
        g.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 14, 14));
        g.setColor(new Color(120, 130, 150));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(bx, by, bw, bh, 14, 14));
        g.setColor(new Color(40, 48, 70));
        g.drawString(text, bx + 9, by + 18);
    }

    // ---------- 툴팁 ----------

    @Override
    public String getToolTipText(MouseEvent e) {
        double x = (e.getX() - offX) / scale, y = (e.getY() - offY) / scale;
        EquipSlot slot = null;
        if (x >= 176 && y >= 80 && y <= 140) slot = EquipSlot.BLADE;
        else if (x >= 138 && x < 176 && y >= 96 && y <= 134) slot = EquipSlot.GRIP;
        else if (Point2D.distance(x, y, 100, 66) <= 30) slot = EquipSlot.MASK;
        else if (x >= 70 && x <= 134 && y >= 96 && y <= 204) slot = EquipSlot.JACKET;
        else if (y >= 280 && y <= 312) slot = EquipSlot.SHOES;
        if (slot == null) return null;
        Equipment eq = gearOf(slot);
        return slot + ": " + (eq == null ? "장착 안 함" : eq.getName() + " (" + eq.statText() + ")");
    }
}
