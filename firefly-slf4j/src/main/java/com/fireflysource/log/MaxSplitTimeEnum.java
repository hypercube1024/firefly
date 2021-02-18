package com.fireflysource.log;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public enum MaxSplitTimeEnum {
    MINUTE("minute"), HOUR("hour"), DAY("day");

    private final String value;

    MaxSplitTimeEnum(String value) {
        this.value = value;
    }

    public static Optional<MaxSplitTimeEnum> from(String value) {
        return Arrays.stream(MaxSplitTimeEnum.values()).filter(e -> e.value.equals(value)).findFirst();
    }

    public String getValue() {
        return value;
    }
}
