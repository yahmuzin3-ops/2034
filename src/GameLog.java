import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** 게임 로그. 어느 스레드에서 post 해도 화면 갱신은 EDT 에서 실행된다. */
public final class GameLog {
    private static final List<String> LINES = new ArrayList<>();
    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();

    private GameLog() {
    }

    public static synchronized void post(String msg) {
        LINES.add(msg);
        if (LINES.size() > 300) LINES.remove(0);
        for (Consumer<String> l : LISTENERS) {
            SwingUtilities.invokeLater(() -> l.accept(msg));
        }
    }

    public static synchronized List<String> recent() {
        return new ArrayList<>(LINES);
    }

    public static void addListener(Consumer<String> listener) {
        LISTENERS.add(listener);
    }
}
