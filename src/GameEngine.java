import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 백그라운드 스레드 관리.
 *  - 선수마다 스레드 1개: 상태를 보고 휴식 / 먹이 탐색 / 놀이 / 개인 연습을 스스로 결정
 *  - 시계 스레드 1개: 일정 시간마다 하루 경과 + 날씨 변화 + 확률 이벤트
 * 공유 데이터는 Fencer / GameState / Inventory 의 synchronized 로 보호된다.
 */
public class GameEngine {
    public static final long DAY_MILLIS = 20_000;
    public static final long WORKER_MILLIS = 3_000;

    private final GameState state;
    private final Map<Fencer, Thread> workers = new ConcurrentHashMap<>();
    private final Random rnd = new Random();
    private volatile boolean running = false;
    private Thread clock;

    public GameEngine(GameState state) {
        this.state = state;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        syncWorkers();
        clock = new Thread(this::clockLoop, "game-clock");
        clock.setDaemon(true);
        clock.start();
    }

    public synchronized void stop() {
        running = false;
        if (clock != null) clock.interrupt();
        for (Thread t : workers.values()) t.interrupt();
        workers.clear();
    }

    /** 새로 영입한 선수에게도 스레드를 붙인다. */
    public synchronized void syncWorkers() {
        if (!running) return;
        for (Fencer f : state.getRoster()) {
            if (!workers.containsKey(f)) {
                Thread t = new Thread(() -> workerLoop(f), "fencer-" + f.getName());
                t.setDaemon(true);
                workers.put(f, t);
                t.start();
            }
        }
    }

    public void advanceDay() {
        state.nextDay();
        GameLog.post("[시간] " + state.getDay() + "일차 - 날씨: " + state.getWeather().getDisplayName());
        for (String m : EventManager.rollDaily(state)) GameLog.post(m);
    }

    private void clockLoop() {
        while (running) {
            try {
                Thread.sleep(DAY_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
            if (running) advanceDay();
        }
    }

    private void workerLoop(Fencer f) {
        while (running) {
            try {
                Thread.sleep(WORKER_MILLIS + rnd.nextInt(1000));
            } catch (InterruptedException e) {
                return;
            }
            if (!running) return;
            if (f.isBusy()) continue; // 시합 중
            act(f);
        }
    }

    /** 조건문 기반 자율 행동. 성향(Personality)에 따라 기준값이 다르다. */
    private void act(Fencer f) {
        Personality p = f.getPersonality();
        String n = f.getName();

        if (f.getHp() < f.getMaxHp() * 0.3 || f.isInjured() || f.getFatigue() >= p.getRestThreshold()) {
            f.rest(state.getRestBonus());
            if (f.isInjured() && rnd.nextDouble() < 0.15) {
                f.cure();
                GameLog.post("[자율] " + n + "의 부상이 자연스럽게 나았어요.");
            } else {
                GameLog.post("[자율] " + n + "이(가) 스스로 휴식을 취해요.");
            }
        } else if (f.getHunger() >= 60) {
            if (rnd.nextDouble() < 0.7) {
                f.eat(15, 1);
                GameLog.post("[자율] " + n + "이(가) 먹이를 찾아 먹었어요.");
            } else {
                GameLog.post("[자율] " + n + "이(가) 먹이를 찾지 못했어요...");
            }
        } else if (f.getMood() < 30) {
            f.play();
            GameLog.post("[자율] " + n + "이(가) 기분 전환으로 놀고 있어요.");
        } else if (f.getMood() >= p.getPracticeMood() && f.getFatigue() < 40 && rnd.nextDouble() < 0.5) {
            f.selfPractice();
            GameLog.post("[자율] " + n + "이(가) 개인 연습을 해요. (경험치 +3)");
        }
    }
}
