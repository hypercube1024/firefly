package com.firefly.utils.lang;

/**
 * @author Pengtao Qiu
 */
public class JDK {
    /**
     * True if JDK is 1.5 (or newer)
     */
    public static final boolean IS_5 = isJavaVersionAtLeast(1, 5);
    /**
     * True if JDK is 1.6 (or newer)
     */
    public static final boolean IS_6 = isJavaVersionAtLeast(1, 6);
    /**
     * True if JDK is 1.7 (or newer)
     */
    public static final boolean IS_7 = isJavaVersionAtLeast(1, 7);
    /**
     * True if JDK is 1.8 (or newer)
     */
    public static final boolean IS_8 = isJavaVersionAtLeast(1, 8);
    /**
     * True if JDK is 9.0 (or newer)
     */
    public static final boolean IS_9 = isJavaVersionAtLeast(9, 0);

    private static boolean isJavaVersionAtLeast(int maj, int min) {
        String jver = System.getProperty("java.version");
        if (jver == null) {
            System.err.println("## ERROR: System.getProperty('java.version') == null !?");
            return false;
        }
        String vparts[] = jver.split("[-.]");
        if (vparts.length < 2) {
            System.err.println("## ERROR: Invalid java version format '" + jver + "'");
            return false;
        }
        return toInt(vparts[0]) > maj || (toInt(vparts[0]) == maj && toInt(vparts[1]) >= min);
    }

    private static int toInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
