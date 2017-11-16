package com.firefly.codec.http2.model;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class AcceptMIMEType {
    private String parentType;
    private String childType;
    private float quality = 1.0f;
    private AcceptMIMEMatchType matchType;

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    public String getChildType() {
        return childType;
    }

    public void setChildType(String childType) {
        this.childType = childType;
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    public AcceptMIMEMatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(AcceptMIMEMatchType matchType) {
        this.matchType = matchType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcceptMIMEType that = (AcceptMIMEType) o;
        return Objects.equals(parentType, that.parentType) &&
                Objects.equals(childType, that.childType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentType, childType);
    }
}
