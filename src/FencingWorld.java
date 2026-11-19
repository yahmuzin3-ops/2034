import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 펜싱 세계: 종목마다 세계 각국 선수들이 살고 있다. 내 선수의 소속 국가(기본 한국)는 고를 수 있다.
 * 대회 출전 선수 명단(자국 선수권 / 세계 선수권 / 올림픽)과 단체전 팀을 여기서 만든다.
 */
public class FencingWorld implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int KOREANS = 32;
    private static final int HOME_DEPTH = 32;   // 소속 국가로 고른 나라는 선수가 이만큼 있도록 채워진다.
    private static final int PER_NATION = 7;

    /** 국가 코드 -> {국가 이름, 실력 평균(환산 레벨)} */
    private static final Map<String, Object[]> NATIONS = new LinkedHashMap<>();

    static {
        NATIONS.put("KOR", new Object[]{"대한민국", 6.5});
        NATIONS.put("FRA", new Object[]{"프랑스", 14.0});
        NATIONS.put("ITA", new Object[]{"이탈리아", 14.0});
        NATIONS.put("HUN", new Object[]{"헝가리", 13.0});
        NATIONS.put("USA", new Object[]{"미국", 12.5});
        NATIONS.put("CHN", new Object[]{"중국", 12.5});
        NATIONS.put("UKR", new Object[]{"우크라이나", 12.5});
        NATIONS.put("JPN", new Object[]{"일본", 10.5});
        NATIONS.put("GER", new Object[]{"독일", 11.5});
        NATIONS.put("ROU", new Object[]{"루마니아", 10.5});
        NATIONS.put("EGY", new Object[]{"이집트", 9.5});
        NATIONS.put("ESP", new Object[]{"스페인", 9.5});
        NATIONS.put("GBR", new Object[]{"영국", 9.5});
        NATIONS.put("POL", new Object[]{"폴란드", 10.5});
        NATIONS.put("CAN", new Object[]{"캐나다", 8.5});
        NATIONS.put("IRN", new Object[]{"이란", 7.5});
    }

    private static final String[] KOR_FAMILY = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권", "황", "안", "송", "류", "홍"};
    private static final String[] KOR_GIVEN = {"민준", "서준", "도윤", "예준", "시우", "하준", "주원", "지호", "지후", "준서", "건우", "현우", "우진", "선우",
            "유찬", "연우", "정우", "승현", "태양", "지훈", "민재", "승우", "재윤", "은우", "도현", "서연", "하윤", "지우", "수아", "채원"};
    private static final String[] WEST_GIVEN = {"Luca", "Marco", "Hugo", "Louis", "Matteo", "Felix", "Jonas", "Adam", "Viktor", "Pavel", "Leo", "Noah",
            "Elias", "Mateo", "Oscar", "Ivan", "Tomas", "Andre", "Emil", "Samir", "Yusuf", "Kenji", "Wei", "Hiro", "Anna", "Clara", "Sofia", "Elena", "Mia", "Nora"};
    private static final String[] WEST_FAMILY = {"Rossi", "Bianchi", "Dubois", "Moreau", "Kovacs", "Nagy", "Smith", "Miller", "Wang", "Li", "Kovalenko",
            "Shevchenko", "Sato", "Tanaka", "Muller", "Weber", "Popescu", "Ionescu", "Hassan", "Farouk", "Garcia", "Lopez", "Taylor", "Evans", "Nowak", "Wisniewski",
            "Tremblay", "Roy", "Rezai", "Karimi", "Bernard", "Conti", "Horvath", "Szabo", "Zhang", "Chen"};

    private final EnumMap<WeaponType, List<Rival>> rivals = new EnumMap<>(WeaponType.class);

    public FencingWorld() {
        this(new Random());
    }

    public FencingWorld(Random rnd) {
        for (WeaponType w : WeaponType.values()) {
            List<Rival> list = new ArrayList<>();
            for (Map.Entry<String, Object[]> n : NATIONS.entrySet()) {
                int count = "KOR".equals(n.getKey()) ? KOREANS : PER_NATION;
                double mu = (Double) n.getValue()[1];
                for (int i = 0; i < count; i++) {
                    double level = Math.max(1, Math.min(RatingSystem.MAX_LEVEL, mu + rnd.nextGaussian() * 3.5 + (i == 0 ? 2 : 0)));
                    Personality p = Personality.values()[rnd.nextInt(Personality.values().length)];
                    list.add(new Rival(makeName(n.getKey(), rnd, list), n.getKey(), w, p, RatingSystem.ratingForLevel(level)));
                }
            }
            rivals.put(w, list);
        }
    }

    private static String makeName(String nation, Random rnd, List<Rival> existing) {
        for (int tries = 0; ; tries++) {
            String name;
            if ("KOR".equals(nation)) name = KOR_FAMILY[rnd.nextInt(KOR_FAMILY.length)] + KOR_GIVEN[rnd.nextInt(KOR_GIVEN.length)];
            else name = WEST_GIVEN[rnd.nextInt(WEST_GIVEN.length)] + " " + WEST_FAMILY[rnd.nextInt(WEST_FAMILY.length)];
            boolean dup = false;
            for (Rival r : existing) if (r.getName().equals(name)) { dup = true; break; }
            if (!dup || tries > 40) return name;
        }
    }

    public static String nationName(String code) {
        Object[] n = NATIONS.get(code);
        return n == null ? code : (String) n[0];
    }

    /** 대회 이름 등에 쓰는 짧은 국가 이름 (한국, 프랑스 ...) */
    public static String shortName(String code) {
        return "KOR".equals(code) ? "한국" : nationName(code);
    }

    public static List<String> nationCodes() {
        return new ArrayList<>(NATIONS.keySet());
    }

    public static boolean isNation(String code) {
        return code != null && NATIONS.containsKey(code);
    }

    /** 국내 대회와 세계 대회의 난이도 느낌: 그 나라의 펜싱 수준 */
    public static String levelLabel(String code) {
        Object[] n = NATIONS.get(code);
        if (n == null) return "보통";
        double mu = (Double) n[1];
        return mu <= 8.5 ? "쉬움" : mu <= 11.5 ? "보통" : "어려움";
    }

    /**
     * 소속 국가로 고른 나라의 선수 수를 채운다. 국내 선수권에 나올 국내 선수가 충분해야 하기 때문이다.
     * 한국은 처음부터 충분하다. 새로 채워지는 선수는 그 나라 대표급보다 조금 약한 실력이다.
     */
    public synchronized void ensureNation(String code, Random rnd) {
        Object[] n = NATIONS.get(code);
        if (n == null) return;
        double mu = (Double) n[1];
        for (WeaponType w : WeaponType.values()) {
            List<Rival> list = rivals.get(w);
            int have = 0;
            for (Rival r : list) if (code.equals(r.getNation())) have++;
            for (int i = have; i < HOME_DEPTH; i++) {
                double level = Math.max(1, Math.min(RatingSystem.MAX_LEVEL, Math.max(4.0, mu - 3.0) + rnd.nextGaussian() * 3.5));
                Personality p = Personality.values()[rnd.nextInt(Personality.values().length)];
                list.add(new Rival(makeName(code, rnd, list), code, w, p, RatingSystem.ratingForLevel(level)));
            }
        }
    }

    public synchronized List<Rival> getRivals(WeaponType w) {
        return new ArrayList<>(rivals.get(w));
    }

    private List<Rival> sortedByRating(WeaponType w, String onlyNation, String excludeNation) {
        List<Rival> list = new ArrayList<>();
        for (Rival r : rivals.get(w)) {
            if (onlyNation != null && !onlyNation.equals(r.getNation())) continue;
            if (excludeNation != null && excludeNation.equals(r.getNation())) continue;
            list.add(r);
        }
        list.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return list;
    }

    // ---------- 대회 출전 선수 ----------

    /** 자국 선수권: 소속 국가 선수 n명 (실력 상위권에서 뽑는다) */
    public synchronized List<Rival> homeField(String nation, WeaponType w, int n, Random rnd) {
        List<Rival> pool = sortedByRating(w, nation, null);
        return pick(pool.subList(0, Math.min(pool.size(), n + 8)), n, rnd);
    }

    /** 세계 선수권: 세계 상위 선수 중에서 n명 */
    public synchronized List<Rival> worldField(WeaponType w, int n, Random rnd) {
        List<Rival> pool = sortedByRating(w, null, null);
        return pick(pool.subList(0, Math.min(pool.size(), n + 24)), n, rnd);
    }

    /** 올림픽: 세계 랭킹 상위 n명 */
    public synchronized List<Rival> topField(WeaponType w, int n) {
        List<Rival> pool = sortedByRating(w, null, null);
        return new ArrayList<>(pool.subList(0, Math.min(pool.size(), n)));
    }

    private static List<Rival> pick(List<Rival> pool, int n, Random rnd) {
        List<Rival> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, rnd);
        return new ArrayList<>(copy.subList(0, Math.min(n, copy.size())));
    }

    /** 자국 선수권 단체전: 클럽 팀 count 개 (소속 국가 선수로만 구성) */
    public synchronized List<List<Rival>> homeTeams(String nation, WeaponType w, int count, int teamSize, Random rnd) {
        List<Rival> pool = sortedByRating(w, nation, null);
        int need = count * teamSize;
        List<Rival> chosen = pick(pool.subList(0, Math.min(pool.size(), need + 6)), need, rnd);
        chosen.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        List<List<Rival>> teams = new ArrayList<>();
        for (int i = 0; i < count; i++) teams.add(new ArrayList<>());
        // 강한 선수부터 팀에 번갈아 나눠서 팀 실력이 고르게 되도록 한다.
        for (int i = 0; i < chosen.size(); i++) {
            int row = i / count;
            int idx = row % 2 == 0 ? i % count : count - 1 - (i % count);
            teams.get(idx).add(chosen.get(i));
        }
        return teams;
    }

    /** 세계 선수권 / 올림픽 단체전: 각국 대표팀 (내 소속 국가 제외). 나라마다 그 나라 최고 선수들로 구성한다. */
    public synchronized List<List<Rival>> nationTeams(String homeNation, WeaponType w, int count, int teamSize, boolean olympic, Random rnd) {
        Map<String, List<Rival>> byNation = new LinkedHashMap<>();
        for (Rival r : sortedByRating(w, null, homeNation)) byNation.computeIfAbsent(r.getNation(), k -> new ArrayList<>()).add(r);
        List<List<Rival>> teams = new ArrayList<>();
        for (List<Rival> list : byNation.values()) {
            if (list.size() >= teamSize) teams.add(new ArrayList<>(list.subList(0, teamSize)));
        }
        teams.sort((a, b) -> Double.compare(avg(b), avg(a)));
        int pool = olympic ? count : Math.min(teams.size(), count + 3);
        List<List<Rival>> top = new ArrayList<>(teams.subList(0, Math.min(pool, teams.size())));
        if (!olympic) {
            Collections.shuffle(top, rnd);
        }
        return new ArrayList<>(top.subList(0, Math.min(count, top.size())));
    }

    private static double avg(List<Rival> team) {
        double s = 0;
        for (Rival r : team) s += r.getRating();
        return team.isEmpty() ? 0 : s / team.size();
    }

    // ---------- 하루가 지날 때 ----------

    /** 컴퓨터 선수들도 훈련하고 경기를 치르며 레이팅이 조금씩 변한다. 7일마다 주간 변동 기준을 새로 잡는다. */
    public synchronized void tick(int day, Random rnd) {
        for (WeaponType w : WeaponType.values()) {
            List<Rival> list = rivals.get(w);
            for (int i = 0; i < 24; i++) {   // 하루 24경기
                Rival a = list.get(rnd.nextInt(list.size()));
                Rival b = list.get(rnd.nextInt(list.size()));
                if (a == b) continue;
                double ra = a.getRating(), rb = b.getRating();
                boolean aWins = rnd.nextDouble() < RatingSystem.expected(ra, rb);
                a.applyElo(rb, aWins, 6);
                b.applyElo(ra, !aWins, 6);
            }
            for (Rival r : list) {
                r.train(0.15 + rnd.nextDouble() * 0.2);   // 꾸준한 훈련 (하루 평균 약 0.25점)
                if (day % 7 == 0) r.snapshot();
            }
        }
    }
}
