import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * 전투 장면: 피스트(펜싱 경기장) 위에서 1대1로 겨루는 모습을 그린다.
 * Battle 이 만든 이벤트(Battle.Event)를 하나씩 재생한다.
 *  - 두 선수가 고른 전술을 동시에 공개하고, 런지/방어/반격/페인트/전진/후퇴 동작을 보여준다.
 *  - 전기 채점기처럼 득점 램프(왼쪽 빨강, 오른쪽 초록, 무효 타격은 흰색)가 켜진다.
 *  - 우선권, 남은 시간, 팀 점수, 이번 경기 점수, 선수 체력, 팀원 목록을 표시한다.
 */
public class FencingStage extends JPanel {

    /** 피스트 위에 서 있는 선수 */
    private static class Fx {
        final Fencer f;
        final boolean rightSide;
        int hp, maxHp;
        double lunge;        // 찌르기 정도 0~1
        double shift;        // 전진(+)/후퇴(-) 이동(px)
        long flashUntil;     // 맞았을 때 번쩍임

        Fx(Fencer f, boolean rightSide) {
            this.f = f;
            this.rightSide = rightSide;
            this.hp = f.getHp();
            this.maxHp = f.getMaxHp();
        }
    }

    private static class FloatText {
        String text;
        Color color;
        double x, y;
        long born, life;
        float size;
    }

    private final List<FloatText> floats = new ArrayList<>();
    private final Deque<Battle.Event> queue = new ArrayDeque<>();
    private final Random rnd = new Random();
    private final Map<Fencer, Integer> boutsDone = new HashMap<>();
    private final javax.swing.Timer timer = new javax.swing.Timer(16, e -> tick());

    private Battle battle;
    private Weather weather = Weather.SUNNY;
    private Fx L, R;
    private int pScore, eScore, boutP, boutR, clockShown, retreatL, retreatR, boutNo = 1, boutTotal = 1, periodShown = 1, targetShown = 15;
    private boolean suddenShown;
    private Fencer priorityShown;
    private long redUntil, greenUntil, whiteUntil, shakeUntil, scorePulseUntil, sparkUntil;
    private String banner;
    private Color bannerColor = Color.WHITE;
    private long bannerStart, bannerUntil;
    private boolean fast;

    private Battle.Event cur;
    private long curStart, curDur;
    private boolean impact, spark;
    private Runnable onDone;
    private Consumer<String> logSink;

    public FencingStage() {
        setBackground(new Color(18, 26, 50));
        setPreferredSize(new Dimension(800, 440));
        setMinimumSize(new Dimension(580, 360));
    }

    // ---------- 외부에서 쓰는 메서드 ----------

    public void setLogSink(Consumer<String> sink) { this.logSink = sink; }

    public void setFast(boolean fast) { this.fast = fast; }

    public boolean isBusy() { return cur != null || !queue.isEmpty(); }

