package com.firefly.db.jdbc.helper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.RowProcessor;
import org.apache.commons.dbutils.handlers.AbstractKeyedHandler;

public class DefaultBeanMapHandler<K, V> extends AbstractKeyedHandler<K, V> {
    /**
     * The Class of beans produced by this handler.
     */
    private final Class<V> type;

    /**
     * The RowProcessor implementation to use when converting rows into Objects.
     */
    private final RowProcessor convert;

    /**
     * The column index to retrieve key values from. Defaults to 1.
     */
    private final int columnIndex;

    /**
     * The column name to retrieve key values from. Either columnName or
     * columnIndex will be used but never both.
     */
    private final String columnName;

    /**
     * Creates a new instance of BeanMapHandler. The value of the first column
     * of each row will be a key in the Map.
     *
     * @param type       The Class that objects returned from <code>createRow()</code>
     *                   are created from.
     * @param convert    The <code>RowProcessor</code> implementation to use when
     *                   converting rows into Beans
     * @param columnName The values to use as keys in the Map are retrieved from the
     *                   column with this name.
     */
    public DefaultBeanMapHandler(Class<V> type, RowProcessor convert, String columnName) {
        this(type, convert, 1, columnName);
    }

    /**
     * Private Helper
     *
     * @param convert     The <code>RowProcessor</code> implementation to use when
     *                    converting rows into Beans
     * @param columnIndex The values to use as keys in the Map are retrieved from the
     *                    column at this index.
     * @param columnName  The values to use as keys in the Map are retrieved from the
     *                    column with this name.
     */
    public DefaultBeanMapHandler(Class<V> type, RowProcessor convert,
                                 int columnIndex, String columnName) {
        this.type = type;
        this.convert = convert;
        this.columnIndex = columnIndex;
        this.columnName = columnName;
    }

    /**
     * This factory method is called by <code>handle()</code> to retrieve the
     * key value from the current <code>ResultSet</code> row.
     *
     * @param rs ResultSet to create a key from
     * @return K from the configured key column name/index
     * @throws SQLException       if a database access error occurs
     * @throws ClassCastException if the class datatype does not match the column type
     * @see org.apache.commons.dbutils.handlers.AbstractKeyedHandler#createKey(ResultSet)
     */
    // We assume that the user has picked the correct type to match the column
    // so getObject will return the appropriate type and the cast will succeed.
    @SuppressWarnings("unchecked")
    @Override
    protected K createKey(ResultSet rs) throws SQLException {
        return (columnName == null) ?
                (K) rs.getObject(columnIndex) :
                (K) rs.getObject(columnName);
    }

    @Override
    protected V createRow(ResultSet rs) throws SQLException {
        return this.convert.toBean(rs, type);
    }
}
