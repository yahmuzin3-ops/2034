import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 토너먼트(한국 선수권 / 세계 선수권 / 올림픽).
 *
 * [개인전]
 *  - 한국 선수권: 16명, 세계 선수권: 32명 -> 4명씩 풀(예선): 풀 안에서 모두와 5점 경기, 풀 상위 2명이 본선 진출
 *  - 본선: 시드를 배정한 토너먼트(8강 또는 16강부터), 15점 경기(3분 x 3피리어드). 승자만 올라간다.
 *  - 올림픽: 예선 없이 16명이 시드 순으로 곧바로 토너먼트, 준결승 패자끼리 동메달 결정전
 *  - 세계 선수권은 준결승에서 진 두 선수가 모두 동메달(공동 3위)
 * [단체전] 8팀 토너먼트(8강-준결승-결승), 경기는 단체전 릴레이. 올림픽은 동메달 결정전이 있다.
 *
 * 내 경기는 직접 하거나 시뮬레이션하고, 다른 선수들의 경기는 파워 레이팅으로 자동 진행된다.
 * 경기 결과는 모두 파워 레이팅에 반영된다.
 */
public class Tournament implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Random R = new Random();
    private static final int[][] POOL_ORDER = {{0, 3}, {1, 2}, {0, 2}, {1, 3}, {2, 3}, {0, 1}};   // 4명 풀의 경기 순서

    public enum Stage { POOL, DE, DONE }

    /** 출전 선수(또는 팀) */
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String name;
        public final String nation;
        public final List<Rival> rivals;     // 컴퓨터 선수(들). 내 선수면 null
        public final List<Fencer> players;   // 내 선수(들). 컴퓨터면 null

        Entry(String name, String nation, List<Rival> rivals, List<Fencer> players) {
            this.name = name;
            this.nation = nation;
            this.rivals = rivals;
            this.players = players;
        }

        public boolean isMine() { return players != null; }

        /** 선수(팀)의 파워 레이팅 (팀이면 평균) */
        public double rating() {
            double s = 0;
            int n = 0;
            if (players != null) {
                for (Fencer f : players) { s += f.getRating(); n++; }
            } else {
                for (Rival r : rivals) { s += r.getRating(); n++; }
            }
            return n == 0 ? 0 : s / n;
        }

        @Override
        public String toString() { return name; }
    }

    /** 경기 한 판 */
    public static class Match implements Serializable {
        private static final long serialVersionUID = 1L;
        public Entry a, b;
        public int sa, sb;
        public Entry winner;
        public boolean played;
        public String round;
        public int target = 15;
        public boolean pool;
        public boolean bronze;

        public boolean involves(Entry e) { return a == e || b == e; }

        public Entry other(Entry e) { return a == e ? b : a; }
    }

    /** 풀 순위표의 한 줄 */
    public static class Row {
        public Entry entry;
        public int wins, losses, ts, tr;

        public int indicator() { return ts - tr; }
    }

    private final Region region;
    private final WeaponType weapon;
    private final boolean team;
    private final int teamSize;
    private final int startDay;
    private final List<Entry> entries = new ArrayList<>();
    private Entry me;
    private boolean usesPool;
    private int deSize;
    private final List<List<Entry>> pools = new ArrayList<>();
    private final List<Match> poolMatches = new ArrayList<>();
    private final List<List<Match>> rounds = new ArrayList<>();
    private Match bronze;
    private Stage stage = Stage.POOL;
    private boolean finished;
    private boolean meOut;
    private int placement;
    private final List<String> log = new ArrayList<>();

    private Tournament(Region region, WeaponType weapon, boolean team, int teamSize, int startDay) {
        this.region = region;
        this.weapon = weapon;
        this.team = team;
        this.teamSize = teamSize;
        this.startDay = startDay;
    }

    public static Tournament create(GameState state, Region region, WeaponType weapon, List<Fencer> mine) {
        Tournament t = new Tournament(region, weapon, mine.size() >= 2, mine.size(), state.getDay());
        t.build(state.getWorld(), new ArrayList<>(mine));
        return t;
    }

    // ---------- 만들기 ----------

    private void build(FencingWorld world, List<Fencer> mine) {
        String myName = team ? "우리 팀 (KOR)" : mine.get(0).getName() + " (KOR)";
        me = new Entry(myName, "KOR", null, mine);
        entries.add(me);
        if (!team) {
            List<Rival> field;
            switch (region) {
                case NATIONAL:
                    field = world.koreanField(weapon, 15, R);
                    usesPool = true;
                    deSize = 8;
                    break;
                case WORLD:
                    field = world.worldField(weapon, 31, R);
                    usesPool = true;
                    deSize = 16;
                    break;
                default:   // 올림픽: 예선 없이 시드 순으로 16명 토너먼트
                    field = world.topField(weapon, 15);
                    usesPool = false;
                    deSize = 16;
                    break;
            }
            for (Rival r : field) {
                List<Rival> one = new ArrayList<>();
                one.add(r);
                entries.add(new Entry(r.getDisplayName(), r.getNation(), one, null));
            }
        } else {
            usesPool = false;
            deSize = 8;
            if (region == Region.NATIONAL) {
                List<List<Rival>> teams = world.koreanTeams(weapon, 7, teamSize, R);
                for (int i = 0; i < teams.size(); i++) entries.add(new Entry("클럽 " + (char) ('A' + i) + "팀 (KOR)", "KOR", teams.get(i), null));
            } else {
                List<List<Rival>> teams = world.nationTeams(weapon, 7, teamSize, region == Region.OLYMPIC, R);
                for (List<Rival> t : teams) {
                    String nation = t.get(0).getNation();
                    entries.add(new Entry(FencingWorld.nationName(nation) + " 대표팀 (" + nation + ")", nation, t, null));
                }
            }
        }
        List<Entry> seeded = new ArrayList<>(entries);
        seeded.sort((x, y) -> Double.compare(y.rating(), x.rating()));
        if (usesPool) {
            buildPools(seeded);
            stage = Stage.POOL;
            for (Match m : poolMatches) if (!m.involves(me)) simulate(m);
            addLog(region.getDisplayName() + " 예선 풀이 시작됐어요. 내 풀에서 상위 2명이 본선에 올라가요.");
        } else {
            startDE(seeded);
        }
        advance();
    }

    /** 레이팅 순으로 뱀 모양(1,2,3,4 / 8,7,6,5 ...)으로 풀에 나눈다. */
    private void buildPools(List<Entry> seeded) {
        int count = seeded.size() / 4;
        for (int i = 0; i < count; i++) pools.add(new ArrayList<>());
        for (int i = 0; i < seeded.size(); i++) {
            int row = i / count;
            int idx = row % 2 == 0 ? i % count : count - 1 - (i % count);
            pools.get(idx).add(seeded.get(i));
        }
        for (int p = 0; p < pools.size(); p++) {
            List<Entry> pool = pools.get(p);
            for (int[] o : POOL_ORDER) {
                Match m = new Match();
                m.a = pool.get(o[0]);
                m.b = pool.get(o[1]);
                m.target = 5;
                m.pool = true;
                m.round = "예선 풀 " + (p + 1);
                poolMatches.add(m);
            }
        }
    }

    private void startDE(List<Entry> seeded) {
        stage = Stage.DE;
        int[] ord = seedOrder(deSize);
        List<Match> first = new ArrayList<>();
        for (int i = 0; i < deSize / 2; i++) {
            Match m = new Match();
            m.a = seeded.get(ord[2 * i] - 1);
            m.b = seeded.get(ord[2 * i + 1] - 1);
            m.round = roundName(deSize);
            first.add(m);
        }
        rounds.add(first);
    }

    /** 표준 시드 배치: 1번과 2번 시드는 결승에서만 만난다. */
    static int[] seedOrder(int m) {
        List<Integer> order = new ArrayList<>();
        order.add(1);
        while (order.size() < m) {
            List<Integer> next = new ArrayList<>();
            int size = order.size() * 2;
            for (int x : order) {
                next.add(x);
                next.add(size + 1 - x);
            }
            order = next;
        }
        int[] arr = new int[m];
        for (int i = 0; i < m; i++) arr[i] = order.get(i);
        return arr;
    }

    private static String roundName(int participants) {
        switch (participants) {
            case 32: return "32강";
            case 16: return "16강";
            case 8: return "8강";
            case 4: return "준결승";
            default: return "결승";
        }
    }

    // ---------- 내 경기 ----------

    /** 내가 다음에 치를 경기 (없으면 null) */
    public Match nextPlayerMatch() {
        if (finished || meOut) return null;
        if (stage == Stage.POOL) {
            for (Match m : poolMatches) if (!m.played && m.involves(me)) return m;
            return null;
        }
        if (bronze != null && !bronze.played && bronze.involves(me)) return bronze;
        if (rounds.isEmpty()) return null;
        for (Match m : rounds.get(rounds.size() - 1)) if (!m.played && m.involves(me)) return m;
        return null;
    }

    /** 내 경기를 직접 치른 결과를 반영한다. */
    public void recordPlayerMatch(Match m, int myScore, int oppScore, boolean won) {
        boolean meIsA = m.a == me;
        m.sa = meIsA ? myScore : oppScore;
        m.sb = meIsA ? oppScore : myScore;
        m.winner = won ? me : m.other(me);
        m.played = true;

        Entry opp = m.other(me);
        double myBefore = me.rating(), oppRating = opp.rating();
        double k = team ? 12 : (m.pool ? 14 : 20);
        for (Fencer f : me.players) f.applyRating(oppRating, won, k);
        for (Rival r : opp.rivals) r.applyElo(myBefore, !won, team ? 12 : 16);
        for (Fencer f : me.players) {   // 경기 사이 휴식
            f.heal(25);
            f.addFatigue(-10);
        }
        addLog((won ? "승리 " : "패배 ") + myScore + " : " + oppScore + "  vs " + opp.name + "  (" + m.round + (m.bronze ? ", 동메달 결정전" : "") + ")");

        if (!m.pool) {
            if (m.bronze) {
                placement = won ? 3 : 4;
                meOut = true;
            } else if (!won) {
                meOut = true;
                int size = rounds.get(rounds.size() - 1).size() * 2;   // 이 라운드의 참가 인원
                if (size == 2) placement = 2;
                else if (size == 4) {
                    if (region == Region.OLYMPIC) meOut = false;       // 올림픽은 동메달 결정전으로
                    else placement = 3;                                 // 그 밖의 대회는 공동 3위(동메달)
                } else placement = size;
            }
        }
        advance();
    }

    /** 내 경기를 직접 하지 않고, 두 쪽의 파워 레이팅으로 결과를 정한다. */
    public void simulatePlayerMatch(Match m) {
        Entry opp = m.other(me);
        double p = RatingSystem.expected(me.rating(), opp.rating());
        boolean won = R.nextDouble() < p;
        int total = team ? 5 * teamSize * teamSize : m.target;
        double closeness = 1 - Math.abs(p - 0.5) * 2;
        int loser = (int) Math.round((total - 1) * (0.30 + 0.55 * closeness * R.nextDouble() + 0.1 * R.nextDouble()));
        loser = Math.max(0, Math.min(total - 1, loser));
        recordPlayerMatch(m, won ? total : loser, won ? loser : total, won);
    }

    /** 대회 도중 기권 */
    public void withdraw() {
        if (finished) return;
        meOut = true;
        placement = 0;
        finished = true;
        stage = Stage.DONE;
        addLog("대회를 기권했어요.");
    }

    // ---------- 진행 ----------

    private void advance() {
        while (!finished) {
            if (stage == Stage.POOL) {
                for (Match m : poolMatches) if (!m.played) return;   // 내 풀 경기가 남아 있다
                finishPool();
                continue;
            }
            if (rounds.isEmpty()) return;
            List<Match> cur = rounds.get(rounds.size() - 1);
            for (Match m : cur) if (!m.played && !m.involves(me)) simulate(m);
            if (bronze != null && !bronze.played && !bronze.involves(me)) simulate(bronze);
            boolean allPlayed = bronze == null || bronze.played;
            for (Match m : cur) if (!m.played) allPlayed = false;
            if (!allPlayed) return;   // 내 경기를 기다린다

            if (cur.size() == 1) {    // 결승까지 끝났다
                finishTournament(cur.get(0));
                return;
            }
            buildNextRound(cur);
        }
    }

    private void buildNextRound(List<Match> cur) {
        List<Match> next = new ArrayList<>();
        for (int i = 0; i < cur.size(); i += 2) {
            Match m = new Match();
            m.a = cur.get(i).winner;
            m.b = cur.get(i + 1).winner;
            m.round = roundName(cur.size());
            next.add(m);
        }
        if (cur.size() == 2 && region == Region.OLYMPIC) {   // 올림픽 준결승 패자끼리 동메달 결정전
            bronze = new Match();
            bronze.a = loser(cur.get(0));
            bronze.b = loser(cur.get(1));
            bronze.round = "동메달 결정전";
            bronze.bronze = true;
            if (!team) bronze.target = 15;
        }
        rounds.add(next);
        if (!meOut) addLog(next.get(0).round + " 대진이 정해졌어요.");
    }

    private Entry loser(Match m) { return m.winner == m.a ? m.b : m.a; }

    private void finishPool() {
        List<Entry> qualifiers = new ArrayList<>();
        List<Row> all = new ArrayList<>();
        int perPool = deSize / pools.size();
        for (List<Entry> pool : pools) {
            List<Row> rows = standings(pool);
            for (int i = 0; i < perPool; i++) qualifiers.add(rows.get(i).entry);
            all.addAll(rows);
        }
        // 본선 시드: 풀 성적(승수, 득실 차, 득점, 레이팅) 순
        final List<Row> rowsAll = all;
        qualifiers.sort((x, y) -> {
            Row a = rowOf(rowsAll, x), b = rowOf(rowsAll, y);
            if (a.wins != b.wins) return b.wins - a.wins;
            if (a.indicator() != b.indicator()) return b.indicator() - a.indicator();
            if (a.ts != b.ts) return b.ts - a.ts;
            return Double.compare(y.rating(), x.rating());
        });
        if (!qualifiers.contains(me)) {
            meOut = true;
            placement = 99;
            Row mine = rowOf(rowsAll, me);
            addLog("예선 풀 " + mine.wins + "승 " + mine.losses + "패로 본선에 오르지 못했어요.");
        } else {
            addLog("예선을 통과했어요! 본선 " + (qualifiers.indexOf(me) + 1) + "번 시드로 " + roundName(deSize) + "부터 시작해요.");
        }
        startDE(qualifiers);
    }

    private static Row rowOf(List<Row> rows, Entry e) {
        for (Row r : rows) if (r.entry == e) return r;
        return null;
    }

    /** 풀 순위: 승수, 득실 차, 득점, 레이팅 순 */
    public List<Row> standings(List<Entry> pool) {
        List<Row> rows = new ArrayList<>();
        for (Entry e : pool) {
            Row r = new Row();
            r.entry = e;
            for (Match m : poolMatches) {
                if (!m.played || !m.involves(e) || !pool.contains(m.a) || !pool.contains(m.b)) continue;
                boolean isA = m.a == e;
                int mine = isA ? m.sa : m.sb, theirs = isA ? m.sb : m.sa;
                r.ts += mine;
                r.tr += theirs;
                if (m.winner == e) r.wins++;
                else r.losses++;
            }
            rows.add(r);
        }
        rows.sort((x, y) -> {
            if (x.wins != y.wins) return y.wins - x.wins;
            if (x.indicator() != y.indicator()) return y.indicator() - x.indicator();
            if (x.ts != y.ts) return y.ts - x.ts;
            return Double.compare(y.entry.rating(), x.entry.rating());
        });
        return rows;
    }

    private void finishTournament(Match fin) {
        stage = Stage.DONE;
        finished = true;
        if (!meOut) placement = fin.winner == me ? 1 : 2;
        else if (placement == 0) placement = 99;
        // 올림픽 동메달 결정전에 내가 없었다면(= 준결승에서 지지 않았다면) 위에서 처리된다
        addLog("대회 종료: " + getPlacementText());
    }

    /** 컴퓨터 선수들의 경기는 파워 레이팅으로 승패를 정하고 점수를 만든다. */
    private void simulate(Match m) {
        double ra = m.a.rating(), rb = m.b.rating();
        double p = RatingSystem.expected(ra, rb);
        boolean aWins = R.nextDouble() < p;
        int total = team ? 5 * teamSize * teamSize : m.target;
        double closeness = 1 - Math.abs(p - 0.5) * 2;
        int loser = (int) Math.round((total - 1) * (0.30 + 0.55 * closeness * R.nextDouble() + 0.1 * R.nextDouble()));
        loser = Math.max(0, Math.min(total - 1, loser));
        m.sa = aWins ? total : loser;
        m.sb = aWins ? loser : total;
        m.winner = aWins ? m.a : m.b;
        m.played = true;
        double k = m.pool ? 8 : 12;
        if (m.a.rivals != null) for (Rival r : m.a.rivals) r.applyElo(rb, aWins, k);
        if (m.b.rivals != null) for (Rival r : m.b.rivals) r.applyElo(ra, !aWins, k);
    }

    // ---------- 내 경기에 쓸 정보 ----------

    /** 상대 선수(들)를 실제 경기용 선수로 만든다. */
    public List<Fencer> enemyFencers(Match m) {
        List<Fencer> list = new ArrayList<>();
        for (Rival r : m.other(me).rivals) list.add(r.toFencer());
        return list;
    }

    public Battle.Options optionsFor(Match m) {
        Battle.Options o = new Battle.Options();
        o.rewards = false;
        o.rated = false;
        o.title = getTitle() + " " + m.round + (m.bronze ? " (동메달 결정전)" : "");
        if (!team) {
            o.target = m.target;
            o.periods = m.pool ? 1 : 3;
        }
        return o;
    }

    // ---------- 결과 ----------

    public String getPlacementText() {
        switch (placement) {
            case 1: return "우승!";
            case 2: return "준우승";
            case 3: return region == Region.OLYMPIC ? "3위 (동메달)" : "공동 3위 (동메달)";
            case 4: return "4위";
            case 0: return "기권";
            case 99: return "예선 탈락";
            default: return placement + "강 탈락";
        }
    }

    /** 메달: 1 금, 2 은, 3 동, 없으면 0 */
    public int getMedal() { return placement >= 1 && placement <= 3 ? placement : 0; }

    public Entry getChampion() {
        if (!finished || rounds.isEmpty()) return null;
        Match fin = rounds.get(rounds.size() - 1).get(0);
        return fin.winner;
    }

    public Entry getRunnerUp() {
        Entry c = getChampion();
        if (c == null) return null;
        Match fin = rounds.get(rounds.size() - 1).get(0);
        return fin.winner == fin.a ? fin.b : fin.a;
    }

    /** 동메달 선수들 (올림픽은 결정전 승자 1명, 그 밖에는 준결승 패자 2명) */
    public List<Entry> getBronzeEntries() {
        List<Entry> list = new ArrayList<>();
        if (bronze != null) {
            if (bronze.played) list.add(bronze.winner);
        } else if (rounds.size() >= 2) {
            List<Match> semi = rounds.get(rounds.size() - 2);
            if (semi.size() == 2 && semi.get(0).played && semi.get(1).played) {
                list.add(loser(semi.get(0)));
                list.add(loser(semi.get(1)));
            }
        }
        return list;
    }

    private void addLog(String s) { log.add(s); }

    public String getTitle() {
        return region.getDisplayName() + " " + weapon.getDisplayName() + "부 " + (team ? "단체전" : "개인전");
    }

    public String getStageText() {
        if (finished) return "대회 종료: " + getPlacementText();
        if (stage == Stage.POOL) return "예선 풀 (5점 경기, 상위 2명 본선 진출)";
        if (rounds.isEmpty()) return "본선";
        String r = rounds.get(rounds.size() - 1).get(0).round;
        return "본선 토너먼트 - " + r + (meOut ? " (탈락, 남은 경기는 자동 진행)" : "");
    }

    public Region getRegion() { return region; }
    public WeaponType getWeapon() { return weapon; }
    public boolean isTeam() { return team; }
    public boolean isFinished() { return finished; }
    public boolean isMeOut() { return meOut; }
    public int getPlacement() { return placement; }
    public Stage getStage() { return stage; }
    public Entry getMe() { return me; }
    public List<Fencer> getMyFencers() { return me.players; }
    public List<Entry> getEntries() { return entries; }
    public List<List<Entry>> getPools() { return pools; }
    public List<List<Match>> getRounds() { return rounds; }
    public Match getBronze() { return bronze; }
    public List<String> getLog() { return log; }
    public int getStartDay() { return startDay; }

    /** 내가 속한 풀 (예선이 없으면 null) */
    public List<Entry> getMyPool() {
        for (List<Entry> p : pools) if (p.contains(me)) return p;
        return null;
    }

    /** 내 풀에서 치른/남은 경기 수 */
    public int[] getMyPoolProgress() {
        int done = 0, total = 0;
        for (Match m : poolMatches) {
            if (!m.involves(me)) continue;
            total++;
            if (m.played) done++;
        }
        return new int[]{done, total};
    }
}
