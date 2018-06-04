package com.firefly.db.jdbc.utils;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class TableMetaData {
    private String name;
    private List<ColumnMetaData> columnMetaDataList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ColumnMetaData> getColumnMetaDataList() {
        return columnMetaDataList;
    }

    public void setColumnMetaDataList(List<ColumnMetaData> columnMetaDataList) {
        this.columnMetaDataList = columnMetaDataList;
    }

    @Override
    public String toString() {
        return "TableMetaData{" +
                "name='" + name + '\'' +
                ", columnMetaDataList=" + columnMetaDataList +
                '}';
    }
}
