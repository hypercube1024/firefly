package com.firefly.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static com.firefly.db.JDBCConnectionUtils.close;
import static com.firefly.db.JDBCConnectionUtils.setAutoCommit;
import static com.firefly.db.Transaction.Status.*;

/**
 * @author Pengtao Qiu
 */
public class Transaction {

    protected final static Logger log = LoggerFactory.getLogger("firefly-system");

    protected final DataSource dataSource;

    protected Connection connection;
    protected Status status = INIT;
    protected int count = 0;

    public Transaction(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public enum Status {
        INIT, START, COMMIT, ROLLBACK, END
    }

    public synchronized void beginTransaction() {
        if (status == INIT) {
            connection = getConnectionFromDataSource();
            setAutoCommit(connection, false);
            status = START;
        }
        count++;
        log.debug("begin transaction {}", count);
    }

    public synchronized Connection getConnection() {
        check();
        return connection;
    }

    public synchronized void rollback() {
        check();
        status = ROLLBACK;
    }

    public synchronized void commit() {
        check();
        if (status != ROLLBACK) {
            status = COMMIT;
        }
    }

    protected synchronized void check() {
        if (status == INIT) {
            throw new IllegalStateException("The transaction has not started, " + status);
        }
        if (status == END) {
            throw new IllegalStateException("The transaction has ended, " + status);
        }
    }

    public synchronized boolean endTransaction() {
        count--;
        log.debug("end transaction {}", count);
        if (count == 0) {
            switch (status) {
                case START:
                case COMMIT:
                    JDBCConnectionUtils.commit(connection);
                    break;
                case ROLLBACK:
                    JDBCConnectionUtils.rollback(connection);
                    break;
                default:
                    break;
            }
            setAutoCommit(connection, true);
            close(connection);
            status = Status.END;
            return true;
        } else {
            return false;
        }
    }

    protected Connection getConnectionFromDataSource() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.error("get connection exception", e);
            throw new DBException(e);
        }
    }
}
