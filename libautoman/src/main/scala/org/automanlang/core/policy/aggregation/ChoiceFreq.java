package org.automanlang.core.policy.aggregation;

class ChoiceFreq {
    private int[] outcomes;
    public ChoiceFreq(int totalTrials) {
        outcomes = new int[totalTrials];
    }
    public void recordOutcome(int maxAgree) {
        // max agreement of 0 is impossible;
        // shift everything over by 1
        outcomes[maxAgree - 1]++;
    }
    public int countForOutcome(int maxAgree) {
        // max agreement of 0 is impossible;
        // shift everything over by 1
        return outcomes[maxAgree - 1];
    }
    public int numSimulations() {
        int total = 0;
        for (int i = 0; i < outcomes.length; i++) {
            total += outcomes[i];
        }
        return total;
    }
}