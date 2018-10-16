package com.firefly.utils;

abstract public class RandomUtils {
    public static final String ALL_CHAR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Generates a random number from a specified range
     *
     * @param min The minimal number of the range
     * @param max The maximal number of the range
     * @return A random number from minimal number to maximal number, which contains minimal and maximal number.
     */
    public static long random(long min, long max) {
        return Math.round(ThreadLocalRandom.current().nextDouble()
                * (max - min) + min);
    }

    /**
     * Returns a index of a specified probability, e.g. the string is "100:1:32:200:16:30".
     * If it returns 0 that probability is 100/(100+1+32+200+16+30)
     *
     * @param conf Configures specified probability
     * @return The index of a specified probability
     */
    public static int randomSegment(String conf) {
        String[] tmp = StringUtils.split(conf, ":");
        int[] probability = new int[tmp.length];
        for (int i = 0; i < probability.length; i++)
            probability[i] = Integer.parseInt(tmp[i].trim());

        return randomSegment(probability);
    }

    /**
     * Returns the index of array that specifies probability.
     *
     * @param probability The element of array represents the probability.
     * @return The index of array.
     */
    public static int randomSegment(int[] probability) {
        int total = 0;
        for (int i = 0; i < probability.length; i++) {
            total += probability[i];
            probability[i] = total;
        }
        int rand = (int) random(0, total - 1);
        for (int i = 0; i < probability.length; i++) {
            if (rand < probability[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns a random string.
     *
     * @param length The random string's length
     * @return A random string.
     */
    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) random(0, ALL_CHAR.length() - 1);
            sb.append(ALL_CHAR.charAt(index));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String conf = "100:1:32:200:16:30";
        System.out.println(randomSegment(conf));

        System.out.println(random(0, 5));
        System.out.println(randomString(16));
    }
}
