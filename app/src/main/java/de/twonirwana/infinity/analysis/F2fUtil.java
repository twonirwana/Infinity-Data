package de.twonirwana.infinity.analysis;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class F2fUtil {

    private static final Random rand = new Random();

    public static F2fResult roll(int player1DiceNumber, int player1SpecialDie, int player1Skill,
                                 int player2DiceNumber, int player2SpecialDie, int player2Skill) {
        int player1TotalDice = player1DiceNumber + player1SpecialDie;
        int player2TotalDice = player2DiceNumber + player2SpecialDie;
        List<Integer> roll1 = rollOnePlayer(player1TotalDice, player1Skill);
        List<Integer> roll2 = rollOnePlayer(player2TotalDice, player2Skill);
        if (roll1.isEmpty() && roll2.isEmpty()) {
            return new F2fResult(0, 0, 0, 0);
        }
        if (hasCrit(roll1, player1Skill) && hasCrit(roll2, player2Skill)) {
            return new F2fResult(0, 0, 0, 0);
        }
        if (hasCrit(roll1, player1Skill) && !hasCrit(roll2, player2Skill)) {
            return removeSpecialDice(new F2fResult(successWithoutCrit(roll1, player1Skill, roll2), critNumber(roll1, player1Skill), 0, 0), player1SpecialDie, player1TotalDice, player2SpecialDie, player2TotalDice);
        }
        if (!hasCrit(roll1, player1Skill) && hasCrit(roll2, player2Skill)) {
            return removeSpecialDice(new F2fResult(0, 0, successWithoutCrit(roll2, player2Skill, roll1), critNumber(roll2, player2Skill)), player1SpecialDie, player1TotalDice, player2SpecialDie, player2TotalDice);
        }
        if (getMax(roll1) == getMax(roll2)) {
            return new F2fResult(0, 0, 0, 0);
        }
        int player1HigherRolls = numberOfHigherRolls(roll1, roll2);
        int player2HigherRolls = numberOfHigherRolls(roll2, roll1);

        return removeSpecialDice(new F2fResult(player1HigherRolls, 0, player2HigherRolls, 0), player1SpecialDie, player1TotalDice, player2SpecialDie, player2TotalDice);
    }

    private static F2fResult removeSpecialDice(F2fResult result, int player1SpecialDice, int player1TotalDice, int player2SpecialDice, int player2TotalDice) {
        return new F2fResult(removeMultiSpecialDice(result.trooper1(), player1SpecialDice, player1TotalDice), removeMultiSpecialDice(result.trooper2(), player2SpecialDice, player2TotalDice));
    }

    private static Result removeMultiSpecialDice(Result result, int numberOfSpecialDice, int totalNumberOfDice) {
        Result updatedResult = result;
        int missedDice = totalNumberOfDice - result.success() - result.crits();
        int specialDiceNumberWithoutMissedDice = Math.max(0, numberOfSpecialDice - missedDice);
        for (int i = 0; i < specialDiceNumberWithoutMissedDice; i++) {
            updatedResult = removeSpecialDice(result);
        }
        return updatedResult;
    }

    private static Result removeSpecialDice(Result result) {
        if (result.success() == 0 && result.crits() == 0) {
            return result;
        }
        if (result.success() > 0) {
            return new Result(result.success() - 1, result.crits());
        }
        return new Result(0, result.crits() - 1);

    }


    private static int getMax(List<Integer> rolls) {
        return rolls.stream().max(Integer::compareTo).orElse(0);
    }

    private static List<Integer> rollOnePlayer(int diceNumber, int skill) {
        return IntStream.range(0, diceNumber)
                .boxed()
                .map(x -> rollD20())
                .filter(r -> r <= skill)
                .toList();
    }

    private static int rollD20() {
        return rand.nextInt(1, 21);
    }


    private static boolean hasCrit(List<Integer> roll, int skill) {
        return roll.stream().anyMatch(x -> isCrit(x, skill));
    }

    private static boolean isCrit(int roll, int skill) {
        if (skill > 20) {
            return roll == 20 || roll <= (skill - 20);
        }
        return roll == skill;
    }

    private static int numberOfHigherRolls(List<Integer> rollsPlayer1, List<Integer> rollsPlayer2) {
        return (int) rollsPlayer1.stream()
                .filter(r -> rollsPlayer2.stream().allMatch(r2 -> r > r2))
                .count();
    }

    private static int successWithoutCrit(List<Integer> roll, int skill, List<Integer> otherPlayerRoll) {
        List<Integer> player1WithoutCrit = roll.stream()
                .filter(r -> !isCrit(r, skill))
                .toList();
        return numberOfHigherRolls(player1WithoutCrit, otherPlayerRoll);
    }

    private static int critNumber(List<Integer> roll, int skill) {
        return (int) roll.stream()
                .filter(r -> isCrit(r, skill))
                .count();
    }

    public record F2fResult(Result trooper1, Result trooper2) {
        public F2fResult(int success1, int crits1, int success2, int crits2) {
            this(new Result(success1, crits1), new Result(success2, crits2));
        }

        boolean isTrooper1Hit() {
            return trooper2().success() > 0 || trooper2().crits() > 0;
        }
    }

    public record Result(int success, int crits) {
    }

}