    public void setup(Battle battle) {
        this.battle = battle;
        this.weather = battle.getWeather();
        floats.clear();
        queue.clear();
        boutsDone.clear();
        cur = null;
        onDone = null;
        banner = null;
        pScore = eScore = boutP = boutR = 0;
        L = R = null;
        priorityShown = null;
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    public void stop() {
        timer.stop();
        queue.clear();
        cur = null;
        onDone = null;
    }

    /** 이벤트들을 순서대로 재생하고, 다 끝나면 done 을 실행한다. */
    public void play(List<Battle.Event> events, Runnable done) {
        queue.addAll(events);
        onDone = done;
        if (!timer.isRunning()) timer.start();
    }

    // ---------- 재생 ----------

    private void tick() {
        long now = System.currentTimeMillis();
        floats.removeIf(f -> now - f.born > f.life);
        if (cur == null) {
            if (!queue.isEmpty()) {
                begin(queue.poll(), now);
            } else if (onDone != null) {
                Runnable r = onDone;
                onDone = null;
                r.run();
            }
        } else {
            double p = Math.min(1.0, (now - curStart) / (double) Math.max(1, curDur));
            if (cur.kind == Battle.Event.Kind.PHRASE) animatePhrase(p, now);
            if (p >= 1.0) endEvent();
        }
        repaint();
    }

    private long durationOf(Battle.Event e) {
        switch (e.kind) {
            case BANNER: return 1000;
            case BOUT: return 1100;
            case PHRASE: return 1500;
            case ITEM: return 500;
            case END: return 1500;
            default: return 90;
        }
    }

    private void begin(Battle.Event e, long now) {
        cur = e;
        curStart = now;
        impact = false;
        spark = false;
        curDur = (long) (durationOf(e) * (fast ? 0.4 : 1.0));
        if (!e.text.isEmpty() && logSink != null) logSink.accept(e.text);

        switch (e.kind) {
            case BANNER:
                Sfx.round();
                showBanner(e.label == null ? "휴식" : e.label, new Color(255, 220, 90), now, curDur);
                break;
            case BOUT:
                setupBout(e);
                Sfx.round();
                showBanner(e.label == null ? "" : e.label, new Color(255, 220, 90), now, curDur);
                break;
            case ITEM:
                if (L != null) L.hp = L.f.getHp();   // 아이템으로 체력이 바뀐 경우
                if (R != null) R.hp = R.f.getHp();
                spawnAt(getWidth() / 2.0, 150, "아이템 사용", new Color(110, 230, 140), 20, 900);
                break;
            case END:
                if (e.value == 1) Sfx.win();
                else Sfx.lose();
                showBanner(e.value == 1 ? "VICTORY!" : "DEFEAT", e.value == 1 ? new Color(255, 215, 80) : new Color(230, 90, 90), now, curDur);
                break;
            case PHRASE:
                // 공개된 전술에 따라 칼이 휘두르는 소리
                boolean sabre = (L != null && L.f.getWeapon() == WeaponType.SABRE);
                if (isAction(e.leftTactic) || isAction(e.rightTactic)) Sfx.swish(sabre);
                break;
            default:
                break;
        }
    }

    private static boolean isAction(Battle.Tactic t) {
        return t == Battle.Tactic.ATTACK || t == Battle.Tactic.FEINT || t == Battle.Tactic.COUNTER;
    }

    private void setupBout(Battle.Event e) {
        boolean samePair = L != null && R != null && e.left == L.f && e.right == R.f;   // 개인전의 새 피리어드
        if (L != null && !samePair) {   // 지난 경기에 나선 선수들의 경기 수를 올린다
            boutsDone.merge(L.f, 1, Integer::sum);
            boutsDone.merge(R.f, 1, Integer::sum);
        }
        L = new Fx(e.left, false);
        R = new Fx(e.right, true);
        pScore = e.leftScore;
        eScore = e.rightScore;
        boutP = e.boutLeft;
        boutR = e.boutRight;
        clockShown = e.clock;
        priorityShown = null;
        retreatL = retreatR = 0;
        boutNo = e.value;
        boutTotal = battle.getBoutCount();
        periodShown = e.period;
        targetShown = e.target;
        suddenShown = e.sudden;
    }

    // ---------- 한 구간의 동작 ----------

    /** a~b 사이에서 올라갔다가 peak 에서 최대, c 까지 내려오는 곡선 */
    private static double bump(double p, double a, double peak, double c) {
        if (p <= a || p >= c) return 0;
        double t = p < peak ? (p - a) / (peak - a) : 1 - (p - peak) / (c - peak);
        return t * t * (3 - 2 * t);
    }

    private void animatePhrase(double p, long now) {
        if (L == null || R == null) return;
        Battle.Event e = cur;
        L.lunge = L.shift = R.lunge = R.shift = 0;
        script(L, 0, e, p);
        script(R, 1, e, p);

        if (e.parried && !spark && p >= 0.33) {
            spark = true;
            sparkUntil = now + (long) (350 * (fast ? 0.5 : 1));
            Sfx.guard();
        }
        if (!impact && p >= 0.52) {
            impact = true;
            onImpact(e, now);
        }
    }

    private void script(Fx fx, int side, Battle.Event e, double p) {
        Battle.Tactic t = side == 0 ? e.leftTactic : e.rightTactic;
        if (t == null) return;
        boolean scores = !e.exit && ((e.outcome == Battle.Outcome.TOUCH_LEFT && side == 0)
                || (e.outcome == Battle.Outcome.TOUCH_RIGHT && side == 1) || e.outcome == Battle.Outcome.DOUBLE);
        boolean ripostes = e.riposteSide == side;
        switch (t) {
            case ATTACK:
                fx.lunge = (scores ? 1.0 : 0.75) * bump(p, 0.10, 0.42, 0.80);
                if (e.evaded && !scores) fx.lunge = 0.6 * bump(p, 0.10, 0.42, 0.80);   // 닿지 못함
                break;
            case FEINT:
                fx.lunge = 0.45 * bump(p, 0.06, 0.20, 0.34) + (scores ? bump(p, 0.38, 0.58, 0.84) : 0);
                break;
            case COUNTER:
                fx.shift = 10 * bump(p, 0.15, 0.4, 0.8);
                fx.lunge = (scores ? 1.0 : 0.3) * bump(p, 0.26, 0.5, 0.8);
                break;
            case PARRY:
                if (ripostes) fx.lunge = (scores ? 1.0 : 0.75) * bump(p, 0.48, 0.66, 0.92);
                break;
            case ADVANCE:
                fx.shift = 22 * bump(p, 0.05, 0.4, 0.95);
                break;
            default:   // RETREAT
                fx.shift = -(e.evaded ? 40 : 22) * bump(p, 0.05, 0.4, 0.95);
                if (e.exit) fx.shift = -70 * Math.min(1.0, p * 1.5);
                if (ripostes) fx.lunge = (scores ? 1.0 : 0.7) * bump(p, 0.5, 0.68, 0.92);
                break;
        }
    }

    private void onImpact(Battle.Event e, long now) {
        pScore = e.leftScore;
        eScore = e.rightScore;
        boutP = e.boutLeft;
        boutR = e.boutRight;
        clockShown = e.clock;
        priorityShown = e.priority;
        retreatL = e.leftRetreat;
        retreatR = e.rightRetreat;
        L.hp = e.leftHp;
        R.hp = e.rightHp;

        switch (e.outcome) {
            case TOUCH_LEFT:
                redUntil = now + 1200;
                R.flashUntil = now + 260;
                touchFx(0, e, now);
                break;
            case TOUCH_RIGHT:
                greenUntil = now + 1200;
                L.flashUntil = now + 260;
                touchFx(1, e, now);
                break;
            case DOUBLE:
                redUntil = greenUntil = now + 1200;
                L.flashUntil = R.flashUntil = now + 260;
                Sfx.buzz();
                scorePulseUntil = now + 700;
                spawnAt(getWidth() / 2.0, 190, "동시타!", new Color(255, 160, 60), 28, 1100);
                spawnAt(getWidth() / 2.0 - 90, 66, "+1", new Color(255, 225, 90), 22, 1100);
                spawnAt(getWidth() / 2.0 + 90, 66, "+1", new Color(255, 225, 90), 22, 1100);
                break;
            case ANNUL:
                if (e.offTarget) {
                    whiteUntil = now + 1200;
                    Sfx.thud();
                    spawnAt(getWidth() / 2.0, 190, "무효 타격 (백색등)", new Color(225, 228, 240), 22, 1200);
                } else {
                    spawnAt(getWidth() / 2.0, 190, "경기 중단 - 무득점", new Color(225, 228, 240), 22, 1200);
                }
                break;
            default:
                if (e.parried) {
                    Fx d = e.riposteSide == 0 ? L : R;
                    spawnFx(d, "방어!", new Color(110, 210, 255), 20, 800);
                } else if (e.evaded) {
                    Fx d = (e.leftTactic == Battle.Tactic.RETREAT) ? L : R;
                    Sfx.dodge();
                    spawnFx(d, "회피", new Color(200, 225, 255), 20, 800);
                }
                break;
        }
        if (e.exit) spawnAt(getWidth() / 2.0, 220, "후방 라인 이탈!", new Color(255, 120, 100), 24, 1300);
    }

    private void touchFx(int side, Battle.Event e, long now) {
        Sfx.hit(false);
        Sfx.score();
        scorePulseUntil = now + 700;
        shakeUntil = now + 180;
        double x = getWidth() / 2.0 + (side == 0 ? -90 : 90);
        spawnAt(x, 66, "+1", new Color(255, 225, 90), 24, 1100);
        if (!e.exit && e.riposteSide == side) {
            spawnFx(side == 0 ? L : R, "리포스트!", new Color(120, 220, 255), 20, 1000);
        }
    }

    private void endEvent() {
        if (L != null) L.lunge = L.shift = 0;
        if (R != null) R.lunge = R.shift = 0;
        cur = null;
    }

    private void showBanner(String text, Color color, long now, long dur) {
        banner = text;
        bannerColor = color;
        bannerStart = now;
        bannerUntil = now + dur;
    }

    private void spawnFx(Fx fx, String text, Color color, int size, long life) {
        if (fx == null) return;
        spawnAt(basePos(fx).getX() + fx.shift, basePos(fx).getY() - 150, text, color, size, life);
    }

    private void spawnAt(double x, double y, String text, Color color, int size, long life) {
        FloatText f = new FloatText();
        f.text = text;
        f.color = color;
        f.x = x;
        f.y = y;
        f.born = System.currentTimeMillis();
        f.life = (long) (life * (fast ? 0.6 : 1.0));
        f.size = size;
        floats.add(f);
    }

    // ---------- 배치 ----------

    private double feetY() { return getHeight() - 150; }

    private Point2D basePos(Fx fx) {
        int w = getWidth();
        double gap = w * 0.30;
        int streak = fx.rightSide ? retreatR : retreatL;
        double x = fx.rightSide ? w / 2.0 + gap / 2 + streak * 24 : w / 2.0 - gap / 2 - streak * 24;
        return new Point2D.Double(x, feetY());
    }

    private double reach() { return getWidth() * 0.30 / 2 - 30; }

    // ---------- 그리기 ----------

    private static Color weaponColor(WeaponType w) {
        switch (w) {
            case FOIL: return new Color(70, 130, 230);
            case EPEE: return new Color(60, 175, 105);
            default: return new Color(225, 75, 75);
        }
    }

    private static Color tacticColor(Battle.Tactic t) {
        switch (t) {
            case ATTACK: return new Color(220, 80, 80);
            case FEINT: return new Color(200, 120, 220);
            case PARRY: return new Color(70, 150, 220);
            case COUNTER: return new Color(235, 150, 50);
            case ADVANCE: return new Color(80, 180, 120);
            default: return new Color(130, 140, 165);
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        long now = System.currentTimeMillis();
        int w = getWidth(), h = getHeight();

        if (now < shakeUntil) g.translate(rnd.nextInt(7) - 3, rnd.nextInt(7) - 3);

        g.setPaint(new GradientPaint(0, 0, new Color(18, 26, 50), 0, h, new Color(46, 60, 102)));
        g.fillRect(-10, -10, w + 20, h + 20);
        drawWeatherBack(g, w, h, now);
        drawPiste(g, w, h);

        if (L != null && R != null) {
            drawFencer(g, R, now);
            drawFencer(g, L, now);
            drawTactics(g, now);
            if (now < sparkUntil) drawSpark(g, now);
        }
        drawWeatherFront(g, w, h, now);
        drawWeatherChip(g);
        drawScoreboard(g, now);

        for (FloatText f : floats) {
            double q = (now - f.born) / (double) f.life;
            float alpha = (float) Math.max(0, 1 - q * q);
            g.setFont(getFont().deriveFont(Font.BOLD, f.size));
            FontMetrics fm = g.getFontMetrics();
            int tx = (int) f.x - fm.stringWidth(f.text) / 2;
            int ty = (int) (f.y - 40 * q);
            g.setColor(new Color(0, 0, 0, (int) (170 * alpha)));
            g.drawString(f.text, tx + 2, ty + 2);
            g.setColor(new Color(f.color.getRed(), f.color.getGreen(), f.color.getBlue(), (int) (255 * alpha)));
            g.drawString(f.text, tx, ty);
        }

        drawRosters(g, now);

        if (banner != null && now < bannerUntil) {
            double q = (now - bannerStart) / (double) Math.max(1, bannerUntil - bannerStart);
            float alpha = (float) Math.min(1, Math.min(q * 6, (1 - q) * 4));
            float size = (float) (34 + 10 * Math.min(1, q * 3));
            g.setFont(getFont().deriveFont(Font.BOLD, size));
            FontMetrics fm = g.getFontMetrics();
            int tx = (w - fm.stringWidth(banner)) / 2;
            int ty = 190;
            g.setColor(new Color(0, 0, 0, (int) (170 * alpha)));
            g.drawString(banner, tx + 3, ty + 3);
            g.setColor(new Color(bannerColor.getRed(), bannerColor.getGreen(), bannerColor.getBlue(), (int) (255 * alpha)));
            g.drawString(banner, tx, ty);
        }
        g.dispose();
    }

    /** 피스트: 중앙선, 앙가르드 선, 경고선, 끝선 */
    private void drawPiste(Graphics2D g, int w, int h) {
        int px = (int) (w * 0.04), pw = (int) (w * 0.92), py = 120, ph = (int) feetY() + 38 - py;
        g.setPaint(new GradientPaint(0, py, new Color(214, 218, 228), 0, py + ph, new Color(160, 168, 188)));
        g.fillRoundRect(px, py, pw, ph, 24, 24);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(88, 94, 122));
        g.drawRoundRect(px, py, pw, ph, 24, 24);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(120, 126, 152));
        g.drawLine(w / 2, py + 6, w / 2, py + ph - 6);                         // 중앙선
        g.setColor(new Color(215, 85, 85, 170));
        double gap = w * 0.30;
        g.drawLine((int) (w / 2 - gap / 2), py + 6, (int) (w / 2 - gap / 2), py + ph - 6);   // 앙가르드 선
        g.drawLine((int) (w / 2 + gap / 2), py + 6, (int) (w / 2 + gap / 2), py + ph - 6);
        g.setColor(new Color(235, 190, 60, 170));
        g.drawLine(px + 70, py + 6, px + 70, py + ph - 6);                      // 경고선
        g.drawLine(px + pw - 70, py + 6, px + pw - 70, py + ph - 6);
        g.setColor(new Color(60, 66, 94));
        g.setStroke(new BasicStroke(4f));
        g.drawLine(px + 14, py + 6, px + 14, py + ph - 6);                      // 후방 끝선 (이탈하면 실점)
        g.drawLine(px + pw - 14, py + 6, px + pw - 14, py + ph - 6);
    }

