package com.egr.snookerrank.bl;

import com.egr.snookerrank.dto.CorrectScoreDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class SnookerStats {

    // Method to calculate the chance to win
    public double chanceToWin(double playerFdi, double opponentFdi, String legOrSet, int firstTo) {
        int bestOfLegs = (firstTo * 2) - 1;
        double chanceOfOneSetOrLeg = chanceOfOneSetOrLeg(playerFdi, opponentFdi, legOrSet);
        return 1 - binomialDist(((bestOfLegs - 1) / 2), bestOfLegs, chanceOfOneSetOrLeg, true);
    }

    // Method to calculate the chance of winning one set or leg
    public double chanceOfOneSetOrLeg(double playerFdi, double opponentFdi, String legOrSet) {
        double diff = playerFdi - opponentFdi;
        double chanceOfOneLeg = 1 - (1 / (1 + Math.pow(10, diff / 800)));

        if ("S".equalsIgnoreCase(legOrSet)) {
            return 1 - binomialDist(2, 5, chanceOfOneLeg, true); // Set case
        } else {
            return chanceOfOneLeg; // Leg case
        }
    }

    // Method to calculate the binomial distribution (Cumulative or not)
    public double binomialDist(int successes, int trials, double chanceOfOne, boolean cumulative) {
        double result = 0;

        if (cumulative) {
            for (int i = 0; i <= successes; i++) {
                result += binomialDist(i, trials, chanceOfOne, false);
            }
        } else {
            result = combin(trials, successes) * Math.pow(chanceOfOne, successes) * Math.pow((1 - chanceOfOne), trials - successes);
        }

        return result;
    }

    // Method to calculate combinations (n choose k)
    public double combin(double choices, double choose) {
        if (choose == 0 || choices == choose) {
            return 1;
        } else {
            return factorial(choices) / (factorial(choices - choose) * factorial(choose));
        }
    }

    // Method to calculate factorial
    public double factorial(double num) {
        double result = 1;
        for (int i = 1; i <= num; i++) {
            result *= i;
        }
        return result;
    }

    // Main function to test the methods
    public static void main(String[] args) {
        SnookerStats stats = new SnookerStats();

        double playerFdi = 3000.0;
        int opponentFdi = 2500;
        String legOrSet = "L"; // L for Leg, S for Set
        int firstTo = 7;

        double chanceToWin = stats.chanceToWin(playerFdi, opponentFdi, legOrSet, firstTo);
        System.out.println("Chance to Win: " + chanceToWin);
    }

    public List<CorrectScoreDTO> calculateCorrectScores(int lFirstTo, Double player1FDI, Double player2FDI) {
        int lPlayer1Score = lFirstTo;
        int lPlayer2Score = 0;
        int lRow = 1; // Starting row
        List<CorrectScoreDTO> resultTable = new ArrayList<>();

        while (true) {
            // Calculate nChance
            double nChance;

                nChance = chanceOfOneSetOrLeg(player1FDI, player2FDI, "L");


            // Calculate binomial distribution for the chance
            nChance = binomialDist(lPlayer1Score, lPlayer1Score + lPlayer2Score, nChance, false) * (lFirstTo / (double)(lPlayer1Score + lPlayer2Score));

            // Format the output as percentage and other values
            DecimalFormat df = new DecimalFormat("0.00%");
            String formattedChance = df.format(nChance);

            df = new DecimalFormat("0.00");
            String inverseChance = df.format(1 / nChance);

            // Collect data in tabular form (as a DTO)
            CorrectScoreDTO row = new CorrectScoreDTO(lPlayer1Score + "-" + lPlayer2Score, formattedChance, inverseChance);
            resultTable.add(row); // Add the row to the result table

            // Increment row for next simulation
            lRow++;

            // Adjust player scores based on the conditions in the loop
            if (lPlayer2Score < lFirstTo) {
                lPlayer2Score++;
                if (lPlayer2Score == lFirstTo) {
                    lPlayer1Score--;
                }
            } else {
                lPlayer1Score--;
            }

            if (lPlayer1Score < 0) {
                break; // Exit loop if Player 1 score is less than 0
            }
        }

        return resultTable;
    }
}
