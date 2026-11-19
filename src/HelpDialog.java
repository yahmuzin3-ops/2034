import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 도움말 창. 처음 시작하기 / 경기 방법 / 대회와 시즌 / 파워 랭킹 / 코치·스태프 / 단축키.
 * 전술 표, 해금 조건, 스태프 효과는 게임 데이터에서 읽어서 실제 값과 항상 같다.
 */
public final class HelpDialog {
    public static final String START = "start", MATCH = "match", SEASON = "season", RATING = "rating", STAFF = "staff", KEYS = "keys";

    private HelpDialog() {
    }

    private static final String STYLE = "<style>body{font-family:'Malgun Gothic','Apple SD Gothic Neo',sans-serif;font-size:13px;color:#222;margin:10px}"
            + "h2{margin:0 0 6px 0;color:#1c2541}h3{margin:14px 0 4px 0;color:#3a4a8a}p{margin:4px 0}li{margin:3px 0}"
            + "table{border-collapse:collapse}td,th{border:1px solid #c8cde0;padding:4px 8px}th{background:#e8ecf8}.note{color:#667}</style>";

    /** 주제 이름 -> 탭 제목 (순서대로) */
    public static Map<String, String> topics() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(START, "처음 시작하기");
        m.put(MATCH, "경기 방법");
        m.put(SEASON, "대회와 시즌");
        m.put(RATING, "파워 랭킹");
        m.put(STAFF, "코치 · 스태프");
        m.put(KEYS, "단축키");
        return m;
    }

    /** 한 탭의 내용 (HTML) */
    public static String html(String topic) {
        StringBuilder sb = new StringBuilder("<html><head>").append(STYLE).append("</head><body>");
        switch (topic) {
            case MATCH: match(sb); break;
            case SEASON: season(sb); break;
            case RATING: rating(sb); break;
            case STAFF: staff(sb); break;
            case KEYS: keys(sb); break;
            default: start(sb);
        }
        return sb.append("</body></html>").toString();
    }

    private static void start(StringBuilder sb) {
        sb.append("<h2>Rise of Champions에 오신 걸 환영해요!</h2>");
        sb.append("<p>펜서를 키워서 클럽 대회 → 도 대회 → <b>자국 선수권</b> → <b>세계 선수권</b> → <b>올림픽</b>까지 도전하는 게임이에요.</p>");
        sb.append("<h3>처음 해볼 순서</h3><ol>");
        sb.append("<li><b>선수 관리</b>에서 선수의 능력치와 컨디션을 확인해요.</li>");
        sb.append("<li><b>훈련</b>에서 훈련을 시켜요. 시간이 걸리지만 능력치가 올라요.</li>");
        sb.append("<li><b>경기 · 대회</b>에서 클럽 대회를 고르고 선수를 선택한 뒤 '경기 시작'을 눌러요. 이기면 골드와 경험치를 받아요.</li>");
        sb.append("<li>번 골드로 <b>상점</b>에서 장비·식단·약을 사고, <b>코치 · 스태프</b>를 영입해요.</li>");
        sb.append("<li>승리를 쌓으면 더 큰 대회가 열려요. (자국 선수권 ").append(Region.NATIONAL.getRequiredWins()).append("승, 세계 선수권 ")
                .append(Region.WORLD.getRequiredWins()).append("승, 올림픽 ").append(Region.OLYMPIC.getRequiredWins()).append("승 + 파워 랭킹 8위 이내)</li>");
        sb.append("</ol>");
        sb.append("<h3>알아두면 좋아요</h3><ul>");
        sb.append("<li><b>홈 화면의 '오늘의 추천'</b>과 왼쪽 메뉴 옆의 <b>●</b>, <b>!</b> 표시가 지금 할 일을 알려줘요.</li>");
        sb.append("<li>시간은 ").append(GameEngine.DAY_MILLIS / 1000).append("초가 하루예요. 홈에서 '하루 넘기기'로 직접 넘길 수도 있고, 대회 중에는 시간이 멈춰요.</li>");
        sb.append("<li>선수들은 스스로 쉬고, 먹고, 연습해요. 선수 관리에서 선수별로 끌 수 있어요.</li>");
        sb.append("<li><b>저장</b>: 3분마다 자동 저장되고(autosave.dat), 왼쪽 '저장' 버튼으로 직접 저장도 해요. 다시 시작하면 가장 최근 저장을 이어서 할 수 있어요.</li>");
        sb.append("<li>화면 위쪽의 <b>도움말</b> 버튼(또는 F1)으로 언제든 이 창을 열 수 있어요.</li></ul>");
    }

    private static void match(StringBuilder sb) {
        sb.append("<h2>경기 방법</h2>");
        sb.append("<p><b>출전 선수 1명 = 개인전</b>: ").append(Battle.TARGET_SCORE).append("점을 먼저 따면 이겨요. 3분씩 3피리어드이고, 동점이면 서든데스(우선권 추첨 후 1분)로 가려요.</p>");
        sb.append("<p><b>2~3명 = 단체전 릴레이</b>: 경기당 3분, 5점씩 점수가 누적돼요. 3명이면 9경기 45점이에요.</p>");
        sb.append("<h3>매 구간 전술을 골라요</h3>");
        sb.append("<p>두 선수가 동시에 전술을 고르고 한꺼번에 공개해요. 상대의 성향을 읽고 맞는 전술을 고르는 게 핵심이에요.</p>");
        sb.append("<table><tr><th>전술</th><th>체력 소모</th><th>설명</th></tr>");
        for (Battle.Tactic t : Battle.Tactic.values()) {
            sb.append("<tr><td><b>").append(t.getDisplayName()).append("</b></td><td align='center'>").append(t.getCost()).append("</td><td>")
                    .append(t.getHint()).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<h3>종목별 특징</h3><ul>");
        sb.append("<li><b>플뢰레</b>: 몸통만 유효해요. 우선권이 있고, 방어 후 리포스트와 페인트가 강해요.</li>");
        sb.append("<li><b>에페</b>: 전신이 유효해요. 우선권이 없어서 동시에 맞으면 둘 다 득점(동시타)하고, 반격이 강해요.</li>");
        sb.append("<li><b>사브르</b>: 허리 위가 유효해요. 우선권이 있고 속공이 빠르지만 방어와 후퇴가 약해요.</li></ul>");
        sb.append("<h3>꼭 알아둘 것</h3><ul>");
        sb.append("<li>전술마다 <b>체력</b>을 써요. 체력이 낮으면 성공률이 떨어져요. 휴식 시간에만 아이템을 쓸 수 있어요.</li>");
        sb.append("<li>4번 연속 후퇴하면 후방 라인 이탈로 실점해요.</li>");
        sb.append("<li>경기 화면 위쪽 줄에 <b>상대의 성향</b>이 나와요. <b>전력 분석관</b>을 영입하면 더 자세히 알려줘요.</li>");
        sb.append("<li>경기가 길면 <b>자동 진행</b>, <b>빠른 진행</b>을 켜 보세요.</li></ul>");
        sb.append("<p class='note'>키보드: 1~6 전술 선택, Space(또는 Enter) 계속하기</p>");
    }

    private static void season(StringBuilder sb) {
        sb.append("<h2>대회와 시즌</h2>");
        sb.append("<p>1시즌은 ").append(SeasonCalendar.YEAR).append("일(10일씩 12달)이에요. 동네 클럽·도 대회는 언제든 열려 있고, 큰 대회는 <b>대회 기간</b>에만 나갈 수 있어요.</p>");
        sb.append("<table><tr><th>대회</th><th>열리는 때</th><th>출전 조건</th><th>방식</th></tr>");
        sb.append("<tr><td><b>자국 선수권</b></td><td>시즌마다 2번 (20일째, 80일째부터 8일간)</td><td>해당 종목 ").append(Region.NATIONAL.getRequiredWins()).append("승</td><td>예선 풀 → 본선 8강</td></tr>");
        sb.append("<tr><td><b>세계 선수권</b></td><td>시즌마다 1번 (50일째부터 10일간)</td><td>해당 종목 ").append(Region.WORLD.getRequiredWins()).append("승</td><td>예선 풀 → 본선 16강</td></tr>");
        sb.append("<tr><td><b>올림픽</b></td><td>4시즌에 한 번 (100일째부터 15일간)</td><td>").append(Region.OLYMPIC.getRequiredWins()).append("승 + 파워 랭킹 8위 이내</td><td>16강 + 동메달 결정전</td></tr></table>");
        sb.append("<h3>대회 진행</h3><ul>");
        sb.append("<li>한 대회에는 <b>종목마다 한 번</b>만 나갈 수 있어요.</li>");
        sb.append("<li>예선 풀은 5점 먼저 따는 경기예요. 풀 성적(승수 → 득실 차 → 득점)이 본선 시드가 돼요.</li>");
        sb.append("<li>내 경기를 <b>직접 하거나</b>, <b>시뮬레이션</b>하거나, <b>끝까지 시뮬레이션</b>할 수 있어요. 대회 중에는 시간이 멈춰요.</li>");
        sb.append("<li>메달을 따면 시상식이 열리고, 기록실의 트로피 룸에 걸려요. 한 번 만난 상대는 라이벌 전적에 남아요.</li>");
        sb.append("<li>소속 국가는 <b>파워 랭킹</b> 화면에서 바꿀 수 있어요. 나라마다 자국 선수권 난이도가 달라요.</li></ul>");
    }

    private static void rating(StringBuilder sb) {
        sb.append("<h2>파워 랭킹</h2>");
        sb.append("<p>종목별로 세계 선수 약 140명과 내 선수의 실력을 <b>파워 레이팅</b>으로 비교해요. (Elo 방식)</p>");
        sb.append("<ul><li>레이팅은 <b>능력치(장비 포함)</b>로 정해지는 기본 점수에 경기 결과 보정이 더해져요. 훈련하면 바로 올라요.</li>");
        sb.append("<li>이기면 오르고 지면 내려가요. 자기보다 센 상대를 이기면 더 많이 올라요.</li>");
        sb.append("<li>레이팅 차이로 이길 확률을 알 수 있어요. 예: 상대보다 100점 높으면 이길 확률이 약 64%예요.</li>");
        sb.append("<li>컴퓨터 선수도 훈련하고 경기해서 조금씩 변해요. ▲▼ 는 최근 1~2주 변동이에요.</li></ul>");
        sb.append("<h3>등급</h3><p>입문 → 아마추어 → 유망주 → 프로 → 엘리트 → 월드클래스 → 전설</p>");
    }

    private static void staff(StringBuilder sb) {
        sb.append("<h2>코치 · 스태프</h2>");
        sb.append("<p>역할마다 한 명씩 고용해요. 레벨(1~").append(Staff.MAX_LEVEL).append(")이 높을수록 효과가 크고 주급도 높아요. 주급은 ").append(Staff.PAY_DAYS)
                .append("일마다 나가고, 골드가 모자라면 주급이 높은 스태프부터 떠나요.</p>");
        sb.append("<table><tr><th>역할</th><th>Lv.1 효과</th><th>Lv.").append(Staff.MAX_LEVEL).append(" 효과</th><th>주급 (Lv.1 ~ Lv.").append(Staff.MAX_LEVEL).append(")</th></tr>");
        for (Staff.Role r : Staff.Role.values()) {
            sb.append("<tr><td><b>").append(r.getDisplayName()).append("</b></td><td>").append(Staff.effectText(r, 1)).append("</td><td>")
                    .append(Staff.effectText(r, Staff.MAX_LEVEL)).append("</td><td>").append(Staff.weeklySalary(1)).append("G ~ ").append(Staff.weeklySalary(Staff.MAX_LEVEL)).append("G</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p class='note'>영입 후보는 14일마다 바뀌어요. 영입 비용은 주급의 3배이고, 승급은 영입보다 저렴해요.</p>");
    }

    private static void keys(StringBuilder sb) {
        sb.append("<h2>단축키</h2><table><tr><th>키</th><th>하는 일</th></tr>");
        sb.append("<tr><td>F1</td><td>도움말 열기</td></tr>");
        sb.append("<tr><td>Ctrl + S</td><td>저장</td></tr>");
        sb.append("<tr><td>Alt + 1 ~ 9</td><td>왼쪽 메뉴의 1번~9번 화면으로 이동</td></tr>");
        sb.append("<tr><td>1 ~ 6</td><td>경기 중 전술 선택 (공격, 페인트, 방어·리포스트, 반격, 전진, 후퇴 순서)</td></tr>");
        sb.append("<tr><td>Space / Enter</td><td>경기 중 '계속' 누르기</td></tr></table>");
        sb.append("<p class='note'>왼쪽 메뉴 버튼에 마우스를 올리면 단축키가 나와요.</p>");
    }

    /** 모든 탭이 들어 있는 도움말 내용 (창 없이도 만들 수 있어서 테스트하기 쉽다) */
    public static JTabbedPane content(String selectedTopic) {
        JTabbedPane tabs = new JTabbedPane();
        int sel = 0, i = 0;
        for (Map.Entry<String, String> t : topics().entrySet()) {
            JEditorPane pane = new JEditorPane();
            pane.setEditorKit(new HTMLEditorKit());
            pane.setEditable(false);
            pane.setText(html(t.getKey()));
            pane.setCaretPosition(0);
            JScrollPane sp = new JScrollPane(pane);
            sp.setBorder(null);
            tabs.addTab(t.getValue(), sp);
            if (t.getKey().equals(selectedTopic)) sel = i;
            i++;
        }
        tabs.setSelectedIndex(sel);
        return tabs;
    }

    /** 도움말 창을 연다. */
    public static void show(Component parent, String topic) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, "도움말", Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout(0, 8));
        d.add(content(topic), BorderLayout.CENTER);
        JButton close = UI.button("닫기");
        close.addActionListener(e -> d.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        south.add(close);
        d.add(south, BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(close);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setSize(780, 580);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }
}
