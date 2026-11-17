import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 게임의 모든 진행 상황. 저장/불러오기는 이 객체 하나만 다루면 된다.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 2L;
    public static final int MAX_ROSTER = 8;
    public static final int MAX_FACILITY_LEVEL = 5;
    private static final Random RANDOM = new Random();

    private int gold = 1000;
    private int day = 1;
    private Weather weather = Weather.SUNNY;
    private Weather forecast = null;   // 내일 일기예보 (예전 저장 파일에는 없을 수 있다)

    private final List<Fencer> roster = new CopyOnWriteArrayList<>();
    private final Inventory inventory = new Inventory();

    private int wins = 0;
    private int matches = 0;
    private int bossWins = 0;
    private List<MatchRecord> history = new ArrayList<>();   // 경기 기록 (예전 저장 파일에는 없을 수 있다)
    private List<Mission> missions = new ArrayList<>();       // 오늘의 의뢰
    private int missionDay = 0;
    private int trainingHallLevel = 0;
    private int clinicLevel = 0;
    private double rewardBoost = 1.0;
    private final EnumSet<Achievement> achievements = EnumSet.noneOf(Achievement.class);
    private final EnumSet<Region> cleared = EnumSet.noneOf(Region.class);   // 예전 저장 파일 호환용 (전체 기록)
    // 종목별 전적과 대회 우승 기록 (플뢰레부 / 에페부 / 사브르부는 따로 진행된다)
    private EnumMap<WeaponType, Integer> winsByWeapon = new EnumMap<>(WeaponType.class);
    private EnumMap<WeaponType, EnumSet<Region>> clearedByWeapon = new EnumMap<>(WeaponType.class);
    // 세계(컴퓨터 선수들)와 메달 기록 (예전 저장 파일에는 없을 수 있다)
    private FencingWorld world;
    private List<MedalRecord> medals = new ArrayList<>();

    /** 새 게임: 선수 1명 + 기본 아이템 */
    public static GameState newGame() {
        GameState s = new GameState();
        s.world = new FencingWorld();
        s.addFencer(new Fencer("아델", WeaponType.SABRE, Personality.PASSIONATE));
        s.getInventory().add(ItemCatalog.ALL.get(0), 3); // 에너지바
        s.getInventory().add(ItemCatalog.ALL.get(3), 1); // 파스
        s.generateMissions();
        return s;
    }

    // ---------- 재화 ----------

    public synchronized int getGold() { return gold; }

    public synchronized void addGold(int amount) { gold += amount; }

    public synchronized boolean spendGold(int amount) {
        if (amount < 0 || gold < amount) return false;
        gold -= amount;
        return true;
    }

    // ---------- 시간 / 날씨 ----------

    public synchronized int getDay() { return day; }

    public synchronized Weather getWeather() { return weather; }

    /** 내일 날씨 예보. 내일은 예보대로 날씨가 바뀐다. */
    public synchronized Weather getForecast() {
        if (forecast == null) forecast = Weather.next(weather);
        return forecast;
    }

    /** 날씨를 직접 정한다. (테스트/이벤트용) */
    public synchronized void setWeather(Weather w) {
        weather = w;
        forecast = Weather.next(w);
    }

    /** 하루 경과: 일차 +1, 예보대로 날씨 변경, 모든 선수 tick + 날씨에 따른 컨디션 변화 */
    public synchronized void nextDay() {
        day++;
        weather = getForecast();
        forecast = Weather.next(weather);
        for (Fencer f : roster) {
            f.tick();
            switch (weather) {
                case SUNNY: f.addMood(3); break;          // 맑은 날은 기분이 좋아진다
                case RAINY: f.addMood(-3); break;         // 비 오는 날은 기분이 가라앉는다
                case HOT:   f.eat(-4, -2); break;         // 무더위에는 배고픔이 빨리 늘고 기분이 나빠진다
                default: break;
            }
        }
        generateMissions();   // 하루가 지나면 새 의뢰가 나온다
        getWorld().tick(day, RANDOM);   // 컴퓨터 선수들도 훈련하고 경기를 치른다
        if (day % 7 == 0) {              // 7일마다 주간 레이팅 변동 기준을 새로 잡는다
            for (Fencer f : roster) f.snapshotRating();
        }
    }

    // ---------- 선수 ----------

    public void addFencer(Fencer fencer) {
        fencer.snapshotRating();
        roster.add(fencer);
    }

    public List<Fencer> getRoster() { return roster; }

    public synchronized int recruitCost() { return 400 + 300 * roster.size(); }

    /** 선수 영입. 재화 부족/정원 초과면 null */
    public synchronized Fencer recruit(String name, WeaponType weapon) {
        if (roster.size() >= MAX_ROSTER) return null;
        if (!spendGold(recruitCost())) return null;
        Personality p = Personality.values()[RANDOM.nextInt(Personality.values().length)];
        Fencer f = new Fencer(name, weapon, p);
        f.snapshotRating();
        roster.add(f);
        return f;
    }

    /** 선수 방출. 마지막 선수와 시합/훈련 중인 선수는 방출할 수 없다. */
    public synchronized String releaseFencer(Fencer f) {
        if (f == null || !roster.contains(f)) return "방출할 선수를 찾을 수 없어요.";
        if (roster.size() <= 1) return "마지막 선수는 방출할 수 없어요. 먼저 다른 선수를 영입해주세요.";
        if (f.isBusy()) return f.getName() + "은(는) 시합 중이라 방출할 수 없어요.";
        if (f.isTraining()) return f.getName() + "은(는) 훈련 중이라 방출할 수 없어요.";
        f.stopRest();
        roster.remove(f);
        return f.getName() + " 선수를 방출했어요.";
    }

    // ---------- 아이템 ----------

    public Inventory getInventory() { return inventory; }

    public String useItem(Fencer target, Item item) {
        String why = item.cannotUse(target);   // 전용 장비 등: 아이템이 사라지기 전에 확인
        if (why != null) return why;
        if (!inventory.remove(item, 1)) {
            return item.getName() + "이(가) 없어요.";
        }
        String msg;
        if (item instanceof Equipment && ((Equipment) item).getSlot() != null) {
            Equipment eq = (Equipment) item;
            Equipment old = eq.equipOn(target);       // 장착하고, 끼고 있던 장비는 인벤토리로 되돌린다
            if (old != null) inventory.add(old, 1);
            msg = eq.equipMessage(target, old);
        } else {
            msg = item.use(target);
        }
        progressMissions(Mission.Type.USE_ITEM, null, 1);
        return msg;
    }

    /** 장착한 장비를 벗어서 인벤토리로 되돌린다. */
    public String unequip(Fencer f, EquipSlot slot) {
        Equipment e = f.unequip(slot);
        if (e == null) return f.getName() + "은(는) " + slot + " 장비를 끼고 있지 않아요.";
        inventory.add(e, 1);
        return f.getName() + "이(가) " + e.getName() + "을(를) 벗었어요.";
    }

    // ---------- 훈련 ----------

    /** 날씨와 훈련장 레벨이 반영된 외부 배율 */
    public synchronized double getTrainingMultiplier() {
        return weather.getTrainingModifier() * (1 + 0.05 * trainingHallLevel);
    }

    public long getTrainingDurationMinutes(Fencer f) {
        return f == null ? 0 : Math.max(1, f.getLevel()) * 5L;
    }

    /** 즉시 완료 비용은 훈련 시간의 5분 단위마다 100G다. */
    public int getInstantTrainingCost(Fencer f) {
        return f == null ? 0 : Math.max(1, f.getLevel()) * 100;
    }

    public String train(Fencer f, TrainingType t) {
        if (f == null || t == null) return "선수와 훈련 종류를 선택해주세요.";
        if (f.isBusy()) return f.getName() + "은(는) 지금 시합 중이에요.";
        if (f.isTraining()) return f.getName() + "은(는) 이미 훈련 중이에요.";
        if (f.isResting()) return f.getName() + "은(는) 휴식 중이라 훈련할 수 없어요.";
        if (f.isDown()) return f.getName() + "은(는) HP가 없어 훈련할 수 없어요. 먼저 회복이 필요해요.";
        if (f.getFatigue() >= 95) return f.getName() + "은(는) 너무 지쳐서 훈련할 수 없어요. 휴식이 필요해요.";

        long durationMinutes = getTrainingDurationMinutes(f);
        double efficiency = f.getTrainingEfficiency() * getTrainingMultiplier()
                * (f.getWeapon().isFavored(t) ? 1.2 : 1.0);
        if (!f.startTraining(t, durationMinutes * 60_000L, efficiency)) {
            return f.getName() + "은(는) 다른 활동 중이라 훈련을 시작할 수 없어요.";
        }
        return f.getName() + " - " + t.getDisplayName() + " 훈련을 시작했어요. 예상 시간: "
                + durationMinutes + "분";
    }

    /** 시간이 끝난 훈련의 효과를 적용한다. */
    public String completeTraining(Fencer f, Fencer.TrainingSession session) {
        if (f == null || session == null) return "훈련 완료 정보를 찾지 못했어요.";
        int oldLevel = f.getLevel();
        Grade oldGrade = f.getGrade();
        TrainingType type = session.getType();
        int oldTrainingLevel = f.getTrainingLevel(type);
        int hp0 = f.getMaxHp(), atk0 = f.getAttack(), def0 = f.getDefense();
        int agi0 = f.getAgility(), tec0 = f.getTechnique();
        double efficiency = f.applyTrainingAtEfficiency(type.getHp(), type.getAtk(), type.getDef(),
                type.getAgi(), type.getTec(), type.getFatigueCost(), session.getEfficiency());
        int newTrainingLevel = f.increaseTrainingLevel(type);
        progressMissions(Mission.Type.TRAIN, null, 1);

        StringBuilder message = new StringBuilder();
        message.append(f.getName()).append(" - ").append(type.getDisplayName())
                .append(String.format(" 완료 (효율 %d%%)", Math.round(efficiency * 100)))
                .append(f.getWeapon().isFavored(type) ? " [전문 훈련]" : "");
        String gain = gainText(f.getMaxHp() - hp0, f.getAttack() - atk0, f.getDefense() - def0,
                f.getAgility() - agi0, f.getTechnique() - tec0);
        message.append(gain.isEmpty() ? " | 효율이 낮아 능력치는 오르지 않았어요" : " | " + gain);
        if (newTrainingLevel > oldTrainingLevel) {
            message.append("\n").append(type.getDisplayName()).append(" 숙련 레벨 상승! Lv.")
                    .append(oldTrainingLevel).append(" → Lv.").append(newTrainingLevel);
        }
        String progression = progressionNotice(f, oldLevel, oldGrade);
        if (!progression.isEmpty()) message.append("\n").append(progression);
        return message.toString();
    }

    public String finishTrainingImmediately(Fencer f) {
        if (f == null) return "선수를 선택해주세요.";
        synchronized (this) {
            synchronized (f) {
                if (f.isBusy()) return f.getName() + "은(는) 지금 시합 중이에요.";
                if (!f.isTraining()) return "즉시 완료할 훈련이 없어요.";
                int cost = getInstantTrainingCost(f);
                boolean alreadyDue = f.getTrainingRemainingMillis() == 0;
                if (!alreadyDue && !spendGold(cost)) return "재화가 모자라요. 즉시 완료에는 " + cost + "G가 필요해요.";
                Fencer.TrainingSession session = alreadyDue
                        ? f.finishTrainingIfDue(System.currentTimeMillis()) : f.finishTrainingImmediately();
                if (session == null) {
                    if (!alreadyDue) addGold(cost);
                    return "훈련을 완료하지 못했어요. 다시 시도해주세요.";
                }
                String completed = completeTraining(f, session);
                return (alreadyDue ? "훈련 시간이 끝나 비용 없이 완료했어요.\n"
                        : "즉시 완료 비용: -" + cost + "G\n") + completed;
            }
        }
    }

    public static String progressionNotice(Fencer f, int oldLevel, Grade oldGrade) {
        if (f == null) return "";
        int newLevel = f.getLevel();
        Grade newGrade = f.getGrade();
        StringBuilder message = new StringBuilder();
        if (newLevel > oldLevel) {
            message.append(f.getName()).append(" 레벨 상승! Lv.").append(oldLevel).append(" → Lv.").append(newLevel);
        }
        if (oldGrade != newGrade) {
            if (message.length() > 0) message.append("\n");
            message.append(f.getName()).append(" 등급 상승! ").append(oldGrade.getDisplayName())
                    .append(" → ").append(newGrade.getDisplayName());
        }
        return message.toString();
    }

    private static String gainText(int hp, int atk, int def, int agi, int tec) {
        StringBuilder sb = new StringBuilder();
        if (hp > 0) sb.append("최대 체력 +").append(hp).append(" ");
        if (atk > 0) sb.append("공격 +").append(atk).append(" ");
        if (def > 0) sb.append("방어 +").append(def).append(" ");
        if (agi > 0) sb.append("민첩 +").append(agi).append(" ");
        if (tec > 0) sb.append("기술 +").append(tec).append(" ");
        return sb.toString().trim();
    }

    // ---------- 시설 ----------

    public synchronized int getTrainingHallLevel() { return trainingHallLevel; }

    public synchronized int getClinicLevel() { return clinicLevel; }

    /** 의무실 레벨에 따른 휴식 보너스 */
    public synchronized int getRestBonus() { return clinicLevel * 3; }

    public synchronized int upgradeCost(int level) { return 300 * (level + 1); }

    public synchronized String upgradeTrainingHall() {
        if (trainingHallLevel >= MAX_FACILITY_LEVEL) return "이미 최대 레벨이에요.";
        if (!spendGold(upgradeCost(trainingHallLevel))) return "재화가 모자라요.";
        trainingHallLevel++;
        return "훈련장이 Lv." + trainingHallLevel + "이 되었어요!";
    }

    public synchronized String upgradeClinic() {
        if (clinicLevel >= MAX_FACILITY_LEVEL) return "이미 최대 레벨이에요.";
        if (!spendGold(upgradeCost(clinicLevel))) return "재화가 모자라요.";
        clinicLevel++;
        return "의무실이 Lv." + clinicLevel + "이 되었어요!";
    }

    // ---------- 경기 기록 / 목표 ----------

    public synchronized int getWins() { return wins; }

    public synchronized int getMatches() { return matches; }

    public synchronized void recordResult(boolean won, Region region) {
        recordResult(won, region, false, null);
    }

    public synchronized void recordResult(boolean won, Region region, boolean boss) {
        recordResult(won, region, boss, null);
    }

    /** weapon 은 경기가 열린 종목 (종목별 전적과 우승 기록에 반영) */
    public synchronized void recordResult(boolean won, Region region, boolean boss, WeaponType weapon) {
        matches++;
        if (won) {
            progressMissions(Mission.Type.WIN_MATCH, weapon, 1);
            progressMissions(Mission.Type.WEAPON_WIN, weapon, 1);
            wins++;
            if (weapon != null) winsByWeapon.merge(weapon, 1, Integer::sum);
            if (boss) {
                bossWins++;
            } else {
                cleared.add(region);
                if (weapon != null) clearedByWeapon.computeIfAbsent(weapon, k -> EnumSet.noneOf(Region.class)).add(region);
            }
        }
    }

    /** 해당 종목에서 거둔 승리 수 (대회 해금 기준) */
    public synchronized int getWins(WeaponType w) { return winsByWeapon.getOrDefault(w, 0); }

    public synchronized boolean isCleared(Region r, WeaponType w) {
        EnumSet<Region> set = clearedByWeapon.get(w);
        return set != null && set.contains(r);
    }

    public synchronized boolean isCleared(Region r) { return cleared.contains(r); }

    public synchronized double consumeRewardBoost() {
        double b = rewardBoost;
        rewardBoost = 1.0;
        return b;
    }

    public synchronized void setRewardBoost(double boost) { rewardBoost = boost; }

    public synchronized boolean hasAchievement(Achievement a) { return achievements.contains(a); }

    /** 새로 달성한 목표를 확인하고 보상을 지급한다. 달성 메시지 목록 반환 */
    public synchronized List<String> checkAchievements() {
        List<String> out = new ArrayList<>();
        boolean pro = false, master = false;
        for (Fencer f : roster) {
            if (f.getGrade().ordinal() >= Grade.PRO.ordinal()) pro = true;
            if (f.getGrade() == Grade.MASTER) master = true;
        }
        check(Achievement.FIRST_WIN, wins >= 1, out);
        check(Achievement.WINS_10, wins >= 10, out);
        check(Achievement.COLLECTOR, roster.size() >= 3, out);
        check(Achievement.PRO_GRADE, pro, out);
        check(Achievement.MASTER_GRADE, master, out);
        check(Achievement.RICH, gold >= 5000, out);
        check(Achievement.BOSS_SLAYER, bossWins >= 1, out);
        boolean slam = true;
        for (WeaponType w : WeaponType.values()) {
            EnumSet<Region> set = clearedByWeapon.get(w);
            if (set == null || !set.contains(Region.WORLD)) slam = false;
        }
        check(Achievement.GRAND_SLAM, slam, out);
        check(Achievement.WORLD_CHAMPION, cleared.contains(Region.WORLD), out);
        boolean korean = false, olyMedal = false, olyGold = false;
        for (MedalRecord m : medals) {
            if (m.getRegion() == Region.NATIONAL && m.getRank() == 1) korean = true;
            if (m.getRegion() == Region.OLYMPIC) {
                olyMedal = true;
                if (m.getRank() == 1) olyGold = true;
            }
        }
        check(Achievement.KOREAN_CHAMPION, korean, out);
        check(Achievement.FIRST_MEDAL, !medals.isEmpty(), out);
        check(Achievement.OLYMPIC_MEDAL, olyMedal, out);
        check(Achievement.OLYMPIC_CHAMPION, olyGold, out);
        boolean no1 = false;
        for (WeaponType w : WeaponType.values()) {
            List<RankEntry> rk = getRanking(w);
            if (!rk.isEmpty() && rk.get(0).isMine()) no1 = true;
        }
        check(Achievement.WORLD_NO1, no1, out);
        return out;
    }

    private void check(Achievement a, boolean cond, List<String> out) {
        if (cond && achievements.add(a)) {
            gold += a.getReward();
            out.add("목표 달성: " + a.getDisplayName() + " (+" + a.getReward() + "G)");
        }
    }

    // ---------- 세계 / 파워 랭킹 / 대회 ----------

    public synchronized FencingWorld getWorld() {
        if (world == null) world = new FencingWorld();
        return world;
    }

    public synchronized List<MedalRecord> getMedals() {
        return new ArrayList<>(medals);
    }

    /** 종목별 세계 파워 랭킹 (컴퓨터 선수 + 내 선수). rank 가 채워진다. */
    public synchronized List<RankEntry> getRanking(WeaponType w) {
        List<RankEntry> all = new ArrayList<>();
        for (Rival r : getWorld().getRivals(w)) all.add(new RankEntry(r));
        for (Fencer f : roster) if (f.getWeapon() == w) all.add(new RankEntry(f));
        all.sort((a, b) -> Double.compare(b.rating, a.rating));
        for (int i = 0; i < all.size(); i++) all.get(i).rank = i + 1;
        return all;
    }

    /** 내 선수의 종목 세계 순위 (없으면 0) */
    public synchronized int bestRank(WeaponType w, List<Fencer> team) {
        int best = 0;
        for (RankEntry e : getRanking(w)) {
            if (e.isMine() && team.contains(e.fencer)) {
                best = e.rank;
                break;
            }
        }
        return best;
    }

    /** 대회에 나갈 수 있는지. 나갈 수 있으면 null, 없으면 이유를 돌려준다. */
    public synchronized String canEnter(Region r, WeaponType w, List<Fencer> team) {
        if (!r.isUnlocked(getWins(w))) return "잠김: " + w.getDisplayName() + "부 " + r.getRequiredWins() + "승 필요 (지금 " + getWins(w) + "승)";
        if (r == Region.OLYMPIC) {
            int rank = bestRank(w, team);
            if (rank == 0 || rank > 8) return "파워 랭킹 8위 이내만 출전 가능 (지금 " + (rank == 0 ? "-" : rank + "위") + ")";
        }
        return null;
    }

    /** 토너먼트가 끝났을 때 보상, 메달, 우승 기록, 목표를 정산하고 결과 문구를 돌려준다. */
    public synchronized String finishTournament(Tournament t) {
        Region r = t.getRegion();
        WeaponType w = t.getWeapon();
        int place = t.getPlacement();
        int medal = t.getMedal();
        double factor = place == 1 ? 1.0 : place == 2 ? 0.5 : place == 3 ? 0.3 : place == 0 ? 0 : 0.1;
        StringBuilder sb = new StringBuilder();
        sb.append(t.getTitle()).append(" - ").append(t.getPlacementText()).append("\n");

        int gold = 0, exp = 0;
        if (factor > 0) {
            double boost = medal > 0 ? consumeRewardBoost() : 1.0;
            gold = (int) (r.getRewardGold() * factor * boost);
            exp = (int) (r.getRewardExp() * factor);
            addGold(gold);
            sb.append("보상: ").append(gold).append("G").append(boost > 1 ? " (보너스 x" + boost + ")" : "").append("\n");
        }
        for (Fencer f : t.getMyFencers()) {
            f.addFatigue(10);
            if (exp > 0 && f.gainExp(exp)) {
                sb.append(f.getName()).append("이(가) Lv.").append(f.getLevel()).append("(으)로 성장했어요!\n");
            }
        }
        if (medal > 0) {
            medals.add(new MedalRecord(day, r, w, medal, t.isTeam(), t.getMe().name));
            int bonusWins = medal == 1 ? 2 : 1;   // 메달은 승리 수로 쳐서 다음 대회가 열리는 데 도움이 된다.
            wins += bonusWins;
            winsByWeapon.merge(w, bonusWins, Integer::sum);
            progressMissions(Mission.Type.WIN_MATCH, w, 1);
            progressMissions(Mission.Type.WEAPON_WIN, w, 1);
        }
        if (place == 1) {
            cleared.add(r);
            clearedByWeapon.computeIfAbsent(w, k -> EnumSet.noneOf(Region.class)).add(r);
        }
        for (String m : checkAchievements()) sb.append(m).append("\n");
        return sb.toString().trim();
    }

    // ---------- 경기 기록 ----------

    public synchronized void addRecord(MatchRecord r) {
        history.add(r);
        if (history.size() > 200) history.remove(0);
    }

    public synchronized List<MatchRecord> getHistory() {
        return new ArrayList<>(history);
    }

    // ---------- 일일 의뢰 ----------

    public synchronized List<Mission> getMissions() {
        return Collections.unmodifiableList(new ArrayList<>(missions));
    }

    /** 오늘의 의뢰를 3개 만든다. (훈련 / 경기 승리 / 종목 승리 / 아이템 사용 중에서 무작위) */
    public synchronized void generateMissions() {
        missionDay = day;
        missions = new ArrayList<>();
        Random r = new Random(day * 7919L + wins * 31L);
        List<Mission> pool = new ArrayList<>();
        int train = 2 + r.nextInt(3);
        pool.add(new Mission(Mission.Type.TRAIN, null, train, 40 * train));
        pool.add(new Mission(Mission.Type.WIN_MATCH, null, 1, 250));
        pool.add(new Mission(Mission.Type.WEAPON_WIN, WeaponType.values()[r.nextInt(WeaponType.values().length)], 1, 300));
        int use = 1 + r.nextInt(3);
        pool.add(new Mission(Mission.Type.USE_ITEM, null, use, 50 * use));
        Collections.shuffle(pool, r);
        missions.addAll(pool.subList(0, 3));
    }

    /** 의뢰 진행도를 올리고, 달성하면 보상을 바로 지급한다. */
    private void progressMissions(Mission.Type type, WeaponType weapon, int amount) {
        synchronized (this) {
            for (Mission m : missions) {
                if (m.advance(type, weapon, amount)) {
                    gold += m.getReward();
                    GameLog.post("[의뢰] 완료! " + m.describe() + " (+" + m.getReward() + "G)");
                }
            }
        }
    }

    // 저장 중에 값이 바뀌지 않도록 잠금
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    // 종목별 기록이 없는 예전 저장 파일은 전체 기록을 모든 종목에 똑같이 적용해서 불러온다.
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (history == null) history = new ArrayList<>();       // 기록/의뢰가 없던 예전 저장 파일
        if (medals == null) medals = new ArrayList<>();         // 메달/세계가 없던 예전 저장 파일
        if (missions == null) {
            missions = new ArrayList<>();
            generateMissions();
        }
        if (winsByWeapon == null) {
            winsByWeapon = new EnumMap<>(WeaponType.class);
            for (WeaponType w : WeaponType.values()) winsByWeapon.put(w, wins);
        }
        if (clearedByWeapon == null) {
            clearedByWeapon = new EnumMap<>(WeaponType.class);
            for (WeaponType w : WeaponType.values()) clearedByWeapon.put(w, EnumSet.copyOf(cleared));
        }
    }
}
