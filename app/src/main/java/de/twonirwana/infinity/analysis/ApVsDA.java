package de.twonirwana.infinity.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.stream.IntStream;

public class ApVsDA {
    private static final Random rand = new Random();

    public static void main(String[] args) {
        IntStream.range(1, 13).forEach(i ->
                System.out.printf("armor: %d AP: %s DA: %s K1: %s %n", i,
                        averageWoundsAP(5, i), //multi sniper ap
                        averageWoundsDA(5, i), //multi sniper dp
                        averageWoundsK1(7)) //k1 sniper
        );
    }

    private static int rollD20() {
        return rand.nextInt(1, 21);
    }

    private static boolean savingRollSucceed(int ps, int armor) {
        return rollD20() <= (armor + ps);
    }

    private static double averageWoundsAP(int ps, int armor) {
        int halfArmor = BigDecimal.valueOf(armor).divide(BigDecimal.TWO, RoundingMode.UP).intValue();
        return IntStream.range(0, 1_000_000)
                .mapToDouble(i -> {
                    if (savingRollSucceed(ps, halfArmor)) {
                        return 0;
                    }
                    return 1;
                }).average().orElseThrow();
    }

    private static double averageWoundsK1(int ps) {
        return IntStream.range(0, 1_000_000)
                .mapToDouble(i -> {
                    if (savingRollSucceed(ps, 0)) {
                        return 0;
                    }
                    return 1;
                }).average().orElseThrow();
    }

    private static double averageWoundsDA(int ps, int armor) {
        return IntStream.range(0, 100_000)
                .mapToDouble(i -> {
                    int w = 0;
                    if (!savingRollSucceed(ps, armor)) {
                        w++;
                    }
                    if (!savingRollSucceed(ps, armor)) {
                        w++;
                    }
                    return w;
                }).average().orElseThrow();
    }
}
