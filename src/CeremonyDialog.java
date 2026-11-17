import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 시상식. 메달을 딴 대회가 끝나면 포디움 위에서 메달을 받는 장면이 나온다.
 * 금메달은 큰 세리머니(색종이, 점프), 은/동메달은 작은 세리머니다.
 */
public class CeremonyDialog extends JDialog {

    public CeremonyDialog(Frame owner, Tournament t) {
        super(owner, "시상식", true);
        String[] names = new String[3];
        names[0] = t.getChampion() == null ? "-" : t.getChampion().name;
        names[1] = t.getRunnerUp() == null ? "-" : t.getRunnerUp().name;
        StringBuilder bronze = new StringBuilder();
        for (Tournament.Entry e : t.getBronzeEntries()) {
            if (bronze.length() > 0) bronze.append(" / ");
            bronze.append(e.name);
        }
        names[2] = bronze.length() == 0 ? "-" : bronze.toString();
        CeremonyPanel panel = new CeremonyPanel(t.getTitle(), t.getMedal(), names, t.getMyFencers(), t.getWeapon(),
                t.isTeam(), t.getRegion() == Region.OLYMPIC);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        JButton ok = UI.button("확인");
        ok.addActionListener(e -> {
            panel.stop();
            dispose();
        });
        JPanel south = new JPanel();
        south.setBackground(new Color(18, 26, 50));
        south.add(ok);
        add(south, BorderLayout.SOUTH);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) { panel.stop(); }
        });
        pack();
        setLocationRelativeTo(owner);
        panel.start();
    }

    /** 시상대 장면 (테스트하기 쉽게 따로 분리) */
    public static class CeremonyPanel extends JPanel {
        private static class Bit {
            double x, y, vx, vy, rot, vrot;
            Color c;
            int w, h;
        }

        private final String title;
        private final int medal;              // 1 금, 2 은, 3 동
        private final String[] names;         // 금, 은, 동 순서
        private final List<Fencer> mine;
        private final WeaponType weapon;
        private final boolean team, olympic;
        private final List<Bit> confetti = new ArrayList<>();
        private final Random rnd = new Random();
        private final javax.swing.Timer timer = new javax.swing.Timer(16, e -> tick());
        private long t0;

        public CeremonyPanel(String title, int medal, String[] names, List<Fencer> mine, WeaponType weapon, boolean team, boolean olympic) {
            this.title = title;
            this.medal = medal;
            this.names = names;
            this.mine = mine;
            this.weapon = weapon;
            this.team = team;
            this.olympic = olympic;
            setPreferredSize(new Dimension(720, 440));
            setBackground(new Color(18, 26, 50));
        }

        public void start() {
            t0 = System.currentTimeMillis();
            Sfx.fanfare();
            timer.start();
        }

        public void stop() { timer.stop(); }

        public void setStartTime(long t) { t0 = t; }

        private void tick() {
            long el = System.currentTimeMillis() - t0;
            if (el > 700 && el < 7000 && rnd.nextInt(100) < (medal == 1 ? 90 : 35)) {   // 금메달은 색종이가 많이 떨어진다
                int n = medal == 1 ? 4 : 1;
                for (int i = 0; i < n; i++) confetti.add(newBit());
            }
            for (Bit b : confetti) {
                b.x += b.vx * 0.016;
                b.y += b.vy * 0.016;
                b.rot += b.vrot * 0.016;
                b.vx += Math.sin(el / 280.0 + b.x) * 0.6;
            }
            confetti.removeIf(b -> b.y > getHeight() + 20);
            repaint();
        }

        private Bit newBit() {
            Color[] pal = {new Color(255, 90, 90), new Color(255, 205, 70), new Color(90, 190, 255), new Color(120, 220, 130), new Color(220, 120, 255)};
            Bit b = new Bit();
            b.x = rnd.nextDouble() * Math.max(400, getWidth());
            b.y = -10;
            b.vx = (rnd.nextDouble() - 0.5) * 70;
            b.vy = 70 + rnd.nextDouble() * 110;
            b.rot = rnd.nextDouble() * 6;
            b.vrot = (rnd.nextDouble() - 0.5) * 9;
            b.c = pal[rnd.nextInt(pal.length)];
            b.w = 6 + rnd.nextInt(6);
            b.h = 4 + rnd.nextInt(5);
            return b;
        }

        private static double easeOutBack(double t) {
            double c1 = 1.70158, c3 = c1 + 1;
            return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            long el = System.currentTimeMillis() - t0;
            int w = getWidth(), h = getHeight();

            // 무대 배경과 스포트라이트
            g.setPaint(new GradientPaint(0, 0, new Color(18, 26, 60), 0, h, new Color(60, 48, 100)));
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(new Point(w / 2, 90), 330, new float[]{0f, 1f},
                    new Color[]{new Color(255, 230, 150, medal == 1 ? 150 : 90), new Color(255, 230, 150, 0)}));
            g.fillRect(0, 0, w, h);
            drawRays(g, w, el);

            // 시상대: 은(왼쪽) 금(가운데) 동(오른쪽)
            int baseY = h - 40, bw = 170;
            int[] hs = {120, 78, 52};                       // 금, 은, 동 높이
            int[] xs = {w / 2 - bw / 2, w / 2 - bw / 2 - bw - 6, w / 2 + bw / 2 + 6};
            Color[] cs = {new Color(235, 190, 60), new Color(190, 196, 210), new Color(195, 130, 70)};
            String[] rank = {"1", "2", "3"};
            for (int i = 0; i < 3; i++) {
                g.setPaint(new GradientPaint(0, baseY - hs[i], cs[i].brighter(), 0, baseY, cs[i].darker()));
                g.fillRoundRect(xs[i], baseY - hs[i], bw, hs[i], 14, 14);
                g.setColor(new Color(40, 34, 60));
                g.setFont(getFont().deriveFont(Font.BOLD, 44f));
                g.drawString(rank[i], xs[i] + bw / 2 - 12, baseY - hs[i] / 2 + 16);
            }
            // 이름
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            for (int i = 0; i < 3; i++) {
                String nm = names[i];
                int tw = g.getFontMetrics().stringWidth(nm);
                g.setColor(i == medal - 1 ? new Color(255, 236, 150) : new Color(210, 215, 235));
                g.drawString(nm, xs[i] + bw / 2 - tw / 2, baseY + 18);
            }

            // 선수들: 내 선수는 자기 단상 위에서 세리머니, 나머지는 서 있다
            for (int i = 0; i < 3; i++) {
                double cx = xs[i] + bw / 2.0, fy = baseY - hs[i];
                boolean isMe = (i == medal - 1);
                if (isMe) {
                    int n = team ? Math.min(3, mine.size()) : 1;
                    double sc = team ? 0.95 : 1.4;
                    for (int k = 0; k < n; k++) {
                        double fx = cx + (k - (n - 1) / 2.0) * 52;
                        double hop = Math.abs(Math.sin((el + k * 120) / (medal == 1 ? 150.0 : 260.0))) * (medal == 1 ? 18 : 6);
                        FencerArt.draw(g, weapon, fx, fy - hop, sc, 1, 0, false, FencerArt.Pose.CELEBRATE, el + k * 90);
                    }
                    drawMedal(g, cx, fy, el);
                } else if (!"-".equals(names[i])) {
                    FencerArt.draw(g, weapon, cx, fy, 1.0, 1, 0, false, FencerArt.Pose.NORMAL, el);
                }
            }

            // 색종이
            for (Bit b : confetti) {
                Graphics2D pg = (Graphics2D) g.create();
                pg.translate(b.x, b.y);
                pg.rotate(b.rot);
                pg.setColor(b.c);
                pg.fill(new Rectangle2D.Double(-b.w / 2.0, -b.h / 2.0, b.w, b.h));
                pg.dispose();
            }

            // 제목 (튀어나오듯 커진다)
            double q = Math.min(1.0, el / 900.0);
            double scale = 0.2 + 0.8 * easeOutBack(q);
            String head = medal == 1 ? (olympic ? "금메달!" : "우승!") : medal == 2 ? "은메달!" : "동메달!";
            Graphics2D tg = (Graphics2D) g.create();
            tg.translate(w / 2.0, 62);
            tg.scale(scale, scale);
            tg.setFont(getFont().deriveFont(Font.BOLD, 50f));
            FontMetrics fm = tg.getFontMetrics();
            tg.setColor(new Color(0, 0, 0, 140));
            tg.drawString(head, -fm.stringWidth(head) / 2 + 3, 3);
            tg.setColor(medal == 1 ? new Color(255, 215, 80) : medal == 2 ? new Color(225, 230, 240) : new Color(230, 160, 100));
            tg.drawString(head, -fm.stringWidth(head) / 2, 0);
            tg.dispose();
            g.setFont(getFont().deriveFont(Font.BOLD, 16f));
            g.setColor(new Color(235, 238, 255));
            String sub = title;
            g.drawString(sub, (w - g.getFontMetrics().stringWidth(sub)) / 2, 92);
            g.dispose();
        }

        /** 메달이 위에서 떨어져 선수 목에 걸린다. */
        private void drawMedal(Graphics2D g, double cx, double footY, long el) {
            double q = Math.max(0, Math.min(1, (el - 900) / 700.0));
            if (el < 900) return;
            double targetY = footY - 66 * (team ? 0.95 : 1.4);
            double y = -30 + (targetY + 30) * easeOutBack(q);
            Color c = medal == 1 ? new Color(245, 200, 50) : medal == 2 ? new Color(205, 210, 225) : new Color(200, 130, 70);
            g.setStroke(new BasicStroke(3f));
            g.setColor(new Color(200, 50, 60));
            g.draw(new Line2D.Double(cx - 12, y - 28, cx - 2, y));      // 리본
            g.draw(new Line2D.Double(cx + 12, y - 28, cx + 2, y));
            g.setColor(c);
            g.fill(new Ellipse2D.Double(cx - 12, y - 4, 24, 24));
            g.setColor(c.darker());
            g.draw(new Ellipse2D.Double(cx - 12, y - 4, 24, 24));
            g.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g.drawString(String.valueOf(medal), (float) (cx - 4), (float) (y + 14));
            if (q >= 1) {   // 반짝임
                double s = 6 + Math.sin(el / 120.0) * 3;
                g.setColor(new Color(255, 255, 255, 200));
                g.draw(new Line2D.Double(cx + 16 - s, y, cx + 16 + s, y));
                g.draw(new Line2D.Double(cx + 16, y - s, cx + 16, y + s));
            }
        }

        private void drawRays(Graphics2D g, int w, long el) {
            g.setColor(new Color(255, 240, 180, 26));
            for (int i = 0; i < 9; i++) {
                double a = Math.PI * (0.15 + 0.7 * i / 8.0) + Math.sin(el / 900.0 + i) * 0.03;
                Path2D ray = new Path2D.Double();
                ray.moveTo(w / 2.0, 70);
                ray.lineTo(w / 2.0 + Math.cos(a) * 900 - 30, 70 + Math.sin(a) * 900);
                ray.lineTo(w / 2.0 + Math.cos(a) * 900 + 30, 70 + Math.sin(a) * 900);
                ray.closePath();
                g.fill(ray);
            }
        }
    }
}
