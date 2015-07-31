package edu.umass.cs.automan.core.policy.validation;

import java.util.Random;

public final class MonteCarlo {
    private MonteCarlo() {
        throw new AssertionError();
    }

    private static class Occurrences {
        public int[] occurrences;
        public Occurrences(int size) {
            occurrences = new int[size];
        }
    }

    public static int requiredForAgreement(int choices, int trials, double confidence, int iterations) {
        // array to track max result of each test
        Occurrences o = new Occurrences(trials + 1);

        // Spin up a bunch of runs.
        for (int i = 0; i < iterations; i++) {
            iteration(choices, trials, o);
        }

        // Calculate and return minimum number of trials
        return calculate_min_agreement(trials, confidence, iterations, o);
    }

    public static double confidenceOfOutcome(int choices, int trials, int max_agree, int iterations) {
        // array to track max result of each test
        Occurrences o = new Occurrences(trials + 1);

        // Spin up a bunch of runs.
        for (int i = 0; i < iterations; i++) {
            iteration(choices, trials, o);
        }

        // Calculate and return odds
        return calculate_odds(trials, max_agree, iterations, o);
    }

    private static void iteration(int choices, int trials, Occurrences o) {
        Random r = new Random();
        int[] choice = new int[trials];
        // {[0..choices-1], [0..choices-1], ...} // # = trials

        // make a choice for every trial
        for (int j = 0; j < trials; j++) {
            choice[j] = Math.abs(r.nextInt(Integer.MAX_VALUE)) % choices;
        }

        // Make a histogram, adding up occurrences of each choice.
        int[] counter = new int[choices];
        for (int k = 0; k < choices; k++) {
            counter[k] = 0;
        }
        for (int z = 0; z < trials; z++) {
            counter[choice[z]] += 1;
        }

        // Find the biggest choice
        int max = 0;
        for (int k = 0; k < choices; k++) {
            if (counter[k] > max) {
                max = counter[k];
            }
        }

        // Return the number of votes that the biggest choice got
        assert (max <= trials);
        o.occurrences[max]++;
    }

    private static int calculate_min_agreement(int trials, double confidence, int iterations, Occurrences o) {
        // Determine the number of trials in order for the odds
        // to drop below alpha (i.e., 1 - confidence).
        // This is done by subtracting the area under the histogram for each trial
        // from 1.0 until the answer is less than alpha.
        int i = 1;
        double odds = 1.0;
        double alpha = 1.0 - confidence;
        while ((i <= trials) && (odds > alpha)) {
            double odds_i = o.occurrences[i] / (double) iterations;
            odds -= odds_i;
            i++;
        }

        // If we found an answer, then return # of trials
        if ((i <= trials) && (odds <= alpha)) {
            String msg = String.format("MONTECARLO: %s identical answers required for %s tasks", Integer.toString(i), Integer.toString(trials));
            return i;
            // Otherwise
        } else {
            // Error condition: not enough trials to achieve the desired confidence.
            return -1;
        }
    }

    private static double calculate_odds(int trials, int max_agree, int runs, Occurrences o) {
        int i = trials;
        double odds = 0.0;
        while((i >= max_agree)) {
            double odds_i = o.occurrences[i] / (double) runs;
            odds += odds_i;
            i--;
        }
        return 1 - odds;
    }
}
