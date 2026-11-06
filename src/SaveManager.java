import java.io.*;

/** 저장/불러오기. GameState 객체 하나를 파일로 직렬화한다. */
public final class SaveManager {
    public static final File FILE = new File("savegame.dat");

    private SaveManager() {
    }

    public static boolean exists() {
        return FILE.exists();
    }

    public static void save(GameState state) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(state);
        }
    }

    public static GameState load() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
            return (GameState) in.readObject();
        }
    }
}