    private void drawFencer(Graphics2D g, Fx fx, long now) {
        Point2D base = basePos(fx);
        int facing = fx.rightSide ? -1 : 1;
        double dist = reach();
        double x = base.getX() + facing * (fx.lunge * dist + fx.shift);
        double y = base.getY();
        double sc = 1.35;
        boolean idle = cur == null && queue.isEmpty();
        double bob = idle ? Math.sin(now / 280.0 + base.getX()) * 2 : 0;
        Color wc = weaponColor(fx.f.getWeapon());
        boolean flash = now < fx.flashUntil;
        WeaponType wt = fx.f.getWeapon();
        boolean tired = battle != null && battle.isExhausted(fx.f) && fx.hp <= 0;

        Graphics2D gg = (Graphics2D) g.create();
        gg.translate(x, y + bob);
        gg.setColor(new Color(0, 0, 0, 70));
        gg.fill(new Ellipse2D.Double(-34 * sc, -5 * sc, 68 * sc, 12 * sc));
        gg.scale(facing * sc, sc);
        gg.rotate(tired ? -0.12 : wt == WeaponType.EPEE ? 0.10 : wt == WeaponType.SABRE ? -0.04 : 0.0);
        double l = fx.lunge;
        BasicStroke thick = new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        gg.setStroke(thick);
        gg.setColor(new Color(50, 56, 82));
        gg.draw(new Line2D.Double(-4, -30, -12 - 4 * l, 0));
        gg.draw(new Line2D.Double(4, -30, 10 + 22 * l, 0));
        gg.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gg.setColor(new Color(235, 238, 246));
        gg.draw(wt == WeaponType.SABRE ? new Line2D.Double(-8, -54, -18, -76) : new Line2D.Double(-8, -54, -16, -64));
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
        Ellipse2D head = new Ellipse2D.Double(-11, -86, 22, 22);
        gg.setColor(flash ? new Color(255, 160, 160) : Color.WHITE);
        gg.fill(head);
        gg.setColor(new Color(110, 116, 142));
        gg.setStroke(new BasicStroke(1f));
        gg.draw(head);
        for (int i = -6; i <= 6; i += 4) gg.draw(new Line2D.Double(i, -84, i, -66));
        for (int j = -80; j <= -68; j += 4) gg.draw(new Line2D.Double(-9, j, 9, j));
        drawWeaponArm(gg, wt, l, wc, fx.f == (battle == null ? null : battle.getLeft()) ? 0 : 0);
        gg.dispose();

        // 이름 / 체력(지구력)
        g.setFont(getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics fm = g.getFontMetrics();
        String name = fx.f.getName() + " · " + wt.getDisplayName() + " Lv." + fx.f.getLevel();
        double lx = base.getX();
        g.setColor(Color.WHITE);
        g.drawString(name, (float) (lx - fm.stringWidth(name) / 2.0), (float) (base.getY() + 26));
        int bw = 130, bh = 9;
        int bx = (int) (lx - bw / 2.0), by = (int) (base.getY() + 32);
        g.setColor(new Color(10, 14, 28, 220));
        g.fillRoundRect(bx - 1, by - 1, bw + 2, bh + 2, 6, 6);
        double ratio = fx.maxHp == 0 ? 0 : Math.max(0, Math.min(1, fx.hp / (double) fx.maxHp));
        g.setColor(ratio > 0.5 ? new Color(80, 190, 110) : ratio > 0.25 ? new Color(235, 190, 60) : new Color(225, 80, 80));
        g.fillRoundRect(bx, by, (int) (bw * ratio), bh, 6, 6);
        g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        g.setColor(new Color(190, 198, 222));
        g.drawString("체력 " + Math.max(0, fx.hp) + "/" + fx.maxHp + (tired ? "  탈진" : ""), bx, by + bh + 13);

        // 우선권 표시 (플뢰레·사브르)
        if (priorityShown == fx.f) drawBadge(g, "우선권", new Color(240, 170, 40), x - 22, y - 205);
        // 후방 라인에 가까워지면 경고
        int streak = fx.rightSide ? retreatR : retreatL;
        if (streak >= 2) drawBadge(g, "후방 라인 주의 " + streak + "/4", new Color(215, 85, 55), x - 50, y - 227);
    }

    /** 종목별 검과 팔: 플뢰레=가는 검 찌르기, 에페=굵은 검+큰 벨 가드, 사브르=휘어진 날 베기 */
    private void drawWeaponArm(Graphics2D gg, WeaponType wt, double l, Color wc, int unused) {
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

    /** 두 선수가 고른 전술을 머리 위에 동시에 공개한다. */
    private void drawTactics(Graphics2D g, long now) {
        if (cur == null || cur.kind != Battle.Event.Kind.PHRASE) return;
        double q = (now - curStart) / (double) Math.max(1, curDur);
        if (q > 0.8) return;
        drawTacticPill(g, cur.leftTactic, basePos(L).getX() + L.shift, basePos(L).getY() - 180, q);
        drawTacticPill(g, cur.rightTactic, basePos(R).getX() - R.shift, basePos(R).getY() - 180, q);
    }

    private void drawTacticPill(Graphics2D g, Battle.Tactic t, double cx, double y, double q) {
        if (t == null) return;
        float alpha = (float) Math.min(1.0, Math.min(q * 8, (0.8 - q) * 6));
        g.setFont(getFont().deriveFont(Font.BOLD, 14f));
        int tw = g.getFontMetrics().stringWidth(t.getDisplayName()) + 22;
        Color c = tacticColor(t);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (235 * alpha)));
        g.fillRoundRect((int) (cx - tw / 2.0), (int) y, tw, 26, 16, 16);
        g.setColor(new Color(255, 255, 255, (int) (255 * alpha)));
        g.drawString(t.getDisplayName(), (int) (cx - tw / 2.0 + 11), (int) y + 18);
    }

