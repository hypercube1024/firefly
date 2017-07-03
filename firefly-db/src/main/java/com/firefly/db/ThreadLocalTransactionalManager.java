package com.firefly.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Pengtao Qiu
 */
public class ThreadLocalTransactionalManager implements TransactionalManager {

    private final static Logger log = LoggerFactory.getLogger("firefly-system");

    private final ThreadLocal<Transaction> transaction = new ThreadLocal<>();
    private final DataSource dataSource;

    public ThreadLocalTransactionalManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void beginTransaction() {
        getTransaction().beginTransaction();
    }

    @Override
    public Connection getConnection() {
        return getTransaction().getConnection();
    }

    @Override
    public void commit() {
        getTransaction().commit();
    }

    @Override
    public void rollback() {
        getTransaction().rollback();
    }

    @Override
    public void endTransaction() {
        getTransaction().endTransaction();
    }

    private Transaction getTransaction() {
        Transaction t = transaction.get();
        if (t == null) {
            t = new Transaction();
            transaction.set(t);
        }
        return t;
    }

    enum Status {
        INIT, START, COMMIT, ROLLBACK, END
    }

    class Transaction {
        private Connection connection;
        private Status status = Status.INIT;
        private int count = 0;

        synchronized void beginTransaction() {
            if (status == Status.INIT) {
                connection = getConnectionFromDataSource();
                setAutoCommit(connection, false);
                status = Status.START;
            }
            count++;
            log.debug("begin transaction {}", count);
        }

        synchronized Connection getConnection() {
            check();
            return connection;
        }

        synchronized void rollback() {
            check();
            status = Status.ROLLBACK;
        }

        synchronized void commit() {
            check();
            if (status != Status.ROLLBACK) {
                status = Status.COMMIT;
            }
        }

        private synchronized void check() {
            if (status == Status.INIT) {
                throw new IllegalStateException("The transaction has not started, " + status);
            }
            if (status == Status.END) {
                throw new IllegalStateException("The transaction has ended, " + status);
            }
        }

        synchronized void endTransaction() {
            count--;
            if (count == 0) {
                switch (status) {
                    case START:
                    case COMMIT:
                        commit(connection);
                        break;
                    case ROLLBACK:
                        rollback(connection);
                        break;
                    default:
                        break;
                }

                close(connection);
                transaction.set(null);
                status = Status.END;
            }
            log.debug("end transaction {}", count);
        }

        private Connection getConnectionFromDataSource() {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                log.error("get connection exception", e);
                throw new DBException(e);
            }
        }

        public void setAutoCommit(Connection connection, boolean autoCommit) {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                log.error("set auto commit exception", e);
            }
        }

        public void rollback(Connection connection) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                log.error("transaction rollback exception", e);
                throw new DBException(e);
            }
        }

        public void commit(Connection connection) {
            try {
                connection.commit();
            } catch (SQLException e) {
                log.error("commit exception", e);
            }
        }

        public void close(Connection connection) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("close connection exception", e);
            }
        }

    }
}
