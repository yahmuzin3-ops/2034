import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 효과음. 외부 파일 없이 코드로 소리를 합성해서 재생한다.
 * 소리 장치가 없거나 오류가 나도 게임은 그대로 진행된다.
 */
public final class Sfx {
    private static final float RATE = 22050f;
    private static final Random RND = new Random();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sfx");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean enabled = true;

    private Sfx() {
    }

    public static void setEnabled(boolean on) { enabled = on; }

    public static boolean isEnabled() { return enabled; }

    // ---------- 효과음 목록 ----------

    /** 검을 휘두르며 달려드는 소리 (사브르는 더 빠르고 높게) */
    public static void swish(boolean fast) {
        play(synth(fast ? 110 : 150, fast ? 500 : 350, fast ? 1700 : 1000, 0.22, 0.65, false));
    }

    public static void hit(boolean crit) {
        byte[] thud = synth(70, 220, 80, 0.5, 0.5, false);
        play(crit ? concat(thud, synth(140, 900, 1400, 0.3, 0.0, false)) : thud);
    }

    public static void score() {
        play(concat(synth(80, 660, 660, 0.25, 0, false), synth(140, 990, 990, 0.25, 0, false)));
    }

    public static void dodge() { play(synth(120, 950, 500, 0.2, 0.3, false)); }

    public static void guard() { play(concat(synth(80, 620, 620, 0.25, 0, true), synth(60, 930, 930, 0.2, 0, true))); }

    public static void skill() { play(concat(synth(100, 500, 900, 0.28, 0, false), synth(150, 900, 1600, 0.28, 0, false))); }

    public static void stun() { play(synth(220, 1300, 300, 0.28, 0, false)); }

    public static void buzz() { play(synth(220, 150, 150, 0.3, 0, true)); }

    public static void thud() { play(synth(110, 130, 60, 0.45, 0.2, false)); }

    public static void round() { play(synth(90, 740, 740, 0.18, 0, false)); }

    public static void heat() { play(synth(260, 100, 170, 0.14, 0.85, false)); }

    public static void win() {
        play(concat(synth(110, 523, 523, 0.28, 0, false), synth(110, 659, 659, 0.28, 0, false),
                synth(110, 784, 784, 0.28, 0, false), synth(260, 1046, 1046, 0.3, 0, false)));
    }

    /** 시상식 팡파르 */
    public static void fanfare() {
        play(concat(synth(130, 523, 523, 0.28, 0, false), synth(130, 659, 659, 0.28, 0, false),
                synth(130, 784, 784, 0.28, 0, false), synth(240, 1046, 1046, 0.3, 0, false),
                synth(120, 784, 784, 0.26, 0, false), synth(420, 1046, 1318, 0.32, 0, false)));
    }

    /** 알림(업적/의뢰 달성) 소리 */
    public static void chime() {
        play(concat(synth(90, 880, 880, 0.22, 0, false), synth(160, 1320, 1320, 0.22, 0, false)));
    }

    public static void lose() {
        play(concat(synth(170, 392, 392, 0.28, 0, false), synth(170, 330, 330, 0.28, 0, false),
                synth(300, 262, 262, 0.3, 0, false)));
    }

    // ---------- 소리 만들기 ----------

    /** ms 길이의 소리. f0 -> f1 로 음높이가 변하고, noise(0~1)만큼 잡음을 섞는다. */
    private static byte[] synth(int ms, double f0, double f1, double volume, double noise, boolean square) {
        int n = (int) (RATE * ms / 1000.0);
        byte[] out = new byte[n * 2];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double freq = f0 + (f1 - f0) * t;
            phase += 2 * Math.PI * freq / RATE;
            double wave = square ? (Math.sin(phase) >= 0 ? 0.6 : -0.6) : Math.sin(phase);
            double sample = wave * (1 - noise) + (RND.nextDouble() * 2 - 1) * noise;
            double env = Math.min(1.0, i / (RATE * 0.004)) * Math.pow(1 - t, 1.6);   // 짧게 올라가고 서서히 사라짐
            short v = (short) (sample * env * volume * Short.MAX_VALUE);
            out[2 * i] = (byte) (v & 0xff);
            out[2 * i + 1] = (byte) ((v >> 8) & 0xff);
        }
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) out.write(p, 0, p.length);
        return out.toByteArray();
    }

    private static void play(byte[] pcm) {
        if (!enabled) return;
        POOL.execute(() -> {
            try {
                AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
                try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                    line.open(fmt);
                    line.start();
                    line.write(pcm, 0, pcm.length);
                    line.drain();
                }
            } catch (Throwable ignored) {
                // 소리 장치가 없으면 조용히 넘어간다
            }
        });
    }
}
