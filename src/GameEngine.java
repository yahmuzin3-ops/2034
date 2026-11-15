import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 백그라운드 스레드 관리.
 *  - 선수마다 스레드 1개: 훈련 완료, 휴식 회복, 부상 중 피로도를 갱신
 *  - 시계 스레드 1개: 일정 시간마다 하루 경과 + 날씨 변화 + 확률 이벤트
 * 공유 데이터는 Fencer / GameState / Inventory 의 synchronized 로 보호된다.
 */
public class GameEngine {
    public static final long DAY_MILLIS = 20_000;
    public static final long WORKER_MILLIS = 3_000;
    public static final long AUTO_COOLDOWN_MILLIS = 8_000;   // 자율 행동 사이의 최소 간격

    private final GameState state;
    private final Map<Fencer, Thread> workers = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private volatile boolean paused = false;    // 경기 중에는 시간이 멈춘다
    private volatile boolean autoDay = true;    // false 면 "하루 넘기기" 버튼으로만 시간이 흐른다
    private Thread clock;
    private final Random rnd = new Random();

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

    /** 방출된 선수의 작업 스레드를 종료한다. */
    public void removeWorker(Fencer f) {
        if (f == null) return;
        Thread worker = workers.remove(f);
        if (worker != null) worker.interrupt();
    }

    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isPaused() { return paused; }
    public void setAutoDay(boolean autoDay) { this.autoDay = autoDay; }
    public boolean isAutoDay() { return autoDay; }

    public void advanceDay() {
        state.nextDay();
        Weather w = state.getWeather();
        GameLog.post("[시간] " + state.getDay() + "일차 - 날씨: " + w.getDisplayName() + " (" + w.getDayNote() + ")");
        GameLog.post("[날씨] 내일 예보: " + state.getForecast().getDisplayName());
        for (String m : EventManager.rollDaily(state)) GameLog.post(m);
    }

    private void clockLoop() {
        while (running) {
            try {
                Thread.sleep(DAY_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
            if (running && autoDay && !paused) advanceDay();
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
            if (!state.getRoster().contains(f)) {
                workers.remove(f, Thread.currentThread());
                return;
            }
            Fencer.TrainingSession session = f.finishTrainingIfDue(System.currentTimeMillis());
            if (session != null) {
                GameLog.post("[훈련 완료] " + state.completeTraining(f, session));
            }
            if (f.processWorkerTick(state.getRestBonus())) {
                announce(f, f.getName() + "이(가) 충분히 쉬고 다시 몸을 풀었어요.");
            }
            autonomy(f);
        }
    }

    /**
     * 자율 행동: 훈련 / 시합 / 휴식 중이 아닌 선수는 스스로 상태를 보고 행동한다.
     * (휴식 -> 식사 -> 기분 전환 -> 섀도 펜싱 순서로 조건을 검사한다. 규칙은 Fencer.decideAutoAction)
     */
    private void autonomy(Fencer f) {
        long now = System.currentTimeMillis();
        if (!f.isAutoEnabled() || now < f.getNextAutoAt()) return;
        Fencer.AutoAction action = f.decideAutoAction(rnd);
        long cooldown = AUTO_COOLDOWN_MILLIS;
        switch (action) {
            case REST:
                announce(f, f.getName() + "이(가) 스트레칭을 하며 휴식을 시작했어요.");
                break;
            case MEAL: {
                String meal = autoMeal(f);
                if (meal == null) {
                    announce(f, f.getName() + "이(가) 배가 고프지만 먹을 음식이 없어요. (상점에서 식단을 사주세요)");
                    cooldown = 30_000;
                } else {
                    announce(f, meal);
                }
                break;
            }
            case REFRESH:
                f.play();
                announce(f, f.getName() + "이(가) 경기 영상을 보며 기분 전환을 했어요.");
                break;
            case SHADOW:
                f.selfPractice();
                announce(f, f.getName() + "이(가) 거울 앞에서 섀도 펜싱을 했어요.");
                break;
            default:
                return;
        }
        f.setNextAutoAt(now + cooldown);
    }

    /** 인벤토리에서 가장 싼 음식을 하나 먹는다. 음식이 없으면 null (락 순서: GameState -> Inventory / Fencer) */
    private String autoMeal(Fencer f) {
        synchronized (state) {
            Food pick = null;
            for (Item it : state.getInventory().snapshot().keySet()) {
                if (it instanceof Food && (pick == null || it.getPrice() < pick.getPrice())) pick = (Food) it;
            }
            if (pick == null || !state.getInventory().remove(pick, 1)) return null;
            return pick.use(f);
        }
    }

    private void announce(Fencer f, String message) {
        f.setLastAuto(message);
        GameLog.post("[자율] " + message);
    }
}
