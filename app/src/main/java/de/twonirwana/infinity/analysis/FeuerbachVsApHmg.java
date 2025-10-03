package de.twonirwana.infinity.analysis;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class FeuerbachVsApHmg {
    private static final Random rand = new Random();
    private static final int SAMPLE_NUMBER = 100_000;

    public static void main(String[] args) {
        int bs = 13;
        IntStream.range(8, 20).forEach(i ->
                System.out.printf("dodge: %d Feuerbach DA +1B: %s Feuerbach EX +1B: %s AP HMG: %s%n", i, averageHitDiceFeuerbachDA(bs, i), averageHitDiceFeuerbachEx(bs, i), averageHitDiceHMG(bs, i))
        );

        IntStream.range(8, 20).forEach(i ->
                System.out.printf("dodge: %d HMG -3: %s Heavy Pistol +3: %s%n", i, averageHitDice(bs - 3, i, 4, 1), averageHitDice(bs + 3, i, 2, 1))
        );
    }

    private static int rollD20() {
        return rand.nextInt(1, 21);
    }

    private static boolean savingRollSucceed(int value) {
        return rollD20() <= value;
    }

    private static boolean isCrit(int result, int value) {
        return result == value;
    }

    private static double averageHitDiceFeuerbachDA(int bs, int dodge) {
        return averageHitDice(bs, dodge, 3, 2);
    }

    private static double averageHitDiceFeuerbachEx(int bs, int dodge) {
        return averageHitDice(bs, dodge, 2, 1);
    }

    private static double averageHitDice(int bs, int dodge, int burst, int hitDice) {
        return IntStream.range(0, SAMPLE_NUMBER)
                .mapToDouble(i -> hitDice(bs, dodge, burst, hitDice)).average().orElseThrow();
    }

    private static int hitDice(int bs, int dodge, int burst, int hitDice) {
        int dodgeResult = rollD20();
        boolean dodgeSuccess = dodgeResult <= dodge;
        boolean isDodgeCrit = isCrit(dodgeResult, dodge);
        if (isDodgeCrit) {
            return 0;
        }
        List<Integer> bsRoll = IntStream.range(0, burst).map(r -> rollD20()).boxed().toList();
        List<Integer> bsNonCritSuccess = bsRoll.stream()
                .filter(s -> !isCrit(s, bs))
                .filter(s -> s < bs)
                .filter(s -> (s > dodgeResult) || !dodgeSuccess)
                .toList();
        List<Integer> bsCrits = bsRoll.stream()
                .filter(s -> isCrit(s, bs))
                .toList();
        return (bsCrits.size() * 2) + bsNonCritSuccess.size() * hitDice;
    }

    private static double averageHitDiceHMG(int bs, int dodge) {
        return averageHitDice(bs, dodge, 4, 1);
    }
}
