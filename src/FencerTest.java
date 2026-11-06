public class FencerTest {
    public static void main(String[] args) {
        Fencer f = new Fencer("그룹", WeaponType.EPEE, Personality.PASSIONATE);
        System.out.println("생성: " + f);

        // 컨디션 좋을 때 훈련 (풋워크 훈련: 민첩 +3, 공격 +1, 피로 +15)
        double eff = f.applyTraining(1, 10, 3, 0, 10);
        System.out.printf("훈련1 효율 %.2f -> %s%n", eff, f);

        // 피로를 쌓아서 훈련 효율이 떨어지는지 확인
        for (int i = 0; i < 4; i++) {
            eff = f.applyTraining(1, 20, 40, 2, 15);
        }
        System.out.printf("훈련 5회 후 효율 %.2f -> %s%n", eff, f);

        // 휴식 / 식사로 회복
        f.rest();
        f.eat(30, 5);
        System.out.println("휴식+식사: " + f);

        // 경험치로 레벨업
        System.out.println("레벨업? " + f.gainExp(200));
        System.out.println("성장 후: " + f);

        // 부상
        f.injure();
        System.out.printf("부상 시 훈련 효율 %.2f%n", f.getTrainingEfficiency());

        // 상성 확인
        System.out.println("사브르 vs 에페: " + WeaponType.SABRE.matchupAgainst(WeaponType.EPEE));
        System.out.println("사브르 vs 플뢰레: " + WeaponType.SABRE.matchupAgainst(WeaponType.FOIL));
    }
}
