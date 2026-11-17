import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 달성 알림 팝업. 의뢰 완료, 업적 달성, 메달 획득이 있으면 화면 위에서 내려와서 잠깐 보였다가 올라간다.
 * (글라스 패널로 쓰며 마우스 클릭은 아래 화면으로 그대로 전달된다.)
 */
public class ToastPane extends JComponent {
    private static class Toast {
        String title, msg;
        Color accent;
        long start;
    }

    private static final long LIFE = 3200;
    private final Deque<Toast> queue = new ArrayDeque<>();
    private Toast cur;
    private final javax.swing.Timer timer = new javax.swing.Timer(16, e -> tick());

    public ToastPane() {
        setOpaque(false);
        setVisible(false);
    }

    /** 클릭이 막히지 않도록 */
    @Override
    public boolean contains(int x, int y) { return false; }

    public void show(String title, String msg, Color accent) {
        Toast t = new Toast();
        t.title = title;
        t.msg = msg;
        t.accent = accent;
        queue.add(t);
        if (cur == null) next();
    }

    private void next() {
        cur = queue.poll();
        if (cur == null) {
            timer.stop();
            setVisible(false);
            return;
        }
        cur.start = System.currentTimeMillis();
        Sfx.chime();
        setVisible(true);
        timer.start();
    }

    private void tick() {
        if (cur != null && System.currentTimeMillis() - cur.start > LIFE) next();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (cur == null) return;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        long el = System.currentTimeMillis() - cur.start;
        double in = Math.min(1, el / 380.0), out = Math.min(1, Math.max(0, (el - (LIFE - 380)) / 380.0));
        double slide = easeOutBack(in) * (1 - out * out);
        int bw = 380, bh = 66;
        int x = (getWidth() - bw) / 2;
        int y = (int) (-bh + (bh + 78) * slide);

        g.setColor(new Color(16, 20, 40, 240));
        g.fillRoundRect(x, y, bw, bh, 20, 20);
        g.setColor(cur.accent);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(x, y, bw, bh, 20, 20);

        // 별이 튀어나오며 도는 모양
        double sx = x + 36, sy = y + bh / 2.0;
        double pop = 0.6 + 0.4 * easeOutBack(Math.min(1, el / 500.0));
        g.setColor(cur.accent);
        g.fill(star(sx, sy, 20 * pop, 8 * pop, el / 600.0));

        g.setFont(getFont().deriveFont(Font.BOLD, 17f));
        g.setColor(Color.WHITE);
        g.drawString(cur.title, x + 72, y + 28);
        g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
        g.setColor(new Color(200, 208, 235));
        String m = cur.msg;
        FontMetrics fm = g.getFontMetrics();
        while (fm.stringWidth(m) > bw - 90 && m.length() > 4) m = m.substring(0, m.length() - 2);
        g.drawString(m.equals(cur.msg) ? m : m + "…", x + 72, y + 50);
        g.dispose();
    }

    private static double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    private static Path2D star(double cx, double cy, double outer, double inner, double rot) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double a = rot + i * Math.PI / 5 - Math.PI / 2;
            double px = cx + Math.cos(a) * r, py = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(px, py);
            else p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }
}
