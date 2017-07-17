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
    protected long id;

    public Transaction(DataSource dataSource, long id) {
        this.dataSource = dataSource;
        this.id = id;
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
        log.debug("begin transaction, id: {}, count: {}", id, count);
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

    public long getId() {
        return id;
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
        log.debug("end transaction, id: {}, count: {}", id, count);
        if (count == 0) {
            switch (status) {
                case START:
                case COMMIT:
                    log.debug("commit transaction, id: {}", id);
                    JDBCConnectionUtils.commit(connection);
                    break;
                case ROLLBACK:
                    log.debug("rollback transaction, id: {}", id);
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
