package de.twonirwana.infinity.analysis;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class HeavyPistolVsChainRifle {
    private static final Random rand = new Random();

    public static void main(String[] args) {
        int bs = 12;
        IntStream.range(1, 20).forEach(i ->
                System.out.println("dodge: " + i + " Heavy Pistol +1B: " + averageHitDiceHeavyPistol(bs, i) + " Chain Rifle +2B: " + averageHitDiceChainRifle(i))
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

    private static double averageHitDiceHeavyPistol(int bs, int dodge) {
        return IntStream.range(0, 1_000_000)
                .mapToDouble(i -> {
                    int bsRange = bs + 3;
                    int dodgeResult = rollD20();
                    boolean dodgeSuccess = dodgeResult <= dodge;
                    boolean isDodgeCrit = isCrit(dodgeResult, dodge);
                    if (isDodgeCrit) {
                        return 0;
                    }
                    List<Integer> bsRoll = IntStream.range(0, 3).map(r -> rollD20()).boxed().toList();
                    List<Integer> bsNonCritSuccess = bsRoll.stream()
                            .filter(s -> !isCrit(s, bsRange))
                            .filter(s -> s < bsRange)
                            .filter(s -> (s > dodgeResult) || !dodgeSuccess )
                            .toList();
                    List<Integer> bsCrits = bsRoll.stream()
                            .filter(s -> isCrit(s, bsRange))
                            .toList();
                    return (bsCrits.size() * 2) + bsNonCritSuccess.size();
                }).average().orElseThrow();
    }

    private static double averageHitDiceChainRifle(int dodge) {
        return IntStream.range(0, 100_000)
                .mapToDouble(i -> {
                    if (savingRollSucceed(dodge)) {
                        return 0;
                    }
                    return 3;
                }).average().orElseThrow();
    }
}
