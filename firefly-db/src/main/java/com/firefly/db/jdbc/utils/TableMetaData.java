package com.firefly.db.jdbc.utils;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class TableMetaData {
    private String catalog;
    private String name;
    private String pkColumnName;
    private List<ColumnMetaData> columnMetaDataList;

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPkColumnName() {
        return pkColumnName;
    }

    public void setPkColumnName(String pkColumnName) {
        this.pkColumnName = pkColumnName;
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
                "catalog='" + catalog + '\'' +
                ", name='" + name + '\'' +
                ", pkColumnName='" + pkColumnName + '\'' +
                ", columnMetaDataList=" + columnMetaDataList +
                '}';
    }
}
