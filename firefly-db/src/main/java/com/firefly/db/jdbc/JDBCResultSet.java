package com.firefly.db.jdbc;

import com.firefly.db.DBException;
import com.firefly.db.SQLResultSet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author Pengtao Qiu
 */
public class JDBCResultSet implements SQLResultSet {

    private final ResultSet resultSet;

    public JDBCResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    @Override
    public boolean next() {
        try {
            return resultSet.next();
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public boolean wasNull() {
        try {
            return resultSet.wasNull();
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public String getString(String columnLabel) {
        try {
            return resultSet.getString(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public boolean getBoolean(String columnLabel) {
        try {
            return resultSet.getBoolean(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public byte getByte(String columnLabel) {
        try {
            return resultSet.getByte(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public short getShort(String columnLabel) {
        try {
            return resultSet.getShort(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public int getInt(String columnLabel) {
        try {
            return resultSet.getInt(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public long getLong(String columnLabel) {
        try {
            return resultSet.getLong(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public float getFloat(String columnLabel) {
        try {
            return resultSet.getFloat(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public double getDouble(String columnLabel) {
        try {
            return resultSet.getDouble(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public byte[] getBytes(String columnLabel) {
        try {
            return resultSet.getBytes(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public Date getDate(String columnLabel) {
        try {
            return resultSet.getDate(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public Object getObject(String columnLabel) {
        try {
            return resultSet.getObject(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) {
        try {
            return resultSet.getBigDecimal(columnLabel);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public BigInteger getBigInteger(String columnLabel) {
        BigDecimal bigDecimal = getBigDecimal(columnLabel);
        return bigDecimal != null ? bigDecimal.toBigInteger() : null;
    }

    @Override
    public String getString(int columnIndex) {
        try {
            return resultSet.getString(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public boolean getBoolean(int columnIndex) {
        try {
            return resultSet.getBoolean(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public byte getByte(int columnIndex) {
        try {
            return resultSet.getByte(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public short getShort(int columnIndex) {
        try {
            return resultSet.getShort(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public int getInt(int columnIndex) {
        try {
            return resultSet.getInt(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public long getLong(int columnIndex) {
        try {
            return resultSet.getLong(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public float getFloat(int columnIndex) {
        try {
            return resultSet.getFloat(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public double getDouble(int columnIndex) {
        try {
            return resultSet.getDouble(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public byte[] getBytes(int columnIndex) {
        try {
            return resultSet.getBytes(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public Date getDate(int columnIndex) {
        try {
            return resultSet.getDate(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public Object getObject(int columnIndex) {
        try {
            return resultSet.getObject(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) {
        try {
            return resultSet.getBigDecimal(columnIndex);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    @Override
    public BigInteger getBigInteger(int columnIndex) {
        BigDecimal bigDecimal = getBigDecimal(columnIndex);
        return bigDecimal != null ? bigDecimal.toBigInteger() : null;
    }

    @Override
    public void close() {
        try {
            resultSet.close();
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }
}
