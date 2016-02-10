package edu.umass.cs.automan.core.policy.aggregation;

import edu.umass.cs.automan.core.logging.DebugLog;
import edu.umass.cs.automan.core.logging.LogLevelInfo;
import edu.umass.cs.automan.core.logging.LogType;
import java.util.Random;

/**
 * Goal of simulation: to calculate how many trials are
 * needed to confidently reject the null hypothesis that
 * the most popular outcome is due to random chance.
 *
 * This calculation works as follows:
 * A "simulation" is an experiment where a "choices"-sided die
 * is rolled "trials" times and the number of votes for the
 * most common outcome is recorded.  The entire simulation procedure
 * is repeated "num_simulations" times.
 *
 * Finally, a histogram of simulation outcomes is created
 * and, starting from the smallest number of trials, the
 * probability of random agreement is added to the odds
 * until the probability is >= confidence.  The number
 * of trials needed to reach this point is then returned.
 *
 * Since the number of trials is not known a priori, the
 * expected way to call minToWin is in a loop,
 * where "trials" is incremented until the function does
 * not return -1.
 */
public final class AgreementSimulation {
    private AgreementSimulation() {
        throw new AssertionError();
    }

    /**
     * Compute the minimum number of trials that need to
     * agree in order to win a popularity contest with at
     * least conf probability.
     * @param c The number of choices (sides of the die)
     * @param t The total number of trials (rolls of the die)
     * @param conf The confidence level
     * @param numSims The number of simulations to run
     * @return The minimum agreement threshold
     */
    public static int minToWin(int c, int t, double conf, int numSims) {
        Random r = new Random();

        // object to track frequency of outcomes
        ChoiceFreq freq = new ChoiceFreq(t);

        // Spin up a bunch of runs.
        for (int i = 0; i < numSims; i++) {
            simulation(c, t, freq, r);
        }

        // Calculate and return minimum number of trials
        return tailThreshold(t, 1.0 - conf, freq);
    }

    /**
     * Compute the confidence in a given outcome where n of t
     * trials agree on the same answer.
     * @param c The number of choices (sides of the die)
     * @param t The total number of trials (rolls of the die)
     * @param n The number of trials that agree
     * @param numSims The number of simulations to run.
     * @return The confidence.
     */
    public static double confidenceOfOutcome(int c, int t, int n, int numSims) {
        Random r = new Random();

        // array to track max result of each test
        ChoiceFreq o = new ChoiceFreq(t);

        // Spin up a bunch of runs.
        for (int i = 0; i < numSims; i++) {
            simulation(c, t, o, r);
        }

        // the confidence is just 1 - Pr[n or more trials agree]
        return 1.0 - tailProbability(t, n, o);
    }

    /**
     * Run a die-roll experiment.
     * @param c The number of sides of the die.
     * @param t The number of times to roll the die.
     * @param freq The object to store the outcome in.
     * @param r A random number generator.
     */
    private static void simulation(int c, int t, ChoiceFreq freq, Random r) {
        int[] choice = new int[t];
        // {[0..choices-1], [0..choices-1], ...} // # = trials

        // make a choice for every trial
        for (int j = 0; j < t; j++) {
            choice[j] = Math.abs(r.nextInt(Integer.MAX_VALUE)) % c;
        }

        // Make a histogram, adding up occurrences of each choice.
        int[] histogram = new int[c];
        // initialize array
        for (int k = 0; k < c; k++) {
            histogram[k] = 0;
        }
        // sum up choices
        for (int z = 0; z < t; z++) {
            histogram[choice[z]] += 1;
        }

        // Find the most popular choice
        int max = 0;
        for (int k = 0; k < c; k++) {
            if (histogram[k] > max) {
                max = histogram[k];
            }
        }

        // record the number of votes gotten by the most popular choice
        assert (max <= t);
        freq.recordOutcome(max);
    }

    /**
     * Compute the right tail probability for the distribution of the
     * number of t needed to win the popularity contest.
     * @param t The total number of trials.
     * @param n An agreement threshold that defines the right tail.
     * @param freq The outcome for a set of simulations.
     * @return
     */
    private static double tailProbability(int t, int n, ChoiceFreq freq) {
        int i = t;
        double pr = 0.0;
        while((i >= n)) {
            double pr_i = freq.countForOutcome(i) / (double) freq.numSimulations();
            pr += pr_i;
            i--;
        }
        return pr;
    }

    /**
     * Compute the minimum number of votes needed to make winning the
     * popularity contest likely (with confidence greater than 1 - alpha).
     * @param t Number of trials
     * @param alpha 1 - confidence
     * @param freq The outcome for a set of simulations.
     * @return The minimal number of trials needed.
     */
    private static int tailThreshold(int t, double alpha, ChoiceFreq freq) {
        int i = 1;
        double pr = 1.0;
        while ((i <= t) && (pr > alpha)) {
            double pr_i = freq.countForOutcome(i) / (double) freq.numSimulations();
            pr -= pr_i;
            i++;
        }

        if ((i <= t) && (pr <= alpha)) {
            String msg =
                    String.format(
                            "MONTECARLO: %s identical answers required for %s tasks",
                            Integer.toString(i),
                            Integer.toString(t)
                    );
            DebugLog.apply(msg, LogLevelInfo.apply(), LogType.STRATEGY(), null);
            return i;
            // Otherwise
        } else {
            // Error condition: not enough trials to achieve the desired confidence.
            return -1;
        }
    }
}