    private void drawSpark(Graphics2D g, long now) {
        double q = 1 - (sparkUntil - now) / 350.0;
        double cx = getWidth() / 2.0, cy = feetY() - 78;
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(255, 245, 190, (int) (230 * (1 - q))));
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + q;
            g.draw(new Line2D.Double(cx + Math.cos(a) * 8, cy + Math.sin(a) * 8, cx + Math.cos(a) * (18 + 24 * q), cy + Math.sin(a) * (18 + 24 * q)));
        }
    }

    // ---------- 점수판 / 램프 / 팀원 ----------

    private void drawScoreboard(Graphics2D g, long now) {
        int w = getWidth();
        int bw = 400, bh = 102, bx = (w - bw) / 2, by = 8;
        boolean pulse = now < scorePulseUntil;
        g.setColor(new Color(16, 20, 40, 238));
        g.fillRoundRect(bx, by, bw, bh, 20, 20);
        g.setColor(pulse ? new Color(255, 225, 90) : new Color(200, 170, 60));
        g.setStroke(new BasicStroke(pulse ? 4f : 2f));
        g.drawRoundRect(bx, by, bw, bh, 20, 20);

        // 득점 램프 (왼쪽 빨강, 오른쪽 초록, 무효 타격은 가운데 흰색)
        drawLamp(g, bx - 30, by + 26, new Color(235, 50, 50), now < redUntil);        // 왼쪽 선수 득점
        drawLamp(g, bx + bw + 30, by + 26, new Color(60, 210, 90), now < greenUntil); // 오른쪽 선수 득점
        drawLamp(g, bx - 30, by + 72, new Color(250, 250, 250), now < whiteUntil);    // 무효 타격(백색등)
        drawLamp(g, bx + bw + 30, by + 72, new Color(250, 250, 250), now < whiteUntil);

        g.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g.setColor(new Color(110, 170, 255));
        g.drawString("우리 팀", bx + 18, by + 24);
        g.setColor(new Color(255, 130, 130));
        String foe = "상대";
        g.drawString(foe, bx + bw - 18 - g.getFontMetrics().stringWidth(foe), by + 24);

        g.setFont(getFont().deriveFont(Font.BOLD, pulse ? 42f : 38f));
        String score = pScore + "  :  " + eScore;
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(score, bx + (bw - fm.stringWidth(score)) / 2, by + 58);

        g.setFont(getFont().deriveFont(Font.BOLD, 13f));
        int sec = Math.max(0, clockShown);
        String clock = String.format("%d:%02d", sec / 60, sec % 60);
        boolean team = battle != null && !battle.isIndividual();
        String info = suddenShown ? "서든데스" : team ? "제 " + boutNo + "/" + boutTotal + "경기" : periodShown + "피리어드";
        info += "   " + clock;
        g.setColor(new Color(255, 225, 120));
        g.drawString(info, bx + (bw - g.getFontMetrics().stringWidth(info)) / 2, by + 80);

        g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g.setColor(new Color(170, 178, 205));
        String goal = suddenShown ? "먼저 득점하면 승리" : team ? "이번 경기 " + boutP + " : " + boutR + "   (경기 목표 " + targetShown + "점)"
                : "목표 " + targetShown + "점";
        g.drawString(goal, bx + (bw - g.getFontMetrics().stringWidth(goal)) / 2, by + 97);
    }

    private void drawLamp(Graphics2D g, int cx, int cy, Color c, boolean lit) {
        int r = 15;
        if (lit) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
            g.fill(new Ellipse2D.Double(cx - r - 8, cy - r - 8, (r + 8) * 2, (r + 8) * 2));
        }
        g.setColor(lit ? c : new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(new Color(20, 24, 44));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    /** 아래쪽에 양 팀의 선수 목록과 체력, 소화한 경기 수를 표시한다. */
    private void drawRosters(Graphics2D g, long now) {
        if (battle == null) return;
        int w = getWidth(), h = getHeight();
        List<Fencer> ps = battle.getPlayers();
        List<Fencer> es = battle.getEnemies();
        int n = Math.max(1, Math.max(ps.size(), es.size()));
        int cw = Math.max(86, Math.min(138, (w / 2 - 26) / n - 6)), ch = 46, y = h - ch - 8;   // 가운데서 겹치지 않게 폭을 줄인다
        for (int i = 0; i < ps.size(); i++) drawChip(g, ps.get(i), 10 + i * (cw + 6), y, cw, ch, L != null && L.f == ps.get(i));
        for (int i = 0; i < es.size(); i++) drawChip(g, es.get(i), w - 10 - (es.size() - i) * (cw + 6) + 6, y, cw, ch, R != null && R.f == es.get(i));
    }

    private void drawChip(Graphics2D g, Fencer f, int x, int y, int cw, int ch, boolean active) {
        g.setColor(new Color(16, 20, 40, 225));
        g.fillRoundRect(x, y, cw, ch, 12, 12);
        g.setColor(active ? new Color(255, 215, 80) : new Color(80, 90, 130));
        g.setStroke(new BasicStroke(active ? 3f : 1.5f));
        g.drawRoundRect(x, y, cw, ch, 12, 12);
        g.setColor(weaponColor(f.getWeapon()));
        g.fill(new Ellipse2D.Double(x + 8, y + 8, 10, 10));
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g.setColor(Color.WHITE);
        g.drawString(f.getName(), x + 24, y + 18);
        g.setFont(getFont().deriveFont(Font.PLAIN, 10.5f));
        g.setColor(new Color(180, 188, 215));
        String s = cw < 120 ? boutsDone.getOrDefault(f, 0) + "경기" : boutsDone.getOrDefault(f, 0) + "경기 완료";
        g.drawString(s, x + cw - 8 - g.getFontMetrics().stringWidth(s), y + 17);
        double ratio = f.getMaxHp() == 0 ? 0 : Math.max(0, Math.min(1, f.getHp() / (double) f.getMaxHp()));
        g.setColor(new Color(10, 14, 28));
        g.fillRoundRect(x + 8, y + 28, cw - 16, 8, 6, 6);
        g.setColor(ratio > 0.5 ? new Color(80, 190, 110) : ratio > 0.25 ? new Color(235, 190, 60) : new Color(225, 80, 80));
        g.fillRoundRect(x + 8, y + 28, (int) ((cw - 16) * ratio), 8, 6, 6);
    }

    private void drawBadge(Graphics2D g, String text, Color color, double leftX, double topY) {
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        int tw = g.getFontMetrics().stringWidth(text) + 14;
        g.setColor(color);
        g.fillRoundRect((int) leftX, (int) topY, tw, 18, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(text, (int) leftX + 7, (int) topY + 13);
    }

    // ---------- 날씨 연출 ----------

    private void drawWeatherBack(Graphics2D g, int w, int h, long now) {
        switch (weather) {
            case SUNNY:
                g.setPaint(new RadialGradientPaint(new Point2D.Float(70, 30), 180, new float[]{0f, 1f},
                        new Color[]{new Color(255, 220, 120, 130), new Color(255, 220, 120, 0)}));
                g.fillRect(0, 0, w, h);
                break;
            case CLOUDY:
                g.setColor(new Color(160, 170, 195, 70));
                for (int i = 0; i < 4; i++) {
                    double x = ((now / 50.0) + i * w / 3.0) % (w + 260) - 130;
                    int y = 8 + (i % 2) * 28;
                    g.fill(new Ellipse2D.Double(x, y + 10, 190, 44));
                    g.fill(new Ellipse2D.Double(x + 40, y, 130, 52));
                }
                break;
            case RAINY:
                g.setColor(new Color(8, 16, 46, 100));
                g.fillRect(0, 0, w, h);
                break;
            default:
                g.setPaint(new GradientPaint(0, 0, new Color(255, 120, 40, 120), 0, h, new Color(255, 190, 80, 45)));
                g.fillRect(0, 0, w, h);
                break;
        }
    }

    private void drawWeatherFront(Graphics2D g, int w, int h, long now) {
        if (weather == Weather.RAINY) {
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(new Color(190, 215, 255, 125));
            for (int i = 0; i < 90; i++) {
                double x = (i * 53 + now * 0.25) % (w + 40);
                double y = (i * 37 + now * 0.9) % (h + 20);
                g.draw(new Line2D.Double(x, y, x - 4, y + 12));
            }
        } else if (weather == Weather.HOT) {
            g.setStroke(new BasicStroke(2f));
            for (int k = 0; k < 4; k++) {
                g.setColor(new Color(255, 225, 150, 34));
                Path2D path = new Path2D.Double();
                double base = 130 + k * 62;
                path.moveTo(0, base);
                for (int x = 0; x <= w; x += 16) path.lineTo(x, base + Math.sin(x / 28.0 + now / 240.0 + k) * 5);
                g.draw(path);
            }
        } else if (weather == Weather.SUNNY) {
            g.setColor(new Color(255, 240, 180, 24));
            g.fillPolygon(new int[]{30, 110, (int) (w * 0.62), (int) (w * 0.36)}, new int[]{0, 0, h, h}, 4);
            g.fillPolygon(new int[]{130, 170, (int) (w * 0.80), (int) (w * 0.66)}, new int[]{0, 0, h, h}, 4);
        }
    }

    private void drawWeatherChip(Graphics2D g) {
        Color c;
        switch (weather) {
            case SUNNY: c = new Color(225, 160, 40); break;
            case CLOUDY: c = new Color(110, 125, 160); break;
            case RAINY: c = new Color(60, 110, 200); break;
            default: c = new Color(215, 85, 55); break;
        }
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        String text = weather.getDisplayName() + "  " + weather.getChipText();
        int tw = g.getFontMetrics().stringWidth(text) + 16;
        g.setColor(c);
        g.fillRoundRect(12, 12, tw, 22, 12, 12);
        g.setColor(Color.WHITE);
        g.drawString(text, 20, 28);
    }
}
