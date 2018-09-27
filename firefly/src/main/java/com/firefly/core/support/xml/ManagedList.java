package com.firefly.core.support.xml;

import java.util.ArrayList;

public class ManagedList<T> extends ArrayList<T> implements XmlManagedNode {
    private static final long serialVersionUID = -1889497225597681323L;
    private String typeName;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
