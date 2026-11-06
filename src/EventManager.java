import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 확률 기반 이벤트: 부상, 질병, 후원금, 대회 보너스, 응원. 육성/경기에 영향을 준다. */
public final class EventManager {
    private static final Random RND = new Random();

    private EventManager() {
    }

    /** 하루가 지날 때 한 번 호출. 발생한 이벤트 메시지를 반환 */
    public static List<String> rollDaily(GameState s) {
        List<String> out = new ArrayList<>();
        List<Fencer> roster = s.getRoster();
        if (roster.isEmpty()) return out;

        Weather w = s.getWeather();
        if (w.getTrainingModifier() < 1.0) {
            out.add("[날씨] " + w.getDisplayName() + " - 오늘은 훈련 효율이 떨어져요.");
        }

        Fencer target = roster.get(RND.nextInt(roster.size()));
        double r = RND.nextDouble();
        if (r < 0.12 && !target.isBusy()) {
            target.injure();
            target.takeDamage(10);
            out.add("[이벤트] " + target.getName() + "이(가) 연습 중 부상을 입었어요! (치료가 필요해요)");
        } else if (r < 0.22 && !target.isBusy()) {
            target.addFatigue(30);
            target.addMood(-20);
            out.add("[이벤트] " + target.getName() + "이(가) 독감에 걸려 피로가 크게 쌓였어요.");
        } else if (r < 0.37) {
            int gold = 100 + RND.nextInt(201);
            s.addGold(gold);
            out.add("[이벤트] 스폰서 후원금 " + gold + "G를 받았어요!");
        } else if (r < 0.47) {
            s.setRewardBoost(2.0);
            out.add("[이벤트] 대회 특별 보너스! 다음 승리 보상이 2배예요.");
        } else if (r < 0.60) {
            for (Fencer f : roster) f.addMood(15);
            out.add("[이벤트] 팬들의 응원으로 모든 선수의 기분이 좋아졌어요.");
        }
        return out;
    }
}
