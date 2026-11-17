import java.io.*;
import java.util.*;
public class LoadOld {
    public static void main(String[] a) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(a[0]))) {
            GameState s = (GameState) in.readObject();
            System.out.println("불러오기 OK: " + s.getDay() + "일차, 선수 " + s.getRoster().size() + "명, 메달 " + s.getMedals().size());
            for (Fencer f : s.getRoster()) System.out.println("  " + f.getName() + " Lv." + f.getLevel() + " " + f.getWeapon() + " 레이팅 " + Math.round(f.getRating()));
            for (WeaponType w : WeaponType.values()) { List<RankEntry> r = s.getRanking(w); System.out.println("  " + w + " 랭킹 " + r.size() + "명, 1위 " + r.get(0).name); }
            for (int i = 0; i < 10; i++) s.nextDay();
            System.out.println("10일 진행 OK, " + s.getDay() + "일차");
        }
    }
}
