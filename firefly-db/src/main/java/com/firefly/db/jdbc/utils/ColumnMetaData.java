package com.firefly.db.jdbc.utils;

/**
 * @author Pengtao Qiu
 */
public class ColumnMetaData {
    private String name;
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ColumnMetaData{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
