package com.firefly.db;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * @author Pengtao Qiu
 */
public interface Row {

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

    BigDecimal getBigDecimal(String columnLabel);

    BigInteger getBigInteger(String columnLabel);

    String getString(int columnIndex);

    boolean getBoolean(int columnIndex);

    byte getByte(int columnIndex);

    short getShort(int columnIndex);

    int getInt(int columnIndex);

    long getLong(int columnIndex);

    float getFloat(int columnIndex);

    double getDouble(int columnIndex);

    byte[] getBytes(int columnIndex);

    Date getDate(int columnIndex);

    Object getObject(int columnIndex);

    BigDecimal getBigDecimal(int columnIndex);

    BigInteger getBigInteger(int columnIndex);
}
