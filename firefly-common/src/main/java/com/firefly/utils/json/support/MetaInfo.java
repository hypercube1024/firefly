package com.firefly.utils.json.support;

import com.firefly.utils.lang.bean.PropertyAccess;

public class MetaInfo implements Comparable<MetaInfo> {
    protected PropertyAccess propertyAccess;
    protected char[] propertyName;
    protected String propertyNameString;

    public PropertyAccess getPropertyAccess() {
        return propertyAccess;
    }

    public void setPropertyAccess(PropertyAccess propertyAccess) {
        this.propertyAccess = propertyAccess;
    }

    public char[] getPropertyName() {
        return propertyName;
    }

    public String getPropertyNameString() {
        return propertyNameString;
    }

    @Override
    public int compareTo(MetaInfo o) {
        return propertyNameString.compareTo(o.getPropertyNameString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((propertyNameString == null) ? 0 : propertyNameString
                .hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetaInfo other = (MetaInfo) obj;
        if (propertyNameString == null) {
            if (other.propertyNameString != null)
                return false;
        } else if (!propertyNameString.equals(other.propertyNameString))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return propertyNameString;
    }

}
