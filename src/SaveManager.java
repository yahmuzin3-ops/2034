import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 저장/불러오기. GameState 객체 하나를 파일로 직렬화한다.
 *  - savegame.dat : 직접 저장한 파일
 *  - autosave.dat : 자동 저장 파일 (몇 분마다, 대회가 끝날 때마다)
 * 저장은 임시 파일에 먼저 쓴 뒤 바꿔치기해서, 저장 도중 꺼져도 기존 파일이 깨지지 않는다.
 */
public final class SaveManager {
    public static final File FILE = new File("savegame.dat");
    public static final File AUTO_FILE = new File("autosave.dat");

    private SaveManager() {
    }

    public static boolean exists() {
        return FILE.exists() || AUTO_FILE.exists();
    }

    public static void save(GameState state) throws IOException {
        write(state, FILE);
    }

    public static void autosave(GameState state) throws IOException {
        write(state, AUTO_FILE);
    }

    private static void write(GameState state, File target) throws IOException {
        File tmp = new File(target.getPath() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tmp))) {
            out.writeObject(state);
        }
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /** 직접 저장한 파일을 불러온다. */
    public static GameState load() throws IOException, ClassNotFoundException {
        return load(FILE);
    }

    public static GameState load(File f) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (GameState) in.readObject();
        }
    }

    /** 더 최근에 저장된 파일 (둘 다 없으면 null) */
    public static File newest() {
        if (FILE.exists() && AUTO_FILE.exists()) return AUTO_FILE.lastModified() > FILE.lastModified() ? AUTO_FILE : FILE;
        if (FILE.exists()) return FILE;
        return AUTO_FILE.exists() ? AUTO_FILE : null;
    }

    /** 저장 파일을 한눈에 알아볼 수 있는 설명 (예: 자동 저장 - 시즌 2 · 3월 4일, 선수 3명, 2026-11-19 14:05) */
    public static String describe(File f) {
        String kind = f.getName().startsWith("autosave") ? "자동 저장" : "직접 저장";
        String when = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(f.lastModified()));
        try {
            GameState s = load(f);
            return kind + " - " + SeasonCalendar.dateText(s.getDay()) + " (" + s.getDay() + "일차), 선수 " + s.getRoster().size() + "명, 소속 "
                    + FencingWorld.nationName(s.getNation()) + ", " + when;
        } catch (Exception e) {
            return kind + " - 읽을 수 없는 파일 (" + when + ")";
        }
    }
}
