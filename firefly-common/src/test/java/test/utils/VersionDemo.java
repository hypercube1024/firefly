package test.utils;

import com.firefly.utils.ProjectVersion;

/**
 * @author Pengtao Qiu
 */
public class VersionDemo {
    public static void main(String[] args) {
        System.out.println(ProjectVersion.getValue());
        System.out.println(ProjectVersion.getAsciiArt());
    }
}
