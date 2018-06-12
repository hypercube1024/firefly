package com.firefly.utils.math;

/**
 * @author Pengtao Qiu
 */
abstract public class Viterbi {
    /**
     * Compute HMM hidden states
     *
     * @param obs       observations
     * @param states    hidden states
     * @param startProb start probability of the hidden state
     * @param transProb transition probability of the hidden state
     * @param emitProb  emission probability
     * @return The most possible hidden states
     */
    public static int[] compute(int[] obs, int[] states, double[] startProb, double[][] transProb, double[][] emitProb) {
        double[][] v = new double[obs.length][states.length];
        int[][] path = new int[states.length][obs.length];

        for (int y : states) {
            v[0][y] = startProb[y] * emitProb[y][obs[0]];
            path[y][0] = y;
        }

        for (int t = 1; t < obs.length; ++t) {
            int[][] newPath = new int[states.length][obs.length];

            for (int y : states) {
                double prob = -1;
                int state;
                for (int y0 : states) {
                    double newProb = v[t - 1][y0] * transProb[y0][y] * emitProb[y][obs[t]];
                    if (newProb > prob) {
                        prob = newProb;
                        state = y0;
                        // save the max probability
                        v[t][y] = prob;
                        // save the path
                        System.arraycopy(path[state], 0, newPath[y], 0, t);
                        newPath[y][t] = y;
                    }
                }
            }

            path = newPath;
        }

        double prob = -1;
        int state = 0;
        for (int y : states) {
            if (v[obs.length - 1][y] > prob) {
                prob = v[obs.length - 1][y];
                state = y;
            }
        }

        return path[state];
    }
}
