package com.firefly.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Pengtao Qiu
 */
public class JDBCConnectionUtils {

    private final static Logger log = LoggerFactory.getLogger("firefly-system");

    public static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            log.error("transaction rollback exception", e);
            throw new DBException(e);
        }
    }

    public static void setAutoCommit(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            log.error("set auto commit exception", e);
        }
    }

    public static void commit(Connection connection) {
        try {
            connection.commit();
        } catch (SQLException e) {
            log.error("commit exception", e);
        }
    }

    public static void close(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            log.error("close connection exception", e);
        }
    }
}
