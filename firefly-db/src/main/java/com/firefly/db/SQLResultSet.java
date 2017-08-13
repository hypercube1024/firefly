package com.firefly.db;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Pengtao Qiu
 */
public interface SQLResultSet {

    boolean next();

    boolean wasNull();

    String getString(String columnLabel);

    boolean getBoolean(String columnLabel);

    byte getByte(String columnLabel);

    short getShort(String columnLabel);

    int getInt(String columnLabel);

    long getLong(String columnLabel);

    float getFloat(String columnLabel);

    double getDouble(String columnLabel);

    byte[] getBytes(String columnLabel);

    Date getDate(String columnLabel);

    Object getObject(String columnLabel);

    int findColumn(String columnLabel);

    BigDecimal getBigDecimal(String columnLabel);
    
}
