package com.fireflysource.common.math;

public class MathUtils {
    private MathUtils() {
    }

    /**
     * Returns whether the sum of the arguments overflows an {@code int}.
     *
     * @param a the first value
     * @param b the second value
     * @return whether the sum of the arguments overflows an {@code int}
     */
    public static boolean sumOverflows(int a, int b) {
        try {
            Math.addExact(a, b);
            return false;
        } catch (ArithmeticException x) {
            return true;
        }
    }

    /**
     * Returns the sum of its arguments, capping to {@link Long#MAX_VALUE} if they overflow.
     *
     * @param a the first value
     * @param b the second value
     * @return the sum of the values, capped to {@link Long#MAX_VALUE}
     */
    public static long cappedAdd(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException x) {
            return Long.MAX_VALUE;
        }
    }
}
