package com.firefly.db.jdbc.helper.namedparam;

/**
 * @author Pengtao Qiu
 */
public class ParameterHolder {

    private final String parameterName;
    private final int startIndex;
    private final int endIndex;

    public ParameterHolder(String parameterName, int startIndex, int endIndex) {
        this.parameterName = parameterName;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public String getParameterName() {
        return this.parameterName;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }
}
