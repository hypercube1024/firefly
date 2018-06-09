package com.firefly.db.jdbc.utils;

import com.firefly.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pengtao Qiu
 */
public class MetaDataUtils {

    protected final static Logger log = LoggerFactory.getLogger("firefly-system");

    private static final Map<String, String> defaultTypeMap = new HashMap<>();

    static {
        defaultTypeMap.put("bigint", "Long");
        defaultTypeMap.put("int", "Integer");
        defaultTypeMap.put("integer", "Integer");
        defaultTypeMap.put("float", "Double");
        defaultTypeMap.put("double", "Double");
        defaultTypeMap.put("decimal", "Double");
        defaultTypeMap.put("real", "Double");
        defaultTypeMap.put("datetime", "java.util.Date");
        defaultTypeMap.put("timestamp", "java.util.Date");
        defaultTypeMap.put("date", "java.util.Date");
        defaultTypeMap.put("time", "java.util.Date");
    }

    protected DataSource dataSource;
    protected String blankString = "    ";
    protected String lineSeparator = "\r\n";
    protected Map<String, String> typeMap = defaultTypeMap;

    public MetaDataUtils() {
    }

    public MetaDataUtils(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getBlankString() {
        return blankString;
    }

    public void setBlankString(String blankString) {
        this.blankString = blankString;
    }

    public Map<String, String> getTypeMap() {
        return typeMap;
    }

    public void setTypeMap(Map<String, String> typeMap) {
        this.typeMap = typeMap;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public List<TableMetaData> listTableMetaData(String catalog, String schemaPattern, String tableNamePattern) {
        List<TableMetaData> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData dbMetaData = conn.getMetaData();
            ResultSet resultSet = dbMetaData.getTables(catalog, schemaPattern, tableNamePattern, new String[]{"TABLE"});

            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                TableMetaData tableMetaData = new TableMetaData();
                tableMetaData.setCatalog(catalog);
                tableMetaData.setName(tableName.toLowerCase());
                tableMetaData.setColumnMetaDataList(new ArrayList<>());

                ResultSet pkResultSet = dbMetaData.getPrimaryKeys(catalog, null, tableName);
                while (pkResultSet.next()) {
                    tableMetaData.setPkColumnName(pkResultSet.getString("COLUMN_NAME"));
                }

                ResultSet colResultSet = dbMetaData.getColumns(catalog, schemaPattern, tableName, "%");
                while (colResultSet.next()) {
                    String colName = colResultSet.getString("COLUMN_NAME");
                    String colType = colResultSet.getString("TYPE_NAME");
                    ColumnMetaData columnMetaData = new ColumnMetaData();
                    columnMetaData.setName(colName.toLowerCase());
                    columnMetaData.setType(colType.toLowerCase());
                    tableMetaData.getColumnMetaDataList().add(columnMetaData);
                }
                list.add(tableMetaData);
            }
        } catch (SQLException e) {
            log.error("generate POJOs exception", e);
        }
        return list;
    }

    public List<SourceCode> toPojo(List<TableMetaData> list, String tablePrefix, String packageName) {
        return list.parallelStream().filter(m -> m.getName().startsWith(tablePrefix)).map(m -> {
            SourceCode code = new SourceCode();
            code.setName(tableToPojoType(tablePrefix, m.getName()));

            StringBuilder codes = new StringBuilder();
            codes.append("package ").append(packageName).append(";").append(lineSeparator)
                 .append(lineSeparator)
                 .append("import lombok.Data;").append(lineSeparator)
                 .append(lineSeparator)
                 .append("import java.io.Serializable;").append(lineSeparator)
                 .append(lineSeparator)
                 .append("@Data").append(lineSeparator)
                 .append("public class ").append(code.getName()).append(" implements Serializable {").append(lineSeparator)
                 .append(lineSeparator)
                 .append(blankString).append("private static final long serialVersionUID = 1L;").append(lineSeparator)
                 .append(lineSeparator);

            m.getColumnMetaDataList()
             .forEach(c -> codes.append(blankString).append("private ")
                                .append(columnToPojoType(c.getType())).append(" ")
                                .append(columnToPropertyName(c.getName())).append(";")
                                .append(lineSeparator));
            codes.append("}");
            code.setCodes(codes.toString());
            return code;
        }).collect(Collectors.toList());
    }

    public List<SourceCode> toKotlinDataClass(List<TableMetaData> list, String tablePrefix, String packageName) {
        return list.parallelStream().filter(m -> m.getName().startsWith(tablePrefix)).map(m -> {
            SourceCode code = new SourceCode();
            code.setName(tableToPojoType(tablePrefix, m.getName()));

            StringBuilder codes = new StringBuilder();
            codes.append("package ").append(packageName).append(lineSeparator)
                 .append(lineSeparator)
                 .append("import com.firefly.db.annotation.*").append(lineSeparator)
                 .append("import java.io.Serializable").append(lineSeparator)
                 .append("import java.io.Serializable").append(lineSeparator)
                 .append(lineSeparator)
                 .append("@Table(value = ").append('"').append(m.getName()).append('"')
                 .append(", catalog = ").append('"').append(m.getCatalog()).append('"').append(")").append(lineSeparator)
                 .append("data class ").append(code.getName()).append('(').append(lineSeparator);

            m.getColumnMetaDataList().forEach(c -> {
                codes.append(blankString);
                if (c.getName().equals(m.getPkColumnName())) {
                    codes.append("@Id(\"").append(c.getName()).append("\")");
                } else {

                }
            });
            return code;
        }).collect(Collectors.toList());
    }

    public String tableToPojoType(String tablePrefix, String tableName) {
        String[] tableNameArr = StringUtils.hasText(tablePrefix)
                ? StringUtils.split(tableName.substring(tablePrefix.length()), '_')
                : StringUtils.split(tableName, '_');
        return Arrays.stream(tableNameArr)
                     .map(s -> Character.toUpperCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : ""))
                     .collect(Collectors.joining());
    }

    public String columnToPojoType(String columnType) {
        String r = typeMap.get(columnType);
        if (StringUtils.hasText(r)) {
            return r;
        } else {
            return "String";
        }
    }

    public String columnToPropertyName(String columnName) {
        String[] colNameArr = StringUtils.split(columnName, '_');
        String p = Arrays.stream(colNameArr)
                         .map(s -> Character.toUpperCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : ""))
                         .collect(Collectors.joining());
        return Character.toLowerCase(p.charAt(0)) + (p.length() > 1 ? p.substring(1) : "");
    }

}
