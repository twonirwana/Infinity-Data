package de.twonirwana.infinity.analysis;

import java.util.stream.IntStream;

public class SmokeDodge {

    public static void main(String[] args) {
        //Daturazi Smoke vs Daylami
        double daturaziGetHit = IntStream.range(0, 1_000_000)
                .boxed()
                .map(i -> F2fUtil.roll(1, 0, 12, 1, 0, 8))
                .mapToDouble(f -> {
                    if (f.isTrooper1Hit()) {
                        return 1;
                    }
                    return 0;
                })
                .average().orElseThrow();
        System.out.println("Dat vs single daylami: " + daturaziGetHit);
        double daturaziGetHitFt = IntStream.range(0, 1_000_000)
                .boxed()
                .map(i -> F2fUtil.roll(1, 0, 14, 1, 1, 8))
                .mapToDouble(f -> {
                    if (f.isTrooper1Hit()) {
                        return 1;
                    }
                    return 0;
                })
                .average().orElseThrow();
        System.out.println("Dat vs single fireteam daylami: " + daturaziGetHitFt);

        //Daturazi Smoke Grenad Launcher vs Daylami
        double daturaziGetHit2 = IntStream.range(0, 1_000_000)
                .boxed()
                .map(i -> F2fUtil.roll(1, 1, 11, 1, 1, 8))
                .mapToDouble(f -> {
                    if (f.isTrooper1Hit()) {
                        return 1;
                    }
                    return 0;
                })
                .average().orElseThrow();
        System.out.println(daturaziGetHit2);
    }


}
